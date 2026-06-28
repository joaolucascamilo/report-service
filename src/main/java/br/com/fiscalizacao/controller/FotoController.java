package br.com.fiscalizacao.controller;

import br.com.fiscalizacao.dto.PresignedUrlResponse;
import br.com.fiscalizacao.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Fotos", description = "Endpoints para upload de fotos de ocorrências via AWS S3")
@RestController
@RequestMapping("/api/fotos")
public class FotoController {

    private final S3Service s3Service;

    public FotoController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @Operation(
        summary = "Gerar URL pré-assinada para upload",
        description = """
            Gera uma URL temporária (presigned URL) do AWS S3 que permite ao cliente fazer upload
            de uma foto diretamente para o bucket sem expor as credenciais AWS.

            **Fluxo de uso:**
            1. Chame este endpoint passando o nome do arquivo.
            2. Use a `uploadUrl` retornada para fazer um `PUT` com o arquivo diretamente no S3.
            3. Passe o `key` e o `bucket` retornados no campo `fotoOcorrencia` ao registrar a ocorrência.

            Requer perfil **ROLE_CIDADAO**.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL gerada com sucesso",
            content = @Content(schema = @Schema(implementation = PresignedUrlResponse.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido", content = @Content),
        @ApiResponse(responseCode = "403", description = "Perfil sem permissão — requer ROLE_CIDADAO", content = @Content)
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> gerarPresignedUrl(
            @Parameter(description = "Nome do arquivo a ser enviado (ex: foto_buraco.jpg)", required = true, example = "foto_buraco.jpg")
            @RequestParam String nomeArquivo) {
        return ResponseEntity.ok(s3Service.gerarPresignedUrl(nomeArquivo));
    }
}
