package cl.duocuc.seguros.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarContrasenaRequest(
        @NotBlank(message = "La contraseña actual es obligatoria") String contrasenaActual,
        @NotBlank(message = "La nueva contraseña es obligatoria") String nuevaContrasena
) {}
