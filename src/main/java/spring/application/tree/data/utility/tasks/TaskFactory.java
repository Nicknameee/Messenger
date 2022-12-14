package spring.application.tree.data.utility.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.scheduling.service.ScheduleService;
import spring.application.tree.data.users.service.UserService;
import spring.application.tree.data.utility.mailing.models.ActionType;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class TaskFactory {
    private final UserService userService;

    private Runnable getRollbackTaskForSignUpConfirmation(String email) {
        Runnable task = () -> {
            try {
                userService.deleteActivationExpiredAccountByLogin(email);
                ActionHistoryStorage.removeConfirmationCode(email);
                ActionHistoryStorage.removeConfirmationTask(email);
                TaskUtility.removeSuccessConfirmationTask(email);
            } catch (InvalidAttributesException e) {
                log.error(e.getMessage(), e);
                log.error("Unable to remove confirmation data for user expired task: '{}'", email);
            }
        };
        return task;
    }

    private void callRollbackTaskForSignUpConfirmation(String email, ActionType actionType, ScheduleService scheduleService, Map<String, String> properties) {
        Runnable expireConfirmation = getRollbackTaskForSignUpConfirmation(email);
        try {
            ScheduledFuture<?> task = scheduleService.scheduleOnceFireTask(expireConfirmation, Integer.parseInt(properties.get("duration")), TimeUnit.SECONDS);
            ActionHistoryStorage.putPostponedTask(email, task, actionType);
        } catch (InvalidAttributesException e) {
            throw new RuntimeException(e);
        }
    }

    private Runnable getRollbackTaskForPasswordRestoringConfirmation(String email) {
        Runnable task = () -> {
            try {
                ActionHistoryStorage.removeConfirmationCode(email);
                ActionHistoryStorage.removeConfirmationTask(email);
                TaskUtility.removeSuccessConfirmationTask(email);
            } catch (InvalidAttributesException e) {
                log.error(e.getMessage(), e);
                log.error("Unable to remove confirmation data for user expired task: '{}'", email);
            }
        };
        return task;
    }

    private void callRollbackTaskForPasswordRestoringConfirmation(String email, ActionType actionType, ScheduleService scheduleService, Map<String, String> properties) {
        Runnable expireConfirmation = getRollbackTaskForPasswordRestoringConfirmation(email);
        try {
            ScheduledFuture<?> task = scheduleService.scheduleOnceFireTask(expireConfirmation, Integer.parseInt(properties.get("duration")), TimeUnit.SECONDS);
            ActionHistoryStorage.putPostponedTask(email, task, actionType);
        } catch (InvalidAttributesException e) {
            throw new RuntimeException(e);
        }
    }

    public void callRollbackTask(String recipient, ActionType actionType, ScheduleService scheduleService, Map<String, String> properties) {
        log.debug("Calling rollback task for user '{}' on action '{}'", recipient, actionType);
        switch (actionType) {
            case SIGN_UP:
                callRollbackTaskForSignUpConfirmation(recipient, actionType, scheduleService, properties);
                break;
            case RESTORE_PASSWORD:
                callRollbackTaskForPasswordRestoringConfirmation(recipient, actionType, scheduleService, properties);
                break;
        }
    }
}
