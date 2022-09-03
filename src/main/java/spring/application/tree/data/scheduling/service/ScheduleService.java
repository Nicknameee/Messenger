package spring.application.tree.data.scheduling.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import spring.application.tree.data.exceptions.InvalidAttributesException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ScheduleService {
    private List<ScheduledFuture<?>> scheduledTasks;
    private ScheduledExecutorService utilScheduler;

    @PostConstruct
    private void initializeSchedulers() {
        scheduledTasks = new ArrayList<>();
        //@TODO: add default initializing of tasks from db
        utilScheduler = Executors.newScheduledThreadPool(8);
        Runnable clearingTask = () -> {
            scheduledTasks.removeIf(task -> task.isDone() || task.isCancelled());
            log.info("Completed task removal process started");
        };
        utilScheduler.scheduleAtFixedRate(clearingTask, 0, 15, TimeUnit.MINUTES);
    }

    public ScheduledFuture<?> schedulePeriodicTaskConsideringTaskDuration(Runnable task, int delay, int period, TimeUnit timeUnit) throws InvalidAttributesException {
        if (task == null || delay < 0 || period < 0 || timeUnit == null) {
            throw new InvalidAttributesException(String.format("Some of params is invalid, task: %s, delay: %d, period: %d, timeUnit: %s", task, delay, period, timeUnit),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        ScheduledFuture<?> scheduledTask = utilScheduler.scheduleWithFixedDelay(task, delay, period, timeUnit);
        scheduledTasks.add(scheduledTask);
        return scheduledTask;
    }

    public ScheduledFuture<?> schedulePeriodicTaskWithoutConsideringTaskDuration(Runnable task, int delay, int period, TimeUnit timeUnit) throws InvalidAttributesException {
        if (task == null || delay < 0 || period < 0 || timeUnit == null) {
            throw new InvalidAttributesException(String.format("Some of params is invalid, task: %s, delay: %d, period: %d, timeUnit: %s", task, delay, period, timeUnit),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        ScheduledFuture<?> scheduledTask = utilScheduler.scheduleAtFixedRate(task, delay, period, timeUnit);
        scheduledTasks.add(scheduledTask);
        return scheduledTask;
    }

    public ScheduledFuture<?> scheduleOnceFireTask(Runnable task, int delay, TimeUnit timeUnit) throws InvalidAttributesException {
        if (task == null || delay < 0 || timeUnit == null) {
            throw new InvalidAttributesException(String.format("Some of params is invalid, task: %s, delay: %d, timeUnit: %s", task, delay, timeUnit),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(), LocalDateTime.now(),
                                                 HttpStatus.NOT_ACCEPTABLE);
        }
        ScheduledFuture<?> scheduledTask = utilScheduler.schedule(task, delay, timeUnit);
        scheduledTasks.add(scheduledTask);
        return scheduledTask;
    }

    public ScheduledFuture<?> schedulePeriodicTaskConsideringTaskDurationUntilDate(Runnable task, int delay, int period, TimeUnit timeUnit, Date repeatUntil) throws InvalidAttributesException {
        ScheduledFuture<?> scheduledTask = schedulePeriodicTaskConsideringTaskDuration(task, delay, period, timeUnit);
        Runnable cancelTask = () -> {
            scheduledTask.cancel(true);
            log.info(String.format("Task cancelling started: %s", scheduledTask));
        };
        int cancelDelay = (int) (repeatUntil.getTime() - new Date().getTime());
        return scheduleOnceFireTask(cancelTask, Math.max(cancelDelay, 0), TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> schedulePeriodicTaskWithoutConsideringTaskDurationUntilDate(Runnable task, int delay, int period, TimeUnit timeUnit, Date repeatUntil) throws InvalidAttributesException {
        ScheduledFuture<?> scheduledTask = schedulePeriodicTaskWithoutConsideringTaskDuration(task, delay, period, timeUnit);
        Runnable cancelTask = () -> {
            scheduledTask.cancel(true);
            log.info(String.format("Task cancelling started: %s", scheduledTask));
        };
        int cancelDelay = (int) (repeatUntil.getTime() - new Date().getTime());
        return scheduleOnceFireTask(cancelTask, Math.max(cancelDelay, 0), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    private void saveScheduledTasksBefore() {
        scheduledTasks.clear();
        //@TODO: save map values to db
    }
}
