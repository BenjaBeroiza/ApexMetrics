/**
 * Servicio de clasificación global — RF07 (leaderboard público, sin JWT).
 */
import { ApiError } from './http';

export interface LeaderboardEntry {
  rank: number;
  username: string;
  bestLapTime: number;
  categoryName: string;
  uploadedAt: string;
}

export interface LeaderboardPage {
  content: LeaderboardEntry[];
  totalPages: number;
  totalElements?: number;
}

export interface LeaderboardFilters {
  categoryId?: string;
  trackId?: string;
  page: number;
  size?: number;
}

export async function getLeaderboard(filters: LeaderboardFilters): Promise<LeaderboardPage> {
  const params = new URLSearchParams();
  if (filters.categoryId) params.append('categoryId', filters.categoryId);
  if (filters.trackId) params.append('trackId', filters.trackId);
  params.append('page', String(filters.page));
  params.append('size', String(filters.size ?? 10));

  const response = await fetch(`/api/v1/leaderboard?${params.toString()}`);
  if (!response.ok) {
    throw new ApiError(response.status, 'Fallo al obtener la clasificación');
  }
  return response.json();
}
