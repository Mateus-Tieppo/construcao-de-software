import { useEffect, useState } from 'react';
import { api, Ingresso } from '../api/cliente';
import { keycloak } from '../auth';

/** US26: meus ingressos, download do PDF com QR Code. */
export default function MeusIngressos() {
  const [ingressos, setIngressos] = useState<Ingresso[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    if (!keycloak.authenticated) {
      keycloak.login();
      return;
    }
    const carregar = () => api.ingressos().then(setIngressos).catch((e: Error) => setErro(e.message));
    carregar();
    const t = setInterval(carregar, 4000);
    return () => clearInterval(t);
  }, []);

  if (erro) return <p className="erro">{erro}</p>;
  if (!ingressos) return <p>Carregando…</p>;
  return (
    <section>
      <h1>Meus ingressos</h1>
      {ingressos.length === 0 && <p>Você ainda não tem ingressos.</p>}
      <div className="grade">
        {ingressos.map((i) => (
          <div key={i.id} className="cartao">
            <h2>{i.setorAssento}</h2>
            <p className={`estado estado-${i.estado.toLowerCase()}`}>{i.estado}</p>
            {i.emitidoEm && <p className="discreto">Emitido em {new Date(i.emitidoEm).toLocaleString('pt-BR')}</p>}
            {i.estado === 'PENDENTE' ? <p>Gerando PDF…</p> : (
              <>
                <button onClick={() => api.baixarPdf(i.id).catch((e: Error) => setErro(e.message))}>Baixar PDF</button>
                <details>
                  <summary>Código do QR</summary>
                  <code className="qr">{i.codigoQr}</code>
                </details>
              </>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}
