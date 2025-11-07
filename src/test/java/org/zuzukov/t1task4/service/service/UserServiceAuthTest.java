package org.zuzukov.t1task4.service.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.zuzukov.bank_rest.dto.JwtAuthenticationDto;
import org.zuzukov.bank_rest.dto.UserCredentiallsDto;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.repository.RevokedTokenRepository;
import org.zuzukov.bank_rest.repository.UserRepository;
import org.zuzukov.bank_rest.service.JwtService;
import org.zuzukov.bank_rest.service.UserService;

import javax.naming.AuthenticationException;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class UserServiceAuthTest {
    @Test
    void signIn_ok() throws Exception {
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        RevokedTokenRepository revokedRepo = Mockito.mock(RevokedTokenRepository.class);
        JwtService jwtService = new JwtService(revokedRepo);
        setField(jwtService, "jwtSecret", "c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmU=");
        setField(jwtService, "jwtEncryptionSecret", "c2VjcmV0ZW5jcnlwdGlvbg==");
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);

        UserService service = new UserService(revokedRepo, userRepo, jwtService, encoder);
        User user = new User();
        user.setEmail("u@e.com");
        user.setPassword("hashed");
        when(userRepo.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(encoder.matches("p", "hashed")).thenReturn(true);

        UserCredentiallsDto dto = new UserCredentiallsDto();
        dto.setEmail("u@e.com");
        dto.setPassword("p");

        JwtAuthenticationDto tokens = service.singIn(dto);
        assertNotNull(tokens.getToken());
    }

    @Test
    void signIn_wrongPassword() {
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        RevokedTokenRepository revokedRepo = Mockito.mock(RevokedTokenRepository.class);
        JwtService jwtService = new JwtService(revokedRepo);
        try {
            setField(jwtService, "jwtSecret", "c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmU=");
            setField(jwtService, "jwtEncryptionSecret", "c2VjcmV0ZW5jcnlwdGlvbg==");
        } catch (Exception ignored) {}
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);

        UserService service = new UserService(revokedRepo, userRepo, jwtService, encoder);
        User user = new User();
        user.setEmail("u@e.com");
        user.setPassword("hashed");
        when(userRepo.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(encoder.matches("p", "hashed")).thenReturn(false);

        UserCredentiallsDto dto = new UserCredentiallsDto();
        dto.setEmail("u@e.com");
        dto.setPassword("p");

        assertThrows(AuthenticationException.class, () -> service.singIn(dto));
    }

    private static void setField(Object target, String name, String value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}


