package spring.application.tree.data.scheduling.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import spring.application.tree.data.exceptions.DataNotFoundException;
import spring.application.tree.data.exceptions.InvalidAttributesException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ScheduleService {
    private Map<String, ScheduledFuture<?>> scheduledTasks;
    private ScheduledExecutorService utilScheduler;

    @PostConstruct
    private void initializeSchedulers() {
        scheduledTasks = new HashMap<>();
        //@TODO: add default initializing of tasks from db
        utilScheduler = Executors.newScheduledThreadPool(8);
    }

    public ScheduledFuture<?> schedulePeriodicTaskConsideringTaskDuration(Runnable task, int delay, int period, TimeUnit timeUnit) throws InvalidAttributesException {
        if (task == null || delay < 0 || period < 0 || timeUnit == null) {
            throw new InvalidAttributesException(String.format("Some of params is invalid, task: %s, delay: %d, period: %d, timeUnit: %s", task, delay, period, timeUnit),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        ScheduledFuture<?> scheduledTask = utilScheduler.scheduleWithFixedDelay(task, delay, period, timeUnit);
        scheduledTasks.put(UUID.randomUUID().toString(), scheduledTask);
        return scheduledTask;
    }

    public ScheduledFuture<?> schedulePeriodicTaskWithoutConsideringTaskDuration(Runnable task, int delay, int period, TimeUnit timeUnit) throws InvalidAttributesException {
        if (task == null || delay < 0 || period < 0 || timeUnit == null) {
            throw new InvalidAttributesException(String.format("Some of params is invalid, task: %s, delay: %d, period: %d, timeUnit: %s", task, delay, period, timeUnit),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        ScheduledFuture<?> scheduledTask = utilScheduler.scheduleAtFixedRate(task, delay, period, timeUnit);
        scheduledTasks.put(UUID.randomUUID().toString(), scheduledTask);
        return scheduledTask;
    }

    public ScheduledFuture<?> scheduleOnceFireTask(Runnable task, int delay, TimeUnit timeUnit) throws InvalidAttributesException {
        if (task == null || delay < 0 || timeUnit == null) {
            throw new InvalidAttributesException(String.format("Some of params is invalid, task: %s, delay: %d, timeUnit: %s", task, delay, timeUnit),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(), LocalDateTime.now(),
                                                 HttpStatus.NOT_ACCEPTABLE);
        }
        ScheduledFuture<?> scheduledTask = utilScheduler.schedule(task, delay, timeUnit);
        scheduledTasks.put(UUID.randomUUID().toString(), scheduledTask);
        return scheduledTask;
    }

    public ScheduledFuture<?> schedulePeriodicTaskConsideringTaskDurationUntilDate(Runnable task, int delay, int period, TimeUnit timeUnit, Date repeatUntil) throws InvalidAttributesException {
        ScheduledFuture<?> scheduledTask = schedulePeriodicTaskConsideringTaskDuration(task, delay, period, timeUnit);
        Runnable cancelTask = () -> {
            scheduledTask.cancel(true);
        };
        int cancelDelay = (int) (repeatUntil.getTime() - new Date().getTime());
        return scheduleOnceFireTask(cancelTask, Math.max(cancelDelay, 0), TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> schedulePeriodicTaskWithoutConsideringTaskDurationUntilDate(Runnable task, int delay, int period, TimeUnit timeUnit, Date repeatUntil) throws InvalidAttributesException {
        ScheduledFuture<?> scheduledTask = schedulePeriodicTaskWithoutConsideringTaskDuration(task, delay, period, timeUnit);
        Runnable cancelTask = () -> {
            scheduledTask.cancel(true);
        };
        int cancelDelay = (int) (repeatUntil.getTime() - new Date().getTime());
        return scheduleOnceFireTask(cancelTask, Math.max(cancelDelay, 0), TimeUnit.MILLISECONDS);
    }

    public void cancelTask(String uuid) throws DataNotFoundException, InvalidAttributesException {
        if (uuid == null || uuid.isEmpty()) {
            throw new InvalidAttributesException(String.format("UUID is invalid: %s", uuid),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        ScheduledFuture<?> taskToCancel = scheduledTasks.get(uuid);
        if (taskToCancel == null) {
            throw new DataNotFoundException(String.format("Scheduled task was not found by UUID: %s", uuid),
                                            Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                            LocalDateTime.now(), HttpStatus.NOT_FOUND);
        }
        taskToCancel.cancel(false);
        scheduledTasks.remove(uuid);
    }

    @PreDestroy
    private void saveScheduledTasksBefore() {
        scheduledTasks.clear();
        //@TODO: save map values to db
    }
}
