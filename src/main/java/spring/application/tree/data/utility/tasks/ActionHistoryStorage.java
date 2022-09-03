package spring.application.tree.data.utility.tasks;

import org.springframework.http.HttpStatus;
import spring.application.tree.data.exceptions.ConfirmationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.utility.mailing.models.ActionType;
import spring.application.tree.data.utility.models.PairValue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class ActionHistoryStorage {
    /**
     * Key - user email, value - task of confirmation email sending
     */
    private static final Map<String, ScheduledFuture<?>> userToConfirmationTask = new HashMap<>();
    /**
     * Key - user email, value - confirmation code and action type
     */
    private static final Map<String, PairValue<String, ActionType>> userToConfirmationCode = new HashMap<>();

    public static void putConfirmationTask(String email, ScheduledFuture<?> task) throws InvalidAttributesException {
        if (email == null || email.isEmpty() || task == null) {
            throw new InvalidAttributesException(String.format("Email: %s or confirmation task: %s is invalid", email, task),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userToConfirmationTask.remove(email);
        userToConfirmationTask.put(email, task);
    }

    public static void putConfirmationCode(String email, String code, ActionType actionType) throws InvalidAttributesException {
        if (email == null || email.isEmpty() || code == null || code.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email: %s or confirmation code: %s is invalid", email, code),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userToConfirmationCode.remove(email);
        userToConfirmationCode.put(email, new PairValue<>(code, actionType));
    }

    /*public static void removeConfirmationTask(String email) throws InvalidAttributesException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userToConfirmationTask.remove(email);
    }*/

    public static void removeConfirmationCode(String email) throws InvalidAttributesException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        userToConfirmationCode.remove(email);
    }

    /*public static ScheduledFuture<?> getConfirmationTask(String email) throws InvalidAttributesException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        return userToConfirmationTask.get(email);
    }

    public static String getConfirmationCode(String email) throws InvalidAttributesException {
        if (email == null || email.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email is invalid: %s", email),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        return userToConfirmationCode.get(email).getKey();
    }*/

    public static void markTaskAsCompleted(String email, String code, ActionType actionType) throws InvalidAttributesException, ConfirmationException {
        if (email == null || email.isEmpty() || code == null || code.isEmpty()) {
            throw new InvalidAttributesException(String.format("Email: %s or confirmation code: %s is invalid", email, code),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        String confirmationCode = userToConfirmationCode.get(email).getKey();
        if (code.equals(confirmationCode) && actionType == userToConfirmationCode.get(email).getValue()) {
            userToConfirmationTask.remove(email);
            userToConfirmationCode.remove(email);
        } else {
            throw new ConfirmationException(String.format("Confirmation codes does not match: %s", code),
                                            Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                            LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
