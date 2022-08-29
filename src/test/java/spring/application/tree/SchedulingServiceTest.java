package spring.application.tree;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import spring.application.tree.data.scheduling.service.ScheduleService;

@SpringBootTest
@ContextConfiguration(initializers = ApplicationTestContextInitializer.class)
@TestPropertySource(properties = "spring.config.location = classpath:application.properties")
public class SchedulingServiceTest {
    @Autowired
    private ScheduleService scheduleService;
}
