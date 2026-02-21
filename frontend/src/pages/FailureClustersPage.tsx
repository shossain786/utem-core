import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useFailureHotspots, useFailureClusters } from '@/hooks/useApi';
import type { FailureCluster } from '@/api/types';

const RANGE_OPTIONS = [
  { label: '10 runs', value: 10 },
  { label: '20 runs', value: 20 },
  { label: '30 runs', value: 30 },
  { label: '50 runs', value: 50 },
];

const NODE_TYPE_COLORS: Record<string, string> = {
  SCENARIO: 'bg-blue-100 text-blue-700',
  STEP: 'bg-purple-100 text-purple-700',
  SUITE: 'bg-gray-100 text-gray-700',
  FEATURE: 'bg-green-100 text-green-700',
};

export default function FailureClustersPage() {
  const [recentRuns, setRecentRuns] = useState(30);
  const [expandedCluster, setExpandedCluster] = useState<number | null>(null);

  const { data: hotspots = [], isLoading: loadingHotspots } = useFailureHotspots(20, recentRuns);
  const { data: clusters = [], isLoading: loadingClusters } = useFailureClusters(20, recentRuns);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Failure Analysis</h1>
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

      {/* Failure Hotspots */}
      <div className="bg-white rounded-lg border border-gray-200 mb-4">
        <div className="px-4 py-3 border-b border-gray-100">
          <h2 className="text-sm font-semibold text-gray-700">
            Failure Hotspots
            {!loadingHotspots && <span className="ml-2 text-xs font-normal text-gray-400">{hotspots.length} tests</span>}
          </h2>
        </div>

        {loadingHotspots ? (
          <div className="p-8 text-center text-sm text-gray-400">Loading...</div>
        ) : hotspots.length === 0 ? (
          <div className="p-8 text-center text-sm text-gray-400">No failures found in the last {recentRuns} runs.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-gray-500 border-b border-gray-100">
                  <th className="px-4 py-2 font-medium">Test Name</th>
                  <th className="px-4 py-2 font-medium">Type</th>
                  <th className="px-4 py-2 font-medium">Fail Rate</th>
                  <th className="px-4 py-2 font-medium">Failures</th>
                  <th className="px-4 py-2 font-medium">Last Error</th>
                  <th className="px-4 py-2 font-medium">Last Run</th>
                </tr>
              </thead>
              <tbody>
                {hotspots.map((h, i) => (
                  <tr key={i} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-2.5 max-w-xs">
                      <span className="text-gray-900 font-medium truncate block" title={h.testName}>
                        {h.testName}
                      </span>
                    </td>
                    <td className="px-4 py-2.5">
                      <span className={`inline-block px-1.5 py-0.5 text-xs rounded font-medium ${NODE_TYPE_COLORS[h.nodeType] ?? 'bg-gray-100 text-gray-600'}`}>
                        {h.nodeType}
                      </span>
                    </td>
                    <td className="px-4 py-2.5">
                      <div className="flex items-center gap-2">
                        <div className="w-20 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full ${h.failRate >= 75 ? 'bg-red-500' : h.failRate >= 50 ? 'bg-orange-400' : 'bg-yellow-400'}`}
                            style={{ width: `${h.failRate}%` }}
                          />
                        </div>
                        <span className={`text-xs font-medium ${h.failRate >= 75 ? 'text-red-600' : h.failRate >= 50 ? 'text-orange-500' : 'text-yellow-600'}`}>
                          {h.failRate.toFixed(0)}%
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-2.5 text-gray-600 text-xs">
                      {h.failCount}/{h.totalRuns} runs
                    </td>
                    <td className="px-4 py-2.5 max-w-xs">
                      {h.lastErrorMessage ? (
                        <span className="text-xs text-gray-500 truncate block" title={h.lastErrorMessage}>
                          {h.lastErrorMessage.length > 60 ? h.lastErrorMessage.substring(0, 60) + '…' : h.lastErrorMessage}
                        </span>
                      ) : (
                        <span className="text-xs text-gray-300">—</span>
                      )}
                    </td>
                    <td className="px-4 py-2.5">
                      {h.lastRunId ? (
                        <Link to={`/runs/${h.lastRunId}`} className="text-xs text-blue-500 hover:text-blue-700">
                          View run →
                        </Link>
                      ) : (
                        <span className="text-xs text-gray-300">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Error Clusters */}
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="px-4 py-3 border-b border-gray-100">
          <h2 className="text-sm font-semibold text-gray-700">
            Error Clusters
            {!loadingClusters && <span className="ml-2 text-xs font-normal text-gray-400">{clusters.length} distinct patterns</span>}
          </h2>
        </div>

        {loadingClusters ? (
          <div className="p-8 text-center text-sm text-gray-400">Loading...</div>
        ) : clusters.length === 0 ? (
          <div className="p-8 text-center text-sm text-gray-400">No error clusters detected.</div>
        ) : (
          <div className="divide-y divide-gray-50">
            {clusters.map((cluster) => (
              <ClusterRow
                key={cluster.clusterId}
                cluster={cluster}
                expanded={expandedCluster === cluster.clusterId}
                onToggle={() => setExpandedCluster(
                  expandedCluster === cluster.clusterId ? null : cluster.clusterId
                )}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ClusterRow({ cluster, expanded, onToggle }: {
  cluster: FailureCluster;
  expanded: boolean;
  onToggle: () => void;
}) {
  return (
    <div className="px-4 py-3">
      <div className="flex items-start gap-3">
        <span className="inline-flex items-center justify-center min-w-6 h-6 bg-red-100 text-red-700 text-xs font-bold rounded-full">
          {cluster.occurrences}
        </span>
        <div className="flex-1 min-w-0">
          <p className="text-xs font-mono text-gray-700 truncate" title={cluster.representativeError}>
            {cluster.representativeError.length > 120
              ? cluster.representativeError.substring(0, 120) + '…'
              : cluster.representativeError}
          </p>
          <div className="flex items-center gap-3 mt-1">
            <span className="text-xs text-gray-400">{cluster.affectedTests.length} test{cluster.affectedTests.length !== 1 ? 's' : ''}</span>
            <span className="text-xs text-gray-400">{cluster.runIds.length} run{cluster.runIds.length !== 1 ? 's' : ''}</span>
            <button
              type="button"
              onClick={onToggle}
              className="text-xs text-blue-500 hover:text-blue-700 transition-colors"
            >
              {expanded ? 'Hide details ↑' : 'Show details ↓'}
            </button>
          </div>
          {expanded && (
            <div className="mt-2 p-3 bg-gray-50 rounded-md">
              <p className="text-xs font-medium text-gray-600 mb-1">Affected tests:</p>
              <ul className="space-y-0.5">
                {cluster.affectedTests.map((t) => (
                  <li key={t} className="text-xs text-gray-700 font-mono">• {t}</li>
                ))}
              </ul>
              <p className="text-xs font-mono text-gray-500 mt-2 whitespace-pre-wrap break-all">
                {cluster.representativeError}
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
