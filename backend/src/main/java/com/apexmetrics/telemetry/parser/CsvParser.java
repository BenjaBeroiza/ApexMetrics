package com.apexmetrics.telemetry.parser;

import com.apexmetrics.telemetry.entity.TelemetryPoint;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CsvParser {
    List<TelemetryPoint> parse(MultipartFile file);
    String getSimulatorType();
}
