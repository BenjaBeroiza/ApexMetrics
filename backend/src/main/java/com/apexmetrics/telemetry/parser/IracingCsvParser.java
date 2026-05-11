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

    @Override
    public String getSimulatorType() {
        return "IRACING";
    }

    @Override
    public List<TelemetryPoint> parse(MultipartFile file) {
        List<TelemetryPoint> points = new ArrayList<>();
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String[] headers = reader.readNext();
            if (headers == null) {
                throw new CsvInvalidSchemaException("CSV file is empty", "ALL");
            }

            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            validateHeaders(headerIndex);

            String[] row;
            while ((row = reader.readNext()) != null) {
                TelemetryPoint p = new TelemetryPoint();
                p.setDistance(parseDouble(row, headerIndex, "Distance"));
                p.setSpeed(parseDouble(row, headerIndex, "Speed"));
                p.setBrake(parseDouble(row, headerIndex, "Brake"));
                p.setThrottle(parseDouble(row, headerIndex, "Throttle"));
                points.add(p);
            }
        } catch (CsvInvalidSchemaException e) {
            throw e;
        } catch (Exception e) {
            log.error("IracingCsvParser.parse: failed to read CSV — {}", e.getMessage());
            throw new CsvInvalidSchemaException("Failed to parse iRacing CSV: " + e.getMessage(), "UNKNOWN");
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
                throw new CsvInvalidSchemaException("Missing required column: " + required, required);
            }
        }
    }

    private Double parseDouble(String[] row, Map<String, Integer> idx, String col) {
        int i = idx.get(col);
        if (i >= row.length || row[i].isBlank()) return 0.0;
        return Double.parseDouble(row[i].trim());
    }
}
