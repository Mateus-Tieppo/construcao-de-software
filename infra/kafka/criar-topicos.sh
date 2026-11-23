#!/bin/bash
# Cria os tópicos do Pacote 2 (documento 3, seção 3.2) e suas DLQs. Idempotente (--if-not-exists).
set -euo pipefail
K=/opt/kafka/bin/kafka-topics.sh
B=${KAFKA_BOOTSTRAP:-kafka:9092}
criar() { $K --bootstrap-server "$B" --create --if-not-exists --topic "$1" --partitions "$2" --replication-factor 1; }
criar iam.contas.v1 3
criar inventario.catalogo.v1 3
criar inventario.reservas.v1 6
criar checkout.pedidos.v1 6
criar ingressos.v1 3
criar telemetria.navegacao.v1 3
for t in iam.contas.v1 inventario.catalogo.v1 inventario.reservas.v1 checkout.pedidos.v1 ingressos.v1 telemetria.navegacao.v1; do
  criar "$t.dlq" 1
done
$K --bootstrap-server "$B" --list
