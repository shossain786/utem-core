import type { AttachmentSummary } from '@/api/types';

const ATTACHMENT_BASE_URL = '/utem/attachments';

export function attachmentDownloadUrl(id: string): string {
  return `${ATTACHMENT_BASE_URL}/${id}/download`;
}

export function isImageAttachment(att: AttachmentSummary): boolean {
  return att.type === 'SCREENSHOT' || att.mimeType?.startsWith('image/') === true;
}

export function isVideoAttachment(att: AttachmentSummary): boolean {
  return att.type === 'VIDEO' || att.mimeType?.startsWith('video/') === true;
}

export function isTextViewable(att: AttachmentSummary): boolean {
  if (att.type === 'LOG') return true;
  const mime = att.mimeType ?? '';
  return (
    mime.startsWith('text/') ||
    mime === 'application/json' ||
    mime === 'application/xml'
  );
}
