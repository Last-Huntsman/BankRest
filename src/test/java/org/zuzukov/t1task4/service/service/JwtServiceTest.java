package org.zuzukov.t1task4.service.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zuzukov.bank_rest.dto.JwtAuthenticationDto;
import org.zuzukov.bank_rest.repository.RevokedTokenRepository;
import org.zuzukov.bank_rest.service.JwtService;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class JwtServiceTest {
    private RevokedTokenRepository repo;
    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        repo = Mockito.mock(RevokedTokenRepository.class);
        jwtService = new JwtService(repo);

        setPrivate(jwtService, "jwtSecret", "c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmU=");
        setPrivate(jwtService, "jwtEncryptionSecret", "c2VjcmV0ZW5jcnlwdGlvbg==");
    }

    private static void setPrivate(Object target, String field, String value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void generate_and_validate() {
        String email = "user@example.com";
        JwtAuthenticationDto dto = jwtService.generateJwtAuthenticationDto(email);
        assertNotNull(dto.getToken());
        assertTrue(jwtService.validateJwtToken(dto.getToken()));
        assertEquals(email, jwtService.getEmailFromToken(dto.getToken()));
    }

    @Test
    void revoked_token_fails_validation() {
        String email = "user@example.com";
        String token = jwtService.generateJwtToken(email);
        String hash = DigestUtils.sha256Hex(token);
        when(repo.existsByToken(hash)).thenReturn(true);
        assertFalse(jwtService.validateJwtToken(token));
    }
}


