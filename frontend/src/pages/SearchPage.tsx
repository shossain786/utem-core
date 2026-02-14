import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useGlobalSearch } from '@/hooks/useApi';
import { formatDuration, formatRelativeTime, formatPassRate, formatFileSize } from '@/utils/format';
import {
  RUN_STATUS_COLORS, RUN_STATUS_TEXT_COLORS,
  NODE_STATUS_COLORS, NODE_STATUS_TEXT_COLORS,
  STEP_STATUS_COLORS, STEP_STATUS_TEXT_COLORS,
} from '@/utils/status';
import type {
  TestRunSummary, TestNodeSummary, TestStepSummary, AttachmentSummary,
  NodeType, AttachmentType,
} from '@/api/types';

// ── Collapsible section wrapper ─────────────────────────────────────

function ResultSection({
  title,
  count,
  defaultExpanded = true,
  children,
}: {
  title: string;
  count: number;
  defaultExpanded?: boolean;
  children: React.ReactNode;
}) {
  const [expanded, setExpanded] = useState(defaultExpanded);

  if (count === 0) return null;

  return (
    <div className="bg-white rounded-lg border border-gray-200">
      <button
        onClick={() => setExpanded(!expanded)}
        className="flex items-center gap-2 w-full px-4 py-3 text-left hover:bg-gray-50"
      >
        <svg
          className={`w-3.5 h-3.5 text-gray-400 transition-transform ${expanded ? 'rotate-90' : ''}`}
          fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
        </svg>
        <h2 className="text-sm font-semibold text-gray-700">{title}</h2>
        <span className="text-xs text-gray-400">({count})</span>
      </button>
      {expanded && <div className="border-t border-gray-100">{children}</div>}
    </div>
  );
}

// ── Runs results ────────────────────────────────────────────────────

function RunsResults({ runs }: { runs: TestRunSummary[] }) {
  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="text-left text-xs text-gray-500 border-b border-gray-100">
          <th className="px-4 py-2 font-medium">Status</th>
          <th className="px-4 py-2 font-medium">Name</th>
          <th className="px-4 py-2 font-medium">Tests</th>
          <th className="px-4 py-2 font-medium">Pass Rate</th>
          <th className="px-4 py-2 font-medium">Started</th>
        </tr>
      </thead>
      <tbody>
        {runs.map((run) => (
          <tr key={run.id} className="border-b border-gray-50 hover:bg-gray-50">
            <td className="px-4 py-2.5">
              <span className={`inline-flex items-center gap-1.5 text-xs font-medium ${RUN_STATUS_TEXT_COLORS[run.status]}`}>
                <span className={`w-1.5 h-1.5 rounded-full ${RUN_STATUS_COLORS[run.status]}`} />
                {run.status}
              </span>
            </td>
            <td className="px-4 py-2.5">
              <Link to={`/runs/${run.id}`} className="font-medium text-gray-900 hover:text-blue-600">
                {run.name}
              </Link>
            </td>
            <td className="px-4 py-2.5 text-gray-600">{run.totalTests ?? '--'}</td>
            <td className="px-4 py-2.5 text-gray-600">{formatPassRate(run.passRate)}</td>
            <td className="px-4 py-2.5 text-gray-500">{formatRelativeTime(run.startTime)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

// ── Nodes results ───────────────────────────────────────────────────

const NODE_TYPE_COLORS: Record<NodeType, string> = {
  SUITE: 'bg-blue-100 text-blue-700',
  FEATURE: 'bg-purple-100 text-purple-700',
  SCENARIO: 'bg-amber-100 text-amber-700',
  STEP: 'bg-gray-100 text-gray-600',
};

function NodesResults({ nodes }: { nodes: TestNodeSummary[] }) {
  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="text-left text-xs text-gray-500 border-b border-gray-100">
          <th className="px-4 py-2 font-medium">Status</th>
          <th className="px-4 py-2 font-medium">Type</th>
          <th className="px-4 py-2 font-medium">Name</th>
          <th className="px-4 py-2 font-medium">Run</th>
          <th className="px-4 py-2 font-medium">Duration</th>
        </tr>
      </thead>
      <tbody>
        {nodes.map((node) => (
          <tr key={node.id} className="border-b border-gray-50 hover:bg-gray-50">
            <td className="px-4 py-2.5">
              <span className={`inline-flex items-center gap-1.5 text-xs font-medium ${NODE_STATUS_TEXT_COLORS[node.status]}`}>
                <span className={`w-1.5 h-1.5 rounded-full ${NODE_STATUS_COLORS[node.status]}`} />
                {node.status}
              </span>
            </td>
            <td className="px-4 py-2.5">
              <span className={`px-1.5 py-0.5 text-[10px] font-semibold rounded ${NODE_TYPE_COLORS[node.nodeType]}`}>
                {node.nodeType}
              </span>
            </td>
            <td className="px-4 py-2.5 font-medium text-gray-900">{node.name}</td>
            <td className="px-4 py-2.5">
              {node.runId ? (
                <Link to={`/runs/${node.runId}`} className="text-gray-600 hover:text-blue-600 text-xs">
                  {node.runName ?? node.runId}
                </Link>
              ) : (
                <span className="text-gray-400 text-xs">--</span>
              )}
            </td>
            <td className="px-4 py-2.5 text-gray-600">{formatDuration(node.duration)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

// ── Steps results ───────────────────────────────────────────────────

function StepsResults({ steps }: { steps: TestStepSummary[] }) {
  return (
    <div className="divide-y divide-gray-50">
      {steps.map((step) => (
        <div key={step.id} className="flex items-start gap-3 px-4 py-2.5 hover:bg-gray-50">
          <span className={`w-1.5 h-1.5 rounded-full mt-1.5 shrink-0 ${STEP_STATUS_COLORS[step.status]}`} />
          <div className="min-w-0 flex-1">
            <p className={`text-sm font-medium ${STEP_STATUS_TEXT_COLORS[step.status]}`}>
              {step.name}
            </p>
            {step.nodeName && (
              <p className="text-xs text-gray-400 mt-0.5">in {step.nodeName}</p>
            )}
            {step.errorMessage && (
              <p className="text-xs text-red-500 mt-0.5 truncate">{step.errorMessage}</p>
            )}
          </div>
          <span className="text-xs text-gray-400 shrink-0">{formatDuration(step.duration)}</span>
        </div>
      ))}
    </div>
  );
}

// ── Attachments results ─────────────────────────────────────────────

const ATTACHMENT_TYPE_STYLES: Record<AttachmentType, string> = {
  SCREENSHOT: 'bg-indigo-100 text-indigo-700',
  LOG: 'bg-gray-100 text-gray-600',
  VIDEO: 'bg-emerald-100 text-emerald-700',
  FILE: 'bg-gray-100 text-gray-600',
};

function AttachmentsResults({ attachments }: { attachments: AttachmentSummary[] }) {
  return (
    <div className="divide-y divide-gray-50">
      {attachments.map((att) => (
        <div key={att.id} className="flex items-center gap-3 px-4 py-2.5 hover:bg-gray-50">
          <span className={`px-1.5 py-0.5 text-[10px] font-semibold rounded ${ATTACHMENT_TYPE_STYLES[att.type]}`}>
            {att.type}
          </span>
          <span className="text-sm text-gray-900 truncate">{att.name}</span>
          {att.fileSize != null && (
            <span className="text-xs text-gray-400 shrink-0">{formatFileSize(att.fileSize)}</span>
          )}
          <span className="text-xs text-gray-400 ml-auto shrink-0">{formatRelativeTime(att.timestamp)}</span>
        </div>
      ))}
    </div>
  );
}

// ── Main Search Page ────────────────────────────────────────────────

export default function SearchPage() {
  const [searchInput, setSearchInput] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(searchInput.trim()), 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const { data: results, isLoading, isError } = useGlobalSearch(debouncedQuery, 20);

  const hasQuery = debouncedQuery.length >= 2;
  const hasResults = results && results.totalResults > 0;

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Search</h1>

      {/* Search input */}
      <div className="relative mb-6">
        <svg
          className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"
          fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          type="text"
          placeholder="Search runs, nodes, steps, attachments..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 text-sm border border-gray-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          autoFocus
        />
      </div>

      {/* Results */}
      {!hasQuery ? (
        <div className="text-center py-12">
          <svg className="w-12 h-12 text-gray-300 mx-auto mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <p className="text-sm text-gray-500">Type at least 2 characters to search across all test data.</p>
          <p className="text-xs text-gray-400 mt-1">Search by run name, node name, step name, or attachment name.</p>
        </div>
      ) : isLoading ? (
        <div className="text-center py-12">
          <p className="text-sm text-gray-500">Searching...</p>
        </div>
      ) : isError ? (
        <div className="text-center py-12">
          <p className="text-sm text-red-500">Search failed. Please try again.</p>
        </div>
      ) : !hasResults ? (
        <div className="text-center py-12">
          <p className="text-sm text-gray-500">No results found for &ldquo;{debouncedQuery}&rdquo;</p>
          <p className="text-xs text-gray-400 mt-1">Try a different search term.</p>
        </div>
      ) : (
        <div className="space-y-4">
          <p className="text-xs text-gray-500">
            {results.totalResults} result{results.totalResults !== 1 ? 's' : ''} for &ldquo;{results.query}&rdquo;
          </p>

          <ResultSection title="Runs" count={results.runs.length}>
            <RunsResults runs={results.runs} />
          </ResultSection>

          <ResultSection title="Nodes" count={results.nodes.length}>
            <NodesResults nodes={results.nodes} />
          </ResultSection>

          <ResultSection title="Steps" count={results.steps.length}>
            <StepsResults steps={results.steps} />
          </ResultSection>

          <ResultSection title="Attachments" count={results.attachments.length}>
            <AttachmentsResults attachments={results.attachments} />
          </ResultSection>
        </div>
      )}
    </div>
  );
}
