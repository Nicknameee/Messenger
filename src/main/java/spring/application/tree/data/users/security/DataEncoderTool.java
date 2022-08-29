package spring.application.tree.data.users.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class DataEncoderTool {
    private final PasswordEncoder passwordEncoder;
    private static PasswordEncoder passwordEncoderTool;

    @PostConstruct
    public void initialize() {
        passwordEncoderTool = this.passwordEncoder;
    }

    public static String encodeData(String password) {
        return passwordEncoderTool.encode(password);
    }
}
