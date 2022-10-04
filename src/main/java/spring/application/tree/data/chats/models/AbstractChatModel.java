package spring.application.tree.data.chats.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "chats")
@Entity
public class AbstractChatModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "title", nullable = false, unique = true)
    private String title;
    @Column(name = "description")
    private String description;
    @Column(name = "private")
    private boolean isPrivate;
    @Column(name = "password")
    private String password;
    @Column(name = "author_id")
    private int authorId;
}
