import { useNavigate } from 'react-router-dom';
import { useJobs } from '@/hooks/useApi';
import { formatRelativeTime } from '@/utils/format';
import { RUN_STATUS_COLORS, RUN_STATUS_TEXT_COLORS } from '@/utils/status';

export default function JobsPage() {
  const navigate = useNavigate();
  const { data: jobs, isLoading, isError } = useJobs();

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Jobs</h1>
        <p className="text-sm text-gray-500 mt-1">
          Named test pipelines — each card shows the latest run status.
        </p>
      </div>

      {isLoading ? (
        <div className="text-sm text-gray-500">Loading...</div>
      ) : isError ? (
        <div className="text-sm text-red-500">Failed to load jobs.</div>
      ) : !jobs || jobs.length === 0 ? (
        <div className="text-center py-16">
          <p className="text-sm text-gray-500 mb-1">No jobs yet</p>
          <p className="text-xs text-gray-400">
            Set <code className="bg-gray-100 px-1 rounded">-Dutem.job.name=MyJob</code> when
            running tests to group them under a named job.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {jobs.map((job) => (
            <button
              key={job.jobName}
              type="button"
              onClick={() => navigate(`/jobs/${encodeURIComponent(job.jobName)}`)}
              className="text-left bg-white border border-gray-200 rounded-lg p-5 hover:border-blue-300 hover:shadow-sm transition-all"
            >
              <div className="flex items-start justify-between gap-2 mb-3">
                <h2 className="font-semibold text-gray-900 text-sm leading-snug">
                  {job.jobName}
                </h2>
                <span
                  className={`inline-flex items-center gap-1 text-xs font-medium shrink-0 ${RUN_STATUS_TEXT_COLORS[job.latestStatus]}`}
                >
                  <span className={`w-1.5 h-1.5 rounded-full ${RUN_STATUS_COLORS[job.latestStatus]}`} />
                  {job.latestStatus}
                </span>
              </div>
              <p className="text-xs text-gray-400">
                {job.totalRuns} run{job.totalRuns !== 1 ? 's' : ''} &middot;{' '}
                last {formatRelativeTime(job.lastRunAt)}
              </p>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
