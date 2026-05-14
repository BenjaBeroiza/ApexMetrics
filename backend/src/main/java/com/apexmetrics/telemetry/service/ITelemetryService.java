package com.apexmetrics.telemetry.service;

import com.apexmetrics.telemetry.dto.SessionSummaryDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ITelemetryService {
    SessionSummaryDTO uploadSession(MultipartFile file, Long trackId, Long categoryId,
                                    String simulatorType, String userEmail, Double bestLapTime);
    List<SessionSummaryDTO> obtenerHistorial(String userEmail);
    void eliminarSesion(Long id, String userEmail);
}
