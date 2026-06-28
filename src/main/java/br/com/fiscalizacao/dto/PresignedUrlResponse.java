package br.com.fiscalizacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "URL pré-assinada do S3 para upload direto de foto")
@Getter
@AllArgsConstructor
public class PresignedUrlResponse {

    @Schema(
        description = "URL temporária para envio do arquivo via HTTP PUT diretamente ao S3. Expira em alguns minutos.",
        example = "https://bucket.s3.amazonaws.com/fotos/foto.jpg?X-Amz-Signature=..."
    )
    private String uploadUrl;

    @Schema(description = "Chave (path) do arquivo dentro do bucket S3", example = "fotos/uuid-foto.jpg")
    private String key;

    @Schema(description = "Nome do bucket S3 onde o arquivo será armazenado", example = "fiscalizacao-fotos")
    private String bucket;
}
