package spring.application.tree.web.configuration.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.users.security.UserDetailsImplementationService;
import spring.application.tree.data.users.service.UserService;
import spring.application.tree.data.users.security.token.AuthorizationTokenUtility;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile(value = {"token", "boots_token"})
public class AuthenticationLogoutTokenBasedSecurityHandler implements LogoutSuccessHandler {
    private final UserDetailsImplementationService userDetailsImplementationService;
    private final AuthorizationTokenUtility authorizationTokenUtility;
    private final UserService userService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        if (request.getRequestURI().equals("/logout")) {
            String authorizationHeaderValue = request.getHeader("Authorization");
            if (authorizationHeaderValue != null && authorizationHeaderValue.startsWith("Bearer ")) {
                String authorizationToken = authorizationHeaderValue.substring(7);
                String username = authorizationTokenUtility.getUsernameFromToken(authorizationToken);
                UserDetails userDetails = userDetailsImplementationService.loadUserByUsername(username);
                if (!authorizationToken.isEmpty() && authorizationTokenUtility.validateToken(authorizationToken, userDetails)) {
                    authorizationTokenUtility.blacklistToken(authorizationToken);
                    ObjectMapper jacksonMapper = new ObjectMapper();
                    Map<String, Object> responseBodyMap = new HashMap<>();
                    responseBodyMap.put("logout", true);
                    if (authentication != null) {
                        Object principal = authentication.getPrincipal();
                        if (principal instanceof UserDetails) {
                            responseBodyMap.put("user", username);
                        }
                    }
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(HttpStatus.OK.value());
                    response.getWriter().write(jacksonMapper.writeValueAsString(responseBodyMap));
                    response.getWriter().flush();
                    try {
                        userService.updateUserLogoutTime(username);
                    } catch (InvalidAttributesException e) {
                        log.error(String.format("Exception while logout time updating for user: %s", username));
                    }
                }
            }
        }
    }
}
