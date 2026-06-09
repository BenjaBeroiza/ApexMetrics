package com.apexmetrics.leaderboard.service;

import com.apexmetrics.auth.entity.User;
import com.apexmetrics.auth.entity.UserRole;
import com.apexmetrics.leaderboard.dto.LeaderboardEntryDTO;
import com.apexmetrics.leaderboard.dto.LeaderboardFilterDTO;
import com.apexmetrics.leaderboard.repository.LeaderboardRepository;
import com.apexmetrics.telemetry.entity.Category;
import com.apexmetrics.telemetry.entity.TelemetrySession;
import com.apexmetrics.telemetry.entity.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock private LeaderboardRepository leaderboardRepository;
    @InjectMocks private LeaderboardService leaderboardService;

    private TelemetrySession session1;
    private TelemetrySession session2;

    @BeforeEach
    void setUp() {
        User user1 = User.builder().id(1L).username("piloto01").country("Chile").role(UserRole.PILOT).build();
        User user2 = User.builder().id(2L).username("piloto02").country("Argentina").role(UserRole.PILOT).build();
        Track monza = Track.builder().id(1L).name("Monza").country("Italia").build();
        Category gt3 = Category.builder().id(1L).name("GT3").build();

        session1 = TelemetrySession.builder()
                .id(1L).user(user1).track(monza).category(gt3)
                .uploadedAt(LocalDateTime.now()).bestLapTime(95.432)
                .build();
        session2 = TelemetrySession.builder()
                .id(2L).user(user2).track(monza).category(gt3)
                .uploadedAt(LocalDateTime.now()).bestLapTime(96.871)
                .build();
    }

    // ── RF07: Leaderboard ─────────────────────────────────────

    @Test
    @DisplayName("RF07 — getLeaderboard sin filtros retorna todas las sesiones paginadas")
    void getLeaderboard_noFilters_returnsPaginatedSessions() {
        LeaderboardFilterDTO filter = buildFilter(null, null, 0, 20);
        Page<TelemetrySession> page = new PageImpl<>(List.of(session1, session2),
                PageRequest.of(0, 20), 2);

        when(leaderboardRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(filter);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("piloto01");
        assertThat(result.getContent().get(0).getRank()).isEqualTo(1);
        assertThat(result.getContent().get(1).getRank()).isEqualTo(2);
    }

    @Test
    @DisplayName("RF07 — getLeaderboard con filtro de circuito delega correctamente")
    void getLeaderboard_withTrackFilter_delegatesToRepository() {
        LeaderboardFilterDTO filter = buildFilter(1L, null, 0, 20);
        Page<TelemetrySession> page = new PageImpl<>(List.of(session1),
                PageRequest.of(0, 20), 1);

        when(leaderboardRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(filter);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTrackName()).isEqualTo("Monza");
        verify(leaderboardRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("RF07 — getLeaderboard con filtros combinados (circuito + categoría)")
    void getLeaderboard_withCombinedFilters_returnsFilteredResults() {
        LeaderboardFilterDTO filter = buildFilter(1L, 1L, 0, 10);
        Page<TelemetrySession> page = new PageImpl<>(List.of(session1, session2),
                PageRequest.of(0, 10), 2);

        when(leaderboardRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(filter);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("GT3");
    }

    @Test
    @DisplayName("RF07 — getLeaderboard vacío retorna página vacía")
    void getLeaderboard_noSessions_returnsEmptyPage() {
        LeaderboardFilterDTO filter = buildFilter(99L, null, 0, 20);
        Page<TelemetrySession> emptyPage = new PageImpl<>(List.of(),
                PageRequest.of(0, 20), 0);

        when(leaderboardRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(emptyPage);

        Page<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(filter);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("RF07 — rank se calcula correctamente en página 2")
    void getLeaderboard_secondPage_rankStartsCorrectly() {
        LeaderboardFilterDTO filter = buildFilter(null, null, 1, 20);
        Page<TelemetrySession> page = new PageImpl<>(List.of(session1),
                PageRequest.of(1, 20), 21);

        when(leaderboardRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(filter);

        assertThat(result.getContent().get(0).getRank()).isEqualTo(21);
    }

    private LeaderboardFilterDTO buildFilter(Long trackId, Long categoryId, int page, int size) {
        LeaderboardFilterDTO f = new LeaderboardFilterDTO();
        f.setTrackId(trackId);
        f.setCategoryId(categoryId);
        f.setPage(page);
        f.setSize(size);
        return f;
    }
}
