import type { RunStatus, NodeStatus, StepStatus } from '@/api/types';

// ============ RunStatus Colors ============

export const RUN_STATUS_COLORS: Record<RunStatus, string> = {
  PASSED: 'bg-passed',
  FAILED: 'bg-failed',
  RUNNING: 'bg-running',
  ABORTED: 'bg-aborted',
};

export const RUN_STATUS_TEXT_COLORS: Record<RunStatus, string> = {
  PASSED: 'text-passed',
  FAILED: 'text-failed',
  RUNNING: 'text-running',
  ABORTED: 'text-aborted',
};

// ============ NodeStatus Colors ============

export const NODE_STATUS_COLORS: Record<NodeStatus, string> = {
  PASSED: 'bg-passed',
  FAILED: 'bg-failed',
  RUNNING: 'bg-running',
  SKIPPED: 'bg-skipped',
  PENDING: 'bg-pending',
};

export const NODE_STATUS_TEXT_COLORS: Record<NodeStatus, string> = {
  PASSED: 'text-passed',
  FAILED: 'text-failed',
  RUNNING: 'text-running',
  SKIPPED: 'text-skipped',
  PENDING: 'text-pending',
};

// ============ StepStatus Colors (same as NodeStatus) ============

export const STEP_STATUS_COLORS: Record<StepStatus, string> = NODE_STATUS_COLORS;
export const STEP_STATUS_TEXT_COLORS: Record<StepStatus, string> = NODE_STATUS_TEXT_COLORS;
