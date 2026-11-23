package br.pucrs.pulsepass.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Ponto único de entrada HTTP: valida o JWT do Keycloak, aplica RBAC grosso por prefixo e
 * rate limiting por usuário/IP no Redis (RNF11). Os serviços revalidam o token (RNF18).
 */
@Configuration
public class ConfiguracaoGateway {

  @Bean
  public SecurityWebFilterChain seguranca(ServerHttpSecurity http) {
    Converter<Jwt, AbstractAuthenticationToken> conversor = jwt -> new JwtAuthenticationToken(jwt, papeis(jwt),
        jwt.getSubject());
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(a -> a
            .pathMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()
            .pathMatchers(HttpMethod.OPTIONS).permitAll()
            .pathMatchers(HttpMethod.GET, "/api/eventos/**").permitAll()
            .pathMatchers("/api/internal/**").denyAll()
            .pathMatchers("/api/organizador/**").hasRole("ORGANIZADOR")
            .pathMatchers("/api/validacoes/**").hasRole("OPERADOR")
            .pathMatchers("/api/pedidos/**", "/api/ingressos/**").hasAnyRole("CLIENTE", "ADMIN")
            .anyExchange().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(
            new ReactiveJwtAuthenticationConverterAdapter(conversor))))
        .build();
  }

  /** Chave do rate limit: usuário autenticado ou, sem token, o IP de origem. */
  @Bean
  public KeyResolver chaveUsuarioOuIp() {
    return exchange -> exchange.getPrincipal()
        .map(p -> "u:" + p.getName())
        .switchIfEmpty(Mono.fromSupplier(() -> {
          var endereco = exchange.getRequest().getRemoteAddress();
          return "ip:" + (endereco == null ? "desconhecido" : endereco.getAddress().getHostAddress());
        }));
  }

  static Collection<GrantedAuthority> papeis(Jwt jwt) {
    List<GrantedAuthority> lista = new ArrayList<>();
    Object realm = jwt.getClaims().get("realm_access");
    if (realm instanceof Map<?, ?> mapa && mapa.get("roles") instanceof Collection<?> roles) {
      for (Object r : roles) {
        lista.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
      }
    }
    return lista;
  }
}
