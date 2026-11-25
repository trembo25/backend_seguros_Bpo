package cl.duocuc.seguros.usuario.dto;

// Datos del asegurado (vienen de auth-service /auth/me)
public record PerfilResponse(Long id, String nombre, String rut, String correo, String rol) {}
