import { useState } from 'react';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import { usePassRateTrend, useDurationTrend, useTestCountTrend, useFlakinessTrend } from '@/hooks/useApi';
import { formatDuration } from '@/utils/format';
import type { TrendData } from '@/api/types';

const RANGE_OPTIONS = [
  { label: '10 runs', value: 10 },
  { label: '30 runs', value: 30 },
  { label: '50 runs', value: 50 },
  { label: '100 runs', value: 100 },
];

export default function TrendsPage() {
  const [limit, setLimit] = useState(30);

  const { data: passRateData, isLoading: loadingPassRate } = usePassRateTrend(limit);
  const { data: durationData, isLoading: loadingDuration } = useDurationTrend(limit);
  const { data: testCountData, isLoading: loadingTestCount } = useTestCountTrend(limit);
  const { data: flakinessData, isLoading: loadingFlakiness } = useFlakinessTrend(limit);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Trend Analysis</h1>
        <div className="flex gap-1">
          {RANGE_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => setLimit(opt.value)}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                limit === opt.value
                  ? 'bg-gray-900 text-white'
                  : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <TrendChart
          title="Pass Rate (%)"
          data={passRateData}
          isLoading={loadingPassRate}
          color="#22c55e"
          yDomain={[0, 100]}
          valueFormatter={(v) => `${v.toFixed(1)}%`}
          yTickFormatter={(v) => `${v}%`}
        />
        <TrendChart
          title="Run Duration"
          data={durationData}
          isLoading={loadingDuration}
          color="#3b82f6"
          valueFormatter={(v) => formatDuration(v) ?? '—'}
          yTickFormatter={(v) => formatDuration(v) ?? '—'}
        />
        <TrendChart
          title="Test Count"
          data={testCountData}
          isLoading={loadingTestCount}
          color="#8b5cf6"
          valueFormatter={(v) => `${Math.round(v)} tests`}
          yTickFormatter={(v) => String(Math.round(v))}
        />
        <TrendChart
          title="Flakiness (%)"
          data={flakinessData}
          isLoading={loadingFlakiness}
          color="#f59e0b"
          yDomain={[0, 100]}
          valueFormatter={(v) => `${v.toFixed(1)}%`}
          yTickFormatter={(v) => `${v}%`}
        />
      </div>
    </div>
  );
}

// ============ Chart card component ============

interface TrendChartProps {
  title: string;
  data: TrendData | undefined;
  isLoading: boolean;
  color: string;
  yDomain?: [number | 'auto', number | 'auto'];
  valueFormatter: (v: number) => string;
  yTickFormatter: (v: number) => string;
}

function TrendChart({ title, data, isLoading, color, yDomain, valueFormatter, yTickFormatter }: TrendChartProps) {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4">
      <h2 className="text-sm font-semibold text-gray-700 mb-4">{title}</h2>

      {isLoading ? (
        <div className="h-48 flex items-center justify-center">
          <p className="text-sm text-gray-400">Loading...</p>
        </div>
      ) : !data || data.points.length === 0 ? (
        <div className="h-48 flex items-center justify-center">
          <p className="text-sm text-gray-400">Not enough data yet</p>
        </div>
      ) : (
        <>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart
              data={data.points.map((p) => ({
                name: p.runName.length > 16 ? `${p.runName.substring(0, 16)}…` : p.runName,
                value: p.value,
                fullName: p.runName,
              }))}
              margin={{ top: 5, right: 10, bottom: 5, left: 0 }}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
              <XAxis
                dataKey="name"
                tick={{ fontSize: 10, fill: '#9ca3af' }}
                tickLine={false}
                axisLine={false}
              />
              <YAxis
                domain={yDomain ?? ['auto', 'auto']}
                tick={{ fontSize: 10, fill: '#9ca3af' }}
                tickLine={false}
                axisLine={false}
                tickFormatter={yTickFormatter}
                width={48}
              />
              <Tooltip
                contentStyle={{ fontSize: 12, border: '1px solid #e5e7eb', borderRadius: 6 }}
                formatter={(value: number | undefined) => [
                  value !== undefined && value !== null ? valueFormatter(value) : '—',
                  title,
                ]}
                labelFormatter={(_label, payload) =>
                  payload && payload[0] ? (payload[0].payload as { fullName: string }).fullName : _label
                }
              />
              <Line
                type="monotone"
                dataKey="value"
                stroke={color}
                strokeWidth={2}
                dot={{ r: 3, fill: color, strokeWidth: 0 }}
                activeDot={{ r: 5 }}
                connectNulls={false}
              />
            </LineChart>
          </ResponsiveContainer>
          <p className="text-xs text-gray-400 mt-1 text-right">{data.points.length} data point{data.points.length !== 1 ? 's' : ''}</p>
        </>
      )}
    </div>
  );
}
