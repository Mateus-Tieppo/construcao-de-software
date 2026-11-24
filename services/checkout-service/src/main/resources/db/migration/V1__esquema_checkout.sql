-- Checkout Service (banco chk). Pacote 2, documento 2, seção 2.3.2 e 2.7.
CREATE TABLE pedido (
  id                uuid PRIMARY KEY,
  comprador_id      uuid NOT NULL,
  comprador_nome    varchar(200),
  evento_id         uuid NOT NULL,
  reserva_id        uuid UNIQUE,
  estado            varchar(20) NOT NULL CHECK (estado IN ('CRIADO', 'RESERVADO', 'AGUARDANDO', 'PAGO', 'EXPIRADO')),
  motivo_expiracao  varchar(30) CHECK (motivo_expiracao IN ('TIMEOUT', 'CANCELADO_USUARIO', 'FALHA_RESERVA', 'PAGAMENTO_TARDIO')),
  total_centavos    bigint NOT NULL DEFAULT 0,
  expira_em         timestamptz,
  versao            bigint NOT NULL DEFAULT 1,
  criado_em         timestamptz NOT NULL DEFAULT now(),
  atualizado_em     timestamptz,
  CONSTRAINT expirado_tem_motivo CHECK ((estado = 'EXPIRADO') = (motivo_expiracao IS NOT NULL))
);
CREATE INDEX ix_pedido_comprador ON pedido (comprador_id, criado_em DESC);

CREATE TABLE item_pedido (
  id              uuid PRIMARY KEY,
  pedido_id       uuid NOT NULL REFERENCES pedido (id),
  assento_id      uuid NOT NULL,
  lote_id         uuid NOT NULL,
  preco_centavos  bigint NOT NULL,
  descricao       varchar(120) NOT NULL,
  UNIQUE (pedido_id, assento_id)
);

CREATE TABLE pagamento (
  id                  uuid PRIMARY KEY,
  pedido_id           uuid NOT NULL REFERENCES pedido (id),
  estado              varchar(30) NOT NULL CHECK (estado IN ('AUTORIZANDO', 'AUTORIZADO', 'CAPTURADO', 'RECUSADO', 'CANCELADO',
                                                       'ESTORNO_SOLICITADO')),
  valor_centavos      bigint NOT NULL,
  referencia_gateway  varchar(80),
  criado_em           timestamptz NOT NULL DEFAULT now()
);
-- no máximo um pagamento autorizado ou capturado por pedido
CREATE UNIQUE INDEX ux_pagamento_efetivo ON pagamento (pedido_id) WHERE estado IN ('AUTORIZADO', 'CAPTURADO');

CREATE TABLE idempotencia (
  usuario_id       varchar(80) NOT NULL,
  escopo           varchar(200) NOT NULL,
  chave            varchar(100) NOT NULL,
  hash_requisicao  char(64) NOT NULL,
  estado           varchar(20) NOT NULL CHECK (estado IN ('EM_ANDAMENTO', 'CONCLUIDO')),
  status_http      int,
  content_type     varchar(100),
  corpo_resposta   bytea,
  criado_em        timestamptz NOT NULL DEFAULT now(),
  expira_em        timestamptz NOT NULL,
  PRIMARY KEY (usuario_id, escopo, chave)
);

CREATE TABLE outbox (
  id            bigserial PRIMARY KEY,
  event_id      uuid NOT NULL UNIQUE,
  topico        varchar(120) NOT NULL,
  chave         varchar(80) NOT NULL,
  tipo          varchar(80) NOT NULL,
  payload       jsonb NOT NULL,
  criado_em     timestamptz NOT NULL DEFAULT now(),
  publicado_em  timestamptz,
  tentativas    int NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_pendente ON outbox (id) WHERE publicado_em IS NULL;

CREATE TABLE mensagem_processada (
  grupo          varchar(80) NOT NULL,
  event_id       uuid NOT NULL,
  processado_em  timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (grupo, event_id)
);
