package com.apexmetrics.telemetry.parser;

import com.apexmetrics.shared.exception.CsvInvalidSchemaException;
import com.apexmetrics.telemetry.entity.TelemetryPoint;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class IracingCsvParser implements CsvParser {

    private static final Logger log = LoggerFactory.getLogger(IracingCsvParser.class);
    private static final List<String> REQUIRED_HEADERS = List.of("Distance", "Speed", "Brake", "Throttle");

    /**
     * Identifica este parser como la estrategia para archivos exportados desde iRacing.
     * Es la clave usada por {@link com.apexmetrics.telemetry.service.TelemetryService}
     * al elegir el parser adecuado para el simulador indicado por el cliente.
     *
     * @return el literal {@code "IRACING"}
     */
    @Override
    public String getSimulatorType() {
        return "IRACING";
    }

    /**
     * Lee un CSV nativo de iRacing y lo convierte en TelemetryPoint internos.
     * Asume la presencia de las cabeceras nativas {@code Distance}, {@code Speed},
     * {@code Brake} y {@code Throttle}. Construye un índice por nombre para tolerar
     * reordenamiento de columnas. Los errores se traducen a {@link CsvInvalidSchemaException}
     * para que el handler global produzca un 400 con detalle.
     *
     * Contribuye a RF04 — Carga de telemetría CSV.
     *
     * @param file archivo CSV de iRacing recibido como multipart
     * @return lista de TelemetryPoint con los puntos crudos del archivo (antes de downsampling)
     * @throws CsvInvalidSchemaException si el archivo está vacío, falta una cabecera obligatoria
     *                                   o la lectura falla
     */
    @Override
    public List<TelemetryPoint> parse(MultipartFile file) {
        List<TelemetryPoint> points = new ArrayList<>();
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String[] headers = reader.readNext();
            if (headers == null) {
                throw new CsvInvalidSchemaException("El archivo CSV está vacío", "ALL");
            }

            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            validateHeaders(headerIndex);

            // Posición GPS opcional: solo si el CSV trae las columnas Lat y Lon.
            // iRacing exporta coordenadas geográficas → se dibujan sobre tiles OSM.
            boolean hasGps = hasAll(headerIndex, List.of("Lat", "Lon"));

            // Detección de vuelta: se incrementa el contador cuando Distance disminuye
            // respecto al valor anterior, lo que indica un reseteo al inicio de vuelta.
            // Bloque C — Comparación de vueltas.
            int lapNumber = 1;
            double prevDistance = -1.0;

            String[] row;
            while ((row = reader.readNext()) != null) {
                TelemetryPoint p = new TelemetryPoint();
                double distance = parseDouble(row, headerIndex, "Distance");
                if (prevDistance >= 0 && distance < prevDistance) {
                    lapNumber++;
                }
                prevDistance = distance;
                p.setDistance(distance);
                p.setLapNumber(lapNumber);
                p.setSpeed(parseDouble(row, headerIndex, "Speed"));
                p.setBrake(parseDouble(row, headerIndex, "Brake"));
                p.setThrottle(parseDouble(row, headerIndex, "Throttle"));
                if (hasGps) {
                    p.setPosX(parseDouble(row, headerIndex, "Lon"));  // X = longitud
                    p.setPosY(parseDouble(row, headerIndex, "Lat"));  // Y = latitud
                    p.setGeographic(true);
                }
                points.add(p);
            }
        } catch (CsvInvalidSchemaException e) {
            throw e;
        } catch (Exception e) {
            log.error("IracingCsvParser.parse: failed to read CSV — {}", e.getMessage());
            throw new CsvInvalidSchemaException("Error al procesar el CSV de iRacing: " + e.getMessage(), "UNKNOWN");
        }
        return points;
    }

    /** Indica si el índice de cabeceras contiene todas las columnas indicadas (para posición opcional). */
    private boolean hasAll(Map<String, Integer> idx, List<String> cols) {
        return cols.stream().allMatch(idx::containsKey);
    }

    /** Construye un mapa cabecera→posición para acceso a columnas por nombre, tolerando reordenamiento. */
    private Map<String, Integer> buildHeaderIndex(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            map.put(headers[i].trim(), i);
        }
        return map;
    }

    /** Verifica que estén todas las cabeceras nativas requeridas; lanza CsvInvalidSchemaException si falta alguna. */
    private void validateHeaders(Map<String, Integer> headerIndex) {
        for (String required : REQUIRED_HEADERS) {
            if (!headerIndex.containsKey(required)) {
                throw new CsvInvalidSchemaException("Columna requerida no encontrada: " + required, required);
            }
        }
    }

    /** Lee una celda numérica de la fila por nombre de columna; retorna 0.0 si la celda está ausente o vacía. */
    private Double parseDouble(String[] row, Map<String, Integer> idx, String col) {
        int i = idx.get(col);
        if (i >= row.length || row[i].isBlank()) return 0.0;
        return Double.parseDouble(row[i].trim());
    }
}
