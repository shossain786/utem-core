import { useState } from 'react';
import { NODE_STATUS_COLORS, NODE_STATUS_TEXT_COLORS, STEP_STATUS_COLORS, STEP_STATUS_TEXT_COLORS } from '@/utils/status';
import { formatDuration } from '@/utils/format';
import type { HierarchyNode, TestStep, NodeType } from '@/api/types';

const NODE_TYPE_LABELS: Record<NodeType, string> = {
  SUITE: 'Suite',
  FEATURE: 'Feature',
  SCENARIO: 'Scenario',
  STEP: 'Step',
};

const NODE_TYPE_COLORS: Record<NodeType, string> = {
  SUITE: 'bg-blue-100 text-blue-700',
  FEATURE: 'bg-purple-100 text-purple-700',
  SCENARIO: 'bg-amber-100 text-amber-700',
  STEP: 'bg-gray-100 text-gray-600',
};

function ChevronIcon({ expanded }: { expanded: boolean }) {
  return (
    <svg
      className={`w-3.5 h-3.5 text-gray-400 transition-transform ${expanded ? 'rotate-90' : ''}`}
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      strokeWidth={2}
    >
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
    </svg>
  );
}

function StepRow({ step }: { step: TestStep }) {
  const [showError, setShowError] = useState(false);
  const hasError = step.status === 'FAILED' && (step.errorMessage || step.stackTrace);

  return (
    <div>
      <div className="flex items-center gap-2 py-1 px-2 text-xs text-gray-600 hover:bg-gray-50 rounded">
        <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${STEP_STATUS_COLORS[step.status]}`} />
        <span className={`font-medium ${STEP_STATUS_TEXT_COLORS[step.status]}`}>
          {step.name}
        </span>
        {step.duration != null && (
          <span className="text-gray-400 ml-auto">{formatDuration(step.duration)}</span>
        )}
        {hasError && (
          <button
            onClick={() => setShowError(!showError)}
            className="text-red-500 hover:text-red-700 ml-1 shrink-0"
            title="Show error details"
          >
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </button>
        )}
      </div>
      {hasError && showError && (
        <div className="ml-5 mt-1 mb-2 p-2 rounded bg-red-50 border border-red-200 text-xs">
          {step.errorMessage && (
            <p className="text-red-700 font-medium mb-1">{step.errorMessage}</p>
          )}
          {step.stackTrace && (
            <pre className="text-red-600 whitespace-pre-wrap overflow-x-auto max-h-48 text-[11px] leading-4">
              {step.stackTrace}
            </pre>
          )}
        </div>
      )}
    </div>
  );
}

export default function TreeNode({ node, depth = 0 }: { node: HierarchyNode; depth?: number }) {
  const hasChildren = node.children.length > 0;
  const hasSteps = node.steps.length > 0;
  const isExpandable = hasChildren || hasSteps;

  // Suites and features default to expanded, scenarios and steps default to collapsed
  const [expanded, setExpanded] = useState(
    node.nodeType === 'SUITE' || node.nodeType === 'FEATURE',
  );

  return (
    <div className={depth > 0 ? 'ml-4 border-l border-gray-100' : ''}>
      {/* Node header */}
      <div
        className={`flex items-center gap-2 py-1.5 px-2 rounded hover:bg-gray-50 ${
          isExpandable ? 'cursor-pointer' : ''
        }`}
        onClick={() => isExpandable && setExpanded(!expanded)}
      >
        {/* Expand/collapse chevron */}
        <span className="w-4 flex justify-center shrink-0">
          {isExpandable ? <ChevronIcon expanded={expanded} /> : null}
        </span>

        {/* Status dot */}
        <span className={`w-2 h-2 rounded-full shrink-0 ${NODE_STATUS_COLORS[node.status]}`} />

        {/* Node type badge */}
        <span className={`px-1.5 py-0.5 text-[10px] font-semibold rounded ${NODE_TYPE_COLORS[node.nodeType]}`}>
          {NODE_TYPE_LABELS[node.nodeType]}
        </span>

        {/* Name */}
        <span className={`text-sm font-medium truncate ${NODE_STATUS_TEXT_COLORS[node.status]}`}>
          {node.name}
        </span>

        {/* Stats (for nodes with children) */}
        {node.statistics && hasChildren && (
          <span className="text-[10px] text-gray-400 shrink-0 ml-1">
            {node.statistics.passedNodes}P / {node.statistics.failedNodes}F / {node.statistics.skippedNodes}S
          </span>
        )}

        {/* Flaky badge */}
        {node.flaky && (
          <span className="px-1.5 py-0.5 text-[10px] font-medium rounded bg-yellow-100 text-yellow-700 shrink-0">
            Flaky
          </span>
        )}

        {/* Duration */}
        {node.duration != null && (
          <span className="text-xs text-gray-400 ml-auto shrink-0">
            {formatDuration(node.duration)}
          </span>
        )}
      </div>

      {/* Expanded children & steps */}
      {expanded && (
        <div className="pl-2">
          {/* Child nodes */}
          {node.children.map((child) => (
            <TreeNode key={child.id} node={child} depth={depth + 1} />
          ))}

          {/* Steps (inline under scenario/step nodes) */}
          {hasSteps && (
            <div className={`${depth > 0 ? 'ml-4 border-l border-gray-100' : ''} pl-2`}>
              {node.steps
                .sort((a, b) => (a.stepOrder ?? 0) - (b.stepOrder ?? 0))
                .map((step) => (
                  <StepRow key={step.id} step={step} />
                ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
