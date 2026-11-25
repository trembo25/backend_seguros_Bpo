package cl.duocuc.seguros.auth.dto;

public record LoginResponse(String token, UsuarioResponse usuario) {}
