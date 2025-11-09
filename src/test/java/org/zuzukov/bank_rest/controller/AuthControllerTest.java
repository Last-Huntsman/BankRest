package org.zuzukov.bank_rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zuzukov.bank_rest.dto.*;
import org.zuzukov.bank_rest.exception.custom.ConflictException;
import org.zuzukov.bank_rest.exception.custom.UnauthorizedException;
import org.zuzukov.bank_rest.service.UserService;

import java.security.Principal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;
	private UserService userService;

	@BeforeEach
	void setup() {
		userService = Mockito.mock(UserService.class);
		AuthController controller = new AuthController(userService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new org.zuzukov.bank_rest.exception.GlobalExceptionHandler())
				.build();
		objectMapper = new ObjectMapper();
	}

	@Test
	void register_success() throws Exception {
		UUID userId = UUID.randomUUID();
		when(userService.addUser(any())).thenReturn(userId);

		UserCreateDto dto = new UserCreateDto();
		dto.setFirstName("John");
		dto.setLastName("Doe");
		dto.setEmail("john@example.com");
		dto.setPassword("StrongPass123");

		mockMvc.perform(post("/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isOk())
				.andExpect(content().string("User registered with ID: " + userId));
	}

	@Test
	void register_conflict() throws Exception {
		when(userService.addUser(any())).thenThrow(new ConflictException("User already exists"));

		UserCreateDto dto = new UserCreateDto();
		dto.setFirstName("John");
		dto.setLastName("Doe");
		dto.setEmail("john@example.com");
		dto.setPassword("StrongPass123");

		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("User already exists"));
	}


	@Test
	void login_success() throws Exception {
		JwtAuthenticationDto tokens = new JwtAuthenticationDto();
		tokens.setToken("access-token");
		tokens.setRefreshToken("refresh-token");
		when(userService.signIn(any())).thenReturn(tokens);

		UserCredentiallsDto dto = new UserCredentiallsDto();
		dto.setEmail("john@example.com");
		dto.setPassword("StrongPass123");

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"));
	}

	@Test
	void login_unauthorized() throws Exception {
		when(userService.signIn(any())).thenThrow(new UnauthorizedException("Invalid credentials"));

		UserCredentiallsDto dto = new UserCredentiallsDto();
		dto.setEmail("john@example.com");

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid credentials"));
	}

	@Test
	void refresh_success() throws Exception {
		JwtAuthenticationDto tokens = new JwtAuthenticationDto();
		tokens.setToken("new-access-token");
		tokens.setRefreshToken("new-refresh-token");
		when(userService.refreshToken(any())).thenReturn(tokens);

		RefreshTokenDto dto = new RefreshTokenDto();
		dto.setRefreshToken("old-refresh-token");

		mockMvc.perform(post("/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("new-access-token"))
				.andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
	}

	@Test
	void logout_success() throws Exception {
		TokenDto dto = new TokenDto();
		dto.setToken("token");

		mockMvc.perform(post("/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isOk())
				.andExpect(content().string("Token revoked"));
	}

	@Test
	void getCurrentUser_success() throws Exception {
		UserDto userDto = new UserDto();
		userDto.setEmail("john@example.com");
		when(userService.getUserByEmail(anyString())).thenReturn(userDto);

		mockMvc.perform(get("/auth/me").principal((Principal) () -> "john@example.com"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("john@example.com"));
	}
}
