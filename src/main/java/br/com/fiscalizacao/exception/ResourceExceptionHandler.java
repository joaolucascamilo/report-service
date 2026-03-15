package br.com.fiscalizacao.exception;

import br.com.fiscalizacao.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice //Escuta todas as requições que são chamadas no sistema
public class ResourceExceptionHandler {

    // Deve capturar qualquer IllegalArgumentException lançada no Service
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {

        ErroResponse erro = ErroResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value()) // Retorna 400
                .erro("Requisição Inválida")
                .mensagem(e.getMessage()) // Mensagem da exceção
                .caminho(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

}
