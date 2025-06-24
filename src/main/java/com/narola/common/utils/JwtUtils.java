package com.narola.common.utils;

import com.narola.common.enums.RoleType;
import com.narola.entity.Role;
import com.narola.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.util.Date;

@Component
@PropertySource("classpath:application.properties")
public class JwtUtils {

/*    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private String expiration;

    private Key secretKey;
    private long expirationMs;*/

    private final Key secretKey = Keys.hmacShaKeyFor("HCiEUBO$%761OIEBOB)(&))^*^168484%^&".getBytes());
    private final long expirationMs = Duration.ofMinutes(1).toMillis();

/*    @PostConstruct
    public void init() {
        System.out.println("---------------------- jwt.secret = " + secret);
        System.out.println("---------------------- jwt.expiration = " + expiration);
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = Long.parseLong(expiration);
    }*/

    public String generateToken(User user) {
        return Jwts.builder()
                .claim("user", user)
                .claim("userId", user.getUserId())
                .claim("role", user.getRole().getRoleType().name())
                .setSubject(user.getEmailId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public User parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            //User user = new User();
            User user = claims.get("user", User.class);
            System.out.println("User :: " + user);
            user.setUserId(claims.get("userId", Integer.class));
            user.setEmailId(claims.getSubject());
            Role role = new Role();
            role.setRoleType(RoleType.valueOf(claims.get("role", String.class)));
            user.setRole(role);
            return user;
        } catch (JwtException e) {
            return null;
        }
    }
/*
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }*/
}
