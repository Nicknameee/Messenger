package spring.application.tree.data.users.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;

@Component
@PropertySource("classpath:token.properties")
@Profile(value = "token")
public class AuthorizationTokenUtility {
    /**
     * Username -> Token
     */
    private final Map<String, String> blacklistedTokens = new HashMap<>();
    @Value("${token.duration}")
    private int tokenValidityDuration;
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public void blacklistToken(String token) {
        String username = getUsernameFromToken(token);
        blacklistedTokens.put(username, token);
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    private boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        Date now = Date.from(LocalDateTime.now(Clock.systemDefaultZone()).toInstant(ZoneOffset.ofTotalSeconds(0)));
        return expiration.before(now) || blacklistedTokens.containsValue(token);
    }

    public String generateToken(UserDetails userDetails) {
        if (blacklistedTokens.containsKey(userDetails.getUsername()) && !isTokenExpired(blacklistedTokens.get(userDetails.getUsername()))) {
            return blacklistedTokens.get(userDetails.getUsername());
        }
        String token = Jwts.builder()
                           .setClaims(new HashMap<>())
                           .setSubject(userDetails.getUsername())
                           .setIssuedAt(Date.from(LocalDateTime.now(Clock.systemDefaultZone()).toInstant(ZoneOffset.ofTotalSeconds(0))))
                           .setExpiration(Date.from(LocalDateTime.now(Clock.systemDefaultZone()).plusSeconds(tokenValidityDuration).toInstant(ZoneOffset.ofTotalSeconds(0))))
                           .signWith(key)
                           .compact();
        blacklistedTokens.remove(userDetails.getUsername());
        return token;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = getUsernameFromToken(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token) && !blacklistedTokens.containsValue(token);
    }
}
