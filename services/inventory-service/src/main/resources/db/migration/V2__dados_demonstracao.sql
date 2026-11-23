-- Dados de demonstração do checkpoint (Pacote 3): um evento publicado com dois setores.
-- O organizador corresponde ao usuário "organizador" do realm de desenvolvimento do Keycloak.
INSERT INTO evento (id, organizador_id, nome, descricao, local_cidade, inicio, estado) VALUES
  ('0193e0aa-0000-7000-8000-000000000001', 'b3a1f6a2-0000-4000-8000-000000000002',
   'Festival PulsePass 2026', 'Evento de demonstração do checkpoint do Grupo 9.', 'Porto Alegre',
   now() + interval '60 days', 'PUBLICADO');

INSERT INTO setor (id, evento_id, nome, capacidade) VALUES
  ('0193e0aa-0000-7000-8000-0000000000a1', '0193e0aa-0000-7000-8000-000000000001', 'Pista', 50),
  ('0193e0aa-0000-7000-8000-0000000000a2', '0193e0aa-0000-7000-8000-000000000001', 'Camarote', 20);

INSERT INTO lote (id, setor_id, nome, preco_centavos, quantidade, vende_de, vende_ate) VALUES
  ('0193e0aa-0000-7000-8000-0000000000b1', '0193e0aa-0000-7000-8000-0000000000a1', '1º lote', 25000, 50,
   now() - interval '1 day', now() + interval '60 days'),
  ('0193e0aa-0000-7000-8000-0000000000b2', '0193e0aa-0000-7000-8000-0000000000a2', '1º lote', 60000, 20,
   now() - interval '1 day', now() + interval '60 days');

-- Pista: filas A-E x 10; Camarote: filas A-B x 10. Ids determinísticos via md5 para facilitar testes.
INSERT INTO assento (id, setor_id, fila_numero, estado)
SELECT md5('pista-' || f || '-' || n)::uuid, '0193e0aa-0000-7000-8000-0000000000a1', f || '-' || n, 'DISPONIVEL'
  FROM unnest(ARRAY['A', 'B', 'C', 'D', 'E']) AS f, generate_series(1, 10) AS n;
INSERT INTO assento (id, setor_id, fila_numero, estado)
SELECT md5('camarote-' || f || '-' || n)::uuid, '0193e0aa-0000-7000-8000-0000000000a2', f || '-' || n, 'DISPONIVEL'
  FROM unnest(ARRAY['A', 'B']) AS f, generate_series(1, 10) AS n;
