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

    /**
     * Registra un nuevo usuario en el sistema.
     * Verifica unicidad de email y username, cifra la contraseña con BCrypt
     * (strength=12 configurado en {@link com.apexmetrics.shared.config.SecurityConfig}),
     * persiste al usuario con rol PILOT y emite un JWT stateless para autenticación inmediata.
     *
     * Implementa RF01 — Registro de usuario.
     *
     * @param dto datos del registro (username, email, password, country) ya validados a nivel de controlador
     * @return AuthResponseDTO con token JWT, expiración en ms, username y rol asignado
     * @throws UserAlreadyExistsException si el email o username ya están registrados
     */
    @Override
    public AuthResponseDTO register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            log.error("AuthService.register: email already registered — {}", dto.getEmail());
            throw new UserAlreadyExistsException("El correo electrónico ya está registrado");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            log.error("AuthService.register: username already taken — {}", dto.getUsername());
            throw new UserAlreadyExistsException("El nombre de usuario ya está en uso");
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
        return new AuthResponseDTO(token, jwtUtil.getExpirationMs(), user.getUsername(), user.getRole().name(), user.getEmail(), user.getCountry());
    }

    /**
     * Autentica un usuario existente mediante email y contraseña.
     * Busca al usuario por email, compara el password en claro contra el hash BCrypt
     * almacenado y, si coinciden, genera un nuevo JWT con email y rol como claims.
     *
     * Implementa RF02 — Login de usuario.
     *
     * @param dto credenciales del usuario (email y password) ya validadas a nivel de controlador
     * @return AuthResponseDTO con token JWT, expiración en ms, username y rol
     * @throws CredentialException si el email no existe o si la contraseña no coincide con el hash
     */
    @Override
    public AuthResponseDTO authenticate(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> {
                    log.error("AuthService.authenticate: user not found — {}", dto.getEmail());
                    return new CredentialException("Correo electrónico o contraseña incorrectos");
                });

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            log.error("AuthService.authenticate: wrong password for — {}", dto.getEmail());
            throw new CredentialException("Correo electrónico o contraseña incorrectos");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponseDTO(token, jwtUtil.getExpirationMs(), user.getUsername(), user.getRole().name(), user.getEmail(), user.getCountry());
    }
}
