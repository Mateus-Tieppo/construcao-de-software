#!/usr/bin/env bash
# Smoke test ponta a ponta no ambiente do docker compose (job compose-smoke da CI).
# Fluxo: login (Keycloak) -> catálogo -> pedido com Idempotency-Key -> conflito no mesmo assento ->
# pagamento -> ingresso emitido via Kafka -> PDF -> validação de QR (uso único).
set -euo pipefail

BASE=${BASE:-http://localhost:8080/api}
KC=${KC:-http://localhost:8180/realms/pulsepass/protocol/openid-connect/token}
SENHA=${PULSEPASS_DEMO_PASSWORD:?defina PULSEPASS_DEMO_PASSWORD}
EVENTO=0193e0aa-0000-7000-8000-000000000001

passo() { echo -e "\n==> $*"; }
falha() { echo "FALHOU: $*" >&2; exit 1; }

token() {
  curl -sf -X POST "$KC" -d grant_type=password -d client_id=pulsepass-spa -d username="$1" -d password="$SENHA" \
    | jq -r .access_token
}

passo "Login do cliente e do operador no Keycloak"
TC=$(token cliente); TO=$(token operador)
[ -n "$TC" ] && [ "$TC" != null ] || falha "token do cliente"

passo "Auto-cadastro recebe o papel cliente por padrão (US02)"
TA=$(curl -sf -X POST "http://localhost:8180/realms/master/protocol/openid-connect/token" -d grant_type=password \
  -d client_id=admin-cli -d username=admin -d password="${KEYCLOAK_ADMIN_PASSWORD:?defina KEYCLOAK_ADMIN_PASSWORD}" | jq -r .access_token)
curl -sf "http://localhost:8180/admin/realms/pulsepass/roles/default-roles-pulsepass/composites/realm" \
  -H "Authorization: Bearer $TA" | jq -e 'map(.name) | index("cliente") != null' > /dev/null || falha "papel padrão cliente"
POLITICA=$(curl -sf "http://localhost:8180/admin/realms/pulsepass" -H "Authorization: Bearer $TA" | jq -r .passwordPolicy)
echo "política de senha: $POLITICA"
echo "$POLITICA" | grep -q 'length(8)' || falha "política de senha"

passo "Catálogo público"
curl -sf "$BASE/eventos" | jq -e 'length >= 1' > /dev/null || falha "catálogo vazio"
SETOR=$(curl -sf "$BASE/eventos/$EVENTO" | jq -r '.setores[] | select(.nome=="Pista") | .id')
ASSENTO=$(curl -sf "$BASE/eventos/$EVENTO/setores/$SETOR/assentos" | jq -r '[.[] | select(.estado=="DISPONIVEL")][0].id')
echo "setor=$SETOR assento=$ASSENTO"

passo "Pedido sem Idempotency-Key é recusado (400)"
COD=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/pedidos" -H "Authorization: Bearer $TC" \
  -H 'Content-Type: application/json' -d "{\"eventoId\":\"$EVENTO\",\"assentos\":[\"$ASSENTO\"]}")
[ "$COD" = 400 ] || falha "esperado 400, veio $COD"

passo "Criar pedido (reserva atômica)"
CHAVE="smoke-$(date +%s)"
PEDIDO=$(curl -sf -X POST "$BASE/pedidos" -H "Authorization: Bearer $TC" -H "Idempotency-Key: $CHAVE" \
  -H 'Content-Type: application/json' -d "{\"eventoId\":\"$EVENTO\",\"assentos\":[\"$ASSENTO\"]}")
echo "$PEDIDO" | jq .
PID=$(echo "$PEDIDO" | jq -r .id)
[ "$(echo "$PEDIDO" | jq -r .estado)" = RESERVADO ] || falha "pedido não reservado"

passo "Repetir a mesma requisição devolve o mesmo pedido (Idempotency-Key)"
DE_NOVO=$(curl -sf -X POST "$BASE/pedidos" -H "Authorization: Bearer $TC" -H "Idempotency-Key: $CHAVE" \
  -H 'Content-Type: application/json' -d "{\"eventoId\":\"$EVENTO\",\"assentos\":[\"$ASSENTO\"]}")
[ "$(echo "$DE_NOVO" | jq -r .id)" = "$PID" ] || falha "replay devolveu outro pedido"

passo "Outro pedido para o mesmo assento recebe 409 (zero overselling)"
COD=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/pedidos" -H "Authorization: Bearer $TC" \
  -H "Idempotency-Key: outro-$CHAVE" -H 'Content-Type: application/json' \
  -d "{\"eventoId\":\"$EVENTO\",\"assentos\":[\"$ASSENTO\"]}")
[ "$COD" = 409 ] || falha "esperado 409, veio $COD"

passo "Pagar"
PAGO=$(curl -sf -X POST "$BASE/pedidos/$PID/pagamento" -H "Authorization: Bearer $TC" \
  -H "Idempotency-Key: pag-$CHAVE" -H 'Content-Type: application/json' -d '{"cartaoFinal":"4242"}')
echo "$PAGO" | jq .
[ "$(echo "$PAGO" | jq -r .estado)" = PAGO ] || falha "pedido não pago"

passo "Aguardar ingresso emitido (Outbox -> Kafka -> Ticket Service -> PDF)"
for i in $(seq 1 40); do
  ING=$(curl -sf "$BASE/ingressos" -H "Authorization: Bearer $TC" | jq -c "[.[] | select(.pedidoId==\"$PID\")][0]")
  [ "$(echo "$ING" | jq -r .estado)" = EMITIDO ] && break
  sleep 2
done
echo "$ING" | jq .
[ "$(echo "$ING" | jq -r .estado)" = EMITIDO ] || falha "ingresso não emitido"
IID=$(echo "$ING" | jq -r .id); QR=$(echo "$ING" | jq -r .codigoQr)

passo "Baixar PDF"
curl -sf "$BASE/ingressos/$IID/pdf" -H "Authorization: Bearer $TC" -o /tmp/ingresso.pdf
head -c 5 /tmp/ingresso.pdf | grep -q '%PDF-' || falha "PDF inválido"

passo "Outro usuário não baixa o ingresso do cliente (US26): 403"
TG=$(token organizador)
COD=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/ingressos/$IID/pdf" -H "Authorization: Bearer $TG")
[ "$COD" = 403 ] || falha "esperado 403, veio $COD"

passo "Validar QR na entrada: primeira leitura libera, segunda acusa uso"
V1=$(curl -sf -X POST "$BASE/validacoes" -H "Authorization: Bearer $TO" -H 'Content-Type: application/json' \
  -d "{\"codigoQr\":\"$QR\"}" | jq -r .resultado)
R2=$(curl -sf -X POST "$BASE/validacoes" -H "Authorization: Bearer $TO" -H 'Content-Type: application/json' \
  -d "{\"codigoQr\":\"$QR\"}")
V2=$(echo "$R2" | jq -r .resultado)
[ "$(echo "$R2" | jq -r .utilizadoEm)" != null ] || falha "segunda leitura sem data/hora de uso"
echo "1a leitura=$V1 2a leitura=$V2"
[ "$V1" = LIBERADO ] && [ "$V2" = JA_UTILIZADO ] || falha "validação de QR"

passo "Cliente não pode validar QR (RBAC): 403"
COD=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/validacoes" -H "Authorization: Bearer $TC" \
  -H 'Content-Type: application/json' -d "{\"codigoQr\":\"$QR\"}")
[ "$COD" = 403 ] || falha "esperado 403, veio $COD"

echo -e "\nSMOKE OK"
