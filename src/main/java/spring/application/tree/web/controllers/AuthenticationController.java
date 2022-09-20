package spring.application.tree.web.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.application.tree.data.exceptions.SecurityException;
import spring.application.tree.data.users.security.token.AuthenticationProcessingService;

@RestController
@RequiredArgsConstructor
@Profile(value = "token")
public class AuthenticationController {
    private final AuthenticationProcessingService authenticationProcessingService;

    @PostMapping("/login")
    public ResponseEntity<Object> loginUser(@RequestParam("username") String username,
                                            @RequestParam("password") String password) throws SecurityException {
        return ResponseEntity.ok(authenticationProcessingService.authenticateUserWithTokenBasedAuthorizationStrategy(username, password));
    }
}
