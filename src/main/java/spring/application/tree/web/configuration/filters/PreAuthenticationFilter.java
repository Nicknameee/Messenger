package spring.application.tree.web.configuration.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PreAuthenticationFilter extends GenericFilterBean {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (authentication != null && request.getRequestURI().equals("/login")) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            ObjectMapper jacksonMapper = new ObjectMapper();
            Map<String, Object> responseBodyMap = new HashMap<>();
            responseBodyMap.put("Authenticated", true);
            responseBodyMap.put("Exception", "You are already logged in");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpStatus.CONFLICT.value());
            response.getWriter().write(jacksonMapper.writeValueAsString(responseBodyMap));
            response.getWriter().flush();
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}
