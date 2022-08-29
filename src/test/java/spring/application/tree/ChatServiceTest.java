package spring.application.tree;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import spring.application.tree.data.chats.service.ChatService;
import spring.application.tree.data.exceptions.InvalidAttributesException;

@SpringBootTest
@ContextConfiguration(initializers = ApplicationTestContextInitializer.class)
@TestPropertySource(properties = "spring.config.location = classpath:application.properties")
public class ChatServiceTest {
    @Autowired
    private ChatService chatService;

    @Test
    public void testGettingChats() throws InvalidAttributesException {
        chatService.getChats(1).forEach(System.out::println);
    }
}
