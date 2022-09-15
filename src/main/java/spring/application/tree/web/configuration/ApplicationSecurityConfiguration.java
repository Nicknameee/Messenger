package spring.application.tree.web.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import spring.application.tree.data.users.security.UserDetailsImplementationService;
import spring.application.tree.web.configuration.filters.PreAuthenticationFilter;
import spring.application.tree.web.configuration.filters.PreLogoutFilter;
import spring.application.tree.web.configuration.handlers.AuthenticationFailureSecurityHandler;
import spring.application.tree.web.configuration.handlers.AuthenticationLogoutSecurityHandler;
import spring.application.tree.web.configuration.handlers.AuthenticationSuccessSecurityHandler;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class ApplicationSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private final AuthenticationFailureSecurityHandler authenticationFailureSecurityHandler;
    private final AuthenticationSuccessSecurityHandler authenticationSuccessSecurityHandler;
    private final AuthenticationLogoutSecurityHandler authenticationLogoutSecurityHandler;
    private final UserDetailsImplementationService userDetailsService;

    @Override
    public void configure(WebSecurity web) {
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable();
        http
                .sessionManagement()
                .maximumSessions(1)
                .sessionRegistry(sessionRegistry());
        http
                .addFilterBefore(new PreAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new PreLogoutFilter(), LogoutFilter.class);
        http
                .authorizeRequests()
                .antMatchers("/api/user/account/create").permitAll()
                .antMatchers("/api/utility/**").permitAll()
                .antMatchers(HttpMethod.POST, "/login", "/logout").permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .formLogin()
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessSecurityHandler)
                .failureHandler(authenticationFailureSecurityHandler)
                .and()
                .logout()
                .logoutSuccessHandler(authenticationLogoutSecurityHandler)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", HttpMethod.POST.name()))
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder authenticationManagerBuilder) {
        authenticationManagerBuilder.authenticationProvider(repositoryAuthenticationProvider());
    }

    @Bean
    protected PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(7);
    }

    @Bean
    protected DaoAuthenticationProvider repositoryAuthenticationProvider() {
        DaoAuthenticationProvider repositoryAuthenticationProvider = new DaoAuthenticationProvider();
        repositoryAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        repositoryAuthenticationProvider.setUserDetailsService(userDetailsService);
        return repositoryAuthenticationProvider;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}