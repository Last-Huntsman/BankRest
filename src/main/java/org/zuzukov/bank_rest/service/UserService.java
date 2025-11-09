package org.zuzukov.bank_rest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.zuzukov.bank_rest.dto.*;
import org.zuzukov.bank_rest.entity.RevokedToken;
import org.zuzukov.bank_rest.entity.Role;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.exception.custom.*;
import org.zuzukov.bank_rest.repository.RevokedTokenRepository;
import org.zuzukov.bank_rest.repository.UserRepository;
import org.zuzukov.bank_rest.security.JwtService;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public JwtAuthenticationDto signIn(UserCredentiallsDto credentialsDto) {
        log.info("User signIn attempt: email={}", credentialsDto.getEmail());

        User user = userRepository.findByEmail(credentialsDto.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!passwordEncoder.matches(credentialsDto.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        log.debug("User signIn success: email={}", user.getEmail());
        return jwtService.generateJwtAuthenticationDto(user.getEmail());
    }

    public JwtAuthenticationDto refreshToken(RefreshTokenDto refreshTokenDto) {
        String refreshToken = refreshTokenDto.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }

        if (!jwtService.validateJwtToken(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        String email = jwtService.getEmailFromToken(refreshToken);
        log.info("Refresh token rotation for email={}", email);

        return jwtService.refreshBaseToken(email, refreshToken);
    }

    public UUID addUser(UserCreateDto userDto) {
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new ConflictException("User already exists");
        }

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRoles(Set.of(Role.ROLE_USER));

        userRepository.save(user);
        log.info("New user registered: email={}, id={}", user.getEmail(), user.getUserId());
        return user.getUserId();
    }

    public UserDto getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return mapToDto(user);
    }

    public UserDto getUserById(String id) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException("User ID is required");
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid UUID format");
        }

        User user = userRepository.findByUserId(uuid)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return mapToDto(user);
    }

    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Token is required");
        }

        if (!revokedTokenRepository.existsByToken(token)) {
            RevokedToken revokedToken = new RevokedToken();
            String hashedToken = DigestUtils.sha256Hex(token);
            revokedToken.setToken(hashedToken);
            revokedToken.setRevokedAt(LocalDateTime.now());
            revokedTokenRepository.save(revokedToken);
            log.info("Token revoked (sha256)={}", hashedToken);
        }
    }

    public boolean validateToken(String token, String email) {
        if (token == null || token.isBlank()) return false;
        if (email == null || email.isBlank()) return false;

        return jwtService.validateJwtToken(token) && jwtService.getEmailFromToken(token).equals(email);
    }

    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId().toString());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPassword(null);
        return dto;
    }
}
