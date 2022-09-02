package spring.application.tree;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import spring.application.tree.data.exceptions.ApplicationException;
import spring.application.tree.data.users.repository.UserDataAccessObject;
import spring.application.tree.data.users.repository.UserRepository;
import spring.application.tree.data.users.service.UserService;

@SpringBootTest
@Import(ApplicationTestConfiguration.class)
@ContextConfiguration(initializers = ApplicationTestContextInitializer.class)
@TestPropertySource(properties = "spring.config.location = classpath:application.properties")
public class UserServiceTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserDataAccessObject userDataAccessObject;
    @Autowired
    private UserService userService;

    @Test
    public void testGettingUserByLoginCredentials() throws ApplicationException {
        System.out.println(userService.getUserByLoginCredentials("username"));
    }
}
