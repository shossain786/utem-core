import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useJobRuns } from '@/hooks/useApi';
import { formatDuration, formatRelativeTime, formatPassRate } from '@/utils/format';
import { RUN_STATUS_COLORS, RUN_STATUS_TEXT_COLORS } from '@/utils/status';

export default function JobRunsPage() {
  const { jobName: encodedJobName } = useParams<{ jobName: string }>();
  const jobName = encodedJobName ? decodeURIComponent(encodedJobName) : '';
  const [page, setPage] = useState(0);

  const { data: runsPage, isLoading, isError } = useJobRuns(jobName, page);

  return (
    <div>
      <div className="mb-6">
        <Link to="/jobs" className="text-xs text-gray-400 hover:text-gray-600 flex items-center gap-1 mb-2">
          ← Jobs
        </Link>
        <h1 className="text-2xl font-bold text-gray-900">{jobName}</h1>
      </div>

      <div className="bg-white rounded-lg border border-gray-200">
        {isLoading ? (
          <div className="p-6 text-center text-sm text-gray-500">Loading...</div>
        ) : isError ? (
          <div className="p-6 text-center text-sm text-red-500">Failed to load runs.</div>
        ) : !runsPage || runsPage.empty ? (
          <div className="p-6 text-center text-sm text-gray-500">No runs found for this job.</div>
        ) : (
          <>
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
                  <tr
                    key={run.id}
                    className="border-b border-gray-50 hover:bg-gray-50"
                  >
                    <td className="px-4 py-2.5">
                      <span
                        className={`inline-flex items-center gap-1.5 text-xs font-medium ${RUN_STATUS_TEXT_COLORS[run.status]}`}
                      >
                        <span className={`w-1.5 h-1.5 rounded-full ${RUN_STATUS_COLORS[run.status]}`} />
                        {run.status}
                      </span>
                    </td>
                    <td className="px-4 py-2.5">
                      <Link
                        to={`/runs/${run.id}`}
                        className="font-medium text-gray-900 hover:text-blue-600"
                      >
                        {run.name}
                      </Link>
                    </td>
                    <td className="px-4 py-2.5 text-gray-600">{run.totalTests ?? '--'}</td>
                    <td className="px-4 py-2.5 text-gray-600">{formatPassRate(run.passRate)}</td>
                    <td className="px-4 py-2.5 text-gray-600">{formatDuration(run.duration)}</td>
                    <td className="px-4 py-2.5 text-gray-500">{formatRelativeTime(run.startTime)}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100">
              <p className="text-xs text-gray-500">
                {runsPage.totalElements} run{runsPage.totalElements !== 1 ? 's' : ''} total
              </p>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={runsPage.first}
                  className="px-3 py-1 text-xs font-medium rounded-md border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <span className="text-xs text-gray-500">
                  Page {runsPage.number + 1} of {runsPage.totalPages}
                </span>
                <button
                  type="button"
                  onClick={() => setPage((p) => p + 1)}
                  disabled={runsPage.last}
                  className="px-3 py-1 text-xs font-medium rounded-md border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
