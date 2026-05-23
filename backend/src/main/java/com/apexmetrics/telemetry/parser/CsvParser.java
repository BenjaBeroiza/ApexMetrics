package com.apexmetrics.telemetry.parser;

import com.apexmetrics.telemetry.entity.TelemetryPoint;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CsvParser {

    /**
     * Convierte un CSV de telemetría en una lista de {@link TelemetryPoint}.
     * Cada implementación (Strategy Pattern) conoce el esquema de cabeceras y unidades
     * de su simulador origen y se encarga del mapeo a la representación interna común.
     *
     * Contribuye a RF04 — Carga de telemetría CSV.
     *
     * @param file archivo CSV recibido como multipart
     * @return lista de puntos de telemetría crudos (antes de downsampling)
     */
    List<TelemetryPoint> parse(MultipartFile file);

    /**
     * Identifica el simulador soportado por esta implementación de parser.
     * El servicio de telemetría usa este valor como clave de selección al elegir el
     * parser adecuado para el archivo recibido.
     *
     * @return código del simulador soportado (por ejemplo "IRACING" o "ASSETTO_CORSA")
     */
    String getSimulatorType();
}
