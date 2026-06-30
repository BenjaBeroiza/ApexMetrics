package com.apexmetrics.telemetry.service;

import com.apexmetrics.telemetry.dto.AIFeedbackDTO;
import com.apexmetrics.telemetry.dto.ComparacionDTO;
import com.apexmetrics.telemetry.dto.SessionSummaryDTO;
import com.apexmetrics.telemetry.dto.TelemetryPointDTO;
import com.apexmetrics.telemetry.dto.TrackPathDTO;
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
     * Devuelve los puntos de telemetría de una sesión propia para graficar las
     * curvas de velocidad y frenado sincronizadas por distancia recorrida.
     * Las implementaciones deben validar que el solicitante sea el dueño de la
     * sesión para evitar el acceso a datos ajenos (control de autorización a
     * nivel de recurso).
     *
     * Implementa RF05 — Dashboard analítico.
     *
     * @param sessionId identificador de la sesión cuyos puntos se solicitan
     * @param userEmail email del usuario autenticado, dueño esperado de la sesión
     * @return lista de TelemetryPointDTO ordenada por distancia (puede estar vacía)
     */
    List<TelemetryPointDTO> obtenerPuntos(Long sessionId, String userEmail);

    /**
     * Devuelve los puntos de dos sesiones propias del usuario para compararlas.
     * Las implementaciones deben validar que ambas sesiones pertenezcan al solicitante
     * (control de autorización a nivel de recurso); si alguna es ajena se rechaza la
     * operación completa.
     *
     * Implementa RF06 — Comparación de vueltas.
     *
     * @param sessionAId identificador de la primera sesión a comparar
     * @param sessionBId identificador de la segunda sesión a comparar
     * @param userEmail email del usuario autenticado, dueño esperado de ambas sesiones
     * @return ComparacionDTO con los puntos de la sesión A y de la sesión B
     */
    ComparacionDTO compararSesiones(Long sessionAId, Long sessionBId, String userEmail);

    /**
     * Devuelve la traza (recorrido 2D) de una sesión propia para dibujarla sobre el
     * mapa del frontend (Leaflet). Las implementaciones deben validar que la sesión
     * pertenezca al solicitante (control de autorización a nivel de recurso; 403 si es
     * ajena), conservar únicamente los puntos que tengan posición (posX/posY != null) y
     * mapearlos a TrackPointDTO. El flag geographic se toma del primer punto con posición;
     * si la sesión no tiene posición, la lista de puntos queda vacía.
     *
     * Implementa el trazado de pistas (Bloque B — OpenStreetMap / Leaflet).
     *
     * @param sessionId identificador de la sesión cuya traza se solicita
     * @param userEmail email del usuario autenticado, dueño esperado de la sesión
     * @return TrackPathDTO con el flag geographic y los puntos con posición (puede estar vacío)
     */
    TrackPathDTO obtenerTrazado(Long sessionId, String userEmail);

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

    /**
     * Genera retroalimentación de coaching mediante IA (Gemini 2.5 Flash) a partir
     * de la telemetría de una sesión propia del usuario. Valida la propiedad de la
     * sesión antes de procesar los datos.
     *
     * @param sessionId identificador de la sesión a analizar
     * @param userEmail email del usuario autenticado, dueño esperado de la sesión
     * @return AIFeedbackDTO con el sessionId y el texto de retroalimentación generado
     */
    AIFeedbackDTO obtenerFeedbackIA(Long sessionId, String userEmail);
}
