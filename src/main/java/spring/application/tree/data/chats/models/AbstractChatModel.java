package spring.application.tree.data.chats.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbstractChatModel {
    private int id;
    private String title;
    private String description;
    private boolean isPrivate;
    private String password;
    private int authorId;
}
