package spring.application.tree.data.utility.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import spring.application.tree.data.exceptions.ConfirmationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.scheduling.service.ScheduleService;
import spring.application.tree.data.utility.mailing.models.ActionType;
import spring.application.tree.data.utility.models.TrioValue;
import spring.application.tree.data.utility.properties.CustomPropertyDataLoader;
import spring.application.tree.data.utility.properties.CustomPropertySourceConverter;
import spring.application.tree.data.utility.tasks.task.SystemTask;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskUtility {
    /**
     * Key - user email, value - expire time, origin server URI and task for execution
     */
    private static final Map<String, TrioValue<LocalDateTime, String, SystemTask<String>>> onSuccessConfirmationTask = new ConcurrentHashMap<>();
    /**
     * Key - property name, value - property value
     */
    private static Map<String, String> properties;
    private final ScheduleService scheduleService;

    @PostConstruct
    private void initializeTasks() throws InvalidAttributesException {
        properties = CustomPropertySourceConverter.convertToKeyValueFormat(CustomPropertyDataLoader.getResourceContent("classpath:mail.properties"));
        Runnable clearingTask = () -> {
            for (Map.Entry<String, TrioValue<LocalDateTime, String, SystemTask<String>>> entry : onSuccessConfirmationTask.entrySet()) {
                if (entry.getValue().getKey().isBefore(LocalDateTime.now())) {
                    onSuccessConfirmationTask.remove(entry.getKey());
                }
            }
            log.info("Expired task removal process started");
        };
        scheduleService.schedulePeriodicTaskWithoutConsideringTaskDuration(clearingTask, 0, 15, TimeUnit.MINUTES);
    }

    public static void putSuccessConfirmationTask(String ID, String origin, Runnable task) {
        int delay = 3600;
        if (properties.containsKey("duration")) {
            delay = Integer.parseInt(properties.get("duration"));
        }
        onSuccessConfirmationTask.put(ID, new TrioValue<>(LocalDateTime.now().plusSeconds(delay), origin, new SystemTask<>(ID, task)));
    }

    public static void removeSuccessConfirmationTask(String ID) {
        onSuccessConfirmationTask.remove(ID);
    }

    public ResponseEntity<Object> confirmTaskExecution(String code, String email, String action) throws InvalidAttributesException, ConfirmationException {
        boolean isVerified = ActionHistoryStorage.markTaskAsCompleted(email, code, ActionType.fromKey(action));
        ResponseEntity<Object> response;
        if (isVerified) {
            synchronized (onSuccessConfirmationTask) {
                if (onSuccessConfirmationTask.containsKey(email)) {
                    onSuccessConfirmationTask.get(email).getData().run();
                    String origin = onSuccessConfirmationTask.get(email).getValue();
                    onSuccessConfirmationTask.remove(email);
                    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(origin)).build();
                }
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } else {
            response = ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return response;
    }
}
