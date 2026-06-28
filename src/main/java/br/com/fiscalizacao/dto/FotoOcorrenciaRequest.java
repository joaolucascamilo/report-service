package br.com.fiscalizacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Referência a uma foto já enviada ao S3 via presigned URL")
@Getter
@Setter
public class FotoOcorrenciaRequest {

    @Schema(description = "Nome original do arquivo", example = "foto_buraco.jpg")
    private String nomeArquivo;

    @Schema(description = "URL pública do arquivo no S3", example = "https://bucket.s3.amazonaws.com/fotos/foto_buraco.jpg")
    private String url;

    @Schema(description = "Nome do bucket S3 onde o arquivo foi armazenado", example = "fiscalizacao-fotos")
    private String bucket;
}
