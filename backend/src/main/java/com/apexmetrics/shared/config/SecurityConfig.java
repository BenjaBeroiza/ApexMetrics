package com.apexmetrics.shared.config;

import com.apexmetrics.shared.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Define la cadena de filtros de seguridad de la aplicación.
     * Deshabilita CSRF porque la API es stateless con JWT (no usa cookies de sesión),
     * fuerza política STATELESS para que Spring no cree HttpSession, abre los endpoints
     * públicos (autenticación y GET del leaderboard) y exige autenticación para el resto.
     * Inserta {@link JwtAuthFilter} antes del filtro estándar de usuario/contraseña para
     * resolver el principal a partir del token en cada request. La autorización fina por rol
     * se aplica en cada controlador con @PreAuthorize gracias a @EnableMethodSecurity.
     *
     * Implementa RF03 — Autorización con JWT (reglas RBAC, CSRF deshabilitado, JWT stateless).
     *
     * @param http builder de configuración HTTP provisto por Spring Security
     * @return la SecurityFilterChain construida
     * @throws Exception si la configuración HTTP falla al construirse
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/leaderboard").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Provee el codificador BCrypt usado para cifrar y verificar contraseñas.
     * Se eligió strength=12 como balance entre coste computacional y resistencia a
     * ataques de fuerza bruta (recomendación OWASP). Este bean se inyecta en el
     * servicio de autenticación para cifrar las contraseñas al registrar y validarlas
     * al iniciar sesión.
     *
     * Contribuye a RF01 — Registro de usuario (cifrado de contraseña).
     *
     * @return PasswordEncoder configurado como BCryptPasswordEncoder con strength=12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
