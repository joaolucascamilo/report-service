package br.com.fiscalizacao.client;

public class PontuacaoRequestDTO {
    private Long usuarioId;
    private Integer pontos;
    private String descricao;

    public PontuacaoRequestDTO(Long usuarioId, Integer pontos, String descricao) {
        this.usuarioId = usuarioId;
        this.pontos = pontos;
        this.descricao = descricao;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Integer getPontos() {
        return pontos;
    }

    public void setPontos(Integer pontos) {
        this.pontos = pontos;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
