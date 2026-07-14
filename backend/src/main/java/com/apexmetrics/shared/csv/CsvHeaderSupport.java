package com.apexmetrics.shared.csv;

import com.apexmetrics.shared.exception.CsvInvalidSchemaException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidades compartidas por los parsers CSV de telemetría (Strategy Pattern) para el
 * manejo de cabeceras. Centraliza la lógica que antes estaba duplicada en
 * {@code IracingCsvParser} y {@code AssettoCorsaCsvParser}: construir el índice
 * cabecera→posición (tolerante al reordenamiento de columnas), validar la presencia de las
 * columnas obligatorias y comprobar columnas opcionales (posición GPS/local).
 *
 * Refactor de deuda técnica (Hito 6 · B3): reduce duplicación medida por SonarQube sin
 * alterar el comportamiento observable de los parsers.
 */
public final class CsvHeaderSupport {

    private CsvHeaderSupport() {
        // Clase de utilidades: no instanciable.
    }

    /**
     * Construye un mapa cabecera→posición para acceder a las columnas por nombre,
     * tolerando que el simulador reordene las columnas del export. Las cabeceras se
     * normalizan con {@code trim()} para absorber espacios accidentales.
     *
     * @param headers fila de cabeceras leída del CSV
     * @return mapa inmutable-por-uso de nombre de columna a su índice de posición
     */
    public static Map<String, Integer> buildHeaderIndex(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            map.put(headers[i].trim(), i);
        }
        return map;
    }

    /**
     * Verifica que el índice de cabeceras contenga todas las columnas obligatorias.
     * Lanza {@link CsvInvalidSchemaException} indicando la primera columna faltante para
     * que el handler global produzca un 400 con el detalle de la columna ausente.
     *
     * @param headerIndex índice cabecera→posición construido con {@link #buildHeaderIndex}
     * @param required    columnas obligatorias del esquema del simulador
     * @throws CsvInvalidSchemaException si falta alguna columna obligatoria
     */
    public static void validateRequiredHeaders(Map<String, Integer> headerIndex, List<String> required) {
        for (String column : required) {
            if (!headerIndex.containsKey(column)) {
                throw new CsvInvalidSchemaException("Columna requerida no encontrada: " + column, column);
            }
        }
    }

    /**
     * Indica si el índice de cabeceras contiene todas las columnas indicadas.
     * Se usa para detectar columnas opcionales de posición (Lat/Lon en iRacing,
     * posX/posZ en Assetto Corsa) sin las cuales el trazado simplemente no se dibuja.
     *
     * @param headerIndex índice cabecera→posición
     * @param columns     columnas cuya presencia conjunta se comprueba
     * @return true si todas las columnas están presentes, false si falta alguna
     */
    public static boolean hasAll(Map<String, Integer> headerIndex, List<String> columns) {
        return columns.stream().allMatch(headerIndex::containsKey);
    }
}
