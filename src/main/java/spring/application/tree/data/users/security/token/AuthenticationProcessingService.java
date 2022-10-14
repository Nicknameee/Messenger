package spring.application.tree.data.users.security.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.exceptions.SecurityException;
import spring.application.tree.data.users.security.UserDetailsImplementationService;
import spring.application.tree.data.users.service.UserService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile(value = {"token", "boots_token"})
public class AuthenticationProcessingService {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsImplementationService userDetailsService;
    private final AuthorizationTokenUtility authorizationTokenUtility;
    private final UserService userService;

    public Map<String, Object> authenticateUserWithTokenBasedAuthorizationStrategy(String username, String password) throws SecurityException {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        String token;
        if (authentication.isAuthenticated()) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            token = authorizationTokenUtility.generateToken(userDetails);
        } else {
            throw new SecurityException("Authorization token can not be achieved",
                                        Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                        LocalDateTime.now(), HttpStatus.UNAUTHORIZED);
        }
        try {
            userService.updateUserLoginTime(username);
        } catch (InvalidAttributesException e) {
            log.error(String.format("Exception while login time updating for user: %s", username));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("expires_at", authorizationTokenUtility.getExpirationDateFromToken(token).getTime());
        return response;
    }
}
