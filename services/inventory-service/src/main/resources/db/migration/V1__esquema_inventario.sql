-- Inventory Service (banco inv). Modelo do Pacote 2, documento 2, seção 2.3.1.
CREATE TABLE evento (
  id              uuid PRIMARY KEY,
  organizador_id  uuid NOT NULL,
  nome            varchar(200) NOT NULL,
  descricao       text,
  local_cidade    varchar(120) NOT NULL,
  inicio          timestamptz NOT NULL,
  estado          varchar(20) NOT NULL CHECK (estado IN ('RASCUNHO', 'PUBLICADO', 'ENCERRADO')),
  criado_em       timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE setor (
  id          uuid PRIMARY KEY,
  evento_id   uuid NOT NULL REFERENCES evento (id),
  nome        varchar(80) NOT NULL,
  capacidade  int NOT NULL CHECK (capacidade > 0)
);
CREATE INDEX ix_setor_evento ON setor (evento_id);

CREATE TABLE lote (
  id              uuid PRIMARY KEY,
  setor_id        uuid NOT NULL REFERENCES setor (id),
  nome            varchar(80) NOT NULL,
  preco_centavos  bigint NOT NULL CHECK (preco_centavos > 0),
  quantidade      int NOT NULL CHECK (quantidade > 0),
  reservados      int NOT NULL DEFAULT 0 CHECK (reservados >= 0),
  vendidos        int NOT NULL DEFAULT 0 CHECK (vendidos >= 0),
  vende_de        timestamptz NOT NULL,
  vende_ate       timestamptz NOT NULL,
  CONSTRAINT lote_sem_sobrevenda CHECK (reservados + vendidos <= quantidade)
);
CREATE INDEX ix_lote_setor ON lote (setor_id);

CREATE TABLE reserva (
  id            uuid PRIMARY KEY,
  pedido_id     uuid NOT NULL UNIQUE,
  comprador_id  uuid NOT NULL,
  evento_id     uuid NOT NULL REFERENCES evento (id),
  estado        varchar(20) NOT NULL CHECK (estado IN ('ATIVA', 'CONFIRMADA', 'EXPIRADA', 'LIBERADA')),
  criada_em     timestamptz NOT NULL,
  expira_em     timestamptz NOT NULL,
  versao        bigint NOT NULL DEFAULT 1
);
CREATE INDEX ix_reserva_ativa_expira ON reserva (expira_em) WHERE estado = 'ATIVA';

CREATE TABLE assento (
  id           uuid PRIMARY KEY,
  setor_id     uuid NOT NULL REFERENCES setor (id),
  fila_numero  varchar(10) NOT NULL,
  estado       varchar(20) NOT NULL CHECK (estado IN ('DISPONIVEL', 'RESERVADO', 'VENDIDO', 'BLOQUEADO')),
  reserva_id   uuid REFERENCES reserva (id),
  versao       bigint NOT NULL DEFAULT 0,
  UNIQUE (setor_id, fila_numero),
  -- invariante I1: assento reservado ou vendido sempre aponta para exatamente uma reserva
  CONSTRAINT assento_reserva_coerente CHECK (
    (estado IN ('RESERVADO', 'VENDIDO') AND reserva_id IS NOT NULL) OR (estado IN ('DISPONIVEL', 'BLOQUEADO') AND reserva_id IS NULL))
);
CREATE INDEX ix_assento_reserva ON assento (reserva_id);

CREATE TABLE item_reserva (
  reserva_id      uuid NOT NULL REFERENCES reserva (id),
  assento_id      uuid NOT NULL REFERENCES assento (id),
  lote_id         uuid NOT NULL REFERENCES lote (id),
  preco_centavos  bigint NOT NULL,
  descricao       varchar(120) NOT NULL,
  PRIMARY KEY (reserva_id, assento_id)
);

CREATE TABLE operador_evento (
  evento_id    uuid NOT NULL REFERENCES evento (id),
  operador_id  uuid NOT NULL,
  PRIMARY KEY (evento_id, operador_id)
);

-- Transactional Outbox (RNF06) e Inbox (RNF07)
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
