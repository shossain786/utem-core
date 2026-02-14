import { STEP_STATUS_COLORS, STEP_STATUS_TEXT_COLORS } from '@/utils/status';
import { formatDuration, formatTimestamp, formatFileSize } from '@/utils/format';
import { attachmentDownloadUrl, isImageAttachment, isVideoAttachment } from '@/utils/attachmentUtils';
import type { TestStep, AttachmentSummary, AttachmentType } from '@/api/types';

const ATTACHMENT_TYPE_ICONS: Record<AttachmentType, string> = {
  SCREENSHOT: 'Screenshot',
  LOG: 'Log',
  VIDEO: 'Video',
  FILE: 'File',
};

function AttachmentItem({
  attachment,
  onClick,
}: {
  attachment: AttachmentSummary;
  onClick?: () => void;
}) {
  const url = attachmentDownloadUrl(attachment.id);
  const isImage = isImageAttachment(attachment);
  const isVideo = isVideoAttachment(attachment);

  return (
    <div className="border border-gray-100 rounded-md p-2">
      {/* Header row */}
      <div className="flex items-center gap-2 text-xs mb-1">
        <span className="px-1.5 py-0.5 rounded bg-gray-100 text-gray-600 font-medium text-[10px]">
          {ATTACHMENT_TYPE_ICONS[attachment.type]}
        </span>
        <span
          className={`font-medium text-gray-700 truncate ${onClick ? 'cursor-pointer hover:text-blue-600' : ''}`}
          onClick={onClick}
        >
          {attachment.name}
        </span>
        {attachment.fileSize != null && (
          <span className="text-gray-400 ml-auto shrink-0">{formatFileSize(attachment.fileSize)}</span>
        )}
        {attachment.isFailureScreenshot && (
          <span className="px-1.5 py-0.5 rounded bg-red-100 text-red-600 font-medium text-[10px] shrink-0">
            Failure
          </span>
        )}
      </div>

      {/* Preview */}
      {isImage && (
        <div
          className={`block mt-1 ${onClick ? 'cursor-pointer' : ''}`}
          onClick={onClick}
        >
          <img
            src={url}
            alt={attachment.name}
            className="max-h-40 rounded border border-gray-200 object-contain bg-gray-50"
            loading="lazy"
          />
        </div>
      )}

      {isVideo && (
        <div
          className={`mt-1 ${onClick ? 'cursor-pointer' : ''}`}
          onClick={onClick}
        >
          <video
            src={url}
            controls
            preload="metadata"
            className="max-h-40 rounded border border-gray-200 w-full"
          />
        </div>
      )}

      {/* Download link */}
      <a
        href={url}
        download={attachment.name}
        className="inline-flex items-center gap-1 text-[11px] text-blue-600 hover:text-blue-800 mt-1"
      >
        <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2M7 10l5 5m0 0l5-5m-5 5V3" />
        </svg>
        Download
      </a>
    </div>
  );
}

export default function StepDetailPanel({
  step,
  onClose,
  onAttachmentClick,
}: {
  step: TestStep | null;
  onClose: () => void;
  onAttachmentClick?: (attachments: AttachmentSummary[], index: number) => void;
}) {
  if (!step) return null;

  const hasError = step.status === 'FAILED' && (step.errorMessage || step.stackTrace);
  const hasAttachments = step.attachments.length > 0;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/20 z-40"
        onClick={onClose}
      />

      {/* Panel */}
      <div className="fixed top-0 right-0 h-full w-full max-w-md bg-white border-l border-gray-200 shadow-lg z-50 flex flex-col">
        {/* Header */}
        <div className="flex items-start gap-3 p-4 border-b border-gray-200">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className={`w-2 h-2 rounded-full shrink-0 ${STEP_STATUS_COLORS[step.status]}`} />
              <span className={`text-xs font-medium ${STEP_STATUS_TEXT_COLORS[step.status]}`}>
                {step.status}
              </span>
            </div>
            <h2 className="text-sm font-bold text-gray-900 break-words">{step.name}</h2>
            <div className="flex items-center gap-3 mt-1 text-[11px] text-gray-400">
              {step.duration != null && <span>{formatDuration(step.duration)}</span>}
              <span>{formatTimestamp(step.timestamp)}</span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 text-gray-400 hover:text-gray-600 rounded shrink-0"
            title="Close"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {/* Error Section */}
          {hasError && (
            <div>
              <h3 className="text-xs font-semibold text-gray-700 mb-2">Error Details</h3>
              <div className="p-3 rounded-md bg-red-50 border border-red-200">
                {step.errorMessage && (
                  <p className="text-xs text-red-700 font-medium mb-2">{step.errorMessage}</p>
                )}
                {step.stackTrace && (
                  <pre className="text-[11px] leading-4 text-red-600 whitespace-pre-wrap overflow-x-auto max-h-64">
                    {step.stackTrace}
                  </pre>
                )}
              </div>
            </div>
          )}

          {/* Attachments Section */}
          {hasAttachments && (
            <div>
              <h3 className="text-xs font-semibold text-gray-700 mb-2">
                Attachments ({step.attachments.length})
              </h3>
              <div className="space-y-2">
                {step.attachments.map((att, index) => (
                  <AttachmentItem
                    key={att.id}
                    attachment={att}
                    onClick={onAttachmentClick ? () => onAttachmentClick(step.attachments, index) : undefined}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Empty state */}
          {!hasError && !hasAttachments && (
            <p className="text-xs text-gray-400 text-center py-4">
              No additional details for this step.
            </p>
          )}
        </div>
      </div>
    </>
  );
}
