package org.inssg.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.inssg.backend.member.Member;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

@Component
public class JwtTokenProvider {

    @Getter
    @Value("${jwt.key}")
    private String secretKey;

    @Value("${jwt.access-token-expiration-minutes}")
    private int accessTokenExpirationMinutes;

    @Value("${jwt.refresh-token-expiration-minutes}")
    private int refreshTokenExpirationMinutes;


    public String encodedBase64SecretKey(String secretKey) {
        return Encoders.BASE64.encode(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public Key getKeyFromBase64EncodedKey(String base64EncodedSecretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(base64EncodedSecretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        return key;
    }

    public Date getTokenExpiration(int expirationMinutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, expirationMinutes);
        Date expiration = calendar.getTime();

        return expiration;
    }
    //accessToken 생성방법
    public String accessTokenAssembly(Map<String, Object> claims,
                                      String subject,
                                      Date expiration,
                                      String base64EncodedSecretKey) {

        Key key = getKeyFromBase64EncodedKey(base64EncodedSecretKey);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Calendar.getInstance().getTime())
                .setExpiration(expiration)
                .signWith(key)
                .compact();
    }

    // principal 에서 정보 뽑아내서 액세스토큰 생성
    public String createAccessToken(Member member) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", member.getEmail());
        claims.put("roles", member.getAuthority().toString());

        String subject = member.getEmail();
        Date expiration = getTokenExpiration(accessTokenExpirationMinutes);

        String base64EncodedSecretKey = encodedBase64SecretKey(secretKey);
        String accessToken = accessTokenAssembly(claims, subject, expiration, base64EncodedSecretKey);
        return accessToken;
    }

    // refreshToken 생성방법
    public String refreshTokenAssembly(String subject, Date expiration, String base64EncodedSecretKey) {

        Key key = getKeyFromBase64EncodedKey(base64EncodedSecretKey);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Calendar.getInstance().getTime())
                .setExpiration(expiration)
                .signWith(key)
                .compact();
    }
    // principal 에서 정보 뽑아내서 리프레시토큰 생성
    public String createRefreshToken(Member member) {
        String subject = member.getEmail();
        Date expiration = getTokenExpiration(refreshTokenExpirationMinutes);
        String base64EncodedSecretKey = encodedBase64SecretKey(secretKey);

        String refreshToken = refreshTokenAssembly(subject, expiration, base64EncodedSecretKey);
        return refreshToken;
    }

    //토큰 검증후 claim 반환
    public Jws<Claims>  getClaims(String jws, String base64EncodedSecretKey) {
        Key key = getKeyFromBase64EncodedKey(base64EncodedSecretKey);

            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jws);
            return claims;

    }


}
