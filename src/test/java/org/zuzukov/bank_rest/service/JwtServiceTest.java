package org.zuzukov.bank_rest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zuzukov.bank_rest.dto.JwtAuthenticationDto;
import org.zuzukov.bank_rest.repository.RevokedTokenRepository;
import org.zuzukov.bank_rest.security.JwtService;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtServiceTest {

	private RevokedTokenRepository revokedTokenRepository;
	private JwtService jwtService;

	@BeforeEach
	void setup() throws Exception {
		revokedTokenRepository = Mockito.mock(RevokedTokenRepository.class);
		jwtService = new JwtService(revokedTokenRepository);
		byte[] signKey = new byte[32];
		byte[] encKey = new byte[32];
		for (int i = 0; i < 32; i++) { signKey[i] = (byte) i; encKey[i] = (byte) (i + 1); }
		setPrivate(jwtService, "jwtSecret", Base64.getEncoder().encodeToString(signKey));
		setPrivate(jwtService, "jwtEncryptionSecret", Base64.getEncoder().encodeToString(encKey));
	}

	private static void setPrivate(Object target, String field, Object value) throws Exception {
		Field f = target.getClass().getDeclaredField(field);
		f.setAccessible(true);
		f.set(target, value);
	}

	@Test
	void generate_and_validate_token() {
		String token = jwtService.generateJwtToken("a@b.c");
		assertTrue(jwtService.validateJwtToken(token));
		assertEquals("a@b.c", jwtService.getEmailFromToken(token));
	}

	@Test
	void generate_auth_dto() {
		JwtAuthenticationDto dto = jwtService.generateJwtAuthenticationDto("x@y.z");
		assertNotNull(dto.getToken());
		assertNotNull(dto.getRefreshToken());
	}

	@Test
	void refreshBaseToken_revokesOld() {
		when(revokedTokenRepository.existsByToken(anyString())).thenReturn(false);
		String refresh = jwtService.generateRefreshJwtToken("a@b.c");
		JwtAuthenticationDto dto = jwtService.refreshBaseToken("a@b.c", refresh);
		assertNotNull(dto.getToken());
		verify(revokedTokenRepository).save(any());
	}

	@Test
	void validateRefreshToken_checksEmailAndExpiry() {
		String refresh = jwtService.generateRefreshJwtToken("a@b.c");
		assertTrue(jwtService.validateRefreshToken(refresh, "a@b.c"));
		assertFalse(jwtService.validateRefreshToken(refresh, "other@b.c"));
	}
}
