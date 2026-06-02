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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AuthService authService;

    private RegisterRequestDTO registerDTO;
    private LoginRequestDTO loginDTO;
    private User existingUser;

    @BeforeEach
    void setUp() {
        registerDTO = new RegisterRequestDTO();
        registerDTO.setUsername("piloto01");
        registerDTO.setEmail("piloto@apexmetrics.com");
        registerDTO.setPassword("Password123");
        registerDTO.setCountry("Chile");

        loginDTO = new LoginRequestDTO();
        loginDTO.setEmail("piloto@apexmetrics.com");
        loginDTO.setPassword("Password123");

        existingUser = User.builder()
                .id(1L)
                .username("piloto01")
                .email("piloto@apexmetrics.com")
                .passwordHash("$2a$12$hashedPassword")
                .role(UserRole.PILOT)
                .build();
    }

    // ── RF01: Registro ───────────────────────────────────────

    @Test
    @DisplayName("RF01 — registro exitoso retorna token y rol PILOT")
    void register_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("mock.jwt.token");
        when(jwtUtil.getExpirationMs()).thenReturn(3600000L);

        AuthResponseDTO response = authService.register(registerDTO);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getRole()).isEqualTo("PILOT");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("RF01 — registro lanza UserAlreadyExistsException si email duplicado")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("piloto@apexmetrics.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerDTO))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("El correo electrónico ya está registrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF01 — registro lanza UserAlreadyExistsException si username duplicado")
    void register_duplicateUsername_throwsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("piloto01")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerDTO))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("El nombre de usuario ya está en uso");

        verify(userRepository, never()).save(any());
    }

    // ── RF02: Login ───────────────────────────────────────────

    @Test
    @DisplayName("RF02 — login exitoso retorna AuthResponseDTO con token")
    void authenticate_success() {
        when(userRepository.findByEmail("piloto@apexmetrics.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Password123", existingUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("mock.jwt.token");
        when(jwtUtil.getExpirationMs()).thenReturn(3600000L);

        AuthResponseDTO response = authService.authenticate(loginDTO);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getUsername()).isEqualTo("piloto01");
    }

    @Test
    @DisplayName("RF02 — login lanza CredentialException si usuario no existe")
    void authenticate_userNotFound_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(loginDTO))
                .isInstanceOf(CredentialException.class)
                .hasMessageContaining("Correo electrónico o contraseña incorrectos");
    }

    @Test
    @DisplayName("RF02 — login lanza CredentialException si contraseña incorrecta")
    void authenticate_wrongPassword_throwsException() {
        when(userRepository.findByEmail("piloto@apexmetrics.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Password123", existingUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(loginDTO))
                .isInstanceOf(CredentialException.class)
                .hasMessageContaining("Correo electrónico o contraseña incorrectos");
    }
}
