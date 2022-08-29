package spring.application.tree.data.messages.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbstractMessageModel {
    private int id;
    private String message;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sendingDate;
    private int authorId;
    private int chatId;
}
