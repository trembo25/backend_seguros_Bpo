package cl.duocuc.seguros.liquidador.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "documentos_solicitados")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentoSolicitado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_siniestro")
    private Siniestro siniestro;

    @Column(nullable = false, length = 120)
    private String nombre;

    // Mensaje del liquidador para el asegurado (aparece en sus alertas)
    @Column(length = 300)
    private String mensaje;

    @Setter
    @Column(nullable = false)
    private boolean recibido = false;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate fechaSolicitud = LocalDate.now();

    DocumentoSolicitado(Siniestro siniestro, String nombre, String mensaje) {
        this.siniestro = siniestro;
        this.nombre = nombre;
        this.mensaje = mensaje;
    }
}
