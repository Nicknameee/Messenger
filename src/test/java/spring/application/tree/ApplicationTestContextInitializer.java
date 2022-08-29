package spring.application.tree;

import lombok.NonNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class ApplicationTestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
        System.setProperty("DB_URL", "jdbc:postgresql://localhost:5432/chat");
        System.setProperty("DB_USERNAME", "postgres");
        System.setProperty("DB_PASSWORD", "1904");
    }
}
