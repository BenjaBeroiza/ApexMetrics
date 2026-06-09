package com.apexmetrics.auth.repository;

import com.apexmetrics.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su correo electrónico (clave de autenticación).
     * Spring Data JPA deriva la consulta a partir del nombre del método.
     *
     * Usado por el flujo de login (RF02) y por el filtro JWT (RF03) para resolver
     * el principal a partir del email contenido en el token.
     *
     * @param email correo electrónico del usuario a buscar
     * @return Optional con el usuario si existe, vacío si no
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica si ya existe un usuario registrado con el email indicado.
     * Permite validar unicidad antes de persistir un nuevo registro (RF01).
     *
     * @param email correo electrónico a verificar
     * @return true si el email ya está registrado, false en caso contrario
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si ya existe un usuario con el username indicado.
     * Permite validar unicidad del nombre de usuario en el flujo de registro (RF01).
     *
     * @param username nombre de usuario a verificar
     * @return true si el username ya está en uso, false en caso contrario
     */
    boolean existsByUsername(String username);
}
