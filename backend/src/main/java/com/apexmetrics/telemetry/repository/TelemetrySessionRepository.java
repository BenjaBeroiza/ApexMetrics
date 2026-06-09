package com.apexmetrics.telemetry.repository;

import com.apexmetrics.telemetry.entity.TelemetrySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelemetrySessionRepository extends JpaRepository<TelemetrySession, Long> {

    /**
     * Devuelve todas las sesiones de telemetría pertenecientes a un usuario concreto.
     * Spring Data JPA genera la consulta a partir del nombre del método navegando la
     * relación {@code TelemetrySession.user.id}. Es el back-end del historial de
     * sesiones del usuario autenticado.
     *
     * Contribuye a RF05 — Historial de sesiones.
     *
     * @param userId identificador del usuario dueño de las sesiones
     * @return lista de sesiones del usuario (puede estar vacía)
     */
    List<TelemetrySession> findByUserId(Long userId);
}
