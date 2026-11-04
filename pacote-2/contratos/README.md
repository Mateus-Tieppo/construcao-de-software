# Contratos de eventos (exemplos)

Exemplos de mensagens Kafka no envelope comum descrito em `../03-mensageria-kafka.md`, seção 3.3.
São dados fictícios. Os JSON Schemas formais e os testes de contrato serão criados no Pacote 3 (RNF36).

| Pasta (tópico) | Exemplos |
|---|---|
| `inventario.reservas.v1/` | ReservaCriada, ReservaConfirmada, ReservaExpirada |
| `checkout.pedidos.v1/` | PagamentoConfirmado, PedidoExpirado |
| `ingressos.v1/` | IngressoEmitido |
| `telemetria.navegacao.v1/` | EventoVisualizado (versão do agregado 0: evento sem agregado versionado) |
