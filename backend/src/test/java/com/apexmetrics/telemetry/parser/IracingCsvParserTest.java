package com.apexmetrics.telemetry.parser;

import com.apexmetrics.shared.exception.CsvInvalidSchemaException;
import com.apexmetrics.telemetry.entity.TelemetryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IracingCsvParserTest {

    private final IracingCsvParser parser = new IracingCsvParser();

    private MultipartFile csv(String content) {
        return new MockMultipartFile("file", "sesion.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("getSimulatorType retorna IRACING")
    void getSimulatorType_retornaIracing() {
        assertThat(parser.getSimulatorType()).isEqualTo("IRACING");
    }

    // ── Sin posición (CSV antiguos: no deben romperse) ────────

    @Test
    @DisplayName("Trazado — CSV sin Lat/Lon deja posX/posY/geographic en null")
    void parse_sinPosicion_dejaPosicionEnNull() {
        String content = "Distance,Speed,Brake,Throttle\n0,0,0,0\n50,78.5,0,1\n";

        List<TelemetryPoint> points = parser.parse(csv(content));

        assertThat(points).hasSize(2);
        assertThat(points.get(0).getDistance()).isEqualTo(0.0);
        assertThat(points.get(1).getSpeed()).isEqualTo(78.5);
        assertThat(points).allSatisfy(p -> {
            assertThat(p.getPosX()).isNull();
            assertThat(p.getPosY()).isNull();
            assertThat(p.getGeographic()).isNull();
        });
    }

    // ── Con posición (GPS → OSM) ──────────────────────────────

    @Test
    @DisplayName("Trazado — CSV con Lat/Lon puebla posX=Lon, posY=Lat y geographic=true")
    void parse_conPosicion_poblaPosicionGeografica() {
        String content = "Distance,Speed,Brake,Throttle,Lat,Lon\n"
                + "0,0,0,0,45.61891,9.28110\n"
                + "50,78.5,0,1,45.61920,9.28150\n";

        List<TelemetryPoint> points = parser.parse(csv(content));

        assertThat(points).hasSize(2);
        TelemetryPoint first = points.get(0);
        assertThat(first.getPosX()).isEqualTo(9.28110);   // X = longitud
        assertThat(first.getPosY()).isEqualTo(45.61891);  // Y = latitud
        assertThat(first.getGeographic()).isTrue();
    }

    @Test
    @DisplayName("Trazado — el orden de columnas no afecta el mapeo de posición")
    void parse_columnasReordenadas_mapeaPosicionCorrecta() {
        String content = "Lon,Lat,Throttle,Brake,Speed,Distance\n"
                + "9.28110,45.61891,0,0,0,0\n";

        List<TelemetryPoint> points = parser.parse(csv(content));

        assertThat(points.get(0).getPosX()).isEqualTo(9.28110);
        assertThat(points.get(0).getPosY()).isEqualTo(45.61891);
        assertThat(points.get(0).getGeographic()).isTrue();
    }

    // ── Errores de esquema ────────────────────────────────────

    @Test
    @DisplayName("CSV sin columna requerida lanza CsvInvalidSchemaException")
    void parse_faltaColumnaRequerida_lanzaExcepcion() {
        String content = "Distance,Speed,Brake\n0,0,0\n";

        assertThatThrownBy(() -> parser.parse(csv(content)))
                .isInstanceOf(CsvInvalidSchemaException.class)
                .hasMessageContaining("Throttle");
    }

    @Test
    @DisplayName("CSV vacío lanza CsvInvalidSchemaException")
    void parse_csvVacio_lanzaExcepcion() {
        assertThatThrownBy(() -> parser.parse(csv("")))
                .isInstanceOf(CsvInvalidSchemaException.class);
    }

    // ── Detección de vuelta ───────────────────────────────

    @Test
    @DisplayName("Trazado — reset de Distance incrementa lapNumber (Bloque C)")
    void parse_resetDeDistance_incrementaLapNumber() {
        String content = "Distance,Speed,Brake,Throttle\n"
                + "0,0,0,0\n"
                + "500,100,0,1\n"
                + "1000,150,0,1\n"
                + "200,80,0,1\n"   // reset → vuelta 2
                + "600,120,0,0.5\n";

        List<TelemetryPoint> points = parser.parse(csv(content));

        assertThat(points).hasSize(5);
        assertThat(points.get(0).getLapNumber()).isEqualTo(1);
        assertThat(points.get(1).getLapNumber()).isEqualTo(1);
        assertThat(points.get(2).getLapNumber()).isEqualTo(1);
        assertThat(points.get(3).getLapNumber()).isEqualTo(2);
        assertThat(points.get(4).getLapNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Trazado — sin reset Distance todos los puntos en lapNumber 1")
    void parse_sinReset_todosEnLapNumber1() {
        String content = "Distance,Speed,Brake,Throttle\n"
                + "0,0,0,0\n"
                + "500,100,0,1\n"
                + "1000,150,0,1\n";

        List<TelemetryPoint> points = parser.parse(csv(content));

        assertThat(points).allSatisfy(p -> assertThat(p.getLapNumber()).isEqualTo(1));
    }
}
