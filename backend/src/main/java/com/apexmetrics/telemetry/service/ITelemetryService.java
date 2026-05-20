package com.apexmetrics.telemetry.service;

import com.apexmetrics.telemetry.dto.SessionSummaryDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ITelemetryService {

    /**
     * Procesa un archivo CSV de telemetría y persiste la sesión asociada al usuario.
     * Las implementaciones deben elegir el parser apropiado según simulatorType
     * (Strategy Pattern), aplicar downsampling al límite definido (RNF04) y persistir
     * la sesión y sus puntos transaccionalmente.
     *
     * Implementa RF04 — Carga de telemetría CSV.
     *
     * @param file archivo CSV multipart con los datos crudos
     * @param trackId identificador del circuito
     * @param categoryId identificador de la categoría
     * @param simulatorType simulador origen ("IRACING" o "ASSETTO_CORSA")
     * @param userEmail email del usuario autenticado dueño de la sesión
     * @param bestLapTime mejor vuelta declarada en segundos (opcional)
     * @return SessionSummaryDTO con el resumen de la sesión persistida
     */
    SessionSummaryDTO uploadSession(MultipartFile file, Long trackId, Long categoryId,
                                    String simulatorType, String userEmail, Double bestLapTime);

    /**
     * Devuelve el historial de sesiones propias del usuario autenticado.
     *
     * Implementa RF05 — Historial de sesiones.
     *
     * @param userEmail email del usuario dueño de las sesiones
     * @return lista de SessionSummaryDTO con las sesiones del usuario
     */
    List<SessionSummaryDTO> obtenerHistorial(String userEmail);

    /**
     * Elimina una sesión propia del usuario autenticado.
     * Las implementaciones deben validar que el solicitante sea el dueño del recurso.
     *
     * Implementa RF06 — Eliminar sesión propia.
     *
     * @param id identificador de la sesión a eliminar
     * @param userEmail email del usuario autenticado
     */
    void eliminarSesion(Long id, String userEmail);
}
