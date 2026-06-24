package br.com.fiscalizacao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignedUrlResponse {
    private String uploadUrl;
    private String key;
    private String bucket;
}
