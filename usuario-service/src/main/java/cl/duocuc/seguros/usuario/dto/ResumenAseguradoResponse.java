package cl.duocuc.seguros.usuario.dto;

import java.util.Map;

// Pantalla de inicio del asegurado: sus datos, cuantos siniestros tiene y cuantos documentos le faltan
public record ResumenAseguradoResponse(PerfilResponse perfil, int totalSiniestros,
                                       Map<String, Long> porEstado, int documentosPendientes) {}
