package com.apexmetrics.leaderboard.service;

import com.apexmetrics.leaderboard.dto.LeaderboardEntryDTO;
import com.apexmetrics.leaderboard.dto.LeaderboardFilterDTO;
import com.apexmetrics.leaderboard.repository.LeaderboardRepository;
import com.apexmetrics.leaderboard.repository.LeaderboardSpecification;
import com.apexmetrics.telemetry.entity.TelemetrySession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeaderboardService implements ILeaderboardService {

    private final LeaderboardRepository leaderboardRepository;

    /**
     * Devuelve la página del leaderboard global aplicando los filtros recibidos.
     * Construye un Pageable con page/size del DTO, arma una Specification dinámica
     * con {@link LeaderboardSpecification#withFilters(Long, Long)} y consulta el
     * repositorio. Calcula el rank global de cada entrada en función del offset de la
     * página solicitada para que la posición sea consistente entre páginas.
     *
     * Implementa RF07 — Leaderboard global paginado.
     *
     * @param filter filtros y paginación: trackId, categoryId, page, size
     * @return página de LeaderboardEntryDTO con rank calculado y datos del piloto
     */
    @Override
    public Page<LeaderboardEntryDTO> getLeaderboard(LeaderboardFilterDTO filter) {
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());
        Specification<TelemetrySession> spec = LeaderboardSpecification.withFilters(
                filter.getTrackId(), filter.getCategoryId());

        Page<TelemetrySession> sessions = leaderboardRepository.findAll(spec, pageable);

        int[] rankCounter = {filter.getPage() * filter.getSize() + 1};
        return sessions.map(s -> toDTO(s, rankCounter[0]++));
    }

    /** Mapea una TelemetrySession al DTO del leaderboard, anexando el rank global pre-calculado. */
    private LeaderboardEntryDTO toDTO(TelemetrySession session, int rank) {
        return new LeaderboardEntryDTO(
                rank,
                session.getUser().getUsername(),
                session.getUser().getCountry(),
                session.getTrack().getName(),
                session.getCategory().getName(),
                session.getBestLapTime(),
                session.getUploadedAt()
        );
    }
}
