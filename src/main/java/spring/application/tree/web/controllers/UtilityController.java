package spring.application.tree.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.users.service.UserService;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/utility")
@Slf4j
@RequiredArgsConstructor
public class UtilityController {
    private final UserService userService;

    @GetMapping("/credentials/availability")
    public ResponseEntity<Object> checkCredentialsAvailability(@RequestParam("email") String email, @RequestParam("username") String username) throws ApplicationException {
        Map<String, Boolean> response = new HashMap<>();
        response.put("email", userService.checkEmailAvailability(email));
        response.put("username", userService.checkUsernameAvailability(username));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/timezone")
    public ResponseEntity<Object> getAvailableTimezones() {
        return ResponseEntity.ok(ZoneId.getAvailableZoneIds());
    }
}
