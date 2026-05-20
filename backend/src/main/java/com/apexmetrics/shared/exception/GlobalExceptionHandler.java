package com.apexmetrics.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Traduce AccessDeniedException a una respuesta 403 con cuerpo estandarizado.
     * Se dispara cuando un usuario autenticado intenta operar sobre un recurso ajeno
     * o sin el rol necesario (por ejemplo intentar borrar la sesión de otro usuario).
     *
     * @param ex excepción de acceso denegado lanzada por la capa de servicio o por @PreAuthorize
     * @return 403 FORBIDDEN con cuerpo de error uniforme
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.error("AccessDeniedException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(ex.getMessage(), 403));
    }

    /**
     * Traduce CredentialException a una respuesta 401 con cuerpo estandarizado.
     * Se lanza cuando el email no existe o la contraseña no coincide con el hash
     * almacenado durante el flujo de login (RF02).
     *
     * @param ex excepción de credenciales inválidas
     * @return 401 UNAUTHORIZED con cuerpo de error uniforme
     */
    @ExceptionHandler(CredentialException.class)
    public ResponseEntity<Map<String, Object>> handleCredential(CredentialException ex) {
        log.error("CredentialException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(ex.getMessage(), 401));
    }

    /**
     * Traduce UserAlreadyExistsException a una respuesta 409 con cuerpo estandarizado.
     * Se lanza desde el flujo de registro (RF01) cuando el email o username solicitados
     * ya existen en base de datos.
     *
     * @param ex excepción de duplicidad de usuario
     * @return 409 CONFLICT con cuerpo de error uniforme
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserExists(UserAlreadyExistsException ex) {
        log.error("UserAlreadyExistsException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(ex.getMessage(), 409));
    }

    /**
     * Traduce CsvInvalidSchemaException a una respuesta 400 incluyendo la columna faltante.
     * Lanzada por los parsers durante RF04 cuando el CSV está vacío, falta una columna
     * requerida o la lectura falla. Devuelve el atributo {@code missingColumn} para que
     * el frontend pueda guiar al usuario al error específico.
     *
     * @param ex excepción de esquema inválido producida por los parsers CSV
     * @return 400 BAD REQUEST con error, columna faltante, status y timestamp
     */
    @ExceptionHandler(CsvInvalidSchemaException.class)
    public ResponseEntity<Map<String, Object>> handleCsvSchema(CsvInvalidSchemaException ex) {
        log.error("CsvInvalidSchemaException: missing column '{}' — {}", ex.getMissingColumn(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", ex.getMessage(),
                "missingColumn", ex.getMissingColumn(),
                "status", 400,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Traduce MaxUploadSizeExceededException a una respuesta 413 con mensaje localizado.
     * Spring la lanza cuando el archivo enviado al endpoint de carga (RF04) supera el
     * tamaño máximo configurado (10 MB).
     *
     * @param ex excepción de tamaño excedido lanzada por el multipart resolver
     * @return 413 PAYLOAD TOO LARGE con cuerpo de error uniforme
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        log.error("Payload too large: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(errorBody("El archivo supera el tamaño máximo permitido (10 MB)", 413));
    }

    /**
     * Traduce errores de Bean Validation a una respuesta 400 con detalle por campo.
     * Concatena los errores (campo: mensaje) en una sola cadena para que el frontend
     * pueda mostrarlos al usuario y los logs sean fáciles de auditar.
     *
     * @param ex excepción de validación de @Valid en los controladores
     * @return 400 BAD REQUEST con la lista de errores de validación
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody("Error de validación: " + details, 400));
    }

    /**
     * Traduce IllegalArgumentException a una respuesta 404 con cuerpo estandarizado.
     * Convención del proyecto: los servicios lanzan IllegalArgumentException cuando una
     * entidad referenciada no existe (sesión, circuito, categoría o usuario), por lo que
     * el handler la convierte en NOT FOUND para reflejar el caso al cliente.
     *
     * @param ex excepción lanzada cuando una entidad no es encontrada
     * @return 404 NOT FOUND con cuerpo de error uniforme
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage(), 404));
    }

    /**
     * Captura cualquier excepción no manejada para evitar fugar stack traces al cliente.
     * Registra el error completo en el log (incluyendo stack trace) y devuelve un mensaje
     * genérico al cliente, manteniendo la API consistente y segura.
     *
     * @param ex cualquier excepción no capturada por handlers específicos
     * @return 500 INTERNAL SERVER ERROR con un cuerpo genérico
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Error interno del servidor", 500));
    }

    /** Construye el cuerpo de error uniforme (error, status, timestamp) usado por todos los handlers simples. */
    private Map<String, Object> errorBody(String message, int status) {
        return Map.of(
                "error", message,
                "status", status,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
