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
public class AssettoCorsaCsvParser implements CsvParser {

    private static final Logger log = LoggerFactory.getLogger(AssettoCorsaCsvParser.class);
    private static final List<String> REQUIRED_HEADERS = List.of("pos", "speedKmh", "brake", "gas");

    /**
     * Identifica este parser como la estrategia para archivos exportados desde Assetto Corsa.
     * Es la clave que usa {@link com.apexmetrics.telemetry.service.TelemetryService} para
     * seleccionar la estrategia adecuada según el simulador indicado por el cliente.
     *
     * @return el literal {@code "ASSETTO_CORSA"}
     */
    @Override
    public String getSimulatorType() {
        return "ASSETTO_CORSA";
    }

    /**
     * Lee un CSV exportado desde Assetto Corsa y lo convierte en TelemetryPoint internos.
     * Construye un índice por cabecera para tolerar cambios en el orden de columnas, valida
     * que estén presentes las columnas requeridas (pos, speedKmh, brake, gas) y mapea cada
     * fila al modelo común: pos → distancia (índice), speedKmh → speed, brake → brake,
     * gas → throttle. Los errores de I/O o de esquema se traducen a CsvInvalidSchemaException
     * para que la capa de manejo de errores devuelva un 400 con detalle.
     *
     * Contribuye a RF04 — Carga de telemetría CSV.
     *
     * @param file archivo CSV de Assetto Corsa recibido como multipart
     * @return lista de TelemetryPoint con los puntos crudos del archivo (antes de downsampling)
     * @throws CsvInvalidSchemaException si el archivo está vacío, falta una columna obligatoria
     *                                   o falla la lectura del CSV
     */
    @Override
    public List<TelemetryPoint> parse(MultipartFile file) {
        List<TelemetryPoint> points = new ArrayList<>();
        // DESPUÉS — el try queda así
        // DESPUÉS — el try queda así
        try (CSVReader reader = openCsvReader(file)) {

            String[] headers = reader.readNext();
            if (headers == null) {
                throw new CsvInvalidSchemaException("El archivo CSV está vacío", "ALL");
            }

            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            validateHeaders(headerIndex);

            // Posición local opcional: solo si el CSV trae las columnas posX y posZ.
            // Assetto Corsa exporta coordenadas de mundo local → plano CRS.Simple (sin tiles).
            boolean hasPos = hasAll(headerIndex, List.of("posX", "posZ"));

            // Detección de vuelta: se incrementa el contador cuando el valor de la
            // columna "pos" disminuye respecto al punto anterior, lo que indica
            // un reseteo al inicio de una nueva vuelta. Bloque C — Comparación de vueltas.
            int lapNumber = 1;
            int prevPosValue = -1;

            String[] row;
            int rowNum = 0;
            while ((row = reader.readNext()) != null) {
                int posValue = parseIntPos(row, headerIndex);
                if (prevPosValue >= 0 && posValue <= prevPosValue) {
                    lapNumber++;
                }
                prevPosValue = posValue;
                points.add(buildTelemetryPoint(row, headerIndex, rowNum++, hasPos, lapNumber));
            }
        } catch (CsvInvalidSchemaException e) {
            throw e;
        } catch (Exception e) {
            log.error("AssettoCorsaCsvParser.parse: failed to read CSV — {}", e.getMessage());
            throw new CsvInvalidSchemaException("Error al procesar el CSV de Assetto Corsa: " + e.getMessage(), "UNKNOWN");
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

    /** Verifica que el CSV contenga todas las cabeceras obligatorias; lanza CsvInvalidSchemaException si falta alguna. */
    private void validateHeaders(Map<String, Integer> headerIndex) {
        for (String required : REQUIRED_HEADERS) {
            if (!headerIndex.containsKey(required)) {
                throw new CsvInvalidSchemaException("Columna requerida no encontrada: " + required, required);
            }
        }
    }

    /** Lee una celda numérica de la fila por nombre de columna; retorna 0.0 si está ausente, vacía o no es parseable. */
    private Double parseDouble(String[] row, Map<String, Integer> idx, String col) {
        int i = idx.get(col);
        if (i >= row.length || row[i].isBlank()) return 0.0;
        try {
            return Double.parseDouble(row[i].trim());
        } catch (NumberFormatException e) {
            log.warn("AssettoCorsaCsvParser.parseDouble: valor inválido en columna '{}' → '{}', usando 0.0", col, row[i].trim());
            return 0.0;
        }
    }
    /** Construye un TelemetryPoint a partir de una fila del CSV usando el índice de cabeceras y el número de fila como distancia.
     *  Si hasPos es true, además puebla la posición local (posX→x, posZ→y) marcándola como no geográfica.
     *  lapNumber indica el número de vuelta 1-based detectado por el parser (Bloque C). */
    private TelemetryPoint buildTelemetryPoint(String[] row, Map<String, Integer> headerIndex, int rowNum, boolean hasPos, int lapNumber) {
        TelemetryPoint p = new TelemetryPoint();
        p.setDistance((double) rowNum);
        p.setLapNumber(lapNumber);
        p.setSpeed(parseDouble(row, headerIndex, "speedKmh"));
        p.setBrake(parseDouble(row, headerIndex, "brake"));
        p.setThrottle(parseDouble(row, headerIndex, "gas"));
        if (hasPos) {
            p.setPosX(parseDouble(row, headerIndex, "posX"));  // X = coordenada local x
            p.setPosY(parseDouble(row, headerIndex, "posZ"));  // Y = coordenada local z
            p.setGeographic(false);
        }
        return p;
    }
    /** Lee el valor entero de la columna "pos" para detectar reseteos de vuelta. */
    private int parseIntPos(String[] row, Map<String, Integer> idx) {
        if (!idx.containsKey("pos")) return 0;
        int i = idx.get("pos");
        if (i >= row.length || row[i].isBlank()) return 0;
        try {
            return (int) Double.parseDouble(row[i].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Abre un CSVReader (OpenCSV) sobre el stream del MultipartFile asumiendo codificación UTF-8. */
    private CSVReader openCsvReader(MultipartFile file) throws Exception {
        return new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
    }
}
