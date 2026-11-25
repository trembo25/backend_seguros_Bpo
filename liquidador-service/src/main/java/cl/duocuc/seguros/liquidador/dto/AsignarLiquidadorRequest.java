package cl.duocuc.seguros.liquidador.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarLiquidadorRequest(@NotNull(message = "El idLiquidador es obligatorio") Long idLiquidador) {}
