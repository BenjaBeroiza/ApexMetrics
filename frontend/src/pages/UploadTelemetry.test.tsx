import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import UploadTelemetry, { parseLapTimeToSeconds } from './UploadTelemetry';

const renderPage = () =>
  render(
    <MemoryRouter>
      <UploadTelemetry />
    </MemoryRouter>
  );

describe('UploadTelemetry', () => {
  beforeEach(() => {
    localStorage.setItem('apex_token', 'fake-token');
    global.fetch = vi.fn();
  });
  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renderiza el formulario de carga', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: /SUBIR TELEMETRÍA/i })).toBeInTheDocument();
  });

  it('muestra error si se sube un archivo que no es .csv', () => {
    renderPage();
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const badFile = new File(['data'], 'datos.txt', { type: 'text/plain' });
    Object.defineProperty(input, 'files', { value: [badFile], writable: false });
    fireEvent.change(input);
    expect(screen.getByText(/SOLO SE ADMITEN ARCHIVOS .CSV/i)).toBeInTheDocument();
  });

  it('muestra error si se envía el formulario sin completar campos', () => {
    renderPage();
    const form = document.querySelector('form') as HTMLFormElement;
    fireEvent.submit(form);
    expect(screen.getByText(/COMPLETE TODOS LOS CAMPOS/i)).toBeInTheDocument();
  });

  it('muestra error al intentar subir sin archivo CSV seleccionado', async () => {
    renderPage();
    fireEvent.submit(document.querySelector('form') as HTMLFormElement);
    await waitFor(() => {
      expect(screen.getByText(/COMPLETE TODOS LOS CAMPOS/i)).toBeInTheDocument();
    });
  });

  it('redirige a /login si no hay token', () => {
    localStorage.removeItem('apex_token');
    renderPage();
    // Si no hay token, el componente navega a /login y no renderiza el formulario
    expect(document.querySelector('form')).toBeNull();
  });
});

describe('parseLapTimeToSeconds', () => {
  it('convierte el formato m:ss.mmm a segundos', () => {
    expect(parseLapTimeToSeconds('1:23.424')).toBe(83.424);
    expect(parseLapTimeToSeconds('1:07.670')).toBe(67.67);
    expect(parseLapTimeToSeconds('2:05')).toBe(125);
  });

  it('acepta segundos planos por compatibilidad', () => {
    expect(parseLapTimeToSeconds('83.424')).toBe(83.424);
    expect(parseLapTimeToSeconds('70.450')).toBe(70.45);
  });

  it('retorna null ante formato inválido', () => {
    expect(parseLapTimeToSeconds('1:60.0')).toBeNull(); // segundos >= 60
    expect(parseLapTimeToSeconds('abc')).toBeNull();
    expect(parseLapTimeToSeconds('')).toBeNull();
  });
});
