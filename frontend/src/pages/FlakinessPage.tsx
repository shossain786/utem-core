import { Link } from 'react-router-dom';
import { useOverallFlakiness, useTopFlakyTests } from '@/hooks/useApi';
import { NODE_STATUS_COLORS, NODE_STATUS_TEXT_COLORS } from '@/utils/status';
import type { FlakyTest, NodeType } from '@/api/types';

const NODE_TYPE_COLORS: Record<NodeType, string> = {
  SUITE: 'bg-blue-100 text-blue-700',
  FEATURE: 'bg-purple-100 text-purple-700',
  SCENARIO: 'bg-amber-100 text-amber-700',
  STEP: 'bg-gray-100 text-gray-600',
};

function FlakinessBar({ rate }: { rate: number }) {
  const color =
    rate >= 75 ? 'bg-red-500' :
    rate >= 50 ? 'bg-orange-500' :
    rate >= 25 ? 'bg-yellow-500' :
    'bg-yellow-400';

  return (
    <div className="flex items-center gap-2">
      <div className="w-20 h-2 bg-gray-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${Math.min(rate, 100)}%` }} />
      </div>
      <span className="text-xs text-gray-600 w-12">{rate.toFixed(1)}%</span>
    </div>
  );
}

function FlakyTestRow({ test }: { test: FlakyTest }) {
  return (
    <tr className="border-b border-gray-50 hover:bg-gray-50">
      <td className="px-4 py-2.5">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-gray-900">{test.testName}</span>
          {test.frameworkMarked && (
            <span className="px-1.5 py-0.5 text-[10px] font-medium rounded bg-yellow-100 text-yellow-700 shrink-0">
              Framework
            </span>
          )}
        </div>
      </td>
      <td className="px-4 py-2.5">
        <span className={`px-1.5 py-0.5 text-[10px] font-semibold rounded ${NODE_TYPE_COLORS[test.nodeType]}`}>
          {test.nodeType}
        </span>
      </td>
      <td className="px-4 py-2.5">
        <FlakinessBar rate={test.flakinessRate} />
      </td>
      <td className="px-4 py-2.5">
        <div className="flex items-center gap-2 text-xs">
          <span className="text-passed font-medium">{test.passCount}P</span>
          <span className="text-failed font-medium">{test.failCount}F</span>
          <span className="text-skipped font-medium">{test.skipCount}S</span>
        </div>
      </td>
      <td className="px-4 py-2.5 text-xs text-gray-500">{test.totalRuns}</td>
      <td className="px-4 py-2.5">
        {test.lastStatus && (
          <span className={`inline-flex items-center gap-1 text-xs font-medium ${NODE_STATUS_TEXT_COLORS[test.lastStatus]}`}>
            <span className={`w-1.5 h-1.5 rounded-full ${NODE_STATUS_COLORS[test.lastStatus]}`} />
            {test.lastStatus}
          </span>
        )}
      </td>
      <td className="px-4 py-2.5">
        {test.lastRunId ? (
          <Link to={`/runs/${test.lastRunId}`} className="text-xs text-blue-600 hover:text-blue-800">
            View run
          </Link>
        ) : (
          <span className="text-xs text-gray-400">--</span>
        )}
      </td>
    </tr>
  );
}

export default function FlakinessPage() {
  const { data: report, isLoading: reportLoading, isError: reportError } = useOverallFlakiness();
  const { data: topTests, isLoading: testsLoading } = useTopFlakyTests(20);

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Flakiness Report</h1>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <p className="text-sm text-gray-500 mb-1">Total Tests</p>
          <p className="text-2xl font-bold text-gray-900">
            {reportLoading ? '...' : reportError ? '--' : report?.totalTests ?? 0}
          </p>
        </div>
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <p className="text-sm text-gray-500 mb-1">Flaky Tests</p>
          <p className={`text-2xl font-bold ${report && report.flakyTests > 0 ? 'text-yellow-600' : 'text-gray-900'}`}>
            {reportLoading ? '...' : reportError ? '--' : report?.flakyTests ?? 0}
          </p>
        </div>
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <p className="text-sm text-gray-500 mb-1">Flakiness Rate</p>
          <p className={`text-2xl font-bold ${report && report.flakinessPercentage > 0 ? 'text-yellow-600' : 'text-gray-900'}`}>
            {reportLoading ? '...' : reportError ? '--' : `${(report?.flakinessPercentage ?? 0).toFixed(1)}%`}
          </p>
        </div>
      </div>

      {/* Top Flaky Tests Table */}
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="px-4 py-3 border-b border-gray-200">
          <h2 className="text-sm font-semibold text-gray-700">Top Flaky Tests</h2>
          <p className="text-xs text-gray-400 mt-0.5">Tests with inconsistent results across recent runs</p>
        </div>

        {testsLoading ? (
          <div className="p-6 text-center text-sm text-gray-500">Loading...</div>
        ) : !topTests || topTests.length === 0 ? (
          <div className="p-6 text-center">
            <p className="text-sm text-gray-500 mb-1">No flaky tests detected</p>
            <p className="text-xs text-gray-400">
              Tests that produce inconsistent results across runs will appear here.
            </p>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-gray-500 border-b border-gray-100">
                <th className="px-4 py-2 font-medium">Test Name</th>
                <th className="px-4 py-2 font-medium">Type</th>
                <th className="px-4 py-2 font-medium">Flakiness</th>
                <th className="px-4 py-2 font-medium">Results</th>
                <th className="px-4 py-2 font-medium">Runs</th>
                <th className="px-4 py-2 font-medium">Last Status</th>
                <th className="px-4 py-2 font-medium">Last Run</th>
              </tr>
            </thead>
            <tbody>
              {topTests.map((test, i) => (
                <FlakyTestRow key={`${test.testName}-${test.nodeType}-${i}`} test={test} />
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
