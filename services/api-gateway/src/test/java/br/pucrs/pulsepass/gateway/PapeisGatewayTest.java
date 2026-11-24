package br.pucrs.pulsepass.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class PapeisGatewayTest {

  @Test
  void converteRealmRolesEmAuthorities() {
    Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("u1")
        .claim("realm_access", Map.of("roles", List.of("cliente", "operador")))
        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    assertThat(ConfiguracaoGateway.papeis(jwt)).extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_CLIENTE", "ROLE_OPERADOR");
  }

  @Test
  void semPapeis() {
    Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("u1")
        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    assertThat(ConfiguracaoGateway.papeis(jwt)).isEmpty();
  }
}
