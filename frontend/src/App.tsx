import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { keycloak, temPapel } from './auth';
import Catalogo from './paginas/Catalogo';
import EventoPagina from './paginas/EventoPagina';
import PedidoPagina from './paginas/PedidoPagina';
import MeusIngressos from './paginas/MeusIngressos';
import ValidacaoPagina from './paginas/ValidacaoPagina';

export default function App() {
  const logado = !!keycloak.authenticated;
  const nome = keycloak.tokenParsed?.['name'] ?? keycloak.tokenParsed?.['preferred_username'];
  return (
    <>
      <header className="topo">
        <Link to="/" className="marca">PulsePass</Link>
        <nav>
          <Link to="/">Eventos</Link>
          {logado && temPapel('cliente') && <Link to="/ingressos">Meus ingressos</Link>}
          {logado && temPapel('operador') && <Link to="/operador">Validar entrada</Link>}
        </nav>
        <div className="conta">
          {logado ? (
            <>
              <span>{nome}</span>
              <button className="secundario" onClick={() => keycloak.logout({ redirectUri: window.location.origin })}>Sair</button>
            </>
          ) : (
            <>
              <button className="secundario" onClick={() => keycloak.register()}>Criar conta</button>
              <button onClick={() => keycloak.login()}>Entrar</button>
            </>
          )}
        </div>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Catalogo />} />
          <Route path="/eventos/:id" element={<EventoPagina />} />
          <Route path="/pedidos/:id" element={<PedidoPagina />} />
          <Route path="/ingressos" element={<MeusIngressos />} />
          <Route path="/operador" element={<ValidacaoPagina />} />
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </main>
      <footer>Checkpoint do Pacote 3 · Grupo 9 · Construção de Software PUCRS 2026/2</footer>
    </>
  );
}
