package br.com.fiscalizacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Dados de uma ocorrência retornada pela API")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcorrenciaResponse {

    @Schema(description = "Identificador único da ocorrência", example = "42")
    private Long id;

    @Schema(description = "Data e hora do registro da ocorrência", example = "2024-06-15T14:30:00")
    private LocalDateTime data;

    @Schema(
        description = """
            Código do status atual:
            1 = REGISTRADO,
            2 = CANCELADO,
            3 = RESOLVIDO,
            4 = EM_PROCEDIMENTO
            """,
        example = "1"
    )
    private Integer status;

    @Schema(
        description = """
            Código do tipo de ocorrência:
            1 = Buraco em via pública,
            2 = Pavimentação danificada,
            3 = Falha na drenagem,
            4 = Calçada irregular,
            5 = Falha na iluminação,
            6 = Sinalização defeituosa,
            7 = Acúmulo de lixo,
            99 = Outros
            """,
        example = "1"
    )
    private Integer tipo;

    @Schema(description = "Descrição textual do tipo de ocorrência", example = "Buraco em via pública")
    private String descricaoTipo;

    @Schema(description = "Endereço da ocorrência")
    private EnderecoResponse endereco;

    @Schema(description = "Fotos associadas à ocorrência")
    private List<FotoResponse> fotos;

    @Schema(
        description = "Nível de prioridade calculado pelo ms-priorizacao: CRITICA, ALTA, MEDIA ou BAIXA",
        example = "ALTA"
    )
    private String nivelPrioridade;

    @Schema(description = "Descrição detalhada do problema", example = "Grande buraco no meio da via, risco de acidente.")
    private String detalhes;

    @Schema(description = "Quantidade total de apoios (denúncias) acumulados nesta ocorrência", example = "3")
    private Integer quantidadeDenuncias;

    @Schema(description = "Foto vinculada à ocorrência")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FotoResponse {

        @Schema(description = "Identificador da foto", example = "7")
        private Long id;

        @Schema(description = "URL pública da foto no S3", example = "https://bucket.s3.amazonaws.com/foto.jpg")
        private String url;
    }
}
