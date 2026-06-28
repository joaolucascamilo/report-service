package br.com.fiscalizacao.controller;

import br.com.fiscalizacao.dto.OcorrenciaRequest;
import br.com.fiscalizacao.dto.OcorrenciaResponse;
import br.com.fiscalizacao.service.OcorrenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Ocorrências", description = "Endpoints para registro e gestão de ocorrências urbanas")
@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaService service;

    public OcorrenciaController(OcorrenciaService service) {
        this.service = service;
    }

    private Long getUsuarioIdAutenticado() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ── CIDADÃO ──────────────────────────────────────────────────────────────

    @Operation(
        summary = "Registrar nova ocorrência",
        description = "Cria uma ocorrência de problema urbano vinculada ao cidadão autenticado. Requer perfil **ROLE_CIDADAO**."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ocorrência criada com sucesso",
            content = @Content(schema = @Schema(implementation = OcorrenciaResponse.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido", content = @Content),
        @ApiResponse(responseCode = "403", description = "Perfil sem permissão — requer ROLE_CIDADAO", content = @Content)
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping
    public ResponseEntity<OcorrenciaResponse> registrarOcorrencia(@RequestBody OcorrenciaRequest ocorrencia) {
        Long usuarioId = getUsuarioIdAutenticado();
        OcorrenciaResponse novaOcorrencia = service.registrar(ocorrencia, usuarioId);
        return new ResponseEntity<>(novaOcorrencia, HttpStatus.CREATED);
    }

    @Operation(
        summary = "Listar minhas ocorrências",
        description = "Retorna todas as ocorrências registradas pelo cidadão autenticado. Requer perfil **ROLE_CIDADAO**."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = OcorrenciaResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido", content = @Content),
        @ApiResponse(responseCode = "403", description = "Perfil sem permissão — requer ROLE_CIDADAO", content = @Content)
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/minhas")
    public ResponseEntity<List<OcorrenciaResponse>> listarMinhasOcorrencias() {
        Long usuarioId = getUsuarioIdAutenticado();
        return ResponseEntity.ok(service.listarPorUsuarioId(usuarioId));
    }

    // ── AGENTE DA PREFEITURA ─────────────────────────────────────────────────

    @Operation(
        summary = "Listar todas as ocorrências",
        description = "Retorna todas as ocorrências registradas no sistema. Requer perfil **ROLE_AGENTE_PREFEITURA**."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = OcorrenciaResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido", content = @Content),
        @ApiResponse(responseCode = "403", description = "Perfil sem permissão — requer ROLE_AGENTE_PREFEITURA", content = @Content)
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ResponseEntity<List<OcorrenciaResponse>> listarOcorrencias() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @Operation(
        summary = "Atualizar status de ocorrência",
        description = """
            Atualiza o status de uma ocorrência. O corpo da requisição é um inteiro com o código do novo status:

            | Código | Status           |
            |--------|------------------|
            | 1      | REGISTRADO       |
            | 2      | CANCELADO        |
            | 3      | RESOLVIDO        |
            | 4      | EM_PROCEDIMENTO  |

            Ao marcar como **RESOLVIDO (3)**, o sistema concede automaticamente 50 pontos ao cidadão que registrou a ocorrência.
            Requer perfil **ROLE_AGENTE_PREFEITURA**.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = OcorrenciaResponse.class))),
        @ApiResponse(responseCode = "400", description = "Código de status inválido", content = @Content),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido", content = @Content),
        @ApiResponse(responseCode = "403", description = "Perfil sem permissão — requer ROLE_AGENTE_PREFEITURA", content = @Content),
        @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada", content = @Content)
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{id}/status")
    public ResponseEntity<OcorrenciaResponse> atualizarStatus(
            @Parameter(description = "ID da ocorrência", required = true) @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Código numérico do novo status (1=REGISTRADO, 2=CANCELADO, 3=RESOLVIDO, 4=EM_PROCEDIMENTO)",
                required = true,
                content = @Content(schema = @Schema(type = "integer", example = "4"))
            )
            @RequestBody Integer codigoStatus,
            @RequestHeader("Authorization") String token) {
        try {
            OcorrenciaResponse ocorrenciaAtualizada = service.atualizarStatus(id, codigoStatus, token);
            return ResponseEntity.ok(ocorrenciaAtualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Deletar ocorrência",
        description = "Remove uma ocorrência e as fotos associadas do bucket S3. Requer autenticação."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Ocorrência deletada com sucesso", content = @Content),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido", content = @Content),
        @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada", content = @Content)
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarOcorrencia(
            @Parameter(description = "ID da ocorrência", required = true) @PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // ── PÚBLICO ───────────────────────────────────────────────────────────────

    @Operation(
        summary = "Listar ocorrências para mapa",
        description = "Retorna todas as ocorrências com dados de localização para exibição no mapa. **Acesso público**, não requer autenticação."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = OcorrenciaResponse.class))))
    @GetMapping("/mapa")
    public ResponseEntity<List<OcorrenciaResponse>> listarParaMapa() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @Operation(
        summary = "Buscar ocorrência por ID",
        description = "Retorna os detalhes de uma ocorrência específica. **Acesso público**, não requer autenticação."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ocorrência encontrada",
            content = @Content(schema = @Schema(implementation = OcorrenciaResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<OcorrenciaResponse> buscarPorId(
            @Parameter(description = "ID da ocorrência", required = true) @PathVariable Long id) {
        try {
            OcorrenciaResponse response = service.buscarPorId(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
