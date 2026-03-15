package br.com.fiscalizacao.entity;

import br.com.fiscalizacao.enums.StatusOcorrencia;
import br.com.fiscalizacao.enums.TipoOcorrencia;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOcorrencia status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOcorrencia tipo;

    // relacionamento com fotos
    @OneToMany(
            mappedBy = "ocorrencia",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<FotoOcorrencia> fotos;

    // helper para manter consistência do relacionamento
    public void adicionarFoto(FotoOcorrencia foto) {
        foto.setOcorrencia(this);
        this.fotos = new ArrayList<FotoOcorrencia>();
        this.fotos.add(foto);
    }

    public void removerFoto(FotoOcorrencia foto) {
        foto.setOcorrencia(null);
        this.fotos.remove(foto);
    }
}
