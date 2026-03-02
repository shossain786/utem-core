import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/api/client';
import type { TestRunSummary, TestRunHierarchy, Page, RunStatus, RunComparison, SearchResult, FlakinessReport, FlakyTest, TrendData, FailureHotspot, FailureCluster, FailureInsights, PerformanceReport, InsightsSummary } from '@/api/types';

export function useRuns(page = 0, size = 20, refetchInterval?: number | false) {
  return useQuery({
    queryKey: ['runs', page, size],
    queryFn: async () => {
      const { data } = await apiClient.get<Page<TestRunSummary>>('/runs', {
        params: { page, size },
      });
      return data;
    },
    refetchInterval,
  });
}

export function useFilteredRuns(
  page = 0,
  size = 20,
  status?: RunStatus | null,
  name?: string | null,
  label?: string | null,
) {
  return useQuery({
    queryKey: ['runs', page, size, status, name, label],
    queryFn: async () => {
      const params: Record<string, string | number> = { page, size };
      if (status) params.status = status;
      if (name) params.name = name;
      if (label) params.label = label;
      const { data } = await apiClient.get<Page<TestRunSummary>>('/runs', { params });
      return data;
    },
  });
}

export function useRunLabels() {
  return useQuery({
    queryKey: ['runs', 'labels'],
    queryFn: async () => {
      const { data } = await apiClient.get<string[]>('/runs/labels');
      return data;
    },
  });
}

export function useRunDetail(runId: string | undefined, refetchInterval?: number | false) {
  return useQuery({
    queryKey: ['runs', runId, 'detail'],
    queryFn: async () => {
      const { data } = await apiClient.get<TestRunHierarchy>(`/runs/${runId}/detail`);
      return data;
    },
    enabled: !!runId,
    refetchInterval,
  });
}

export function useRunSummaryStats(refetchInterval?: number | false) {
  return useQuery({
    queryKey: ['runs', 'summary'],
    queryFn: async () => {
      const { data } = await apiClient.get<Record<string, number>>('/runs/summary');
      return data;
    },
    refetchInterval,
  });
}

export function useRunComparison(baseRunId: string | undefined, compareRunId: string | undefined) {
  return useQuery({
    queryKey: ['runs', baseRunId, 'compare', compareRunId],
    queryFn: async () => {
      const { data } = await apiClient.get<RunComparison>(`/runs/${baseRunId}/compare`, {
        params: { with: compareRunId },
      });
      return data;
    },
    enabled: !!baseRunId && !!compareRunId,
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

export function useOverallFlakiness() {
  return useQuery({
    queryKey: ['flakiness', 'report'],
    queryFn: async () => {
      const { data } = await apiClient.get<FlakinessReport>('/flakiness/report');
      return data;
    },
  });
}

export function useTopFlakyTests(limit = 10) {
  return useQuery({
    queryKey: ['flakiness', 'top', limit],
    queryFn: async () => {
      const { data } = await apiClient.get<FlakyTest[]>('/flakiness/top', {
        params: { limit },
      });
      return data;
    },
  });
}

export function usePassRateTrend(limit: number) {
  return useQuery({
    queryKey: ['trends', 'pass-rate', limit],
    queryFn: async () => {
      const { data } = await apiClient.get<TrendData>('/trends/pass-rate', { params: { limit } });
      return data;
    },
  });
}

export function useDurationTrend(limit: number) {
  return useQuery({
    queryKey: ['trends', 'duration', limit],
    queryFn: async () => {
      const { data } = await apiClient.get<TrendData>('/trends/duration', { params: { limit } });
      return data;
    },
  });
}

export function useTestCountTrend(limit: number) {
  return useQuery({
    queryKey: ['trends', 'test-count', limit],
    queryFn: async () => {
      const { data } = await apiClient.get<TrendData>('/trends/test-count', { params: { limit } });
      return data;
    },
  });
}

export function useFlakinessTrend(limit: number) {
  return useQuery({
    queryKey: ['trends', 'flakiness', limit],
    queryFn: async () => {
      const { data } = await apiClient.get<TrendData>('/trends/flakiness', { params: { limit } });
      return data;
    },
  });
}

export function useFailureHotspots(limit: number, recentRuns: number) {
  return useQuery({
    queryKey: ['failure-insights', 'hotspots', limit, recentRuns],
    queryFn: async () => {
      const { data } = await apiClient.get<FailureHotspot[]>('/failure-insights/hotspots', { params: { limit, recentRuns } });
      return data;
    },
  });
}

export function useFailureClusters(limit: number, recentRuns: number) {
  return useQuery({
    queryKey: ['failure-insights', 'clusters', limit, recentRuns],
    queryFn: async () => {
      const { data } = await apiClient.get<FailureCluster[]>('/failure-insights/clusters', { params: { limit, recentRuns } });
      return data;
    },
  });
}

export function useFailureInsights(recentRuns: number) {
  return useQuery({
    queryKey: ['failure-insights', 'summary', recentRuns],
    queryFn: async () => {
      const { data } = await apiClient.get<FailureInsights>('/failure-insights/summary', { params: { recentRuns } });
      return data;
    },
  });
}

export function usePerformanceReport(limit: number, recentRuns: number) {
  return useQuery({
    queryKey: ['performance', 'report', limit, recentRuns],
    queryFn: async () => {
      const { data } = await apiClient.get<PerformanceReport>('/performance/report', { params: { limit, recentRuns } });
      return data;
    },
  });
}

export function useInsightsSummary(recentRuns: number) {
  return useQuery({
    queryKey: ['insights', 'summary', recentRuns],
    queryFn: async () => {
      const { data } = await apiClient.get<InsightsSummary>('/insights/summary', { params: { recentRuns } });
      return data;
    },
  });
}

export function useArchivedRuns(page = 0, size = 20) {
  return useQuery({
    queryKey: ['archived-runs', page, size],
    queryFn: async () => {
      const { data } = await apiClient.get<Page<TestRunSummary>>('/runs/archived', { params: { page, size } });
      return data;
    },
  });
}

export function useArchiveRuns() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (ids: string[]) => {
      await apiClient.post('/runs/archive/bulk', { ids });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['runs'] });
      queryClient.invalidateQueries({ queryKey: ['archived-runs'] });
    },
  });
}

export function useUnarchiveRun() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (runId: string) => {
      await apiClient.post(`/runs/${runId}/unarchive`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['runs'] });
      queryClient.invalidateQueries({ queryKey: ['archived-runs'] });
    },
  });
}
