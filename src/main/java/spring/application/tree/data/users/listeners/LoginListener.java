package spring.application.tree.data.users.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.users.service.UserService;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoginListener implements ApplicationListener<AuthenticationSuccessEvent> {
    private final UserService userService;
    @Override
    public void onApplicationEvent(@NonNull AuthenticationSuccessEvent event) {
        String authenticationName = event.getAuthentication().getName();
        log.info(String.format("Login detected for: %s", authenticationName));
        try {
            userService.updateUserLoginTime(authenticationName);
        } catch (InvalidAttributesException e) {
            log.error(String.format("Exception occurs while login time updating for user: %s, exception message: %s", authenticationName, e.getMessage()));
        }
    }
}
