package cl.duocuc.seguros.liquidador.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

// Mismo formato de error que auth-service: {"error": "mensaje"}
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }

    // Por ejemplo ?estado=OTRO (no existe en el enum)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Valor inválido para el parámetro " + e.getName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> jsonInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "El cuerpo de la petición no es un JSON válido o el estado no existe"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> general(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor"));
    }
}
