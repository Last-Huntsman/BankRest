package org.zuzukov.bank_rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zuzukov.bank_rest.dto.*;
import org.zuzukov.bank_rest.service.UserService;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создаёт пользователя, если email ещё не занят.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = UserCreateDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "firstName": "John",
                                      "lastName": "Doe",
                                      "email": "john.doe@example.com",
                                      "password": "StrongPass123"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешная регистрация"),
                    @ApiResponse(responseCode = "409", description = "Пользователь уже существует")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid UserCreateDto userDto) {
        UUID userId = userService.addUser(userDto);
        return ResponseEntity.ok("User registered with ID: " + userId);
    }

    @Operation(
            summary = "Аутентификация (логин)",
            description = "Выдаёт пару access/refresh токенов при корректных данных.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = UserCredentiallsDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "email": "john.doe@example.com",
                                      "password": "StrongPass123"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Токены выданы",
                            content = @Content(schema = @Schema(implementation = JwtAuthenticationDto.class))),
                    @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationDto> login(
            @RequestBody @Valid UserCredentiallsDto credentialsDto) {
        return ResponseEntity.ok(userService.signIn(credentialsDto));
    }

    @Operation(
            summary = "Обновление токена",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = RefreshTokenDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Новая пара токенов выдана"),
                    @ApiResponse(responseCode = "401", description = "Неверный refresh токен")
            }
    )
    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthenticationDto> refresh(
            @RequestBody @Valid RefreshTokenDto refreshTokenDto) {
        return ResponseEntity.ok(userService.refreshToken(refreshTokenDto));
    }

    @Operation(
            summary = "Выход из системы (logout)",
            description = "Помечает токен как аннулированный (revoked).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = TokenDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "token": "eyJhbGciOiJIUzI1NiJ9..."
                                    }
                                    """)
                    )
            ),
            responses = @ApiResponse(responseCode = "200", description = "Токен успешно отозван")
    )
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestBody @Valid TokenDto tokenDto) {
        userService.revokeToken(tokenDto.getToken());
        return ResponseEntity.ok("Token revoked");
    }

    @Operation(
            summary = "Получить данные пользователя по email",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Информация о пользователе"),
                    @ApiResponse(responseCode = "401", description = "Недействительный токен")
            }
    )
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        String email = principal.getName();
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }
}
