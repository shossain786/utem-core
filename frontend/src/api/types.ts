// ============ Enums ============

export type RunStatus = 'RUNNING' | 'PASSED' | 'FAILED' | 'ABORTED';

export type NodeStatus = 'PENDING' | 'RUNNING' | 'PASSED' | 'FAILED' | 'SKIPPED';

export type StepStatus = 'PENDING' | 'RUNNING' | 'PASSED' | 'FAILED' | 'SKIPPED';

export type NodeType = 'SUITE' | 'FEATURE' | 'SCENARIO' | 'STEP';

export type AttachmentType = 'SCREENSHOT' | 'LOG' | 'VIDEO' | 'FILE';

// ============ DTOs ============

export interface TestRunSummary {
  id: string;
  name: string;
  status: RunStatus;
  startTime: string;
  endTime: string | null;
  duration: number | null;
  totalTests: number | null;
  passedTests: number | null;
  failedTests: number | null;
  skippedTests: number | null;
  passRate: number | null;
}

export interface TestNodeSummary {
  id: string;
  runId: string | null;
  runName: string | null;
  nodeType: NodeType;
  name: string;
  status: NodeStatus;
  startTime: string;
  duration: number | null;
  flaky: boolean | null;
}

export interface TestStepSummary {
  id: string;
  nodeId: string | null;
  nodeName: string | null;
  name: string;
  status: StepStatus;
  duration: number | null;
  errorMessage: string | null;
}

export interface AttachmentSummary {
  id: string;
  nodeId: string | null;
  name: string;
  type: AttachmentType;
  mimeType: string | null;
  fileSize: number | null;
  timestamp: string;
  isFailureScreenshot: boolean | null;
}

export interface NodeStatistics {
  totalNodes: number;
  passedNodes: number;
  failedNodes: number;
  skippedNodes: number;
  runningNodes: number;
  pendingNodes: number;
  totalDuration: number;
}

export interface TestStep {
  id: string;
  name: string;
  status: StepStatus;
  timestamp: string;
  duration: number | null;
  stepOrder: number | null;
  errorMessage: string | null;
  stackTrace: string | null;
  attachments: AttachmentSummary[];
}

export interface HierarchyNode {
  id: string;
  parentId: string | null;
  nodeType: NodeType;
  name: string;
  status: NodeStatus;
  startTime: string;
  endTime: string | null;
  duration: number | null;
  flaky: boolean | null;
  retryCount: number | null;
  statistics: NodeStatistics | null;
  children: HierarchyNode[];
  steps: TestStep[];
  attachments: AttachmentSummary[];
}

export interface TestRunHierarchy {
  runId: string;
  name: string;
  status: RunStatus;
  startTime: string;
  endTime: string | null;
  statistics: NodeStatistics;
  rootNodes: HierarchyNode[];
}

export interface RunComparison {
  baseRun: TestRunSummary;
  compareRun: TestRunSummary;
  totalTestsDiff: number;
  passedTestsDiff: number;
  failedTestsDiff: number;
  skippedTestsDiff: number;
  passRateDiff: number | null;
  durationDiff: number | null;
}

export interface FlakyTest {
  testName: string;
  nodeType: NodeType;
  flakinessRate: number;
  totalRuns: number;
  passCount: number;
  failCount: number;
  skipCount: number;
  frameworkMarked: boolean;
  lastRetryCount: number | null;
  lastStatus: NodeStatus | null;
  lastRunId: string | null;
}

export interface FlakinessReport {
  totalTests: number;
  flakyTests: number;
  flakinessPercentage: number;
  topFlakyTests: FlakyTest[];
}

export interface SearchResult {
  query: string;
  runs: TestRunSummary[];
  nodes: TestNodeSummary[];
  steps: TestStepSummary[];
  attachments: AttachmentSummary[];
  totalResults: number;
}

// ============ WebSocket Types ============

export type EventType =
  | 'TEST_RUN_STARTED'
  | 'TEST_RUN_FINISHED'
  | 'TEST_SUITE_STARTED'
  | 'TEST_SUITE_FINISHED'
  | 'TEST_CASE_STARTED'
  | 'TEST_CASE_FINISHED'
  | 'TEST_STEP'
  | 'TEST_PASSED'
  | 'TEST_FAILED'
  | 'TEST_SKIPPED'
  | 'ATTACHMENT';

export interface WebSocketEventMessage {
  eventId: string;
  runId: string;
  eventType: EventType;
  parentId: string | null;
  timestamp: string;
  entityId: string;
  entityType: string;
  name: string;
  status: string;
}

export interface RunSummaryMessage {
  runId: string;
  testRunId: string;
  status: RunStatus;
  totalTests: number;
  passedTests: number;
  failedTests: number;
  skippedTests: number;
  lastUpdated: string;
}

// ============ Spring Data Page ============

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
