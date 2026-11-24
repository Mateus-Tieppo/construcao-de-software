import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, EventoResumo } from '../api/cliente';
import { reais } from '../formatos';

/** US14: lista de eventos publicados. */
export default function Catalogo() {
  const [eventos, setEventos] = useState<EventoResumo[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    api.eventos().then(setEventos).catch((e: Error) => setErro(e.message));
  }, []);

  if (erro) return <p className="erro">Não foi possível carregar o catálogo: {erro}</p>;
  if (!eventos) return <p>Carregando eventos…</p>;
  return (
    <section>
      <h1>Eventos à venda</h1>
      {eventos.length === 0 && <p>Nenhum evento publicado.</p>}
      <div className="grade">
        {eventos.map((e) => (
          <Link key={e.id} to={`/eventos/${e.id}`} className="cartao">
            <h2>{e.nome}</h2>
            <p>{e.localCidade} · {new Date(e.inicio).toLocaleDateString('pt-BR')}</p>
            <p>A partir de <strong>{reais(e.precoMinimoCentavos)}</strong></p>
            {e.disponiveis === 0
              ? <p className="estado estado-expirado">Esgotado</p>
              : <p className="discreto">{e.disponiveis} lugares disponíveis</p>}
          </Link>
        ))}
      </div>
    </section>
  );
}
