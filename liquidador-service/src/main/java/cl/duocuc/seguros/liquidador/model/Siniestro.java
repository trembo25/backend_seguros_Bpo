package cl.duocuc.seguros.liquidador.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "siniestros")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Siniestro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_siniestro")
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 40)
    private String numero;

    @Column(name = "id_asegurado", nullable = false)
    private Long idAsegurado;

    // Copia del nombre al crear el siniestro, para no consultar a auth-service en cada listado
    @Column(name = "nombre_asegurado", nullable = false, length = 120)
    private String nombreAsegurado;

    // El analista puede reasignar el siniestro a otro liquidador
    @Setter
    @Column(name = "id_liquidador", nullable = false)
    private Long idLiquidador;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(length = 500)
    private String descripcion;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSiniestro estado = EstadoSiniestro.EN_REVISION;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion = LocalDate.now();

    @OneToMany(mappedBy = "siniestro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentoSolicitado> documentos = new ArrayList<>();

    public Siniestro(String numero, Long idAsegurado, String nombreAsegurado,
                     Long idLiquidador, String tipo, String descripcion) {
        this.numero = numero;
        this.idAsegurado = idAsegurado;
        this.nombreAsegurado = nombreAsegurado;
        this.idLiquidador = idLiquidador;
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    public DocumentoSolicitado solicitarDocumento(String nombre, String mensaje) {
        DocumentoSolicitado d = new DocumentoSolicitado(this, nombre, mensaje);
        documentos.add(d);
        return d;
    }

    public boolean tieneDocumentosPendientes() {
        return documentos.stream().anyMatch(d -> !d.isRecibido());
    }
}
