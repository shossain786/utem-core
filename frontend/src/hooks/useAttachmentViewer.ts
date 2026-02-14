import { useState, useCallback } from 'react';
import type { AttachmentSummary } from '@/api/types';

export interface AttachmentViewerState {
  isOpen: boolean;
  attachments: AttachmentSummary[];
  currentIndex: number;
}

export interface AttachmentViewerActions {
  open: (attachments: AttachmentSummary[], startIndex?: number) => void;
  close: () => void;
  goNext: () => void;
  goPrev: () => void;
}

export type AttachmentViewerProps = AttachmentViewerState & AttachmentViewerActions;

export function useAttachmentViewer(): AttachmentViewerProps {
  const [state, setState] = useState<AttachmentViewerState>({
    isOpen: false,
    attachments: [],
    currentIndex: 0,
  });

  const open = useCallback((attachments: AttachmentSummary[], startIndex = 0) => {
    if (attachments.length === 0) return;
    setState({ isOpen: true, attachments, currentIndex: startIndex });
  }, []);

  const close = useCallback(() => {
    setState((prev) => ({ ...prev, isOpen: false }));
  }, []);

  const goNext = useCallback(() => {
    setState((prev) => ({
      ...prev,
      currentIndex: Math.min(prev.currentIndex + 1, prev.attachments.length - 1),
    }));
  }, []);

  const goPrev = useCallback(() => {
    setState((prev) => ({
      ...prev,
      currentIndex: Math.max(prev.currentIndex - 1, 0),
    }));
  }, []);

  return { ...state, open, close, goNext, goPrev };
}
