package br.pucrs.pulsepass.checkout.adaptadores.web;

import br.pucrs.pulsepass.checkout.adaptadores.persistencia.IdempotenciaRepositorio;
import br.pucrs.pulsepass.commons.seguranca.PapeisKeycloak;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/** JWT do Keycloak revalidado no serviço (RNF18); pedidos só para o papel CLIENTE. */
@Configuration
public class ConfiguracaoSeguranca {

  @Bean
  public SecurityFilterChain filtros(HttpSecurity http, IdempotenciaRepositorio idem, ObjectMapper mapper)
      throws Exception {
    http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/actuator/health/**", "/actuator/prometheus", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            .requestMatchers("/pedidos/**").hasRole("CLIENTE")
            .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(new PapeisKeycloak())))
        .addFilterAfter(new FiltroIdempotencia(idem, mapper), AuthorizationFilter.class);
    return http.build();
  }
}
