package com.apexmetrics.telemetry.controller;

import com.apexmetrics.telemetry.dto.AIFeedbackDTO;
import com.apexmetrics.telemetry.dto.ComparacionDTO;
import com.apexmetrics.telemetry.dto.SessionSummaryDTO;
import com.apexmetrics.telemetry.dto.TelemetryPointDTO;
import com.apexmetrics.telemetry.dto.TrackPathDTO;
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
     * Devuelve los puntos de telemetría de una sesión propia para alimentar el
     * dashboard analítico del frontend (curvas de velocidad y frenado sincronizadas
     * por distancia recorrida). El servicio valida que la sesión pertenezca al usuario
     * autenticado, devolviendo 403 si se intenta acceder a una sesión ajena.
     * Acceso restringido a roles PILOT y ENGINEER.
     *
     * Implementa RF05 — Dashboard analítico.
     *
     * @param id identificador de la sesión cuyos puntos se solicitan
     * @param userEmail email del usuario autenticado inyectado desde el SecurityContext
     * @return 200 OK con la lista de TelemetryPointDTO de la sesión (puede estar vacía)
     */
    @GetMapping("/sesiones/{id}/puntos")
    @PreAuthorize("hasAnyRole('PILOT', 'ENGINEER')")
    public ResponseEntity<List<TelemetryPointDTO>> obtenerPuntos(
            @PathVariable Long id,
            @AuthenticationPrincipal String userEmail) {
        return ResponseEntity.ok(telemetryService.obtenerPuntos(id, userEmail));
    }

    /**
     * Devuelve los puntos de dos sesiones propias para compararlas en el frontend
     * (superposición de curvas de velocidad y frenado). El servicio valida que ambas
     * sesiones pertenezcan al usuario autenticado, devolviendo 403 si alguna es ajena.
     * Acceso restringido a roles PILOT y ENGINEER.
     *
     * Implementa RF06 — Comparación de vueltas.
     *
     * @param sessionA identificador de la primera sesión a comparar
     * @param sessionB identificador de la segunda sesión a comparar
     * @param userEmail email del usuario autenticado inyectado desde el SecurityContext
     * @return 200 OK con ComparacionDTO (puntos de la sesión A y de la sesión B)
     */
    @GetMapping("/comparacion")
    @PreAuthorize("hasAnyRole('PILOT', 'ENGINEER')")
    public ResponseEntity<ComparacionDTO> compararSesiones(
            @RequestParam("sessionA") Long sessionA,
            @RequestParam("sessionB") Long sessionB,
            @AuthenticationPrincipal String userEmail) {
        return ResponseEntity.ok(telemetryService.compararSesiones(sessionA, sessionB, userEmail));
    }

    /**
     * Devuelve la traza (recorrido 2D) de una sesión propia para dibujarla sobre el mapa
     * del frontend con Leaflet. El servicio valida que la sesión pertenezca al usuario
     * autenticado, devolviendo 403 si se intenta acceder a una sesión ajena. El flag
     * geographic indica si los puntos son coordenadas GPS (tiles OSM) o plano local
     * (CRS.Simple). Acceso restringido a roles PILOT y ENGINEER.
     *
     * Implementa el trazado de pistas (Bloque B — OpenStreetMap / Leaflet).
     *
     * @param id identificador de la sesión cuya traza se solicita
     * @param userEmail email del usuario autenticado inyectado desde el SecurityContext
     * @return 200 OK con TrackPathDTO (flag geographic + puntos con posición, puede estar vacío)
     */
    @GetMapping("/sesiones/{id}/trazado")
    @PreAuthorize("hasAnyRole('PILOT', 'ENGINEER')")
    public ResponseEntity<TrackPathDTO> obtenerTrazado(
            @PathVariable Long id,
            @AuthenticationPrincipal String userEmail) {
        return ResponseEntity.ok(telemetryService.obtenerTrazado(id, userEmail));
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

    /**
     * Genera retroalimentación de coaching mediante IA (Gemini 2.5 Flash) a partir
     * de los datos de telemetría de una sesión propia. Valida la propiedad de la
     * sesión, resume las métricas clave y delega la generación al GeminiService.
     * Acceso restringido a roles PILOT y ENGINEER.
     *
     * @param id identificador de la sesión a analizar
     * @param userEmail email del usuario autenticado inyectado desde el SecurityContext
     * @return 200 OK con AIFeedbackDTO (sessionId + texto de retroalimentación)
     */
    @GetMapping("/sesiones/{id}/feedback-ia")
    @PreAuthorize("hasAnyRole('PILOT', 'ENGINEER')")
    public ResponseEntity<AIFeedbackDTO> obtenerFeedbackIA(
            @PathVariable Long id,
            @AuthenticationPrincipal String userEmail) {
        return ResponseEntity.ok(telemetryService.obtenerFeedbackIA(id, userEmail));
    }
}
