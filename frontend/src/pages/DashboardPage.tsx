import { Link } from 'react-router-dom';
import { useRunSummaryStats, useRuns, useOverallFlakiness } from '@/hooks/useApi';
import { useWebSocket } from '@/hooks/useWebSocket';
import { formatDuration, formatRelativeTime, formatPassRate } from '@/utils/format';
import { RUN_STATUS_COLORS, RUN_STATUS_TEXT_COLORS } from '@/utils/status';

export default function DashboardPage() {
  const { status: wsStatus } = useWebSocket();
  // Poll every 10s when WS is disconnected so the UI stays current without WebSocket
  const pollingInterval = wsStatus !== 'connected' ? 10_000 : false;
  const { data: stats, isLoading: statsLoading, isError: statsError, dataUpdatedAt: statsUpdatedAt } =
    useRunSummaryStats(pollingInterval);
  const hasActiveRuns = (stats?.runningRuns ?? 0) > 0;
  // Keep runs list refreshed whenever tests are actively running
  const runsRefetchInterval = hasActiveRuns || wsStatus !== 'connected' ? 10_000 : false;
  const { data: runsPage, isLoading: runsLoading } = useRuns(0, 5, runsRefetchInterval);
  const { data: flakiness, isLoading: flakinessLoading } = useOverallFlakiness();

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <div className="flex items-center gap-3 text-xs text-gray-500">
          {statsUpdatedAt > 0 && (
            <span>Refreshed {formatRelativeTime(new Date(statsUpdatedAt).toISOString())}</span>
          )}
          <div className="flex items-center gap-1.5">
            <span
              className={`inline-block w-2 h-2 rounded-full ${
                wsStatus === 'connected'
                  ? 'bg-passed'
                  : wsStatus === 'connecting'
                    ? 'bg-running animate-pulse'
                    : 'bg-gray-400'
              }`}
            />
            <span>{wsStatus === 'connected' ? 'Live' : wsStatus === 'connecting' ? 'Connecting...' : 'Offline'}</span>
          </div>
        </div>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-4 mb-8">
        <StatCard
          label="Total Runs"
          value={statsLoading ? '...' : statsError ? '--' : String(stats?.totalRuns ?? 0)}
        />
        <StatCard
          label="Passed"
          value={statsLoading ? '...' : String(stats?.passedRuns ?? 0)}
          color="text-passed"
        />
        <StatCard
          label="Failed"
          value={statsLoading ? '...' : String(stats?.failedRuns ?? 0)}
          color="text-failed"
        />
        <StatCard
          label="Running"
          value={statsLoading ? '...' : String(stats?.runningRuns ?? 0)}
          color="text-running"
        />
        <StatCard
          label="Aborted"
          value={statsLoading ? '...' : String(stats?.abortedRuns ?? 0)}
          color="text-aborted"
        />
        <Link to="/flakiness">
          <StatCard
            label="Flakiness"
            value={flakinessLoading ? '...' : `${(flakiness?.flakinessPercentage ?? 0).toFixed(1)}%`}
            color={flakiness && flakiness.flakinessPercentage > 0 ? 'text-yellow-600' : 'text-gray-900'}
          />
        </Link>
      </div>

      {/* Recent Runs */}
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
          <h2 className="text-sm font-semibold text-gray-700">Recent Runs</h2>
          <Link to="/runs" className="text-xs text-blue-600 hover:text-blue-800">
            View all
          </Link>
        </div>

        {runsLoading ? (
          <div className="p-6 text-center text-sm text-gray-500">Loading...</div>
        ) : !runsPage || runsPage.empty ? (
          <div className="p-6 text-center">
            <p className="text-sm text-gray-500 mb-1">No test runs yet</p>
            <p className="text-xs text-gray-400">
              Send events to the API to see test runs appear here.
            </p>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-gray-500 border-b border-gray-100">
                <th className="px-4 py-2 font-medium">Status</th>
                <th className="px-4 py-2 font-medium">Name</th>
                <th className="px-4 py-2 font-medium">Tests</th>
                <th className="px-4 py-2 font-medium">Pass Rate</th>
                <th className="px-4 py-2 font-medium">Duration</th>
                <th className="px-4 py-2 font-medium">Started</th>
              </tr>
            </thead>
            <tbody>
              {runsPage.content.map((run) => (
                <tr key={run.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="px-4 py-2.5">
                    <span
                      className={`inline-flex items-center gap-1.5 text-xs font-medium ${RUN_STATUS_TEXT_COLORS[run.status]}`}
                    >
                      <span className={`w-1.5 h-1.5 rounded-full ${RUN_STATUS_COLORS[run.status]}`} />
                      {run.status}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 font-medium text-gray-900">{run.name}</td>
                  <td className="px-4 py-2.5 text-gray-600">{run.totalTests ?? '--'}</td>
                  <td className="px-4 py-2.5 text-gray-600">{formatPassRate(run.passRate)}</td>
                  <td className="px-4 py-2.5 text-gray-600">{formatDuration(run.duration)}</td>
                  <td className="px-4 py-2.5 text-gray-500">{formatRelativeTime(run.startTime)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

function StatCard({
  label,
  value,
  color = 'text-gray-900',
}: {
  label: string;
  value: string;
  color?: string;
}) {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4">
      <p className="text-sm text-gray-500 mb-1">{label}</p>
      <p className={`text-2xl font-bold ${color}`}>{value}</p>
    </div>
  );
}
