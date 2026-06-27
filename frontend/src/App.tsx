// src/App.tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import Leaderboard from './pages/Leaderboard';
import Dashboard from './pages/Dashboard';
import UploadTelemetry from './pages/UploadTelemetry';
import Profile from './pages/Profile';
import SessionAnalysis from './pages/SessionAnalysis';
import ComparacionVueltas from './pages/ComparacionVueltas';

/**
 * Componente raíz de enrutamiento de la SPA.
 *
 * Mapa de rutas:
 *  - /login        → pantalla de inicio de sesión (RF02)
 *  - /register     → pantalla de registro (RF01)
 *  - /leaderboard  → clasificación pública (RF07)
 *  - /dashboard    → historial de sesiones del piloto autenticado (RF08, RF09)
 *  - /dashboard/sesiones/:id/analisis → curvas de velocidad/frenado de una sesión (RF05)
 *  - /comparacion  → comparación superpuesta de dos sesiones (RF06)
 *  - /upload       → formulario de carga de telemetría CSV (RF04)
 *  - /profile      → perfil del piloto autenticado (RF03)
 *  - /             → redirige al login por defecto
 *
 * La protección de rutas (verificación de JWT) se realiza dentro de cada
 * página privada leyendo `apex_token` desde localStorage; si no existe,
 * cada componente redirige a /login. Centralizar esto en un guard común
 * queda pendiente para una iteración posterior.
 *
 * @returns {JSX.Element} árbol de rutas envuelto en BrowserRouter
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/leaderboard" element={<Leaderboard />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/dashboard/sesiones/:id/analisis" element={<SessionAnalysis />} />
        <Route path="/comparacion" element={<ComparacionVueltas />} />
        <Route path="/upload" element={<UploadTelemetry />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}