package br.com.fiscalizacao.controller;

import br.com.fiscalizacao.dto.PresignedUrlResponse;
import br.com.fiscalizacao.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fotos")
public class FotoController {

    private final S3Service s3Service;

    public FotoController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> gerarPresignedUrl(@RequestParam String nomeArquivo) {
        return ResponseEntity.ok(s3Service.gerarPresignedUrl(nomeArquivo));
    }
}
