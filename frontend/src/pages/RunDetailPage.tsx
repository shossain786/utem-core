import { useState, useRef } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useRunDetail, useRunLabels, useUpdateRun } from '@/hooks/useApi';
import { useRunEvents, useRunSummary, useWebSocket } from '@/hooks/useWebSocket';
import { RUN_STATUS_COLORS, RUN_STATUS_TEXT_COLORS } from '@/utils/status';
import { formatDuration, formatRelativeTime } from '@/utils/format';
import TreeNode from '@/components/tree/TreeNode';
import StepDetailPanel from '@/components/step/StepDetailPanel';
import AttachmentViewer from '@/components/attachment/AttachmentViewer';
import { useAttachmentViewer } from '@/hooks/useAttachmentViewer';
import ExportDropdown from '@/components/export/ExportDropdown';
import type { TestStep } from '@/api/types';

export default function RunDetailPage() {
  const { runId } = useParams<{ runId: string }>();
  const { status: wsStatus } = useWebSocket();
  const [selectedStep, setSelectedStep] = useState<TestStep | null>(null);
  const viewer = useAttachmentViewer();
  const [editingLabel, setEditingLabel] = useState(false);
  const [labelValue, setLabelValue] = useState('');
  const labelInputRef = useRef<HTMLInputElement>(null);
  const updateRun = useUpdateRun();
  const { data: availableLabels } = useRunLabels();

  // Fall back to polling every 10s when WebSocket is disconnected
  const { data: hierarchy, isLoading, isError, dataUpdatedAt } = useRunDetail(
    runId,
    wsStatus !== 'connected' ? 10_000 : false,
  );

  const runIsActive = hierarchy?.status === 'RUNNING';

  // Subscribe to real-time events — auto-invalidate query cache on each WS event
  useRunEvents(runIsActive ? runId : null);
  useRunSummary(runIsActive ? runId : null);

  if (isLoading) {
    return (
      <div className="p-6 text-center text-sm text-gray-500">Loading run details...</div>
    );
  }

  if (isError || !hierarchy) {
    return (
      <div>
        <BackLink />
        <div className="bg-white rounded-lg border border-gray-200 p-6 text-center">
          <p className="text-sm text-red-500 mb-1">Failed to load run details.</p>
          <p className="text-xs text-gray-400">The run may not exist or the server may be unavailable.</p>
        </div>
      </div>
    );
  }

  const stats = hierarchy.statistics;

  return (
    <div>
      <BackLink />

      {/* Run Header */}
      <div className="bg-white rounded-lg border border-gray-200 p-4 mb-4">
        <div className="flex items-center gap-3 mb-3">
          {/* Status badge — pulsing dot when RUNNING */}
          <span
            className={`inline-flex items-center gap-1.5 text-xs font-medium ${RUN_STATUS_TEXT_COLORS[hierarchy.status]}`}
          >
            <span
              className={`w-2 h-2 rounded-full ${RUN_STATUS_COLORS[hierarchy.status]}${runIsActive ? ' animate-pulse' : ''}`}
            />
            {hierarchy.status}
          </span>

          {/* Run name */}
          <h1 className="text-lg font-bold text-gray-900">{hierarchy.name}</h1>

          {/* Label — click to edit */}
          {editingLabel ? (
            <div className="flex items-center gap-1">
              <input
                ref={labelInputRef}
                value={labelValue}
                onChange={(e) => setLabelValue(e.target.value)}
                onBlur={() => {
                  setEditingLabel(false);
                  const trimmed = labelValue.trim();
                  if (trimmed !== (hierarchy.label ?? '')) {
                    updateRun.mutate({ runId: runId!, label: trimmed });
                  }
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') labelInputRef.current?.blur();
                  if (e.key === 'Escape') { setLabelValue(hierarchy.label ?? ''); setEditingLabel(false); }
                }}
                list="detail-labels"
                placeholder="Add label…"
                autoFocus
                className="px-1.5 py-0.5 text-xs border border-indigo-400 rounded-full w-28 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
              <datalist id="detail-labels">
                {(availableLabels ?? []).map((l) => <option key={l} value={l} />)}
              </datalist>
            </div>
          ) : hierarchy.label ? (
            <button
              type="button"
              onClick={() => { setLabelValue(hierarchy.label!); setEditingLabel(true); }}
              title="Click to edit label"
              className="px-1.5 py-0.5 text-xs font-medium bg-indigo-50 text-indigo-700 rounded-full hover:bg-indigo-100 transition-colors"
            >
              {hierarchy.label}
            </button>
          ) : (
            <button
              type="button"
              onClick={() => { setLabelValue(''); setEditingLabel(true); }}
              title="Add label"
              className="px-1.5 py-0.5 text-xs text-gray-400 rounded-full border border-dashed border-gray-300 hover:border-indigo-400 hover:text-indigo-500 transition-colors"
            >
              + label
            </button>
          )}

          <div className="ml-auto flex items-center gap-3">
            {/* Last updated timestamp */}
            {dataUpdatedAt > 0 && (
              <span className="text-xs text-gray-400">
                Updated {formatRelativeTime(new Date(dataUpdatedAt).toISOString())}
              </span>
            )}
            {/* Duration */}
            {stats.totalDuration > 0 && (
              <span className="text-sm text-gray-400">
                {formatDuration(stats.totalDuration)}
              </span>
            )}
            {/* Export */}
            {runId && <ExportDropdown runId={runId} />}
          </div>
        </div>

        {/* Stats summary bar */}
        <div className="flex items-center gap-4 text-xs">
          <span className="text-gray-500">
            {stats.totalNodes} total
          </span>
          <span className="text-passed font-medium">{stats.passedNodes} passed</span>
          <span className="text-failed font-medium">{stats.failedNodes} failed</span>
          <span className="text-skipped font-medium">{stats.skippedNodes} skipped</span>
          {stats.runningNodes > 0 && (
            <span className="text-running font-medium">{stats.runningNodes} running</span>
          )}
          {stats.pendingNodes > 0 && (
            <span className="text-pending font-medium">{stats.pendingNodes} pending</span>
          )}
        </div>

        {/* Visual pass/fail bar */}
        {stats.totalNodes > 0 && (
          <div className="flex h-1.5 rounded-full overflow-hidden mt-3 bg-gray-100">
            {stats.passedNodes > 0 && (
              <div
                className="bg-passed"
                style={{ width: `${(stats.passedNodes / stats.totalNodes) * 100}%` }}
              />
            )}
            {stats.failedNodes > 0 && (
              <div
                className="bg-failed"
                style={{ width: `${(stats.failedNodes / stats.totalNodes) * 100}%` }}
              />
            )}
            {stats.skippedNodes > 0 && (
              <div
                className="bg-skipped"
                style={{ width: `${(stats.skippedNodes / stats.totalNodes) * 100}%` }}
              />
            )}
          </div>
        )}
      </div>

      {/* Tree View */}
      <div className="bg-white rounded-lg border border-gray-200 p-4">
        <h2 className="text-sm font-semibold text-gray-700 mb-3">Test Hierarchy</h2>
        {hierarchy.rootNodes.length === 0 ? (
          <p className="text-xs text-gray-400">No test nodes found for this run.</p>
        ) : (
          <div>
            {hierarchy.rootNodes.map((node) => (
              <TreeNode key={node.id} node={node} onStepClick={setSelectedStep} onAttachmentClick={viewer.open} />
            ))}
          </div>
        )}
      </div>

      <StepDetailPanel step={selectedStep} onClose={() => setSelectedStep(null)} onAttachmentClick={viewer.open} />
      <AttachmentViewer {...viewer} />
    </div>
  );
}

function BackLink() {
  return (
    <Link
      to="/runs"
      className="inline-flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700 mb-4"
    >
      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
      </svg>
      Back to Runs
    </Link>
  );
}
