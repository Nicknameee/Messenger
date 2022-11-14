package spring.application.tree.data.utility.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import spring.application.tree.data.exceptions.ConfirmationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.utility.mailing.models.ActionType;
import spring.application.tree.data.utility.models.PairValue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Slf4j
public class ActionHistoryStorage {
    /**
     * Key - user email, value - task of confirmation email sending, expiring time
     */
    private static final Map<String, PairValue<ScheduledFuture<?>, LocalDateTime>> userToConfirmationTask = new HashMap<>();
    /**
     * Key - user email, value - confirmation code and action type
     */
    private static final Map<String, PairValue<String, ActionType>> userToConfirmationCode = new HashMap<>();
    /**
     * Key - user email, value - postponed task and action type
     */
    private static final Map<String, PairValue<ScheduledFuture<?>, ActionType>> userToPostponedTask = new HashMap<>();

    public static void putPostponedTask(String email, ScheduledFuture<?> task, ActionType actionType) throws InvalidAttributesException {
        if (email == null || email.isEmpty() || task == null) {
            throw new InvalidAttributesException(String.format("Email: %s or confirmation task: %s is invalid", email, task),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        synchronized (userToPostponedTask) {
            if (userToPostponedTask.containsKey(email)) {
                userToPostponedTask.get(email).getKey().cancel(true);
            }
            userToPostponedTask.remove(email);
            userToPostponedTask.put(email, new PairValue<>(task, actionType));
            log.debug("Adding postponed task for user '{}' and action '{}'", email, actionType);
        }
    }

    public static void putConfirmationTask(String email, ScheduledFuture<?> task, int expiringDelay, ChronoUnit unit) throws InvalidAttributesException {
        if (email == null || email.isEmpty() || task == null) {
            throw new InvalidAttributesException(String.format("Email: %s or confirmation task: %s is invalid", email, task),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        synchronized (userToConfirmationTask) {
            userToConfirmationTask.remove(email);
            userToConfirmationTask.put(email, new PairValue<>(task, LocalDateTime.now().plus(expiringDelay, unit)));
            log.debug("Adding confirmation task for user '{}'", email);
        }
    }

    public static void putConfirmationCode(String email, String code, ActionType actionType) throws InvalidAttributesException {
        if (email == null || email.isEmpty() || code == null || code.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email: %s or confirmation code: %s is invalid", email, code),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        synchronized (userToConfirmationCode) {
            userToConfirmationCode.remove(email);
            userToConfirmationCode.put(email, new PairValue<>(code, actionType));
            log.debug("Adding confirmation code '{}' for user '{}' and action '{}'", code, email, actionType);
        }
    }

    public static void removeConfirmationTask(String email) throws InvalidAttributesException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userToConfirmationTask.remove(email);
        log.debug("Removing confirmation task for user '{}'", email);
    }

    public static void removeConfirmationCode(String email) throws InvalidAttributesException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userToConfirmationCode.remove(email);
        log.debug("Removing confirmation code for user '{}'", email);
    }

    public static ScheduledFuture<?> getConfirmationTask(String email) throws InvalidAttributesException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        if (userToConfirmationTask.containsKey(email)) {
            return userToConfirmationTask.get(email).getKey();
        }
        return null;
    }

    public static String getConfirmationCode(String email) throws InvalidAttributesException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        if (userToConfirmationCode.containsKey(email)) {
            return userToConfirmationCode.get(email).getKey();
        }
        return null;
    }

    public static boolean markTaskAsCompleted(String email, String code, ActionType actionType) throws InvalidAttributesException, ConfirmationException {
        if (email == null || email.isEmpty() || code == null || code.isEmpty() || actionType == null) {
            throw new InvalidAttributesException(String.format("Email: %s or confirmation code: %s or action: %s is invalid", email, code, actionType),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        log.debug("Completing task for user '{}', using code '{}' and action '{}'", email, code, actionType);
        if (!userToConfirmationCode.containsKey(email)) {
            return false;
        }
        if (!userToConfirmationTask.containsKey(email)) {
            return false;
        } else {
            LocalDateTime expireTime = userToConfirmationTask.get(email).getValue();
            if (LocalDateTime.now().isAfter(expireTime)) {
                userToConfirmationTask.remove(email);
                userToConfirmationCode.remove(email);
                throw new ConfirmationException(String.format("Confirmation code is expired: %s", code),
                        Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                        LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
            }
        }
        String confirmationCode = userToConfirmationCode.get(email).getKey();
        if (code.equals(confirmationCode) && actionType == userToConfirmationCode.get(email).getValue()) {
            userToConfirmationTask.remove(email);
            userToConfirmationCode.remove(email);
            return true;
        } else {
            throw new ConfirmationException(String.format("Confirmation codes does not match: %s", code),
                                            Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                            LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
