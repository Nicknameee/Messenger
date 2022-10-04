package spring.application.tree.web.configuration;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.reflections.Reflections;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Field;
import java.util.Set;

@Configuration
@Slf4j
public class ApplicationQuartzConfiguration {
    private Scheduler scheduler;

    @PostConstruct
    protected void initializeQuartzJobs() {
        try {
            Reflections reflections = new Reflections("spring.application.tree.data.scheduling.jobs");
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            Set<Class<? extends QuartzJobBean>> quartzJobClasses = reflections.getSubTypesOf(QuartzJobBean.class);
            for (Class<? extends QuartzJobBean> quartzJobClass : quartzJobClasses) {
                try {
                    log.info(String.format("Initializing QuartzJob %s", quartzJobClass));
                    Field cronExpressionField = quartzJobClass.getDeclaredField("cronExpression");
                    cronExpressionField.setAccessible(true);
                    String cron = (String) cronExpressionField.get("");
                    cronExpressionField.setAccessible(false);
                    JobDetail quartzJobDetail = JobBuilder.newJob(quartzJobClass)
                            .withIdentity(quartzJobClass.getName()).storeDurably().build();
                    Trigger quartzJobTrigger = TriggerBuilder.newTrigger().forJob(quartzJobDetail)
                            .withIdentity(String.format("Trigger for %s", quartzJobClass.getName()))
                            .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                            .build();
                    scheduler.scheduleJob(quartzJobDetail, quartzJobTrigger);
                } catch (SchedulerException | NoSuchFieldException e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (SchedulerException | IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
    }

    @PreDestroy
    protected void destroyQuartzScheduledJobs() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }
}
