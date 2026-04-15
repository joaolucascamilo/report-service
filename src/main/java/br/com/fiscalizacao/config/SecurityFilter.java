package br.com.fiscalizacao.config;

import br.com.fiscalizacao.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public SecurityFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException, IOException {
        String token = recuperarToken(request);

        if (token != null) {

            // Extraindo as informações guardadas dentro do token
            // Pega o Perfil (ROLE_CIDADAO ou ROLE_AGENTE_PREFEITURA) e o ID
            String perfil = jwtService.extrairPerfil(token);
            Long usuarioId = jwtService.extrairIdUsuario(token);

            // Cria a autorização baseada no perfil que veio no token
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(perfil));

            // Salva o usuarioId como "Principal" para usarmos nos Controllers depois
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(usuarioId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.replace("Bearer ", "");
    }
}
