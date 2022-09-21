package spring.application.tree.data.users.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import spring.application.tree.data.users.attributes.Language;
import spring.application.tree.data.users.attributes.Role;
import spring.application.tree.data.users.attributes.Status;
import spring.application.tree.data.users.security.DataEncoderTool;
import spring.application.tree.data.users.service.UserService;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

@Data
@Table(name = "users")
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbstractUserModel implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "username" , nullable = false , unique = true)
    private String username;
    @Column(name = "email" , nullable = false , unique = true)
    private String email;
    @Column(name = "password" , nullable = false)
    private String password;
    @Column(name = "login_time" , nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date loginTime = new Date();
    @Column(name = "logout_time" , nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date logoutTime = new Date(0);
    @Column(name = "role" , nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.ROLE_USER;
    @Column(name = "status" , nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.DISABLED;
    @Column(name = "language" , nullable = false)
    @Enumerated(EnumType.STRING)
    private Language language = Language.ENGLISH;
    @Column(name = "timezone", nullable = false)
    private String timezone;

    @JsonSetter("password")
    private void setPassword(String password) {
        this.password = DataEncoderTool.encodeData(password);
    }
    @JsonGetter("loginTime")
    private OffsetDateTime getLoginTime() {
        AbstractUserModel abstractUserModel = UserService.getCurrentlyAuthenticatedUser();
        if (abstractUserModel != null) {
            return OffsetDateTime.ofInstant(loginTime.toInstant(), ZoneId.of(abstractUserModel.getTimezone()));
        }
        return OffsetDateTime.ofInstant(loginTime.toInstant(), ZoneId.systemDefault());
    }
    @JsonGetter("logoutTime")
    private OffsetDateTime getLogoutTime() {
        AbstractUserModel abstractUserModel = UserService.getCurrentlyAuthenticatedUser();
        if (abstractUserModel != null) {
            return OffsetDateTime.ofInstant(logoutTime.toInstant(), ZoneId.of(abstractUserModel.getTimezone()));
        }
        return OffsetDateTime.ofInstant(logoutTime.toInstant(), ZoneId.systemDefault());
    }
    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return status == Status.ENABLED;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return status == Status.ENABLED;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return status == Status.ENABLED;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return status == Status.ENABLED;
    }

    public void mergeChanges(AbstractUserModel abstractUserModel) {
        if (!Objects.equals(this.id, abstractUserModel.getId())) {
            return;
        }
        if (abstractUserModel.getUsername() != null) {
            this.username = abstractUserModel.getUsername();
        }
        if (abstractUserModel.getPassword() != null) {
            this.password = abstractUserModel.getPassword();
        }
        if (abstractUserModel.getLanguage() != null) {
            this.language = abstractUserModel.getLanguage();
        }
        if (abstractUserModel.getTimezone() != null) {
            this.timezone = abstractUserModel.getTimezone();
        }
    }
}
