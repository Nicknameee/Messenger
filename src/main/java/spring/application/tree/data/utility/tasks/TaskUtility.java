package spring.application.tree.data.utility.tasks;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import spring.application.tree.data.exceptions.ConfirmationException;
import spring.application.tree.data.exceptions.InvalidAttributesException;
import spring.application.tree.data.users.service.UserService;
import spring.application.tree.data.utility.mailing.models.ActionType;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class TaskUtility {
    private final UserService userService;

    public ResponseEntity<Object> confirmTaskExecution(String code, String email, String action) throws InvalidAttributesException, ConfirmationException {
        boolean isVerified = ActionHistoryStorage.markTaskAsCompleted(email, code, ActionType.fromKey(action));
        ResponseEntity<Object> response = null;
        if (isVerified) {
            switch (ActionType.fromKey(action)) {
                case SIGN_UP:
                    response = confirmSigningUpTask(email);
                    break;
            }
        } else {
            response = ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return response;
    }

    private ResponseEntity<Object> confirmSigningUpTask(String email) throws InvalidAttributesException {
        userService.enableUser(email);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("http://localhost:9000")).build();
    }
}
