package cl.duocuc.seguros.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El RUT es obligatorio") String rut,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String correo,
        @NotBlank(message = "La contraseña es obligatoria") String contrasena,
        @NotBlank(message = "Responde la pregunta 1") String respuesta1,
        @NotBlank(message = "Responde la pregunta 2") String respuesta2,
        @NotBlank(message = "Responde la pregunta 3") String respuesta3
) {}
