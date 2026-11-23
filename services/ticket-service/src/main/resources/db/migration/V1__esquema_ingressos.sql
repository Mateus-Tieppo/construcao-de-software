-- Ticket Service (banco tkt). Pacote 2, documento 2, seções 2.3.3 e 2.7.
CREATE TABLE ingresso (
  id              uuid PRIMARY KEY,
  item_pedido_id  uuid NOT NULL UNIQUE,
  pedido_id       uuid NOT NULL,
  evento_id       uuid NOT NULL,
  titular_id      uuid NOT NULL,
  titular_nome    varchar(200) NOT NULL,
  setor_assento   varchar(120) NOT NULL,
  estado          varchar(20) NOT NULL CHECK (estado IN ('PENDENTE', 'EMITIDO', 'UTILIZADO')),
  emitido_em      timestamptz,
  utilizado_em    timestamptz,
  assinatura_qr   varchar(200),
  codigo_qr       varchar(400),
  caminho_pdf     varchar(300)
);
CREATE INDEX ix_ingresso_titular ON ingresso (titular_id);

CREATE TABLE job_pdf (
  ingresso_id  uuid PRIMARY KEY REFERENCES ingresso (id),
  estado       varchar(20) NOT NULL CHECK (estado IN ('PENDENTE', 'CONCLUIDO', 'FALHOU')),
  tentativas   int NOT NULL DEFAULT 0,
  ultimo_erro  text
);

CREATE TABLE validacao (
  id           uuid PRIMARY KEY,
  ingresso_id  uuid NOT NULL REFERENCES ingresso (id),
  operador_id  uuid NOT NULL,
  resultado    varchar(20) NOT NULL CHECK (resultado IN ('LIBERADO', 'JA_UTILIZADO', 'INVALIDO', 'NAO_AUTORIZADO')),
  lido_em      timestamptz NOT NULL DEFAULT now()
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
