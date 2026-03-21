import { useState } from 'react';
import type { QualityGateResult } from '@/api/types';

interface Props {
  gate: QualityGateResult;
}

export default function QualityGateBadge({ gate }: Props) {
  const [open, setOpen] = useState(false);

  const passed = gate.passed;

  return (
    <div className="relative inline-block">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        title={passed ? 'Quality gate passed' : 'Quality gate failed — click for details'}
        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium border transition-colors ${
          passed
            ? 'bg-green-50 text-green-700 border-green-200 hover:bg-green-100'
            : 'bg-red-50 text-red-700 border-red-200 hover:bg-red-100'
        }`}
      >
        <ShieldIcon passed={passed} />
        Gate {passed ? 'Passed' : 'Failed'}
      </button>

      {open && !passed && (
        <div className="absolute left-0 top-full mt-1 z-50 w-72 bg-white border border-gray-200 rounded-lg shadow-lg p-3">
          <p className="text-xs font-semibold text-gray-700 mb-2">Violations</p>
          <ul className="space-y-1.5">
            {gate.violations.map((v, i) => (
              <li key={i} className="text-xs text-gray-600">
                <span className="font-medium text-red-600">{v.rule}</span>
                {' — '}
                {v.message}
              </li>
            ))}
          </ul>
          <div className="mt-2 pt-2 border-t border-gray-100 text-xs text-gray-400 space-y-0.5">
            <div>Fail rate: <span className="text-gray-600">{gate.metrics.failRate.toFixed(1)}%</span></div>
            {gate.metrics.flakyTestCount > 0 && (
              <div>Flaky tests: <span className="text-gray-600">{gate.metrics.flakyTestCount}</span></div>
            )}
            {gate.metrics.newFailures > 0 && (
              <div>New failures: <span className="text-gray-600">{gate.metrics.newFailures}</span></div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function ShieldIcon({ passed }: { passed: boolean }) {
  return (
    <svg
      className={`w-3 h-3 ${passed ? 'text-green-600' : 'text-red-600'}`}
      fill="currentColor"
      viewBox="0 0 20 20"
    >
      <path
        fillRule="evenodd"
        d="M10 1.944A11.954 11.954 0 012.166 5C2.056 5.649 2 6.319 2 7c0 5.225 3.34 9.67 8 11.317C14.66 16.67 18 12.225 18 7c0-.682-.057-1.35-.166-2.001A11.954 11.954 0 0110 1.944zM11 14a1 1 0 11-2 0 1 1 0 012 0zm0-7a1 1 0 10-2 0v3a1 1 0 102 0V7z"
        clipRule="evenodd"
      />
    </svg>
  );
}
