import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getLeaderboard } from './leaderboard.service';

const mockFetch = vi.fn();
global.fetch = mockFetch;

const jsonResponse = (status: number, body: unknown) => ({
  ok: status >= 200 && status < 300,
  status,
  json: () => Promise.resolve(body),
});

beforeEach(() => {
  mockFetch.mockReset();
});

describe('leaderboard.service — getLeaderboard', () => {
  it('construye la query solo con los filtros activos (endpoint público, sin JWT)', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(200, { content: [], totalPages: 1 }));

    await getLeaderboard({ categoryId: '1', trackId: '', page: 0 });

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/leaderboard?categoryId=1&page=0&size=10');
  });

  it('respeta el tamaño de página indicado', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(200, { content: [], totalPages: 1 }));

    await getLeaderboard({ page: 2, size: 25 });

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/leaderboard?page=2&size=25');
  });

  it('retorna el contenido paginado', async () => {
    const page = {
      content: [{ rank: 1, username: 'b_beroiza', bestLapTime: 104.5, categoryName: 'GT3', uploadedAt: '2026-07-01' }],
      totalPages: 3,
    };
    mockFetch.mockResolvedValueOnce(jsonResponse(200, page));

    await expect(getLeaderboard({ page: 0 })).resolves.toEqual(page);
  });

  it('lanza ApiError 500 ante un error del servidor', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(500, {}));

    await expect(getLeaderboard({ page: 0 })).rejects.toMatchObject({ status: 500 });
  });

  it('propaga el error de red', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    await expect(getLeaderboard({ page: 0 })).rejects.toBeInstanceOf(TypeError);
  });
});
