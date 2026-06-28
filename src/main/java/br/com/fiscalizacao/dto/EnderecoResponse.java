package br.com.fiscalizacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Endereço da ocorrência")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoResponse {

    @Schema(description = "Nome da rua ou avenida", example = "Rua das Flores")
    private String rua;

    @Schema(description = "Número do imóvel mais próximo", example = "123")
    private Integer numero;

    @Schema(description = "Bairro", example = "Centro")
    private String bairro;

    @Schema(description = "Cidade", example = "São Paulo")
    private String cidade;

    @Schema(description = "Estado (sigla)", example = "SP")
    private String estado;

    @Schema(description = "CEP (apenas dígitos)", example = "01310100")
    private Integer cep;
}
