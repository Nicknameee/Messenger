package spring.application.tree.data.users.listeners;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogoutListener implements ApplicationListener<SessionDestroyedEvent> {
    @Override
    public void onApplicationEvent(@NonNull SessionDestroyedEvent event) {
        for (SecurityContext securityContext : event.getSecurityContexts()) {
            log.info(String.format("Logout detected for: %s", securityContext.getAuthentication().getName()));
        }
    }
}
