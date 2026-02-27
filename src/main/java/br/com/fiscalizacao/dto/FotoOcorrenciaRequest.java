package br.com.fiscalizacao.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FotoOcorrenciaRequest {
    private String nomeArquivo;
    private String url;
    private String bucket;
}
