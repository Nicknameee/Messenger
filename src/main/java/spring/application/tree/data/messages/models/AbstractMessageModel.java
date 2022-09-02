package spring.application.tree.data.messages.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import spring.application.tree.data.users.models.AbstractUserModel;
import spring.application.tree.data.users.service.UserService;

import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    @JsonGetter("sendingDate")
    public OffsetDateTime getSendingDate() {
        AbstractUserModel abstractUserModel = UserService.getCurrentlyAuthenticatedUser();
        if (abstractUserModel != null) {
            return OffsetDateTime.ofInstant(sendingDate.toInstant(), ZoneId.of(abstractUserModel.getTimezone()));
        }
        return OffsetDateTime.ofInstant(sendingDate.toInstant(), ZoneId.systemDefault());
    }
}
