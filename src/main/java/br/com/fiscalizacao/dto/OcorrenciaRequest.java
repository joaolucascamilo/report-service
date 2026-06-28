package br.com.fiscalizacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Schema(description = "Dados para registro de uma nova ocorrência urbana")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OcorrenciaRequest {

    @Schema(description = "Endereço onde a ocorrência foi identificada")
    private EnderecoRequest enderecoOcorrencia;

    @Schema(
        description = """
            Código do tipo de ocorrência:
            1 = Buraco em via pública,
            2 = Pavimentação danificada,
            3 = Falha na drenagem / bueiro entupido,
            4 = Calçada irregular,
            5 = Falha na iluminação pública,
            6 = Sinalização defeituosa ou ausente,
            7 = Acúmulo de lixo ou entulho,
            99 = Outros
            """,
        example = "1"
    )
    private Integer tipoOcorrencia;

    @Schema(description = "Descrição detalhada do problema", example = "Grande buraco no meio da via, risco de acidente.")
    private String detalhes;

    @Schema(description = "Lista de fotos associadas à ocorrência (após upload via presigned URL)")
    private List<FotoOcorrenciaRequest> fotoOcorrencia;

    @Schema(description = "Latitude da localização da ocorrência", example = "-23.5505")
    private Double latitude;

    @Schema(description = "Longitude da localização da ocorrência", example = "-46.6333")
    private Double longitude;
}
