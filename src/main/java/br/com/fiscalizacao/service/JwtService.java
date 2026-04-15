package br.com.fiscalizacao.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    // Chave secreta estática para fins de desenvolvimento (em produção, use variáveis de ambiente)
    @Value("${api.security.token.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    // Tempo de expiração padrão de 24 horas (em milissegundos)
    @Value("${api.security.token.expiration:86400000}")
    private long jwtExpiration;

    public String gerarToken(String email, String perfil, Long usuarioId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("perfil", perfil); // Perfil (CIDADÃO ou AGENTE)
        claims.put("id", usuarioId);  // ID para o report-service saber quem abriu a ocorrência

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extrairEmail(String token) {
        return extrairTodasClaims(token).getSubject();
    }

    public String extrairPerfil(String token) {
        return extrairTodasClaims(token).get("perfil", String.class);
    }

    public Long extrairIdUsuario(String token) {
        return extrairTodasClaims(token).get("id", Long.class);
    }

    private Claims extrairTodasClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
