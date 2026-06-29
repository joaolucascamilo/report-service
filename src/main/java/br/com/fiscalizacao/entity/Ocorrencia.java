package br.com.fiscalizacao.entity;

import br.com.fiscalizacao.enums.StatusOcorrencia;
import br.com.fiscalizacao.enums.TipoOcorrencia;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "ocorrencia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime data;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "endereco_id", nullable = false)
    private Endereco endereco;

    @JoinColumn(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOcorrencia status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOcorrencia tipo;

    private String nivelPrioridade;

    private Integer quantidadeDenuncias;

    private String detalhes;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ocorrencia_apoiadores", joinColumns = @JoinColumn(name = "ocorrencia_id"))
    @Column(name = "usuario_id")
    private Set<Long> apoiadores = new HashSet<>();

    // relacionamento com fotos
    @OneToMany(
            mappedBy = "ocorrencia",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<FotoOcorrencia> fotos;

    private Double latitude;
    private Double longitude;

    public void adicionarFoto(FotoOcorrencia foto) {
        if (this.fotos == null) {
            this.fotos = new ArrayList<>(); // <- inicializa só se nula
        }
        foto.setOcorrencia(this);
        this.fotos.add(foto);
    }

    public void removerFoto(FotoOcorrencia foto) {
        foto.setOcorrencia(null);
        this.fotos.remove(foto);
    }
}
