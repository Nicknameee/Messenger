package spring.application.tree;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.users.repository.UserDataAccessObject;
import spring.application.tree.data.users.repository.UserRepository;
import spring.application.tree.data.users.service.UserService;

@SpringBootTest
@ContextConfiguration(initializers = ApplicationTestContextInitializer.class)
@TestPropertySource(properties = "spring.config.location = classpath:application.properties")
public class UserServiceTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserDataAccessObject userDataAccessObject;
    @Autowired
    private UserService userService;

    @Test
    public void testGettingAllUsers() {
        System.out.println(userRepository.findAll());
        System.out.println(userDataAccessObject.getAllUsersList());
    }

    @Test
    public void testGettingUserByLoginCredentials() throws ApplicationException {
        System.out.println(userService.getUserByLoginCredentials("username"));
    }
}
