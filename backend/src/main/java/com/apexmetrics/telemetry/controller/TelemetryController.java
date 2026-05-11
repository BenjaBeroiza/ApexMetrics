package com.apexmetrics.telemetry.controller;

import com.apexmetrics.telemetry.dto.SessionSummaryDTO;
import com.apexmetrics.telemetry.service.ITelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final ITelemetryService telemetryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PILOT', 'ENGINEER')")
    public ResponseEntity<SessionSummaryDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("trackId") Long trackId,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "simulatorType", defaultValue = "IRACING") String simulatorType,
            @AuthenticationPrincipal String userEmail) {
        SessionSummaryDTO summary = telemetryService.uploadSession(
                file, trackId, categoryId, simulatorType, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }
}
