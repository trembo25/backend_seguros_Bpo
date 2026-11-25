package cl.duocuc.seguros.auth.dto;

import cl.duocuc.seguros.auth.model.Usuario;

public record UsuarioResponse(Long id, String nombre, String rut, String correo, String rol) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getRut(), u.getCorreo(), u.getRol().getNombre());
    }
}
