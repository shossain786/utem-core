import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useInsightsSummary } from '@/hooks/useApi';
import { formatDuration } from '@/utils/format';

const RANGE_OPTIONS = [
  { label: '10 runs', value: 10 },
  { label: '20 runs', value: 20 },
  { label: '30 runs', value: 30 },
  { label: '50 runs', value: 50 },
];

export default function InsightsPage() {
  const [recentRuns, setRecentRuns] = useState(30);
  const { data: summary, isLoading } = useInsightsSummary(recentRuns);

  const score = summary?.overallHealthScore ?? null;
  const scoreColor = score === null ? 'text-gray-400'
    : score >= 80 ? 'text-green-600'
    : score >= 60 ? 'text-yellow-600'
    : 'text-red-600';
  const scoreBg = score === null ? 'bg-gray-100'
    : score >= 80 ? 'bg-green-50 border-green-200'
    : score >= 60 ? 'bg-yellow-50 border-yellow-200'
    : 'bg-red-50 border-red-200';

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Insights</h1>
        <div className="flex gap-1">
          {RANGE_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => setRecentRuns(opt.value)}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                recentRuns === opt.value
                  ? 'bg-gray-900 text-white'
                  : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-sm text-gray-400">Loading insights...</div>
      ) : !summary ? (
        <div className="p-12 text-center text-sm text-gray-400">No data available yet.</div>
      ) : (
        <>
          {/* Health score + quick stats */}
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-4 mb-4">
            {/* Health Score */}
            <div className={`rounded-lg border p-6 flex flex-col items-center justify-center ${scoreBg}`}>
              <p className="text-xs font-medium text-gray-500 mb-2">Health Score</p>
              <p className={`text-5xl font-bold ${scoreColor}`}>
                {summary.overallHealthScore.toFixed(0)}
              </p>
              <p className="text-xs text-gray-400 mt-1">out of 100</p>
              <p className="text-xs font-medium mt-2" style={{ color: scoreColor.replace('text-', '') }}>
                {summary.overallHealthScore >= 80 ? 'Good' : summary.overallHealthScore >= 60 ? 'Needs attention' : 'At risk'}
              </p>
            </div>

            {/* Quick stat cards */}
            <div className="bg-white rounded-lg border border-gray-200 p-4 flex flex-col justify-center">
              <p className="text-xs text-gray-500 mb-1">Runs Analyzed</p>
              <p className="text-3xl font-bold text-gray-900">{summary.recentRunsAnalyzed}</p>
            </div>
            <div className="bg-white rounded-lg border border-gray-200 p-4 flex flex-col justify-center">
              <p className="text-xs text-gray-500 mb-1">Failure Hotspots</p>
              <p className="text-3xl font-bold text-red-600">{summary.totalHotspots}</p>
              <Link to="/failures" className="text-xs text-blue-500 hover:text-blue-700 mt-1">View details →</Link>
            </div>
            <div className="bg-white rounded-lg border border-gray-200 p-4 flex flex-col justify-center">
              <p className="text-xs text-gray-500 mb-1">Error Clusters</p>
              <p className="text-3xl font-bold text-orange-600">{summary.totalFailureClusters}</p>
              <Link to="/failures" className="text-xs text-blue-500 hover:text-blue-700 mt-1">View clusters →</Link>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            {/* Top Failures */}
            <div className="bg-white rounded-lg border border-gray-200">
              <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
                <h2 className="text-sm font-semibold text-gray-700">Top Failures</h2>
                <Link to="/failures" className="text-xs text-blue-500 hover:text-blue-700">See all →</Link>
              </div>
              {summary.topFailures.length === 0 ? (
                <div className="p-6 text-center text-xs text-gray-400">No failures detected</div>
              ) : (
                <div className="divide-y divide-gray-50">
                  {summary.topFailures.map((f, i) => (
                    <div key={i} className="px-4 py-2.5 flex items-center gap-2">
                      <span className={`text-xs font-bold min-w-8 ${f.failRate >= 75 ? 'text-red-600' : f.failRate >= 50 ? 'text-orange-500' : 'text-yellow-600'}`}>
                        {f.failRate.toFixed(0)}%
                      </span>
                      <span className="text-xs text-gray-700 truncate flex-1" title={f.testName}>
                        {f.testName}
                      </span>
                      {f.lastRunId && (
                        <Link to={`/runs/${f.lastRunId}`} className="text-xs text-blue-400 hover:text-blue-600 shrink-0">→</Link>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Top Error Clusters */}
            <div className="bg-white rounded-lg border border-gray-200">
              <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
                <h2 className="text-sm font-semibold text-gray-700">Error Clusters</h2>
                <Link to="/failures" className="text-xs text-blue-500 hover:text-blue-700">See all →</Link>
              </div>
              {summary.topClusters.length === 0 ? (
                <div className="p-6 text-center text-xs text-gray-400">No error clusters found</div>
              ) : (
                <div className="divide-y divide-gray-50">
                  {summary.topClusters.map((c) => (
                    <div key={c.clusterId} className="px-4 py-2.5">
                      <div className="flex items-start gap-2">
                        <span className="inline-flex items-center justify-center min-w-5 h-5 bg-red-100 text-red-700 text-xs font-bold rounded-full shrink-0">
                          {c.occurrences}
                        </span>
                        <span className="text-xs text-gray-600 font-mono truncate" title={c.representativeError}>
                          {c.representativeError.length > 60
                            ? c.representativeError.substring(0, 60) + '…'
                            : c.representativeError}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Top Slow Tests */}
            <div className="bg-white rounded-lg border border-gray-200">
              <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
                <h2 className="text-sm font-semibold text-gray-700">Slowest Tests</h2>
                <Link to="/performance" className="text-xs text-blue-500 hover:text-blue-700">See all →</Link>
              </div>
              {summary.topSlowTests.length === 0 ? (
                <div className="p-6 text-center text-xs text-gray-400">No performance data</div>
              ) : (
                <div className="divide-y divide-gray-50">
                  {summary.topSlowTests.map((t, i) => (
                    <div key={i} className="px-4 py-2.5 flex items-center gap-2">
                      <span className="text-xs font-bold text-blue-600 min-w-12">
                        {formatDuration(t.avgDurationMs)}
                      </span>
                      <span className="text-xs text-gray-700 truncate flex-1" title={t.testName}>
                        {t.testName}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
