package spring.application.tree.data.messages.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.application.tree.data.chats.attributes.ChatType;
import spring.application.tree.data.chats.service.ChatService;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.exceptions.NotAllowedException;
import spring.application.tree.data.messages.attributes.MessageType;
import spring.application.tree.data.messages.models.AbstractMessageModel;
import spring.application.tree.data.messages.repository.MessageDataAccessObject;
import spring.application.tree.data.scheduling.service.ScheduleService;
import spring.application.tree.data.users.service.UserService;
import spring.application.tree.data.utility.models.PairValue;
import spring.application.tree.web.webscoket.models.Endpoints;
import spring.application.tree.web.webscoket.models.WebSocketEvent;
import spring.application.tree.web.webscoket.service.WebSocketService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageService {
    private final MessageDataAccessObject messageDataAccessObject;
    private final ChatService chatService;
    private final ScheduleService scheduleService;
    private final WebSocketService webSocketService;

    /**
     * key - user ID, value - message ID and sending task
     */
    private static final Map<Integer, List<PairValue<Integer, ScheduledFuture<?>>>> scheduleMessageTasks = new HashMap<>();

    public List<AbstractMessageModel> getMessages(int chatId) throws InvalidAttributesException, NotAllowedException {
        Integer currentUserId = UserService.getIdOfCurrentlyAuthenticatedUser();
        if (currentUserId == null || !chatService.checkUserPresenceInChat(currentUserId, chatId)) {
            throw new NotAllowedException(String.format("User with ID: %s is not participating chat with ID: %s, message reading is forbidden", currentUserId, chatId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        return messageDataAccessObject.getMessages(chatId);
    }

    public int addMessage(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException, NotAllowedException {
        checkSendingMessageAvailability(abstractMessageModel);
        return messageDataAccessObject.addMessage(abstractMessageModel);
    }

    public void updateMessage(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException, NotAllowedException {
        int authorId = abstractMessageModel.getAuthorId();
        Integer currentUserId = UserService.getIdOfCurrentlyAuthenticatedUser();
        if (currentUserId == null || currentUserId != authorId) {
            throw new NotAllowedException(String.format("User with ID: %s is not author of this message, editing is forbidden", authorId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        messageDataAccessObject.updateMessage(abstractMessageModel.getId(), abstractMessageModel.getMessage());
    }

    public void updateMessageType(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException {
        messageDataAccessObject.updateMessageType(abstractMessageModel.getId(), abstractMessageModel.getMessageType());
    }

    public void deleteMessage(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException, NotAllowedException {
        int authorId = abstractMessageModel.getAuthorId();
        Integer currentUserId = UserService.getIdOfCurrentlyAuthenticatedUser();
        if (currentUserId == null || currentUserId != authorId) {
            throw new NotAllowedException(String.format("User with ID: %s is not author of this message, deletion is forbidden", authorId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        messageDataAccessObject.deleteMessage(abstractMessageModel.getId());
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void scheduleMessage(AbstractMessageModel abstractMessageModel, Date fireDate, String timezone) throws InvalidAttributesException, NotAllowedException {
        abstractMessageModel.setMessageType(MessageType.SCHEDULED);
        Date now = new Date();
        Date fireDateAtServerTimezone = new Date(fireDate.getTime() - TimeZone.getTimeZone(timezone).getRawOffset() + TimeZone.getDefault().getRawOffset());
        if (now.after(fireDateAtServerTimezone)) {
            throw new InvalidAttributesException(String.format("Invalid fire date, now is: %s, fire date: %s", now, fireDate),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        int messageId = addMessage(abstractMessageModel);
        abstractMessageModel.setId(messageId);
        Runnable task = () -> {
            try {
                sendMessage(abstractMessageModel, WebSocketEvent.SENDING_MESSAGE);
                List<PairValue<Integer, ScheduledFuture<?>>> scheduledMessages = scheduleMessageTasks.get(abstractMessageModel.getAuthorId());
                scheduledMessages.removeIf(scheduledMessage -> scheduledMessage.getKey() == messageId);
            } catch (JsonProcessingException | NotAllowedException | InvalidAttributesException e) {
                log.error(e.getMessage(), e);
            }
        };
        int delay = Math.toIntExact(fireDateAtServerTimezone.getTime() - now.getTime());
        ScheduledFuture<?> scheduledTask = scheduleService.scheduleOnceFireTask(task, delay, TimeUnit.MILLISECONDS);
        if (scheduleMessageTasks.containsKey(abstractMessageModel.getAuthorId())) {
            List<PairValue<Integer, ScheduledFuture<?>>> scheduledMessages = new ArrayList<>(scheduleMessageTasks.get(abstractMessageModel.getAuthorId()));
            scheduledMessages.add(new PairValue<>(messageId, scheduledTask));
            scheduleMessageTasks.put(abstractMessageModel.getAuthorId(), scheduledMessages);
        }
        scheduleMessageTasks.putIfAbsent(abstractMessageModel.getAuthorId(), List.of(new PairValue<>(messageId, scheduledTask)));
    }

    public void cancelScheduledMessage(Integer messageId) throws InvalidAttributesException {
        Integer authorId = UserService.getIdOfCurrentlyAuthenticatedUser();
        List<PairValue<Integer, ScheduledFuture<?>>> scheduledMessages = scheduleMessageTasks.get(authorId);
        scheduleMessageTasks.get(authorId).stream().filter(pair -> pair.getKey().equals(messageId)).findAny().ifPresent(scheduledTask -> scheduledTask.getValue().cancel(true));
        scheduledMessages.removeIf(scheduledMessage -> Objects.equals(scheduledMessage.getKey(), messageId));
        messageDataAccessObject.deleteMessage(messageId);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void sendMessage(AbstractMessageModel abstractMessageModel, WebSocketEvent event) throws JsonProcessingException, NotAllowedException, InvalidAttributesException {
        abstractMessageModel.setMessageType(MessageType.SENT);
        validateMessageModel(abstractMessageModel);
        checkSendingMessageAvailability(abstractMessageModel);
        String destination = String.format("%s/%s", Endpoints.CHAT.getEndpointPrefix(), abstractMessageModel.getChatId());
        updateMessageType(abstractMessageModel);
        webSocketService.sendMessage(abstractMessageModel, destination, event);
    }

    private void validateMessageModel(AbstractMessageModel abstractMessageModel) throws InvalidAttributesException {
        StringBuilder exceptionText = new StringBuilder();
        if (abstractMessageModel.getMessage() == null || abstractMessageModel.getMessage().isEmpty()) {
            exceptionText.append(String.format("Message is invalid: %s ", abstractMessageModel.getMessage()));
        }
        if (abstractMessageModel.getAuthorId() <= 0) {
            exceptionText.append(String.format("Author ID is invalid: %s ", abstractMessageModel.getAuthorId()));
        }
        if (abstractMessageModel.getChatId() <= 0) {
            exceptionText.append(String.format("Chat ID is invalid: %s", abstractMessageModel.getChatId()));
        }
        if (abstractMessageModel.getMessageType() == null) {
            exceptionText.append(String.format("Message type is invalid: %s", abstractMessageModel.getMessageType()));
        }
        if (!exceptionText.toString().isEmpty()) {
            throw new InvalidAttributesException(exceptionText.toString(),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
    }

    private void checkSendingMessageAvailability(AbstractMessageModel abstractMessageModel) throws NotAllowedException, InvalidAttributesException {
        int authorId = abstractMessageModel.getAuthorId();
        int chatId = abstractMessageModel.getChatId();
        ChatType chatType = chatService.getChatType(chatId);
        Integer userId = UserService.getIdOfCurrentlyAuthenticatedUser();
        if (!chatService.checkUserPresenceInChat(authorId, chatId)) {
            throw new NotAllowedException(String.format("User with ID: %s is not participating chat with ID: %s, message sending is forbidden", authorId, chatId),
                                          Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                          LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        switch (chatType) {
            case CHANNEL: {
                if (userId == null || userId != authorId) {
                    throw new NotAllowedException(String.format("User with ID: %s does not owes chat with ID: %s and type: %s, message sending is forbidden", authorId, chatId, chatType),
                                                  Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                  LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
                }
            }
        }
    }
}