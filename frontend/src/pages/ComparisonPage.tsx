import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useRunComparison } from '@/hooks/useApi';
import { formatDuration, formatRelativeTime } from '@/utils/format';
import { RUN_STATUS_TEXT_COLORS } from '@/utils/status';
import type { DiffType, NodeDiffEntry } from '@/api/types';

type DiffFilter = 'ALL' | DiffType;

const DIFF_FILTERS: Array<{ label: string; value: DiffFilter }> = [
  { label: 'All', value: 'ALL' },
  { label: 'Regressions', value: 'REGRESSION' },
  { label: 'Fixes', value: 'FIX' },
  { label: 'New', value: 'NEW' },
  { label: 'Removed', value: 'REMOVED' },
  { label: 'Unchanged', value: 'UNCHANGED' },
];

const DIFF_STYLES: Record<DiffType, { badge: string; row: string; label: string }> = {
  REGRESSION: { badge: 'bg-red-100 text-red-700', row: 'bg-red-50', label: 'Regression' },
  FIX: { badge: 'bg-green-100 text-green-700', row: 'bg-green-50', label: 'Fix' },
  NEW: { badge: 'bg-blue-100 text-blue-700', row: '', label: 'New' },
  REMOVED: { badge: 'bg-gray-100 text-gray-600', row: 'opacity-60', label: 'Removed' },
  UNCHANGED: { badge: 'bg-gray-50 text-gray-500', row: '', label: 'Unchanged' },
};

function DiffArrow({ value, format }: { value: number | null; format: (v: number) => string }) {
  if (value === null || value === 0) return <span className="text-gray-400">—</span>;
  const isPositive = value > 0;
  return (
    <span className={isPositive ? 'text-red-600' : 'text-green-600'}>
      {isPositive ? '+' : ''}{format(value)}
    </span>
  );
}

export default function ComparisonPage() {
  const { runId, compareRunId } = useParams<{ runId: string; compareRunId: string }>();
  const { data: comparison, isLoading, isError } = useRunComparison(runId, compareRunId);
  const [filter, setFilter] = useState<DiffFilter>('ALL');

  if (isLoading) {
    return <div className="p-6 text-center text-sm text-gray-500">Loading comparison...</div>;
  }

  if (isError || !comparison) {
    return (
      <div>
        <BackLink runId={runId} />
        <div className="bg-white rounded-lg border border-gray-200 p-6 text-center">
          <p className="text-sm text-red-500">Failed to load comparison.</p>
        </div>
      </div>
    );
  }

  const { baseRun, compareRun } = comparison;

  const filteredDiffs: NodeDiffEntry[] =
    filter === 'ALL'
      ? comparison.nodeDiffs
      : comparison.nodeDiffs.filter((d) => d.diffType === filter);

  const regressionCount = comparison.nodeDiffs.filter((d) => d.diffType === 'REGRESSION').length;
  const fixCount = comparison.nodeDiffs.filter((d) => d.diffType === 'FIX').length;
  const newCount = comparison.nodeDiffs.filter((d) => d.diffType === 'NEW').length;
  const removedCount = comparison.nodeDiffs.filter((d) => d.diffType === 'REMOVED').length;

  return (
    <div>
      <BackLink runId={runId} />

      {/* Header */}
      <div className="bg-white rounded-lg border border-gray-200 p-4 mb-4">
        <h1 className="text-lg font-bold text-gray-900 mb-3">Run Comparison</h1>
        <div className="grid grid-cols-2 gap-4">
          {/* Base run */}
          <div className="border border-gray-100 rounded-lg p-3">
            <p className="text-xs text-gray-400 mb-1 font-medium uppercase tracking-wide">Base</p>
            <Link to={`/runs/${baseRun.id}`} className="font-medium text-gray-900 hover:text-blue-600 text-sm">
              {baseRun.name}
            </Link>
            <div className="flex items-center gap-2 mt-1 text-xs text-gray-500">
              <span className={RUN_STATUS_TEXT_COLORS[baseRun.status]}>{baseRun.status}</span>
              <span>·</span>
              <span>{formatRelativeTime(baseRun.startTime)}</span>
            </div>
          </div>
          {/* Compare run */}
          <div className="border border-gray-100 rounded-lg p-3">
            <p className="text-xs text-gray-400 mb-1 font-medium uppercase tracking-wide">Compare</p>
            <Link to={`/runs/${compareRun.id}`} className="font-medium text-gray-900 hover:text-blue-600 text-sm">
              {compareRun.name}
            </Link>
            <div className="flex items-center gap-2 mt-1 text-xs text-gray-500">
              <span className={RUN_STATUS_TEXT_COLORS[compareRun.status]}>{compareRun.status}</span>
              <span>·</span>
              <span>{formatRelativeTime(compareRun.startTime)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Summary diff cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
        <DiffCard label="Regressions" count={regressionCount} colorClass="text-red-600" />
        <DiffCard label="Fixes" count={fixCount} colorClass="text-green-600" />
        <DiffCard label="New tests" count={newCount} colorClass="text-blue-600" />
        <DiffCard label="Removed" count={removedCount} colorClass="text-gray-500" />
      </div>

      {/* Stats diff */}
      <div className="bg-white rounded-lg border border-gray-200 p-4 mb-4">
        <h2 className="text-sm font-semibold text-gray-700 mb-3">Summary Delta (compare vs base)</h2>
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 text-center text-sm">
          <div>
            <p className="text-xs text-gray-400">Total</p>
            <DiffArrow value={comparison.totalTestsDiff} format={(v) => String(v)} />
          </div>
          <div>
            <p className="text-xs text-gray-400">Passed</p>
            <DiffArrow value={-comparison.passedTestsDiff} format={(v) => String(Math.abs(v))} />
          </div>
          <div>
            <p className="text-xs text-gray-400">Failed</p>
            <DiffArrow value={comparison.failedTestsDiff} format={(v) => String(v)} />
          </div>
          <div>
            <p className="text-xs text-gray-400">Pass Rate</p>
            <DiffArrow value={comparison.passRateDiff} format={(v) => `${v.toFixed(1)}%`} />
          </div>
          <div>
            <p className="text-xs text-gray-400">Duration</p>
            <DiffArrow value={comparison.durationDiff} format={(v) => formatDuration(Math.abs(v)) ?? '—'} />
          </div>
        </div>
      </div>

      {/* Node diff table */}
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
          <h2 className="text-sm font-semibold text-gray-700">
            Test Differences
            <span className="ml-2 text-xs font-normal text-gray-400">({comparison.nodeDiffs.length} tests)</span>
          </h2>
          <div className="flex gap-1">
            {DIFF_FILTERS.map((f) => (
              <button
                key={f.value}
                type="button"
                onClick={() => setFilter(f.value)}
                className={`px-2 py-1 text-xs font-medium rounded-md transition-colors ${
                  filter === f.value
                    ? 'bg-gray-900 text-white'
                    : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>

        {filteredDiffs.length === 0 ? (
          <div className="p-6 text-center text-sm text-gray-400">No tests match this filter.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-gray-500 border-b border-gray-100">
                <th className="px-4 py-2 font-medium">Type</th>
                <th className="px-4 py-2 font-medium">Test Name</th>
                <th className="px-4 py-2 font-medium">Base Status</th>
                <th className="px-4 py-2 font-medium">Compare Status</th>
                <th className="px-4 py-2 font-medium">Duration Delta</th>
              </tr>
            </thead>
            <tbody>
              {filteredDiffs.map((diff, i) => {
                const style = DIFF_STYLES[diff.diffType];
                const durationDelta =
                  diff.baseDuration !== null && diff.compareDuration !== null
                    ? diff.compareDuration - diff.baseDuration
                    : null;
                return (
                  <tr key={i} className={`border-b border-gray-50 ${style.row}`}>
                    <td className="px-4 py-2.5">
                      <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${style.badge}`}>
                        {style.label}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-gray-900 font-medium max-w-xs truncate" title={diff.name}>
                      {diff.name}
                      <span className="ml-1.5 text-xs font-normal text-gray-400">{diff.nodeType}</span>
                    </td>
                    <td className="px-4 py-2.5 text-xs text-gray-500">{diff.baseStatus ?? '—'}</td>
                    <td className="px-4 py-2.5 text-xs text-gray-500">{diff.compareStatus ?? '—'}</td>
                    <td className="px-4 py-2.5 text-xs">
                      <DiffArrow value={durationDelta} format={(v) => formatDuration(Math.abs(v)) ?? '—'} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

function DiffCard({ label, count, colorClass }: { label: string; count: number; colorClass: string }) {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-3 text-center">
      <p className="text-xs text-gray-400 mb-1">{label}</p>
      <p className={`text-xl font-bold ${colorClass}`}>{count}</p>
    </div>
  );
}

function BackLink({ runId }: { runId: string | undefined }) {
  return (
    <Link
      to={runId ? `/runs/${runId}` : '/runs'}
      className="inline-flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700 mb-4"
    >
      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
      </svg>
      Back to Run
    </Link>
  );
}
