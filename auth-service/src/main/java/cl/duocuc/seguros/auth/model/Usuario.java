package cl.duocuc.seguros.auth.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 20)
    private String rut;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "clave_hash", nullable = false)
    @Setter
    private String claveHash;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol")
    private Rol rol;

    // Respuestas de las preguntas de seguridad (guardadas con hash, nunca en texto plano)
    @Column(name = "respuesta1_hash")
    private String respuesta1Hash;

    @Column(name = "respuesta2_hash")
    private String respuesta2Hash;

    @Column(name = "respuesta3_hash")
    private String respuesta3Hash;

    public Usuario(String nombre, String rut, String correo, String claveHash, Rol rol) {
        this.nombre = nombre;
        this.rut = rut;
        this.correo = correo;
        this.claveHash = claveHash;
        this.rol = rol;
    }

    public void setRespuestas(String r1, String r2, String r3) {
        this.respuesta1Hash = r1;
        this.respuesta2Hash = r2;
        this.respuesta3Hash = r3;
    }

}
