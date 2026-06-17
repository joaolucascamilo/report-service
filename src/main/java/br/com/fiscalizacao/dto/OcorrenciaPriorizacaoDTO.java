package br.com.fiscalizacao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcorrenciaPriorizacaoDTO {

    private Long id;
    private String tipoOcorrencia;    // ex: "BURACO", "LAMPADA"
    private Integer quantidadeDenuncias;
    private LocalDateTime dataCriacao;
    private Double latitude;
    private Double longitude;
}
