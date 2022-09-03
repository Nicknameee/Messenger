package spring.application.tree.data.utility.mailing.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbstractMailMessageModel {
    private String recipient;
    private String subject;
    private String text;
    private MailType mailType;
    private ActionType actionType;
}
