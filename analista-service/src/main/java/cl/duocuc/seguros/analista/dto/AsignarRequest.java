package cl.duocuc.seguros.analista.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarRequest(@NotNull(message = "Debe seleccionar un liquidador") Long idLiquidador) {}
