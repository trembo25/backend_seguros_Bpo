package cl.duocuc.seguros.liquidador.dto;

import cl.duocuc.seguros.liquidador.model.EstadoSiniestro;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(@NotNull(message = "El estado es obligatorio") EstadoSiniestro estado) {}
