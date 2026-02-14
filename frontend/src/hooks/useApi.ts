import { useQuery } from '@tanstack/react-query';
import apiClient from '@/api/client';
import type { TestRunSummary, TestRunHierarchy, Page, RunStatus, SearchResult } from '@/api/types';

export function useRuns(page = 0, size = 20) {
  return useQuery({
    queryKey: ['runs', page, size],
    queryFn: async () => {
      const { data } = await apiClient.get<Page<TestRunSummary>>('/runs', {
        params: { page, size },
      });
      return data;
    },
  });
}

export function useFilteredRuns(
  page = 0,
  size = 20,
  status?: RunStatus | null,
  name?: string | null,
) {
  return useQuery({
    queryKey: ['runs', page, size, status, name],
    queryFn: async () => {
      const params: Record<string, string | number> = { page, size };
      if (status) params.status = status;
      if (name) params.name = name;
      const { data } = await apiClient.get<Page<TestRunSummary>>('/runs', { params });
      return data;
    },
  });
}

export function useRunDetail(runId: string | undefined) {
  return useQuery({
    queryKey: ['runs', runId, 'detail'],
    queryFn: async () => {
      const { data } = await apiClient.get<TestRunHierarchy>(`/runs/${runId}/detail`);
      return data;
    },
    enabled: !!runId,
  });
}

export function useRunSummaryStats() {
  return useQuery({
    queryKey: ['runs', 'summary'],
    queryFn: async () => {
      const { data } = await apiClient.get<Record<string, number>>('/runs/summary');
      return data;
    },
  });
}

export function useGlobalSearch(query: string, limit = 10) {
  return useQuery({
    queryKey: ['search', query, limit],
    queryFn: async () => {
      const { data } = await apiClient.get<SearchResult>('/search', {
        params: { q: query, limit },
      });
      return data;
    },
    enabled: query.length >= 2,
  });
}
