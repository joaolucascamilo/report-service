package br.com.fiscalizacao.enums;

public enum TipoOcorrencia {

    BURACO_VIA(1, "Buraco em via pública"),
    PAVIMENTACAO_DANIFICADA(2, "Pavimentação danificada ou desgastada"),
    FALHA_DRENAGEM(3, "Falha na drenagem ou bueiro entupido"),
    CALCADA_IRREGULAR(4, "Calçada irregular ou quebrada"),
    FALHA_ILUMINACAO(5, "Falha na iluminação pública"),
    SINALIZACAO_DEFEITUOSA(6, "Sinalização defeituosa ou ausente"),
    ACUMULO_LIXO(7, "Acúmulo de lixo ou entulho"),
    OUTROS(99, "Outros problemas");

    private final int codigo;
    private final String descricao;

    TipoOcorrencia(int codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    // Método utilitário para converter o Integer que vem do frontend de volta para o Enum
    public static TipoOcorrencia valueOf(int codigo) {
        for (TipoOcorrencia tipo : TipoOcorrencia.values()) {
            if (tipo.getCodigo() == codigo) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de tipo de ocorrência inválido: " + codigo);
    }
}