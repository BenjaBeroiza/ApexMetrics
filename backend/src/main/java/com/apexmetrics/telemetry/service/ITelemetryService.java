package com.apexmetrics.telemetry.service;

import com.apexmetrics.telemetry.dto.SessionSummaryDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ITelemetryService {
    SessionSummaryDTO uploadSession(MultipartFile file, Long trackId, Long categoryId,
                                    String simulatorType, String userEmail);
}
