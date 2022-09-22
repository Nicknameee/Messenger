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
import spring.application.tree.data.utility.mailing.models.ActionType;
import spring.application.tree.data.utility.mailing.service.MailActionsUtility;
import spring.application.tree.data.utility.tasks.ActionHistoryStorage;

import java.net.URI;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/utility")
@Slf4j
@RequiredArgsConstructor
public class UtilityController {
    private final UserService userService;
    private final MailActionsUtility mailActionsUtility;

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
        return ResponseEntity.ok(ZoneId.getAvailableZoneIds());
    }

    @PostMapping("/mail/send")
    public ResponseEntity<Object> createMessageSendingTask(@RequestBody AbstractMailMessageModel abstractMailMessageModel) throws ApplicationException {
        mailActionsUtility.sendMessage(abstractMailMessageModel);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/task/confirm/{code}/{email}/{action}")
    public ResponseEntity<Object> confirmTaskExecution(@PathVariable("code")   String code,
                                                       @PathVariable("email")  String email,
                                                       @PathVariable("action") String action) throws InvalidAttributesException, ConfirmationException {
        boolean isVerified = ActionHistoryStorage.markTaskAsCompleted(email, code, ActionType.fromKey(action));
        ResponseEntity<Object> response;
        if (isVerified) {
            switch (ActionType.fromKey(action)) {
                case SIGN_UP:
                    userService.enableUser(email);
                    break;
            }
            response = ResponseEntity.status(HttpStatus.FOUND).location(URI.create("http://localhost:9000")).build();
        } else {
            response = ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return response;
    }
}
