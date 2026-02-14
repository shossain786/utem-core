import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '@/api/client';
import { attachmentDownloadUrl, isImageAttachment, isVideoAttachment, isTextViewable } from '@/utils/attachmentUtils';
import { formatFileSize, formatTimestamp } from '@/utils/format';
import type { AttachmentViewerProps } from '@/hooks/useAttachmentViewer';
import type { AttachmentSummary } from '@/api/types';

// ── Image Renderer ──────────────────────────────────────────────────

function ImageRenderer({ attachment }: { attachment: AttachmentSummary }) {
  const [zoom, setZoom] = useState(1);
  const url = attachmentDownloadUrl(attachment.id);

  // Reset zoom when switching attachments
  useEffect(() => { setZoom(1); }, [attachment.id]);

  return (
    <div className="flex flex-col items-center gap-3 w-full">
      {/* Zoom toolbar */}
      <div className="flex items-center gap-2 bg-gray-900/60 rounded-lg px-3 py-1.5">
        <button
          onClick={() => setZoom((z) => Math.max(z - 0.25, 0.25))}
          className="text-white/80 hover:text-white text-sm font-bold w-6 h-6 flex items-center justify-center"
          title="Zoom out"
        >
          -
        </button>
        <span className="text-white/80 text-xs w-12 text-center">{Math.round(zoom * 100)}%</span>
        <button
          onClick={() => setZoom((z) => Math.min(z + 0.25, 4))}
          className="text-white/80 hover:text-white text-sm font-bold w-6 h-6 flex items-center justify-center"
          title="Zoom in"
        >
          +
        </button>
        <div className="w-px h-4 bg-white/20" />
        <button
          onClick={() => setZoom(1)}
          className="text-white/80 hover:text-white text-xs px-2"
          title="Reset zoom"
        >
          Fit
        </button>
      </div>

      {/* Image container */}
      <div className="overflow-auto max-h-[80vh] max-w-full flex items-center justify-center">
        <img
          src={url}
          alt={attachment.name}
          className="transition-transform duration-150 max-w-full max-h-[80vh] object-contain"
          style={{ transform: `scale(${zoom})`, transformOrigin: 'center center' }}
        />
      </div>
    </div>
  );
}

// ── Video Renderer ──────────────────────────────────────────────────

function VideoRenderer({ attachment }: { attachment: AttachmentSummary }) {
  const url = attachmentDownloadUrl(attachment.id);

  return (
    <video
      key={attachment.id}
      src={url}
      controls
      preload="metadata"
      className="max-w-full max-h-[80vh] rounded"
    />
  );
}

// ── Text Renderer ───────────────────────────────────────────────────

function TextRenderer({ attachment }: { attachment: AttachmentSummary }) {
  const { data: textContent, isLoading, isError } = useQuery({
    queryKey: ['attachment-content', attachment.id],
    queryFn: async () => {
      const { data } = await apiClient.get<string>(
        attachmentDownloadUrl(attachment.id),
        { responseType: 'text', transformResponse: [(d: string) => d] },
      );
      return data;
    },
    staleTime: 5 * 60 * 1000,
  });

  if (isLoading) {
    return <div className="text-white/60 text-sm">Loading content...</div>;
  }

  if (isError) {
    return <div className="text-red-400 text-sm">Failed to load content.</div>;
  }

  // Pretty-print JSON if applicable
  let displayText = textContent ?? '';
  if (attachment.mimeType === 'application/json' || attachment.name.endsWith('.json')) {
    try {
      displayText = JSON.stringify(JSON.parse(displayText), null, 2);
    } catch {
      // Not valid JSON — show raw
    }
  }

  const lines = displayText.split('\n');

  return (
    <div className="w-full max-w-4xl max-h-[80vh] overflow-auto rounded-lg bg-gray-900 border border-gray-700">
      <div className="flex font-mono text-xs leading-5">
        {/* Line numbers */}
        <div className="select-none text-right pr-3 pl-3 text-gray-500 border-r border-gray-700 shrink-0 py-3">
          {lines.map((_, i) => (
            <div key={i}>{i + 1}</div>
          ))}
        </div>
        {/* Content */}
        <pre className="pl-4 pr-4 py-3 overflow-x-auto whitespace-pre text-gray-200">
          {displayText}
        </pre>
      </div>
    </div>
  );
}

// ── Fallback Renderer ───────────────────────────────────────────────

function FallbackRenderer({ attachment }: { attachment: AttachmentSummary }) {
  const url = attachmentDownloadUrl(attachment.id);

  return (
    <div className="flex flex-col items-center gap-4 text-center">
      {/* File icon */}
      <svg className="w-16 h-16 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <div className="space-y-1">
        <p className="text-white text-sm font-medium">{attachment.name}</p>
        <p className="text-gray-400 text-xs">
          {attachment.mimeType ?? 'Unknown type'}
          {attachment.fileSize != null && ` · ${formatFileSize(attachment.fileSize)}`}
        </p>
        <p className="text-gray-500 text-xs">{formatTimestamp(attachment.timestamp)}</p>
      </div>
      <a
        href={url}
        download={attachment.name}
        className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium transition-colors"
      >
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2M7 10l5 5m0 0l5-5m-5 5V3" />
        </svg>
        Download
      </a>
    </div>
  );
}

// ── Content Router ──────────────────────────────────────────────────

function AttachmentContent({ attachment }: { attachment: AttachmentSummary }) {
  if (isImageAttachment(attachment)) return <ImageRenderer attachment={attachment} />;
  if (isVideoAttachment(attachment)) return <VideoRenderer attachment={attachment} />;
  if (isTextViewable(attachment)) return <TextRenderer attachment={attachment} />;
  return <FallbackRenderer attachment={attachment} />;
}

// ── Main Viewer Component ───────────────────────────────────────────

export default function AttachmentViewer({
  isOpen,
  attachments,
  currentIndex,
  close,
  goNext,
  goPrev,
}: AttachmentViewerProps) {
  const attachment = attachments[currentIndex] ?? null;
  const hasMultiple = attachments.length > 1;
  const hasPrev = currentIndex > 0;
  const hasNext = currentIndex < attachments.length - 1;

  // Keyboard handling
  useEffect(() => {
    if (!isOpen) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') close();
      else if (e.key === 'ArrowLeft') goPrev();
      else if (e.key === 'ArrowRight') goNext();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [isOpen, close, goNext, goPrev]);

  // Body scroll lock
  useEffect(() => {
    if (!isOpen) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = prev; };
  }, [isOpen]);

  if (!isOpen || !attachment) return null;

  const downloadUrl = attachmentDownloadUrl(attachment.id);

  return (
    <div className="fixed inset-0 z-[60] flex flex-col">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/80" onClick={close} />

      {/* Top bar */}
      <div className="relative z-10 flex items-center gap-3 px-4 py-3 bg-black/60">
        <div className="flex-1 min-w-0">
          <p className="text-white text-sm font-medium truncate">{attachment.name}</p>
          <div className="flex items-center gap-2 text-xs text-gray-400">
            {attachment.mimeType && <span>{attachment.mimeType}</span>}
            {attachment.fileSize != null && <span>{formatFileSize(attachment.fileSize)}</span>}
            {attachment.isFailureScreenshot && (
              <span className="px-1.5 py-0.5 rounded bg-red-900/50 text-red-300 text-[10px] font-medium">
                Failure
              </span>
            )}
          </div>
        </div>

        {hasMultiple && (
          <span className="text-gray-400 text-xs shrink-0">
            {currentIndex + 1} / {attachments.length}
          </span>
        )}

        {/* Download button */}
        <a
          href={downloadUrl}
          download={attachment.name}
          className="p-2 text-gray-400 hover:text-white rounded transition-colors shrink-0"
          title="Download"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2M7 10l5 5m0 0l5-5m-5 5V3" />
          </svg>
        </a>

        {/* Close button */}
        <button
          onClick={close}
          className="p-2 text-gray-400 hover:text-white rounded transition-colors shrink-0"
          title="Close (Esc)"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Content area */}
      <div className="relative z-10 flex-1 flex items-center justify-center p-4 overflow-hidden">
        {/* Prev button */}
        {hasMultiple && (
          <button
            onClick={goPrev}
            disabled={!hasPrev}
            className={`absolute left-4 p-2 rounded-full bg-black/40 transition-colors shrink-0 ${
              hasPrev ? 'text-white hover:bg-black/60' : 'text-gray-600 cursor-default'
            }`}
            title="Previous"
          >
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
        )}

        <AttachmentContent attachment={attachment} />

        {/* Next button */}
        {hasMultiple && (
          <button
            onClick={goNext}
            disabled={!hasNext}
            className={`absolute right-4 p-2 rounded-full bg-black/40 transition-colors shrink-0 ${
              hasNext ? 'text-white hover:bg-black/60' : 'text-gray-600 cursor-default'
            }`}
            title="Next"
          >
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        )}
      </div>
    </div>
  );
}
