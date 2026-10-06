import { BrowserRouter, Navigate, NavLink, Route, Routes } from 'react-router';
import { CATALOG_SECTIONS, CatalogPage } from './catalog/CatalogPage';
import { Icon, Logo } from './components/Icon';
import { ExpeditionPage } from './expedition/ExpeditionPage';
import { ExpeditionsPage } from './expeditions/ExpeditionsPage';

export function App() {
  return (
    <BrowserRouter>
      <div className="shell">
        <aside className="sidebar">
          <NavLink to="/expeditions" className="brand">
            <Logo />
            <div>
              <strong>FieldOps</strong>
              <span>Operaciones de campo</span>
            </div>
          </NavLink>
          <nav className="nav-group" aria-label="Planificación">
            <NavLink to="/expeditions">
              <Icon name="route" />
              Expediciones
            </NavLink>
          </nav>
          <nav className="nav-group" aria-label="Catálogo">
            <span>Catálogo</span>
            {CATALOG_SECTIONS.map((section) => (
              <NavLink key={section.id} to={`/catalog/${section.id}`}>
                <Icon name={section.icon} />
                {section.label}
              </NavLink>
            ))}
          </nav>
          <p className="sidebar-note">
            <Icon name="clock" size={16} />
            Horarios en UTC
          </p>
        </aside>
        <main className="content">
          <Routes>
            <Route path="/" element={<Navigate to="/expeditions" replace />} />
            <Route path="/expeditions" element={<ExpeditionsPage />} />
            <Route path="/expeditions/:id" element={<ExpeditionPage />} />
            <Route path="/catalog" element={<Navigate to="/catalog/people" replace />} />
            <Route path="/catalog/:section" element={<CatalogPage />} />
            <Route path="*" element={<p className="empty">Esta página no existe.</p>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
