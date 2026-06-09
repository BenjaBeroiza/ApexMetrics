package com.apexmetrics.shared.security;

import com.apexmetrics.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    /**
     * Intercepta cada request HTTP una sola vez y, si trae un JWT válido, puebla el SecurityContext.
     * Extrae el token del header {@code Authorization: Bearer ...}, valida su firma y expiración
     * con {@link JwtUtil}, resuelve al usuario en base de datos y registra un
     * {@link UsernamePasswordAuthenticationToken} con el rol como {@code ROLE_<X>} para que
     * los @PreAuthorize de los controladores funcionen. Si no hay token o el token es inválido,
     * la request continúa sin autenticación y serán las reglas de Spring Security las que
     * decidan si responde 401/403.
     *
     * Implementa RF03 — Autorización con JWT.
     *
     * @param request request HTTP entrante
     * @param response response HTTP a continuar
     * @param filterChain cadena de filtros a la que se delega el procesamiento
     * @throws ServletException si la cadena de filtros falla
     * @throws IOException si ocurre un error de I/O en el procesamiento
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (!jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            userRepository.findByEmail(email).ifPresent(user -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        filterChain.doFilter(request, response);
    }
}
