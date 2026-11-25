package cl.duocuc.seguros.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidarRespuestasRequest(
        @NotBlank(message = "El correo es obligatorio") String correo,
        @NotBlank(message = "Responde la pregunta 1") String respuesta1,
        @NotBlank(message = "Responde la pregunta 2") String respuesta2,
        @NotBlank(message = "Responde la pregunta 3") String respuesta3
) {}
