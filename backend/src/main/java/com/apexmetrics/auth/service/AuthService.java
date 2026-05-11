package com.apexmetrics.auth.service;

import com.apexmetrics.auth.dto.AuthResponseDTO;
import com.apexmetrics.auth.dto.LoginRequestDTO;
import com.apexmetrics.auth.dto.RegisterRequestDTO;
import com.apexmetrics.auth.entity.User;
import com.apexmetrics.auth.entity.UserRole;
import com.apexmetrics.auth.repository.UserRepository;
import com.apexmetrics.shared.exception.CredentialException;
import com.apexmetrics.shared.exception.UserAlreadyExistsException;
import com.apexmetrics.shared.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponseDTO register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            log.error("AuthService.register: email already registered — {}", dto.getEmail());
            throw new UserAlreadyExistsException("Email already registered: " + dto.getEmail());
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            log.error("AuthService.register: username already taken — {}", dto.getUsername());
            throw new UserAlreadyExistsException("Username already taken: " + dto.getUsername());
        }

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .country(dto.getCountry())
                .role(UserRole.PILOT)
                .build();
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponseDTO(token, jwtUtil.getExpirationMs(), user.getUsername(), user.getRole().name());
    }

    @Override
    public AuthResponseDTO authenticate(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> {
                    log.error("AuthService.authenticate: user not found — {}", dto.getEmail());
                    return new CredentialException("Invalid email or password");
                });

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            log.error("AuthService.authenticate: wrong password for — {}", dto.getEmail());
            throw new CredentialException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponseDTO(token, jwtUtil.getExpirationMs(), user.getUsername(), user.getRole().name());
    }
}
