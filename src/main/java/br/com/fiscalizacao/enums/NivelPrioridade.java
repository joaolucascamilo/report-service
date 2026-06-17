package br.com.fiscalizacao.enums;

public enum NivelPrioridade {
    CRITICA,
    ALTA,
    MEDIA,
    BAIXA;

    // Converte score final em nível
    public static NivelPrioridade fromScore(int score) {
        if (score >= 80) return CRITICA;
        if (score >= 50) return ALTA;
        if (score >= 25) return MEDIA;
        return BAIXA;
    }
}
