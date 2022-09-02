package spring.application.tree.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import spring.application.tree.data.chats.models.AbstractChatModel;
import spring.application.tree.data.chats.service.ChatService;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.exceptions.NotAllowedException;
import spring.application.tree.data.messages.models.AbstractMessageModel;
import spring.application.tree.data.messages.service.MessageService;
import spring.application.tree.data.users.models.AbstractUserModel;
import spring.application.tree.data.users.service.UserService;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Slf4j
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ChatService chatService;
    private final MessageService messageService;

    @PreAuthorize("!hasAnyAuthority('permission:user:create')")
    @PostMapping("/account/create")
    public ResponseEntity<Object> createUser(@RequestBody AbstractUserModel abstractUserModel) throws ApplicationException {
        userService.saveUser(abstractUserModel);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PreAuthorize("hasAuthority('permission:user:delete')")
    @DeleteMapping("/account/delete")
    public ResponseEntity<Object> deleteUser(HttpServletRequest httpRequest) throws ApplicationException {
        userService.deleteUser(httpRequest);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('permission:user:read')")
    @GetMapping("/members")
    public ResponseEntity<Object> getChatMembers(@RequestParam("chat_id") int chatId) throws InvalidAttributesException, NotAllowedException {
        List<AbstractUserModel> members = userService.getChatMembers(chatId);
        return ResponseEntity.ok(members);
    }

    @PreAuthorize("hasAuthority('permission:user:read')")
    @GetMapping("/chats")
    public ResponseEntity<Object> getChats() throws InvalidAttributesException, NotAllowedException {
        List<AbstractChatModel> chats = chatService.getChatsForCurrentUser();
        return ResponseEntity.ok(chats);
    }

    @PreAuthorize("hasAuthority('permission:user:create')")
    @PostMapping("/chat/create")
    public ResponseEntity<Object> createChat(@RequestBody AbstractChatModel abstractChatModel) throws InvalidAttributesException {
        int chatId = chatService.addChat(abstractChatModel);
        Map<String, Object> response = new HashMap<>();
        response.put("chat_id", chatId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('permission:user:update')")
    @PutMapping("/chat/update")
    public ResponseEntity<Object> updateChat(@RequestBody AbstractChatModel abstractChatModel) throws InvalidAttributesException, NotAllowedException {
        chatService.updateChat(abstractChatModel);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('permission:user:delete')")
    @DeleteMapping("/chat/delete")
    public ResponseEntity<Object> deleteChat(@RequestParam("chat_id")   int chatId,
                                             @RequestParam("author_id") int authorId) throws InvalidAttributesException, NotAllowedException {
        chatService.deleteChat(chatId, authorId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('permission:user:read')")
    @GetMapping("/messages")
    public ResponseEntity<Object> getMessages(@RequestParam("chat_id") int chatId) throws InvalidAttributesException {
        List<AbstractMessageModel> messages = messageService.getMessages(chatId);
        return ResponseEntity.ok(messages);
    }

    @PreAuthorize("hasAuthority('permission:user:create')")
    @PostMapping("/message/create")
    @MessageMapping("/message/create")
    @SendTo("/topic/chat")
    public ResponseEntity<Object> createMessage(@RequestBody AbstractMessageModel abstractMessageModel) throws InvalidAttributesException, NotAllowedException {
        int messageId = messageService.addMessage(abstractMessageModel);
        abstractMessageModel.setId(messageId);
        return ResponseEntity.ok(abstractMessageModel);
    }

    @PreAuthorize("hasAuthority('permission:user:update')")
    @PutMapping("/message/update")
    @MessageMapping("/message/update")
    @SendTo("/topic/chat")
    public ResponseEntity<Object> updateMessage(@RequestBody AbstractMessageModel abstractMessageModel) throws InvalidAttributesException, NotAllowedException {
        messageService.updateMessage(abstractMessageModel);
        return ResponseEntity.ok(abstractMessageModel);
    }

    @PreAuthorize("hasAuthority('permission:user:delete')")
    @PutMapping("/message/delete")
    @MessageMapping("/message/delete")
    @SendTo("/topic/chat")
    public ResponseEntity<Object> deleteMessage(@RequestBody AbstractMessageModel abstractMessageModel) throws InvalidAttributesException, NotAllowedException {
        messageService.deleteMessage(abstractMessageModel);
        Map<String, Object> response = new HashMap<>();
        response.put("message_id", abstractMessageModel.getId());
        response.put("chat_id", abstractMessageModel.getChatId());
        response.put("deleted", true);
        return ResponseEntity.ok(response);
    }
}
