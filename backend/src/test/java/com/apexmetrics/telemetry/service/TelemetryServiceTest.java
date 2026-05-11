package com.apexmetrics.telemetry.service;

import com.apexmetrics.auth.entity.User;
import com.apexmetrics.auth.entity.UserRole;
import com.apexmetrics.auth.repository.UserRepository;
import com.apexmetrics.shared.exception.CsvInvalidSchemaException;
import com.apexmetrics.telemetry.dto.SessionSummaryDTO;
import com.apexmetrics.telemetry.entity.Category;
import com.apexmetrics.telemetry.entity.TelemetryPoint;
import com.apexmetrics.telemetry.entity.TelemetrySession;
import com.apexmetrics.telemetry.entity.Track;
import com.apexmetrics.telemetry.parser.CsvParser;
import com.apexmetrics.telemetry.repository.CategoryRepository;
import com.apexmetrics.telemetry.repository.TelemetryPointRepository;
import com.apexmetrics.telemetry.repository.TelemetrySessionRepository;
import com.apexmetrics.telemetry.repository.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock private TelemetrySessionRepository sessionRepository;
    @Mock private TelemetryPointRepository pointRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private CsvParser mockParser;

    private TelemetryService telemetryService;

    private Track track;
    private Category category;
    private User user;

    @BeforeEach
    void setUp() {
        when(mockParser.getSimulatorType()).thenReturn("IRACING");
        telemetryService = new TelemetryService(
                List.of(mockParser),
                sessionRepository, pointRepository,
                trackRepository, categoryRepository, userRepository
        );

        track = Track.builder().id(1L).name("Monza").country("Italia").build();
        category = Category.builder().id(1L).name("GT3").build();
        user = User.builder().id(1L).username("piloto01")
                .email("piloto@apexmetrics.com").role(UserRole.PILOT).build();
    }

    // ── RF04: Ingesta CSV ─────────────────────────────────────

    @Test
    @DisplayName("RF04 — uploadSession exitoso persiste sesión y puntos")
    void uploadSession_success() {
        List<TelemetryPoint> points = buildPoints(100);
        when(mockParser.parse(any())).thenReturn(points);
        when(trackRepository.findById(1L)).thenReturn(Optional.of(track));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("piloto@apexmetrics.com")).thenReturn(Optional.of(user));

        TelemetrySession savedSession = TelemetrySession.builder()
                .id(10L).user(user).track(track).category(category)
                .uploadedAt(LocalDateTime.now()).build();
        when(sessionRepository.save(any())).thenReturn(savedSession);
        when(pointRepository.saveAll(anyList())).thenReturn(points);

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);
        SessionSummaryDTO result = telemetryService.uploadSession(
                file, 1L, 1L, "IRACING", "piloto@apexmetrics.com");

        assertThat(result.getSessionId()).isEqualTo(10L);
        assertThat(result.getTrackName()).isEqualTo("Monza");
        assertThat(result.getPointsCount()).isEqualTo(100);
        verify(sessionRepository).save(any());
        verify(pointRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("RF04 — uploadSession con simulador desconocido lanza CsvInvalidSchemaException")
    void uploadSession_unknownSimulator_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> telemetryService.uploadSession(
                file, 1L, 1L, "UNKNOWN_SIM", "piloto@apexmetrics.com"))
                .isInstanceOf(CsvInvalidSchemaException.class)
                .hasMessageContaining("Unknown simulator type");
    }

    @Test
    @DisplayName("RF04 — uploadSession propaga CsvInvalidSchemaException del parser")
    void uploadSession_invalidCsvHeaders_throwsException() {
        when(mockParser.parse(any()))
                .thenThrow(new CsvInvalidSchemaException("Missing required column: Speed", "Speed"));

        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> telemetryService.uploadSession(
                file, 1L, 1L, "IRACING", "piloto@apexmetrics.com"))
                .isInstanceOf(CsvInvalidSchemaException.class)
                .hasMessageContaining("Speed");
    }

    // ── Downsample ────────────────────────────────────────────

    @Test
    @DisplayName("Downsample — lista <= 10k no se modifica")
    void downsample_belowThreshold_returnsOriginal() {
        List<TelemetryPoint> points = buildPoints(5_000);
        List<TelemetryPoint> result = telemetryService.downsample(points);
        assertThat(result).hasSize(5_000);
    }

    @Test
    @DisplayName("Downsample — lista > 10k se reduce a <= 10k puntos")
    void downsample_aboveThreshold_reducesPoints() {
        List<TelemetryPoint> points = buildPoints(25_000);
        List<TelemetryPoint> result = telemetryService.downsample(points);
        assertThat(result.size()).isLessThanOrEqualTo(10_000);
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Downsample — lista exactamente 10k no se modifica")
    void downsample_exactThreshold_returnsOriginal() {
        List<TelemetryPoint> points = buildPoints(10_000);
        List<TelemetryPoint> result = telemetryService.downsample(points);
        assertThat(result).hasSize(10_000);
    }

    private List<TelemetryPoint> buildPoints(int count) {
        List<TelemetryPoint> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            TelemetryPoint p = new TelemetryPoint();
            p.setDistance((double) i);
            p.setSpeed(100.0 + i);
            p.setBrake(0.0);
            p.setThrottle(1.0);
            list.add(p);
        }
        return list;
    }
}
