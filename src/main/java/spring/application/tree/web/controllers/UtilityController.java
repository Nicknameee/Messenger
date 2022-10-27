package spring.application.tree.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.exceptions.ConfirmationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.users.service.UserService;
import spring.application.tree.data.utility.mailing.models.AbstractMailMessageModel;
import spring.application.tree.data.utility.mailing.service.MailActionsUtility;
import spring.application.tree.data.utility.tasks.TaskUtility;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@RestController
@RequestMapping("/api/utility")
@Slf4j
@RequiredArgsConstructor
public class UtilityController {
    private final UserService userService;
    private final MailActionsUtility mailActionsUtility;
    private final TaskUtility taskUtility;

    @GetMapping("/credentials/availability")
    public ResponseEntity<Object> checkCredentialsAvailability(@RequestParam(required = false) String email,
                                                               @RequestParam(required = false) String username) throws ApplicationException {
        Map<String, Object> response = new HashMap<>();
        if (email == null && username == null) {
            response.put("error", "No passed parameter detected, email and/or username checking is impossible");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (email != null) {
            response.put("email", userService.checkEmailAvailability(email));
        }
        if (username != null) {
            response.put("username", userService.checkUsernameAvailability(username));
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/timezone")
    public ResponseEntity<Object> getAvailableTimezones() {
        Map<String, Object> zones = new HashMap<>();
        ZoneId.getAvailableZoneIds().forEach(id -> {
            TimeZone zone = TimeZone.getTimeZone(id);
            zones.put(id, zone.toZoneId().getRules().getOffset(Instant.now()).getId());
        });
        return ResponseEntity.ok(zones);
    }

    @PostMapping("/mail/send")
    public ResponseEntity<Object> createMessageSendingTask(@RequestBody AbstractMailMessageModel abstractMailMessageModel,
                                                           HttpServletRequest httpServletRequest) throws ApplicationException, ParseException {
        mailActionsUtility.sendMessage(abstractMailMessageModel, httpServletRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/task/confirm/{code}/{email}/{action}")
    public ResponseEntity<Object> confirmTaskExecution(@PathVariable("code")   String code,
                                                       @PathVariable("email")  String email,
                                                       @PathVariable("action") String action) throws InvalidAttributesException, ConfirmationException {
        return taskUtility.confirmTaskExecution(code, email, action);
    }

    @PostMapping("/restoring/password")
    public ResponseEntity<Object> restoreUserPassword(@RequestParam("email")    String email,
                                                      @RequestParam("password") String password,
                                                      HttpServletRequest httpServletRequest) throws ApplicationException {
        userService.restoreUserPassword(email, password, httpServletRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restoring/email")
    public ResponseEntity<Object> restoreUserEmail(@RequestParam("email")    String email,
                                                   @RequestParam("username") String username,
                                                   HttpServletRequest httpServletRequest) throws ApplicationException {
        userService.restoreUserEmail(email, username, httpServletRequest);
        return ResponseEntity.ok().build();
    }
}
