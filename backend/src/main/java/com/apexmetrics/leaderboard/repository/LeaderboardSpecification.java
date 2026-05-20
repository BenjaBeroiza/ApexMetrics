package com.apexmetrics.leaderboard.repository;

import com.apexmetrics.telemetry.entity.TelemetrySession;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardSpecification {

    private LeaderboardSpecification() {}

    /**
     * Construye una {@link Specification} dinámica para filtrar sesiones del leaderboard.
     * Implementa filtros opcionales sobre la entidad TelemetrySession usando la JPA Criteria
     * API: descarta sesiones sin bestLapTime, agrega igualdades por track y category si los
     * parámetros vienen distintos de null y aplica orden ascendente por mejor tiempo de vuelta
     * (lo que produce el ranking esperado). Patrón Specification: permite componer filtros sin
     * crear un método de repositorio por cada combinación posible.
     *
     * Implementa RF07 — Leaderboard global paginado (filtros dinámicos vía JPA Criteria API).
     *
     * @param trackId identificador del circuito a filtrar (null = no aplicar)
     * @param categoryId identificador de la categoría a filtrar (null = no aplicar)
     * @return Specification componible que produce los predicados y la cláusula de orden
     */
    public static Specification<TelemetrySession> withFilters(Long trackId, Long categoryId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(root.get("bestLapTime").isNotNull());

            if (trackId != null) {
                predicates.add(cb.equal(root.get("track").get("id"), trackId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            query.orderBy(cb.asc(root.get("bestLapTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
