package br.com.fiscalizacao.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "foto_ocorrencia")
@Getter
@Setter
public class FotoOcorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeArquivo;

    // URL completa ou key do S3
    @Column(nullable = false, length = 500)
    private String url;

    // opcional: bucket
    private String bucket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocorrencia_id", nullable = false)
    private Ocorrencia ocorrencia;

    public FotoOcorrencia() {}

    public FotoOcorrencia(String nomeArquivo, String url, String bucket) {
        this.nomeArquivo = nomeArquivo;
        this.url = url;
        this.bucket = bucket;
    }

    // getters e setters
}