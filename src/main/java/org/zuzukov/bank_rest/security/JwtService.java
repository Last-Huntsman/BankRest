package org.zuzukov.bank_rest.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zuzukov.bank_rest.dto.JwtAuthenticationDto;
import org.zuzukov.bank_rest.entity.RevokedToken;
import org.zuzukov.bank_rest.repository.RevokedTokenRepository;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@RequiredArgsConstructor
@Component
@Slf4j
public class JwtService {
    private final RevokedTokenRepository revokedTokenRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.encryption-secret}")
    private String jwtEncryptionSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    private SecretKey getEncryptionKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtEncryptionSecret));
    }

    public JwtAuthenticationDto generateJwtAuthenticationDto(String email) {
        JwtAuthenticationDto jwtAuthenticationDto = new JwtAuthenticationDto();
        jwtAuthenticationDto.setToken(generateJwtToken(email));
        jwtAuthenticationDto.setRefreshToken(generateRefreshJwtToken(email));
        return jwtAuthenticationDto;
    }

    public String generateJwtToken(String email) {
        Date expiry = Date.from(LocalDateTime.now().plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .subject(email)
                .expiration(expiry)
                .id(UUID.randomUUID().toString())
                .signWith(getSigningKey(), Jwts.SIG.HS256)
//                .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
                .compact();
    }

    public String generateRefreshJwtToken(String email) {
        Date expiry = Date.from(LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .subject(email)
                .expiration(expiry)
                .id(UUID.randomUUID().toString())
                .signWith(getSigningKey(), Jwts.SIG.HS256)
//                .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
                .compact();
    }

    public JwtAuthenticationDto refreshBaseToken(String email, String refreshToken) {
        if (!validateRefreshToken(refreshToken, email)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String hashedToken = DigestUtils.sha256Hex(refreshToken);

        if (!revokedTokenRepository.existsByToken(hashedToken)) {
            RevokedToken revoked = new RevokedToken();
            revoked.setToken(hashedToken);
            revoked.setRevokedAt(LocalDateTime.now());
            revokedTokenRepository.save(revoked);
        }

        JwtAuthenticationDto dto = new JwtAuthenticationDto();
        dto.setToken(generateJwtToken(email));
        dto.setRefreshToken(generateRefreshJwtToken(email));
        return dto;
    }

    public boolean validateJwtToken(String token) {
        try {
            String hashedToken = DigestUtils.sha256Hex(token);
            if (revokedTokenRepository.existsByToken(hashedToken)) {
                log.warn("Token is revoked");
                return false;
            }

            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (JwtException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }


    public boolean validateRefreshToken(String token, String email) {
        try {
            if (revokedTokenRepository.existsByToken(token)) {
                log.warn("Refresh token is revoked");
                return false;
            }

            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            Date expiration = claims.getExpiration();

            if (!subject.equals(email)) return false;
            if (expiration.before(new Date())) return false;

            return true;
        } catch (JwtException e) {
            log.warn("Refresh token invalid: {}", e.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
//                .decryptWith(getEncryptionKey())
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }
}
