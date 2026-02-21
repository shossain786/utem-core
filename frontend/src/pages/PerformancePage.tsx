import { useState } from 'react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import { usePerformanceReport } from '@/hooks/useApi';
import { formatDuration } from '@/utils/format';

const RANGE_OPTIONS = [
  { label: '10 runs', value: 10 },
  { label: '20 runs', value: 20 },
  { label: '30 runs', value: 30 },
  { label: '50 runs', value: 50 },
];

const NODE_TYPE_COLORS: Record<string, string> = {
  SUITE: '#6366f1',
  FEATURE: '#22c55e',
  SCENARIO: '#3b82f6',
  STEP: '#8b5cf6',
};

const NODE_TYPE_TEXT_COLORS: Record<string, string> = {
  SCENARIO: 'bg-blue-100 text-blue-700',
  STEP: 'bg-purple-100 text-purple-700',
  SUITE: 'bg-indigo-100 text-indigo-700',
  FEATURE: 'bg-green-100 text-green-700',
};

export default function PerformancePage() {
  const [recentRuns, setRecentRuns] = useState(30);
  const { data: report, isLoading } = usePerformanceReport(20, recentRuns);

  const chartData = report
    ? Object.entries(report.durationByNodeType).map(([type, stats]) => ({
        type,
        avgMs: stats.avgMs,
        maxMs: stats.maxMs,
      }))
    : [];

  const slowest = report?.slowestTests?.[0];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Performance</h1>
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

      {/* Summary cards */}
      {report && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
          <StatCard label="Runs Analyzed" value={String(report.recentRunsAnalyzed)} />
          <StatCard label="Tests Profiled" value={String(report.slowestTests.length)} />
          <StatCard
            label="Slowest Avg"
            value={slowest ? formatDuration(slowest.avgDurationMs) : '—'}
            sub={slowest?.testName ?? undefined}
          />
          <StatCard
            label="Node Types"
            value={String(Object.keys(report.durationByNodeType).length)}
          />
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
        {/* Duration by node type bar chart */}
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Avg Duration by Node Type</h2>
          {isLoading ? (
            <div className="h-48 flex items-center justify-center text-sm text-gray-400">Loading...</div>
          ) : chartData.length === 0 ? (
            <div className="h-48 flex items-center justify-center text-sm text-gray-400">No data yet</div>
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={chartData} margin={{ top: 5, right: 10, bottom: 5, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
                <XAxis dataKey="type" tick={{ fontSize: 11, fill: '#9ca3af' }} tickLine={false} axisLine={false} />
                <YAxis
                  tick={{ fontSize: 10, fill: '#9ca3af' }}
                  tickLine={false}
                  axisLine={false}
                  tickFormatter={(v: number) => formatDuration(v)}
                  width={52}
                />
                <Tooltip
                  contentStyle={{ fontSize: 12, border: '1px solid #e5e7eb', borderRadius: 6 }}
                  formatter={(value: number | undefined) => [
                    value !== undefined ? formatDuration(value) : '—',
                    'Avg duration',
                  ]}
                />
                <Bar
                  dataKey="avgMs"
                  fill="#3b82f6"
                  radius={[3, 3, 0, 0]}
                  // Color each bar by node type
                  label={false}
                />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Top 5 slowest summary */}
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Top 5 Slowest</h2>
          {isLoading ? (
            <div className="h-48 flex items-center justify-center text-sm text-gray-400">Loading...</div>
          ) : !report || report.slowestTests.length === 0 ? (
            <div className="h-48 flex items-center justify-center text-sm text-gray-400">No data yet</div>
          ) : (
            <div className="space-y-2">
              {report.slowestTests.slice(0, 5).map((t, i) => (
                <div key={i} className="flex items-center gap-2">
                  <span className="text-xs text-gray-400 w-4 shrink-0">{i + 1}.</span>
                  <span
                    className={`inline-block px-1.5 py-0.5 text-xs rounded font-medium shrink-0 ${NODE_TYPE_TEXT_COLORS[t.nodeType] ?? 'bg-gray-100 text-gray-600'}`}
                  >
                    {t.nodeType}
                  </span>
                  <span className="text-xs text-gray-700 truncate flex-1" title={t.testName}>{t.testName}</span>
                  <span className="text-xs font-medium text-gray-900 shrink-0">{formatDuration(t.avgDurationMs)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Full slowest tests table */}
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="px-4 py-3 border-b border-gray-100">
          <h2 className="text-sm font-semibold text-gray-700">
            Slowest Tests
            {report && <span className="ml-2 text-xs font-normal text-gray-400">{report.slowestTests.length} tests</span>}
          </h2>
        </div>

        {isLoading ? (
          <div className="p-8 text-center text-sm text-gray-400">Loading...</div>
        ) : !report || report.slowestTests.length === 0 ? (
          <div className="p-8 text-center text-sm text-gray-400">No performance data yet. Run some tests first.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-gray-500 border-b border-gray-100">
                  <th className="px-4 py-2 font-medium w-8">#</th>
                  <th className="px-4 py-2 font-medium">Test Name</th>
                  <th className="px-4 py-2 font-medium">Type</th>
                  <th className="px-4 py-2 font-medium">Avg Duration</th>
                  <th className="px-4 py-2 font-medium">Max</th>
                  <th className="px-4 py-2 font-medium">Min</th>
                  <th className="px-4 py-2 font-medium">Runs</th>
                </tr>
              </thead>
              <tbody>
                {report.slowestTests.map((t, i) => {
                  const maxDuration = report.slowestTests[0]?.avgDurationMs ?? 1;
                  const pct = Math.round((t.avgDurationMs / maxDuration) * 100);
                  return (
                    <tr key={i} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                      <td className="px-4 py-2.5 text-xs text-gray-400">{i + 1}</td>
                      <td className="px-4 py-2.5 max-w-xs">
                        <span className="text-gray-900 truncate block" title={t.testName}>{t.testName}</span>
                      </td>
                      <td className="px-4 py-2.5">
                        <span className={`inline-block px-1.5 py-0.5 text-xs rounded font-medium ${NODE_TYPE_TEXT_COLORS[t.nodeType] ?? 'bg-gray-100 text-gray-600'}`}
                          style={{ color: NODE_TYPE_COLORS[t.nodeType] }}>
                          {t.nodeType}
                        </span>
                      </td>
                      <td className="px-4 py-2.5">
                        <div className="flex items-center gap-2">
                          <div className="w-16 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                            <div className="h-full bg-blue-400 rounded-full" style={{ width: `${pct}%` }} />
                          </div>
                          <span className="text-xs font-medium text-gray-900">{formatDuration(t.avgDurationMs)}</span>
                        </div>
                      </td>
                      <td className="px-4 py-2.5 text-xs text-gray-500">{formatDuration(t.maxDurationMs)}</td>
                      <td className="px-4 py-2.5 text-xs text-gray-500">{formatDuration(t.minDurationMs)}</td>
                      <td className="px-4 py-2.5 text-xs text-gray-500">{t.runCount}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function StatCard({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4">
      <p className="text-xs text-gray-500 mb-1">{label}</p>
      <p className="text-xl font-bold text-gray-900">{value}</p>
      {sub && <p className="text-xs text-gray-400 mt-0.5 truncate" title={sub}>{sub}</p>}
    </div>
  );
}
