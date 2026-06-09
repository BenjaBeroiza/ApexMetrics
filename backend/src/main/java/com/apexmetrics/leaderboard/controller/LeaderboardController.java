package com.apexmetrics.leaderboard.controller;

import com.apexmetrics.leaderboard.dto.LeaderboardEntryDTO;
import com.apexmetrics.leaderboard.dto.LeaderboardFilterDTO;
import com.apexmetrics.leaderboard.service.ILeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final ILeaderboardService leaderboardService;

    /**
     * Endpoint público y paginado del leaderboard global.
     * Acepta filtros opcionales por circuito y categoría que el servicio traduce a
     * {@link com.apexmetrics.leaderboard.repository.LeaderboardSpecification} (filtros
     * dinámicos vía JPA Criteria API). El tamaño de página se acota a 100 para proteger
     * el backend de respuestas excesivas. No requiere autenticación: el ranking es público.
     *
     * Implementa RF07 — Leaderboard global paginado.
     *
     * @param trackId identificador del circuito para filtrar (opcional; null = todos)
     * @param categoryId identificador de la categoría para filtrar (opcional; null = todas)
     * @param page número de página (0-indexed); por defecto 0
     * @param size tamaño de página solicitado; por defecto 20, máximo 100
     * @return 200 OK con la página de LeaderboardEntryDTO ordenada por mejor tiempo de vuelta ascendente
     */
    @GetMapping
    public ResponseEntity<Page<LeaderboardEntryDTO>> getLeaderboard(
            @RequestParam(required = false) Long trackId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LeaderboardFilterDTO filter = new LeaderboardFilterDTO();
        filter.setTrackId(trackId);
        filter.setCategoryId(categoryId);
        filter.setPage(page);
        filter.setSize(Math.min(size, 100));

        return ResponseEntity.ok(leaderboardService.getLeaderboard(filter));
    }
}
