package spring.application.tree.data.utility.mailing.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.TimeZone;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbstractMailMessageModel {
    private String recipient;
    private String subject;
    private String text;
    private MailType mailType;
    private ActionType actionType;
    private String clientTimezone = TimeZone.getDefault().getID();
}
