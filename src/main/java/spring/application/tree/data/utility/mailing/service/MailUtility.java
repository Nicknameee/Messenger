package spring.application.tree.data.utility.mailing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.scheduling.service.ScheduleService;
import spring.application.tree.data.utility.mailing.models.AbstractMailMessageModel;
import spring.application.tree.data.utility.mailing.models.ActionType;
import spring.application.tree.data.utility.models.PairValue;
import spring.application.tree.data.utility.tasks.ActionHistoryStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailUtility {
    private final ScheduleService scheduleService;
    private final MailService mailService;
    /**
     * Key - user email, value - action type and cancel task
     */
    public static final Map<String, PairValue<ActionType, ScheduledFuture<?>>> userToCancellationTask = new HashMap<>();

    public void sendConfirmationEmailMessage(AbstractMailMessageModel abstractMailMessageModel) throws ApplicationException {
        Runnable task = mailService.sendMessage(abstractMailMessageModel);
        ScheduledFuture<?> scheduledTask = scheduleService.scheduleOnceFireTask(task, 0, TimeUnit.SECONDS);
        ActionHistoryStorage.putConfirmationTask(abstractMailMessageModel.getRecipient(), scheduledTask);
        synchronized (userToCancellationTask) {
            if (userToCancellationTask.containsKey(abstractMailMessageModel.getRecipient()) &&
                userToCancellationTask.get(abstractMailMessageModel.getRecipient()).getKey() == abstractMailMessageModel.getActionType()) {
                userToCancellationTask.get(abstractMailMessageModel.getRecipient()).getValue().cancel(true);
                userToCancellationTask.remove(abstractMailMessageModel.getRecipient());
            }
        }
        Runnable invalidateConfirmationTask = () -> {
            try {
                ActionHistoryStorage.removeConfirmationCode(abstractMailMessageModel.getRecipient());
                ActionHistoryStorage.removeConfirmationTask(abstractMailMessageModel.getRecipient());
            } catch (InvalidAttributesException e) {
                log.error(String.format("Error occurs while invalidation of confirmation task, email: %s", abstractMailMessageModel.getRecipient()));
            }
        };
        ScheduledFuture<?> cancelTask = scheduleService.scheduleOnceFireTask(invalidateConfirmationTask, Integer.parseInt(MailService.properties.get("duration")), TimeUnit.SECONDS);
        userToCancellationTask.put(abstractMailMessageModel.getRecipient(), new PairValue<>(abstractMailMessageModel.getActionType(), cancelTask));
    }
}
