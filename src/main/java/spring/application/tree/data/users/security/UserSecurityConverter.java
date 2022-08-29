package spring.application.tree.data.users.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import spring.application.tree.data.users.attributes.Status;
import spring.application.tree.data.users.models.AbstractUserModel;

import java.util.Collection;
import java.util.Set;

@Data
public class UserSecurityConverter implements UserDetails {
    private final String username;
    private final String password;
    private final Set<SimpleGrantedAuthority> authorities;
    private final boolean isActive;

    public UserSecurityConverter(String username, String password, Set<SimpleGrantedAuthority> authorities, Boolean isActive) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.isActive = isActive;
    }

    public static UserDetails convertUser(AbstractUserModel user) {
        return new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        user.getStatus().equals(Status.ENABLED),
                        user.getStatus().equals(Status.ENABLED),
                        user.getStatus().equals(Status.ENABLED),
                        user.getStatus().equals(Status.ENABLED),
                        user.getRole().getAuthorities());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return isActive;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isActive;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}