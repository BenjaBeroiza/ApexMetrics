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

    @Override
    public String getSimulatorType() {
        return "ASSETTO_CORSA";
    }

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

            String[] row;
            int rowNum = 0;
            while ((row = reader.readNext()) != null) {
                points.add(buildTelemetryPoint(row, headerIndex, rowNum++));
            }
        } catch (CsvInvalidSchemaException e) {
            throw e;
        } catch (Exception e) {
            log.error("AssettoCorsaCsvParser.parse: failed to read CSV — {}", e.getMessage());
            throw new CsvInvalidSchemaException("Error al procesar el CSV de Assetto Corsa: " + e.getMessage(), "UNKNOWN");
        }
        return points;
    }

    private Map<String, Integer> buildHeaderIndex(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            map.put(headers[i].trim(), i);
        }
        return map;
    }

    private void validateHeaders(Map<String, Integer> headerIndex) {
        for (String required : REQUIRED_HEADERS) {
            if (!headerIndex.containsKey(required)) {
                throw new CsvInvalidSchemaException("Columna requerida no encontrada: " + required, required);
            }
        }
    }

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
    private TelemetryPoint buildTelemetryPoint(String[] row, Map<String, Integer> headerIndex, int rowNum) {
        TelemetryPoint p = new TelemetryPoint();
        p.setDistance((double) rowNum);
        p.setSpeed(parseDouble(row, headerIndex, "speedKmh"));
        p.setBrake(parseDouble(row, headerIndex, "brake"));
        p.setThrottle(parseDouble(row, headerIndex, "gas"));
        return p;
    }
    private CSVReader openCsvReader(MultipartFile file) throws Exception {
        return new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
    }
}
