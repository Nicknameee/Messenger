package spring.application.tree.web.controllers.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import spring.application.tree.data.exceptions.ApplicationException;

import java.time.LocalDateTime;
import java.util.Arrays;

@RestControllerAdvice
public class ExceptionHandlerController {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApplicationException> handleApplicationExceptions(Exception e) {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String exception = e.getMessage();
        String trace = Arrays.toString(e.getStackTrace());
        LocalDateTime exceptionTime = LocalDateTime.now();
        if (e instanceof ApplicationException) {
            httpStatus = ((ApplicationException) e).getHttpStatus();
            exception = ((ApplicationException) e).getException();
            trace = ((ApplicationException) e).getTrace();
            exceptionTime = ((ApplicationException) e).getErrorTime();
        }
        ApplicationException applicationException = ApplicationException.builder()
                .exception(exception)
                .errorTime(exceptionTime)
                .httpStatus(httpStatus)
                .trace(trace)
                .build();
        return ResponseEntity.status(applicationException.getHttpStatus()).body(applicationException);
    }
}
