package spring.application.tree.data.users.listeners;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoginListener implements ApplicationListener<AuthenticationSuccessEvent> {
    @Override
    public void onApplicationEvent(@NonNull AuthenticationSuccessEvent event) {
        log.info(String.format("Login detected for: %s", event.getAuthentication().getName()));
    }
}
