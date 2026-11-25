package cl.duocuc.seguros.liquidador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearSiniestroRequest(
        @NotNull(message = "El asegurado es obligatorio") Long idAsegurado,
        @NotBlank(message = "El tipo de siniestro es obligatorio") String tipo,
        String descripcion
) {}
