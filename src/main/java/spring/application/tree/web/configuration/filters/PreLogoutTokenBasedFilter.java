package spring.application.tree.web.configuration.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import spring.application.tree.data.users.security.UserDetailsImplementationService;
import spring.application.tree.data.users.security.token.AuthorizationTokenUtility;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Profile(value = {"token", "boots_token"})
public class PreLogoutTokenBasedFilter extends GenericFilterBean {
    private final UserDetailsImplementationService userDetailsImplementationService;
    private final AuthorizationTokenUtility authorizationTokenUtility;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (request.getRequestURI().equals("/logout")) {
            String authorizationHeaderValue = request.getHeader("Authorization");
            if (authorizationHeaderValue != null && authorizationHeaderValue.startsWith("Bearer ")) {
                String authorizationToken = authorizationHeaderValue.substring(7);
                String username = authorizationTokenUtility.getUsernameFromToken(authorizationToken);
                UserDetails userDetails = userDetailsImplementationService.loadUserByUsername(username);
                if (authorizationToken.isEmpty() || !authorizationTokenUtility.validateToken(authorizationToken, userDetails)) {
                    HttpServletResponse response = (HttpServletResponse) servletResponse;
                    ObjectMapper jacksonMapper = new ObjectMapper();
                    Map<String, Object> responseBodyMap = new HashMap<>();
                    responseBodyMap.put("authenticated", false);
                    responseBodyMap.put("exception", "You are not authenticated");
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.getWriter().write(jacksonMapper.writeValueAsString(responseBodyMap));
                    response.getWriter().flush();
                }
            } else {
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                ObjectMapper jacksonMapper = new ObjectMapper();
                Map<String, Object> responseBodyMap = new HashMap<>();
                responseBodyMap.put("authenticated", false);
                responseBodyMap.put("exception", "You are not authenticated");
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.getWriter().write(jacksonMapper.writeValueAsString(responseBodyMap));
                response.getWriter().flush();
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
