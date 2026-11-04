# ADR-004: Integração IAM, Keycloak e refresh tokens no Redis

**Estado:** Proposta para revisão do grupo · pendente de spike técnico · 04/11/2026

## Contexto
O enunciado pede "autenticação stateless via JWT com refresh tokens em chave-valor" e um provedor como Keycloak. O Keycloak **não** guarda sessões no Redis por padrão: ele usa banco próprio e cache Infinispan. O RNF19 do Pacote 1 pede refresh tokens no Redis com rotação, rejeição de reuso e revogação em até 5 s.

## Decisão proposta
1. O **Keycloak** é o único emissor de tokens (OIDC, RS256) e o único lugar com senhas (RNF23). Realm `pulsepass`, papéis `cliente`, `organizador`, `operador`, `admin`.
2. O `iam-service` atua como **BFF de autenticação** para a SPA: faz o fluxo Authorization Code + PKCE com o Keycloak, recebe access e refresh tokens e **guarda o refresh token do Keycloak no Redis**, cifrado (AES-GCM, chave via variável de ambiente), sob a chave `iam:sessao:{sessaoId}` com TTL de 8 h.
3. A SPA recebe o access token (≤ 5 min) e um **identificador de sessão opaco** em cookie `HttpOnly; Secure; SameSite=Strict`. A SPA nunca vê o refresh token do Keycloak.
4. **Rotação:** cada `POST /auth/refresh` troca o refresh no Keycloak (opção "Revoke Refresh Token" ligada no realm), grava o novo no Redis e emite novo identificador de sessão. O identificador anterior fica marcado como usado por 8 h (`iam:sessao-usada:{id}`); reuso dele revoga a família inteira de sessões e registra auditoria.
5. **Revogação:** logout ou bloqueio apagam as chaves no Redis, chamam o logout do Keycloak e gravam o `sid` em `iam:revogado:{sid}` com TTL igual à vida do access token. O gateway consulta essa lista a cada requisição, atingindo revogação em ≤ 5 s.
6. Redis com AOF ligado e usuário ACL próprio do IAM, com acesso só ao prefixo `iam:*`.

## Alternativas
1. SPA guarda o refresh token do Keycloak direto: simples, mas não atende "refresh em chave-valor" e expõe o token ao navegador.
2. Extensão (SPI) do Keycloak para sessões em Redis: acopla o grupo ao código interno do Keycloak.
3. Cognito: exige nuvem paga, contra a premissa P07.

## Consequências
- O IAM vira dependência do *refresh*, não de cada requisição (o access token continua stateless e é validado pela chave pública JWKS).
- Queda do Redis derruba renovações: usuários precisam logar de novo. Aceito para o contexto acadêmico.
- **Pendência:** validar em spike (início do Pacote 3) a troca de tokens com rotação no Keycloak 26 e o tempo de revogação. Até lá, este ADR é proposta.
