package br.com.fiscalizacao.dto;

import br.com.fiscalizacao.entity.Endereco;
import br.com.fiscalizacao.entity.Usuario;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcorrenciaResponse {

    private Long id;
    private LocalDateTime data;
    private Integer status;
    private String nomeUsuario;
    private EnderecoResponse endereco;
    private List<FotoResponse> fotos;
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FotoResponse {
        private Long id;
        private String url;
    }
}
