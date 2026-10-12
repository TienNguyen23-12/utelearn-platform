package vn.edu.ute.utelearn.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import vn.edu.ute.utelearn.config.UtelearnProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final UtelearnProperties properties;
    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        String secret = properties.getJwt().getSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            byte[] padded = new byte[64];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String username, String email, List<String> roles) {
        return generateAccessToken(username, email, roles, List.of());
    }

    public String generateAccessToken(String username, String email, List<String> roles, List<String> permissions) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getJwt().getAccessTokenExpirationMs(), ChronoUnit.MILLIS);

        return Jwts.builder()
            .subject(username)
            .claim("email", email)
            .claim("roles", roles)
            .claim("permissions", permissions)
            .claim("type", "ACCESS")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact();
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getJwt().getRefreshTokenExpirationMs(), ChronoUnit.MILLIS);

        return Jwts.builder()
            .subject(username)
            .claim("type", "REFRESH")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("roles", List.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("permissions", List.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT Token: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        String prefix = properties.getJwt().getHeaderPrefix();
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(prefix)) {
            return bearerToken.substring(prefix.length()).trim();
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String cookieName = properties.getJwt().getCookieName();
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    public void setTokenCookie(HttpServletResponse response, String token, boolean rememberMe) {
        int maxAgeSeconds = rememberMe 
            ? (int) (properties.getJwt().getRefreshTokenExpirationMs() / 1000)
            : (int) (properties.getJwt().getAccessTokenExpirationMs() / 1000);

        Cookie cookie = new Cookie(properties.getJwt().getCookieName(), token);
        cookie.setHttpOnly(true); 
        cookie.setSecure(false); 
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
        response.addHeader("Set-Cookie", String.format(
            "%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",
            properties.getJwt().getCookieName(), token, maxAgeSeconds
        ));
    }

    public void clearTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(properties.getJwt().getCookieName(), null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        response.addHeader("Set-Cookie", String.format(
            "%s=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax",
            properties.getJwt().getCookieName()
        ));
    }
}
