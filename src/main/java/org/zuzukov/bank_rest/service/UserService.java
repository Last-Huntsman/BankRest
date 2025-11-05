package org.zuzukov.bank_rest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.zuzukov.bank_rest.dto.*;
import org.zuzukov.bank_rest.entity.RevokedToken;
import org.zuzukov.bank_rest.entity.Role;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.repository.RevokedTokenRepository;
import org.zuzukov.bank_rest.exception.ConflictException;
import org.zuzukov.bank_rest.repository.UserRepository;
import org.zuzukov.bank_rest.security.UserDetail;

import javax.naming.AuthenticationException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class UserService implements UserDetailsService {
        private final RevokedTokenRepository revokedTokenRepository;
        private final UserRepository userRepository;
        private final JwtService jwtService;
        private final PasswordEncoder passwordEncoder;

        public JwtAuthenticationDto singIn(UserCredentiallsDto userCredentialsDto) throws AuthenticationException {
            log.info("User signIn attempt: email={}", userCredentialsDto.getEmail());
            Optional<User> userOpt = userRepository.findByEmail(userCredentialsDto.getEmail());
            if (userOpt.isEmpty()) throw new AuthenticationException("User not found");

            User user = userOpt.get();

            if (!passwordEncoder.matches(userCredentialsDto.getPassword(), user.getPassword())) {
                throw new AuthenticationException("Invalid credentials");
            }

            JwtAuthenticationDto tokens = jwtService.generateJwtAuthenticationDto(user.getEmail());
            log.debug("User signIn success: email={}", user.getEmail());
            return tokens;
        }

        public JwtAuthenticationDto refreshToken(RefreshTokenDto refreshTokenDto) throws Exception {
            String email = jwtService.getEmailFromToken(refreshTokenDto.getRefreshToken());
            if (!jwtService.validateJwtToken(refreshTokenDto.getRefreshToken())) {
                throw new Exception("Invalid or expired refresh token");
            }
            log.info("Refresh token rotation for email={}", email);
            return jwtService.refreshBaseToken(email, refreshTokenDto.getRefreshToken());
        }

        public UserDto getUserById(String id) throws ChangeSetPersister.NotFoundException {
            UUID uuid = UUID.fromString(id);
            User user = userRepository.findByUserId(uuid)
                    .orElseThrow(ChangeSetPersister.NotFoundException::new);
            return mapToDto(user);
        }

        public UserDto getUserByEmail(String email) throws ChangeSetPersister.NotFoundException {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(ChangeSetPersister.NotFoundException::new);
            return mapToDto(user);
        }

        public UUID addUser(UserCreateDto userDto) {
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

        private UserDto mapToDto(User user) {
            UserDto dto = new UserDto();
            dto.setUserId(user.getUserId().toString());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setPassword(user.getPassword());
            return dto;
        }
        public void revokeToken(String token) {
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
            return jwtService.validateJwtToken(token)
           && jwtService.getEmailFromToken(token).equals(email);
        }

        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
            return userRepository.findByEmail(email)
                    .map(UserDetail::new)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        }
    }


