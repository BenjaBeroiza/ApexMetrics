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

    /**
     * Endpoint de carga de telemetría desde un archivo CSV.
     * Acepta archivos multipart (≤10MB) y delega al servicio la selección del parser
     * según simulatorType (Strategy Pattern), el downsampling a 10 000 puntos y la
     * persistencia transaccional de la sesión asociada al usuario autenticado.
     * Protegido por RBAC: solo PILOT y ENGINEER pueden subir sesiones.
     *
     * Implementa RF04 — Carga de telemetría CSV.
     *
     * @param file archivo CSV multipart con los puntos de telemetría
     * @param trackId identificador del circuito asociado a la sesión
     * @param categoryId identificador de la categoría/serie de la sesión
     * @param simulatorType simulador origen del archivo ("IRACING" o "ASSETTO_CORSA"); por defecto IRACING
     * @param bestLapTime mejor vuelta declarada por el usuario en segundos (opcional)
     * @param userEmail email del usuario autenticado inyectado por Spring Security desde el JWT
     * @return 201 CREATED con SessionSummaryDTO (resumen persistido)
     */
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

    /**
     * Devuelve el historial de sesiones de telemetría del usuario autenticado.
     * Solo expone las sesiones cuyo propietario coincide con el email del JWT,
     * lo que aísla el historial entre usuarios y respeta la regla de privacidad
     * por defecto. Acceso restringido a roles PILOT y ENGINEER.
     *
     * Implementa RF05 — Historial de sesiones.
     *
     * @param userEmail email del usuario autenticado inyectado desde el SecurityContext
     * @return 200 OK con la lista de SessionSummaryDTO del usuario (puede estar vacía)
     */
    @GetMapping("/sesiones")
    @PreAuthorize("hasAnyRole('PILOT', 'ENGINEER')")
    public ResponseEntity<List<SessionSummaryDTO>> obtenerHistorial(
            @AuthenticationPrincipal String userEmail) {
        return ResponseEntity.ok(telemetryService.obtenerHistorial(userEmail));
    }

    /**
     * Elimina una sesión de telemetría propia del usuario autenticado.
     * El servicio valida que la sesión pertenezca a quien la solicita para evitar
     * que un usuario borre datos de otro (control de autorización a nivel de recurso).
     * Acceso restringido a roles PILOT y ENGINEER.
     *
     * Implementa RF06 — Eliminar sesión propia.
     *
     * @param id identificador de la sesión a eliminar
     * @param userEmail email del usuario autenticado inyectado desde el SecurityContext
     * @return 204 No Content si la eliminación tuvo éxito
     */
    @DeleteMapping("/sesiones/{id}")
    @PreAuthorize("hasAnyRole('PILOT', 'ENGINEER')")
    public ResponseEntity<Void> eliminarSesion(
            @PathVariable Long id,
            @AuthenticationPrincipal String userEmail) {
        telemetryService.eliminarSesion(id, userEmail);
        return ResponseEntity.noContent().build();
    }
}
