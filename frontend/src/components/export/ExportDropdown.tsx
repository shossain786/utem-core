import { useState, useEffect, useRef } from 'react';

const EXPORT_FORMATS = [
  { label: 'JSON', format: 'json', description: 'Full run data as JSON' },
  { label: 'CSV', format: 'csv', description: 'Test nodes as spreadsheet' },
  { label: 'JUnit XML', format: 'junit-xml', description: 'For Jenkins / GitLab CI' },
  { label: 'PDF', format: 'pdf', description: 'Formatted report document' },
] as const;

type ExportFormat = typeof EXPORT_FORMATS[number]['format'];

interface ExportDropdownProps {
  runId: string;
}

export default function ExportDropdown({ runId }: ExportDropdownProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  // Close on outside click
  useEffect(() => {
    if (!open) return;
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, [open]);

  const download = (format: ExportFormat) => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/utem';
    const a = document.createElement('a');
    a.href = `${baseUrl}/export/${runId}/${format}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setOpen(false);
  };

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 transition-colors"
        aria-haspopup="true"
        aria-expanded={open}
      >
        <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
        </svg>
        Export
        <svg className={`w-3 h-3 transition-transform ${open ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {open && (
        <div className="absolute right-0 mt-1 w-48 bg-white rounded-lg border border-gray-200 shadow-lg z-20 overflow-hidden">
          {EXPORT_FORMATS.map(({ label, format, description }) => (
            <button
              key={format}
              type="button"
              onClick={() => download(format)}
              className="w-full text-left px-3 py-2.5 hover:bg-gray-50 transition-colors border-b border-gray-50 last:border-b-0"
            >
              <p className="text-xs font-medium text-gray-900">{label}</p>
              <p className="text-xs text-gray-400">{description}</p>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
