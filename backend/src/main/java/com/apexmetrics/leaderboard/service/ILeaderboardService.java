package com.apexmetrics.leaderboard.service;

import com.apexmetrics.leaderboard.dto.LeaderboardEntryDTO;
import com.apexmetrics.leaderboard.dto.LeaderboardFilterDTO;
import org.springframework.data.domain.Page;

public interface ILeaderboardService {

    /**
     * Devuelve la página del leaderboard global filtrada por los criterios indicados.
     * Las implementaciones deben aplicar los filtros opcionales (track, category) de
     * forma dinámica y devolver una página ordenada por mejor tiempo de vuelta
     * ascendente con el rank global calculado.
     *
     * Implementa RF07 — Leaderboard global paginado.
     *
     * @param filter filtros y paginación: trackId, categoryId, page, size
     * @return página de LeaderboardEntryDTO ordenada por mejor tiempo de vuelta
     */
    Page<LeaderboardEntryDTO> getLeaderboard(LeaderboardFilterDTO filter);
}
