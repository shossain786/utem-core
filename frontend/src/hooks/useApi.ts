import { useQuery } from '@tanstack/react-query';
import apiClient from '@/api/client';
import type { TestRunSummary, Page } from '@/api/types';

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

export function useRunSummaryStats() {
  return useQuery({
    queryKey: ['runs', 'summary'],
    queryFn: async () => {
      const { data } = await apiClient.get<Record<string, number>>('/runs/summary');
      return data;
    },
  });
}
