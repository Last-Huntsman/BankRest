package org.zuzukov.bank_rest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.zuzukov.bank_rest.dto.*;
import org.zuzukov.bank_rest.entity.RevokedToken;
import org.zuzukov.bank_rest.entity.Role;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.exception.*;
import org.zuzukov.bank_rest.repository.RevokedTokenRepository;
import org.zuzukov.bank_rest.repository.UserRepository;
import org.zuzukov.bank_rest.service.JwtService;
import org.zuzukov.bank_rest.service.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

	private RevokedTokenRepository revokedTokenRepository;
	private UserRepository userRepository;
	private JwtService jwtService;
	private PasswordEncoder passwordEncoder;
	private UserService userService;

	@BeforeEach
	void setup() {
		revokedTokenRepository = Mockito.mock(RevokedTokenRepository.class);
		userRepository = Mockito.mock(UserRepository.class);
		jwtService = Mockito.mock(JwtService.class);
		passwordEncoder = Mockito.mock(PasswordEncoder.class);
		userService = new UserService(revokedTokenRepository, userRepository, jwtService, passwordEncoder);
	}

	@Test
	void signIn_success() {
		User user = new User();
		user.setEmail("a@b.c");
		user.setPassword("encoded");
		when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("pwd", "encoded")).thenReturn(true);
		when(jwtService.generateJwtAuthenticationDto("a@b.c")).thenReturn(new JwtAuthenticationDto());

		UserCredentiallsDto dto = new UserCredentiallsDto();
		dto.setEmail("a@b.c");
		dto.setPassword("pwd");
		JwtAuthenticationDto res = userService.signIn(dto);
		assertNotNull(res);
	}

	@Test
	void signIn_userNotFound() {
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
		UserCredentiallsDto dto = new UserCredentiallsDto();
		dto.setEmail("no@x");
		assertThrows(UnauthorizedException.class, () -> userService.signIn(dto));
	}

	@Test
	void signIn_badPassword() {
		User user = new User();
		user.setEmail("a@b.c");
		user.setPassword("encoded");
		when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
		UserCredentiallsDto dto = new UserCredentiallsDto();
		dto.setEmail("a@b.c");
		dto.setPassword("bad");
		assertThrows(UnauthorizedException.class, () -> userService.signIn(dto));
	}

	@Test
	void refreshToken_success() {
		RefreshTokenDto dto = new RefreshTokenDto();
		dto.setRefreshToken("rt");
		when(jwtService.validateJwtToken("rt")).thenReturn(true);
		when(jwtService.getEmailFromToken("rt")).thenReturn("a@b.c");
		when(jwtService.refreshBaseToken("a@b.c", "rt")).thenReturn(new JwtAuthenticationDto());
		JwtAuthenticationDto res = userService.refreshToken(dto);
		assertNotNull(res);
	}

	@Test
	void refreshToken_missing() {
		RefreshTokenDto dto = new RefreshTokenDto();
		assertThrows(BadRequestException.class, () -> userService.refreshToken(dto));
	}

	@Test
	void refreshToken_invalid() {
		RefreshTokenDto dto = new RefreshTokenDto();
		dto.setRefreshToken("rt");
		when(jwtService.validateJwtToken("rt")).thenReturn(false);
		assertThrows(InvalidRefreshTokenException.class, () -> userService.refreshToken(dto));
	}

	@Test
	void addUser_missingEmail() {
		UserCreateDto dto = new UserCreateDto();
		dto.setPassword("pwd");
		assertThrows(BadRequestException.class, () -> userService.addUser(dto));
	}

	@Test
	void addUser_conflict() {
		UserCreateDto dto = new UserCreateDto();
		dto.setEmail("a@b.c");
		dto.setPassword("pwd");
		when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(new User()));
		assertThrows(ConflictException.class, () -> userService.addUser(dto));
	}

	@Test
	void getUserByEmail_success() {
		User u = new User();
		u.setUserId(UUID.randomUUID());
		u.setEmail("a@b.c");
		when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(u));
		UserDto dto = userService.getUserByEmail("a@b.c");
		assertEquals("a@b.c", dto.getEmail());
	}

	@Test
	void getUserByEmail_missing() {
		assertThrows(BadRequestException.class, () -> userService.getUserByEmail(" "));
	}

	@Test
	void getUserById_success() {
		UUID id = UUID.randomUUID();
		User u = new User();
		u.setUserId(id);
		u.setEmail("a@b.c");
		when(userRepository.findByUserId(id)).thenReturn(Optional.of(u));
		UserDto dto = userService.getUserById(id.toString());
		assertEquals("a@b.c", dto.getEmail());
	}

	@Test
	void getUserById_invalidUuid() {
		assertThrows(BadRequestException.class, () -> userService.getUserById("bad"));
	}

	@Test
	void revokeToken_savesHashOnce() {
		when(revokedTokenRepository.existsByToken(anyString())).thenReturn(false);
		userService.revokeToken("tok");
		verify(revokedTokenRepository).save(any(RevokedToken.class));
	}

	@Test
	void validateToken_delegates() {
		when(jwtService.validateJwtToken("t")).thenReturn(true);
		when(jwtService.getEmailFromToken("t")).thenReturn("a@b.c");
		assertTrue(userService.validateToken("t", "a@b.c"));
	}
}
