package com.apexmetrics.telemetry.parser;

import com.apexmetrics.shared.exception.CsvInvalidSchemaException;
import com.apexmetrics.telemetry.entity.TelemetryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de {@link IracingCsvParser} contra un fixture de "export real" de iRacing
 * (telemetry/iracing_monza_real.csv), generado con alta fidelidad por
 * docs/generate_samples.py: muestreo denso (~360 puntos/vuelta, 2 vueltas), columnas
 * extra de un export real (Gear, RPM) que el parser debe ignorar, y orden de columnas
 * no canónico (Distance no va primero) para ejercitar el índice por nombre de cabecera.
 *
 * Cubre B1 del Hito 6: caso feliz sobre CSV real + casos de error (cabecera inválida y
 * archivo vacío). Estas pruebas también protegen el refactor de parsers (B3).
 */
class IracingRealCsvFixtureTest {

    private final IracingCsvParser parser = new IracingCsvParser();

    private MultipartFile fixture(String classpathName) throws IOException {
        try (InputStream in = new ClassPathResource(classpathName).getInputStream()) {
            return new MockMultipartFile("file", classpathName, "text/csv", in.readAllBytes());
        }
    }

    private MultipartFile rawCsv(String content) {
        return new MockMultipartFile("file", "sesion.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    // ── Caso feliz: export real completo ─────────────────────

    @Test
    @DisplayName("CSV real de iRacing (Monza) — parsea 720 puntos con GPS y columnas extra ignoradas")
    void parse_exportRealMonza_parseaTodoElFlujo() throws IOException {
        List<TelemetryPoint> points = parser.parse(fixture("telemetry/iracing_monza_real.csv"));

        // 360 puntos/vuelta × 2 vueltas.
        assertThat(points).hasSize(720);

        // Las columnas extra (Gear, RPM) y el orden no canónico no rompen el mapeo:
        // se puebla posición geográfica desde Lat/Lon.
        assertThat(points).allSatisfy(p -> {
            assertThat(p.getGeographic()).isTrue();
            assertThat(p.getPosX()).isNotNull();   // X = Lon
            assertThat(p.getPosY()).isNotNull();   // Y = Lat
        });

        // Coordenadas dentro del recuadro de Monza (sanity de alineación GPS).
        assertThat(points).allSatisfy(p -> {
            assertThat(p.getPosY()).isBetween(45.60, 45.63);   // Lat
            assertThat(p.getPosX()).isBetween(9.27, 9.30);     // Lon
        });

        // Velocidades plausibles de un GT sobre Monza.
        assertThat(points).allSatisfy(p -> assertThat(p.getSpeed()).isBetween(60.0, 360.0));
    }

    @Test
    @DisplayName("CSV real — el reseteo de Distance por vuelta produce 2 vueltas (Bloque C)")
    void parse_exportReal_detectaDosVueltas() throws IOException {
        List<TelemetryPoint> points = parser.parse(fixture("telemetry/iracing_monza_real.csv"));

        int maxLap = points.stream().mapToInt(TelemetryPoint::getLapNumber).max().orElse(0);
        assertThat(maxLap).isEqualTo(2);
        assertThat(points.get(0).getLapNumber()).isEqualTo(1);
        assertThat(points.get(points.size() - 1).getLapNumber()).isEqualTo(2);
    }

    // ── Casos de error ───────────────────────────────────────

    @Test
    @DisplayName("CSV corrupto (cabecera sin columnas requeridas) lanza CsvInvalidSchemaException")
    void parse_fixtureCorrupto_lanzaExcepcion() throws IOException {
        assertThatThrownBy(() -> parser.parse(fixture("telemetry/corrupt.csv")))
                .isInstanceOf(CsvInvalidSchemaException.class);
    }

    @Test
    @DisplayName("CSV vacío lanza CsvInvalidSchemaException")
    void parse_archivoVacio_lanzaExcepcion() {
        assertThatThrownBy(() -> parser.parse(rawCsv("")))
                .isInstanceOf(CsvInvalidSchemaException.class)
                .hasMessageContaining("vacío");
    }
}
