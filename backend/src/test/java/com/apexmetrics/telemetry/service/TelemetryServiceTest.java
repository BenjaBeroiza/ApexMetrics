package com.apexmetrics.telemetry.service;

import com.apexmetrics.auth.entity.User;
import com.apexmetrics.auth.entity.UserRole;
import com.apexmetrics.auth.repository.UserRepository;
import com.apexmetrics.shared.exception.CsvInvalidSchemaException;
import com.apexmetrics.telemetry.dto.SessionSummaryDTO;
import com.apexmetrics.telemetry.dto.TelemetryPointDTO;
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
import org.springframework.security.access.AccessDeniedException;

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
        lenient().when(mockParser.getSimulatorType()).thenReturn("IRACING");
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
            file, 1L, 1L, "IRACING", "piloto@apexmetrics.com", null);

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
            file, 1L, 1L, "UNKNOWN_SIM", "piloto@apexmetrics.com", null))
                .isInstanceOf(CsvInvalidSchemaException.class)
                .hasMessageContaining("Tipo de simulador no reconocido");
    }

    @Test
    @DisplayName("RF04 — uploadSession propaga CsvInvalidSchemaException del parser")
    void uploadSession_invalidCsvHeaders_throwsException() {
        when(mockParser.parse(any()))
                .thenThrow(new CsvInvalidSchemaException("Missing required column: Speed", "Speed"));

        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> telemetryService.uploadSession(
            file, 1L, 1L, "IRACING", "piloto@apexmetrics.com", null))
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

    // ── RF04: Casos de error en uploadSession ─────────────────

    @Test
    @DisplayName("RF04 — uploadSession con circuito inexistente lanza IllegalArgumentException")
    void uploadSession_trackNoExiste_lanzaExcepcion() {
        List<TelemetryPoint> points = buildPoints(10);
        when(mockParser.parse(any())).thenReturn(points);
        when(trackRepository.findById(99L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> telemetryService.uploadSession(
                file, 99L, 1L, "IRACING", "piloto@apexmetrics.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Circuito no encontrado");
    }

    @Test
    @DisplayName("RF04 — uploadSession con categoría inexistente lanza IllegalArgumentException")
    void uploadSession_categoriaNoExiste_lanzaExcepcion() {
        List<TelemetryPoint> points = buildPoints(10);
        when(mockParser.parse(any())).thenReturn(points);
        when(trackRepository.findById(1L)).thenReturn(Optional.of(track));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> telemetryService.uploadSession(
                file, 1L, 99L, "IRACING", "piloto@apexmetrics.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoría no encontrada");
    }

    @Test
    @DisplayName("RF04 — uploadSession con usuario inexistente lanza IllegalArgumentException")
    void uploadSession_usuarioNoExiste_lanzaExcepcion() {
        List<TelemetryPoint> points = buildPoints(10);
        when(mockParser.parse(any())).thenReturn(points);
        when(trackRepository.findById(1L)).thenReturn(Optional.of(track));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("desconocido@test.com")).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> telemetryService.uploadSession(
                file, 1L, 1L, "IRACING", "desconocido@test.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ── RF05: Historial de sesiones ───────────────────────────

    @Test
    @DisplayName("RF05 — obtenerHistorial retorna lista de sesiones del usuario")
    void obtenerHistorial_usuarioExistente_retornaListaDeSesiones() {
        TelemetrySession sesion = TelemetrySession.builder()
                .id(10L).user(user).track(track).category(category)
                .uploadedAt(LocalDateTime.now()).bestLapTime(85.4).build();

        when(userRepository.findByEmail("piloto@apexmetrics.com")).thenReturn(Optional.of(user));
        when(sessionRepository.findByUserId(1L)).thenReturn(List.of(sesion));

        List<SessionSummaryDTO> resultado = telemetryService.obtenerHistorial("piloto@apexmetrics.com");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getSessionId()).isEqualTo(10L);
        assertThat(resultado.get(0).getTrackName()).isEqualTo("Monza");
    }

    @Test
    @DisplayName("RF05 — obtenerHistorial con usuario inexistente lanza IllegalArgumentException")
    void obtenerHistorial_usuarioNoExiste_lanzaExcepcion() {
        when(userRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> telemetryService.obtenerHistorial("nadie@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("RF05 — obtenerHistorial retorna lista vacía si el usuario no tiene sesiones")
    void obtenerHistorial_sinSesiones_retornaListaVacia() {
        when(userRepository.findByEmail("piloto@apexmetrics.com")).thenReturn(Optional.of(user));
        when(sessionRepository.findByUserId(1L)).thenReturn(List.of());

        List<SessionSummaryDTO> resultado = telemetryService.obtenerHistorial("piloto@apexmetrics.com");

        assertThat(resultado).isEmpty();
    }

    // ── RF05: Dashboard analítico (obtenerPuntos) ─────────────

    @Test
    @DisplayName("RF05 — obtenerPuntos del dueño retorna lista de puntos mapeados")
    void obtenerPuntos_propietario_retornaPuntos() {
        TelemetrySession sesion = TelemetrySession.builder()
                .id(10L).user(user).track(track).category(category)
                .uploadedAt(LocalDateTime.now()).build();
        List<TelemetryPoint> puntos = buildPoints(3);

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(sesion));
        when(pointRepository.findBySessionId(10L)).thenReturn(puntos);

        List<TelemetryPointDTO> resultado = telemetryService.obtenerPuntos(10L, "piloto@apexmetrics.com");

        assertThat(resultado).hasSize(3);
        assertThat(resultado.get(0).getDistance()).isEqualTo(0.0);
        assertThat(resultado.get(0).getSpeed()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("RF05 — obtenerPuntos de sesión ajena lanza AccessDeniedException")
    void obtenerPuntos_usuarioAjeno_lanzaAccessDeniedException() {
        User otroUsuario = User.builder()
                .id(2L).email("otro@test.com").username("otro").role(UserRole.PILOT).build();
        TelemetrySession sesionAjena = TelemetrySession.builder()
                .id(10L).user(otroUsuario).track(track).category(category)
                .uploadedAt(LocalDateTime.now()).build();

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(sesionAjena));

        assertThatThrownBy(() -> telemetryService.obtenerPuntos(10L, "piloto@apexmetrics.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tienes permiso");

        verify(pointRepository, never()).findBySessionId(any());
    }

    @Test
    @DisplayName("RF05 — obtenerPuntos con sesión inexistente lanza IllegalArgumentException")
    void obtenerPuntos_sesionNoExiste_lanzaExcepcion() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> telemetryService.obtenerPuntos(99L, "piloto@apexmetrics.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sesión no encontrada");
    }

    // ── RF06: Eliminar sesión ─────────────────────────────────

    @Test
    @DisplayName("RF06 — eliminarSesion exitoso elimina del repositorio")
    void eliminarSesion_propietario_eliminaCorrectamente() {
        TelemetrySession sesion = TelemetrySession.builder()
                .id(10L).user(user).track(track).category(category)
                .uploadedAt(LocalDateTime.now()).build();

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(sesion));

        telemetryService.eliminarSesion(10L, "piloto@apexmetrics.com");

        verify(sessionRepository).deleteById(10L);
    }

    @Test
    @DisplayName("RF06 — eliminarSesion de sesión ajena lanza AccessDeniedException")
    void eliminarSesion_usuarioAjeno_lanzaAccessDeniedException() {
        User otroUsuario = User.builder()
                .id(2L).email("otro@test.com").username("otro").role(UserRole.PILOT).build();
        TelemetrySession sesionAjena = TelemetrySession.builder()
                .id(10L).user(otroUsuario).track(track).category(category)
                .uploadedAt(LocalDateTime.now()).build();

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(sesionAjena));

        assertThatThrownBy(() -> telemetryService.eliminarSesion(10L, "piloto@apexmetrics.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tienes permiso");

        verify(sessionRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("RF06 — eliminarSesion con id inexistente lanza IllegalArgumentException")
    void eliminarSesion_sesionNoExiste_lanzaExcepcion() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> telemetryService.eliminarSesion(99L, "piloto@apexmetrics.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sesión no encontrada");
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
