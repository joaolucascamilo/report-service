package br.com.fiscalizacao.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "http://localhost:8082")
public interface UserClient {
    @PostMapping("/api/usuarios/pontuar")
    void pontuarUsuario(
            @RequestBody PontuacaoRequestDTO dto,
            @RequestHeader("Authorization") String token
    );
}
