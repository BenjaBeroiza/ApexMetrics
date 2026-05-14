package com.apexmetrics.leaderboard.repository;

import com.apexmetrics.telemetry.entity.TelemetrySession;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardSpecification {

    private LeaderboardSpecification() {}

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
