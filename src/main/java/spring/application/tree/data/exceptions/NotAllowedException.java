package spring.application.tree.data.exceptions;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class NotAllowedException extends ApplicationException {
    public NotAllowedException(String exception, String trace, LocalDateTime errorTime, HttpStatus httpStatus) {
        super(exception, trace, errorTime, httpStatus);
    }
}
