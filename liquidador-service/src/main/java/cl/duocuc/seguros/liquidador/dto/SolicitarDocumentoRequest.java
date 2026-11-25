package cl.duocuc.seguros.liquidador.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitarDocumentoRequest(
        @NotBlank(message = "El nombre del documento es obligatorio") String nombre,
        String mensaje
) {}
