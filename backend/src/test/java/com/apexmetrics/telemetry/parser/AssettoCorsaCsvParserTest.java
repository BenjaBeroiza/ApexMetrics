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

class AssettoCorsaCsvParserTest {

    private final AssettoCorsaCsvParser parser = new AssettoCorsaCsvParser();

    private MultipartFile csv(String content) {
        return new MockMultipartFile("file", "sesion.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("getSimulatorType retorna ASSETTO_CORSA")
    void getSimulatorType_retornaAssettoCorsa() {
        assertThat(parser.getSimulatorType()).isEqualTo("ASSETTO_CORSA");
    }

    // ── Sin posición (CSV antiguos: no deben romperse) ────────

    @Test
    @DisplayName("Trazado — CSV sin posX/posZ deja posX/posY/geographic en null")
    void parse_sinPosicion_dejaPosicionEnNull() {
        String content = "pos,speedKmh,brake,gas\n0,0,0,0\n1,45.2,0,1\n";

        List<TelemetryPoint> points = parser.parse(csv(content));

        assertThat(points).hasSize(2);
        assertThat(points.get(0).getDistance()).isEqualTo(0.0);   // distancia = nº de fila
        assertThat(points.get(1).getDistance()).isEqualTo(1.0);
        assertThat(points.get(1).getSpeed()).isEqualTo(45.2);
        assertThat(points).allSatisfy(p -> {
            assertThat(p.getPosX()).isNull();
            assertThat(p.getPosY()).isNull();
            assertThat(p.getGeographic()).isNull();
        });
    }

    // ── Con posición (local → CRS.Simple) ─────────────────────

    @Test
    @DisplayName("Trazado — CSV con posX/posZ puebla posX, posY=posZ y geographic=false")
    void parse_conPosicion_poblaPosicionLocal() {
        String content = "pos,speedKmh,brake,gas,posX,posZ\n"
                + "0,0,0,0,100.5,200.3\n"
                + "1,45.2,0,1,118.2,205.1\n";

        List<TelemetryPoint> points = parser.parse(csv(content));

        assertThat(points).hasSize(2);
        TelemetryPoint first = points.get(0);
        assertThat(first.getPosX()).isEqualTo(100.5);   // X = coordenada local x
        assertThat(first.getPosY()).isEqualTo(200.3);   // Y = coordenada local z
        assertThat(first.getGeographic()).isFalse();
    }

    // ── Errores de esquema ────────────────────────────────────

    @Test
    @DisplayName("CSV sin columna requerida lanza CsvInvalidSchemaException")
    void parse_faltaColumnaRequerida_lanzaExcepcion() {
        String content = "pos,speedKmh,brake\n0,0,0\n";

        assertThatThrownBy(() -> parser.parse(csv(content)))
                .isInstanceOf(CsvInvalidSchemaException.class)
                .hasMessageContaining("gas");
    }

    @Test
    @DisplayName("CSV vacío lanza CsvInvalidSchemaException")
    void parse_csvVacio_lanzaExcepcion() {
        assertThatThrownBy(() -> parser.parse(csv("")))
                .isInstanceOf(CsvInvalidSchemaException.class);
    }
}
