package spring.application.tree.data.users.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.stereotype.Component;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.users.service.UserService;

@Component
@Profile("!token")
@Slf4j
@RequiredArgsConstructor
public class LogoutListener implements ApplicationListener<SessionDestroyedEvent> {
    private final UserService userService;
    @Override
    public void onApplicationEvent(@NonNull SessionDestroyedEvent event) {
        for (SecurityContext securityContext : event.getSecurityContexts()) {
            String authenticationName = securityContext.getAuthentication().getName();
            log.info(String.format("Logout detected for: %s", authenticationName));
            try {
                userService.updateUserLogoutTime(authenticationName);
            } catch (InvalidAttributesException e) {
                log.error(String.format("Exception occurs while logout time updating for user: %s, exception message: %s", authenticationName, e.getMessage()));
            }
        }
    }
}
