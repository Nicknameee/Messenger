package spring.application.tree.data.exceptions;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class InvalidAttributesException extends ApplicationException {

    public InvalidAttributesException(String exception, String trace, LocalDateTime errorTime, HttpStatus httpStatus) {
        super(exception, trace, errorTime, httpStatus);
    }
}
