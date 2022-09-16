package spring.application.tree.web.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
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
import spring.application.tree.web.configuration.handlers.AuthenticationLogoutTokenBasedSecurityHandler;
import spring.application.tree.web.configuration.entries.AuthenticationTokenBasedEntryPoint;
import spring.application.tree.web.configuration.filters.AuthorizationTokenRequestFilter;
import spring.application.tree.web.configuration.filters.PreLogoutTokenBasedFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
@Profile(value = "token")
public class ApplicationSecurityConfigurationTokenBased extends WebSecurityConfigurerAdapter {
    private final AuthenticationTokenBasedEntryPoint authenticationTokenBasedEntryPoint;
    private final AuthenticationLogoutTokenBasedSecurityHandler authenticationLogoutTokenBasedSecurityHandler;
    private final UserDetailsImplementationService userDetailsService;
    private final AuthorizationTokenRequestFilter authorizationTokenRequestFilter;
    private final PreLogoutTokenBasedFilter preLogoutTokenBasedFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .httpBasic().disable()
                .formLogin().disable();
        http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http
                .addFilterBefore(authorizationTokenRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(preLogoutTokenBasedFilter, LogoutFilter.class);
        http
                .authorizeRequests()
                .antMatchers(HttpMethod.POST, "/api/user/account/create").permitAll()
                .antMatchers("/api/utility/**").permitAll()
                .antMatchers(HttpMethod.POST, "/login", "/logout").permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .logout()
                .logoutSuccessHandler(authenticationLogoutTokenBasedSecurityHandler)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", HttpMethod.POST.name()))
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY);
        http
                .exceptionHandling()
                .authenticationEntryPoint(authenticationTokenBasedEntryPoint);
    }

    @Override
    public void configure(WebSecurity web) {
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