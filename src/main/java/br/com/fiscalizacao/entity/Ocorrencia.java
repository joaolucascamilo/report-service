package br.com.fiscalizacao.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ocorrencia")
@Getter
@Setter
@Builder
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime data;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "endereco_id", nullable = false)
    private Endereco endereco;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Integer tipo;

    // relacionamento com fotos
    @OneToMany(
            mappedBy = "ocorrencia",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<FotoOcorrencia> fotos = new ArrayList<>();

    public Ocorrencia() {
    }

    public Ocorrencia(LocalDateTime data, Endereco endereco, Usuario usuario) {
        this.data = data;
        this.endereco = endereco;
        this.usuario = usuario;
    }

    // helper para manter consistência do relacionamento
    public void adicionarFoto(FotoOcorrencia foto) {
        foto.setOcorrencia(this);
        this.fotos.add(foto);
    }

    public void removerFoto(FotoOcorrencia foto) {
        foto.setOcorrencia(null);
        this.fotos.remove(foto);
    }
}
