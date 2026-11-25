package cl.duocuc.seguros.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record NuevaContrasenaRequest(
        @NotBlank(message = "Falta el token de recuperación") String resetToken,
        @NotBlank(message = "La nueva contraseña es obligatoria") String nuevaContrasena
) {}
