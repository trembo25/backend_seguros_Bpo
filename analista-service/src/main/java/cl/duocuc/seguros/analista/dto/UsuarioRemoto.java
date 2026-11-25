package cl.duocuc.seguros.analista.dto;

// Usuario tal como lo entrega auth-service
public record UsuarioRemoto(Long id, String nombre, String rut, String correo, String rol) {}
