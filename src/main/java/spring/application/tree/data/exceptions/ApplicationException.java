package spring.application.tree.data.exceptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Builder
@Getter
@JsonIgnoreProperties({"cause", "stackTrace", "message", "suppressed", "localizedMessage"})
public class ApplicationException extends Exception {
    private String exception;
    private String trace;
    private LocalDateTime errorTime;
    private HttpStatus httpStatus;
}
