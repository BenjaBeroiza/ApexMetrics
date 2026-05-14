package com.apexmetrics.telemetry.controller;

import com.apexmetrics.telemetry.dto.SessionSummaryDTO;
import com.apexmetrics.telemetry.service.ITelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
            @RequestParam(value = "bestLapTime", required = false) Double bestLapTime,
            @AuthenticationPrincipal String userEmail) {
        SessionSummaryDTO summary = telemetryService.uploadSession(
                file, trackId, categoryId, simulatorType, userEmail, bestLapTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @GetMapping("/sesiones")
    @PreAuthorize("hasAnyRole('PILOT', 'ENGINEER')")
    public ResponseEntity<List<SessionSummaryDTO>> obtenerHistorial(
            @AuthenticationPrincipal String userEmail) {
        return ResponseEntity.ok(telemetryService.obtenerHistorial(userEmail));
    }

    @DeleteMapping("/sesiones/{id}")
    @PreAuthorize("hasAnyRole('PILOT', 'ENGINEER')")
    public ResponseEntity<Void> eliminarSesion(
            @PathVariable Long id,
            @AuthenticationPrincipal String userEmail) {
        telemetryService.eliminarSesion(id, userEmail);
        return ResponseEntity.noContent().build();
    }
}
