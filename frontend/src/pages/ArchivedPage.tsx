import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useArchivedRuns, useUnarchiveRun } from '@/hooks/useApi';
import { formatDuration, formatRelativeTime, formatPassRate } from '@/utils/format';
import { RUN_STATUS_COLORS, RUN_STATUS_TEXT_COLORS } from '@/utils/status';

export default function ArchivedPage() {
  const [page, setPage] = useState(0);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  const { data: runsPage, isLoading, isError } = useArchivedRuns(page, 20);
  const unarchiveMutation = useUnarchiveRun();

  const allIds = runsPage?.content.map((r) => r.id) ?? [];
  const allSelected = allIds.length > 0 && allIds.every((id) => selectedIds.includes(id));

  function toggleSelectAll() {
    if (allSelected) {
      setSelectedIds((prev) => prev.filter((id) => !allIds.includes(id)));
    } else {
      setSelectedIds((prev) => [...new Set([...prev, ...allIds])]);
    }
  }

  function handleUnarchive() {
    const ids = [...selectedIds];
    const next = async () => {
      for (const id of ids) {
        await unarchiveMutation.mutateAsync(id);
      }
      setSelectedIds([]);
    };
    next();
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Archive</h1>
          <p className="text-xs text-gray-500 mt-0.5">
            Archived runs are excluded from analytics and trends.
          </p>
        </div>
        {selectedIds.length > 0 && (
          <div className="flex items-center gap-2">
            <span className="text-xs text-gray-500">{selectedIds.length} selected</span>
            <button
              type="button"
              onClick={handleUnarchive}
              disabled={unarchiveMutation.isPending}
              className="px-3 py-1.5 text-xs font-medium bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50"
            >
              Unarchive
            </button>
          </div>
        )}
      </div>

      <div className="bg-white rounded-lg border border-gray-200">
        {isLoading ? (
          <div className="p-6 text-center text-sm text-gray-500">Loading...</div>
        ) : isError ? (
          <div className="p-6 text-center text-sm text-red-500">Failed to load archived runs.</div>
        ) : !runsPage || runsPage.empty ? (
          <div className="p-6 text-center">
            <p className="text-sm text-gray-500 mb-1">No archived runs</p>
            <p className="text-xs text-gray-400">
              Select runs on the Test Runs page and click Archive, or drag a row onto the Archive item in the sidebar.
            </p>
          </div>
        ) : (
          <>
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-gray-500 border-b border-gray-100">
                  <th className="px-3 py-2 w-8" scope="col">
                    <input
                      type="checkbox"
                      aria-label="Select all archived runs"
                      checked={allSelected}
                      onChange={toggleSelectAll}
                      className="w-3.5 h-3.5 rounded border-gray-300 text-green-600 cursor-pointer"
                    />
                  </th>
                  <th className="px-4 py-2 font-medium">Status</th>
                  <th className="px-4 py-2 font-medium">Name</th>
                  <th className="px-4 py-2 font-medium">Tests</th>
                  <th className="px-4 py-2 font-medium">Pass Rate</th>
                  <th className="px-4 py-2 font-medium">Duration</th>
                  <th className="px-4 py-2 font-medium">Started</th>
                </tr>
              </thead>
              <tbody>
                {runsPage.content.map((run) => {
                  const isSelected = selectedIds.includes(run.id);
                  return (
                    <tr
                      key={run.id}
                      className={`border-b border-gray-50 hover:bg-gray-50 ${isSelected ? 'bg-green-50' : ''}`}
                    >
                      <td className="px-3 py-2.5">
                        <input
                          type="checkbox"
                          aria-label={`Select run ${run.name}`}
                          checked={isSelected}
                          onChange={() =>
                            setSelectedIds((prev) =>
                              prev.includes(run.id)
                                ? prev.filter((id) => id !== run.id)
                                : [...prev, run.id],
                            )
                          }
                          className="w-3.5 h-3.5 rounded border-gray-300 text-green-600 cursor-pointer"
                        />
                      </td>
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
                  );
                })}
              </tbody>
            </table>

            {/* Pagination */}
            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100">
              <p className="text-xs text-gray-500">
                {runsPage.totalElements} run{runsPage.totalElements !== 1 ? 's' : ''} archived
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
