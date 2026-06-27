import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import SessionChart from './SessionChart';

// ResponsiveContainer no tiene dimensiones en jsdom; lo reemplazamos por un
// contenedor de tamaño fijo para que recharts renderice las curvas en el test.
vi.mock('recharts', async () => {
  const actual = await vi.importActual<typeof import('recharts')>('recharts');
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div style={{ width: 800, height: 420 }}>{children}</div>
    ),
  };
});

describe('SessionChart — RF05 Dashboard analítico', () => {
  beforeEach(() => {
    localStorage.setItem('apex_token', 'fake-jwt-token');
  });

  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('muestra estado de carga inicialmente', () => {
    global.fetch = vi.fn().mockReturnValue(new Promise(() => {})) as unknown as typeof fetch;
    render(<SessionChart sessionId={1} />);
    expect(screen.getByText(/Cargando análisis/i)).toBeInTheDocument();
  });

  it('muestra aviso de permiso cuando la API responde 403', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      json: async () => ({}),
    }) as unknown as typeof fetch;

    render(<SessionChart sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByText(/No tienes permiso/i)).toBeInTheDocument();
    });
  });

  it('muestra aviso cuando la sesión no tiene puntos', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    }) as unknown as typeof fetch;

    render(<SessionChart sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByText(/no tiene puntos de telemetría/i)).toBeInTheDocument();
    });
  });

  it('renderiza el gráfico cuando hay puntos (sin estados de carga/error/vacío)', async () => {
    const puntos = [
      { distance: 0, speed: 150, brake: 0, throttle: 1 },
      { distance: 50, speed: 160, brake: 0.2, throttle: 0.8 },
      { distance: 100, speed: 120, brake: 0.9, throttle: 0 },
    ];
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => puntos,
    }) as unknown as typeof fetch;

    const { container } = render(<SessionChart sessionId={1} />);

    // jsdom no asigna dimensiones reales a recharts, por lo que no podemos
    // afirmar sobre la leyenda/SVG; verificamos que se tomó la rama de datos:
    // desaparece "Cargando" y no aparecen los avisos de error ni de vacío.
    await waitFor(() => {
      expect(screen.queryByText(/Cargando análisis/i)).not.toBeInTheDocument();
    });
    expect(screen.queryByText(/no tiene puntos de telemetría/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/No se pudo conectar/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/No tienes permiso/i)).not.toBeInTheDocument();
    expect(container.querySelector('.recharts-wrapper')).toBeInTheDocument();
  });

  it('muestra error de conexión cuando fetch falla', async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error('network')) as unknown as typeof fetch;

    render(<SessionChart sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByText(/No se pudo conectar/i)).toBeInTheDocument();
    });
  });
});
