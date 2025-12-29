import type { ReactNode } from 'react';
import { useEffect } from 'react';

interface ModalProps {
  /** Controls whether modal is visible */
  isOpen: boolean;
  /** Called when modal should close (backdrop click or ESC key) */
  onClose: () => void;
  /** Modal title */
  title?: string;
  /** Modal content */
  children: ReactNode;
  /** Maximum width of modal */
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | 'full';
}

/**
 * Modal dialog component with modern styling.
 * Professional look with gradients, shadows, and smooth animations.
 */
export function Modal({
  isOpen,
  onClose,
  title,
  children,
  maxWidth = 'md',
}: ModalProps) {
  // Handle ESC key press
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen, onClose]);

  // Prevent body scroll when modal is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }

    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const maxWidthClasses = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    full: 'max-w-[1400px]', // Match the game info width
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0, 0, 0, 0.95)' }}
      onClick={onClose}
    >
      {/* Modal content with solid background */}
      <div
        className={`rounded-2xl shadow-2xl ${maxWidthClasses[maxWidth]} w-full relative`}
        style={{
          background: 'linear-gradient(135deg, #1f2937 0%, #111827 100%)',
          border: '3px solid rgba(248, 207, 44, 0.5)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Prominent Close Button - Top Right */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-white hover:text-yellow-300 transition-colors"
          style={{
            fontSize: '32px',
            fontWeight: 'bold',
            lineHeight: '1',
            zIndex: 10,
            background: 'rgba(0, 0, 0, 0.6)',
            borderRadius: '50%',
            width: '48px',
            height: '48px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            border: '2px solid rgba(248, 207, 44, 0.6)',
          }}
        >
          ✕
        </button>

        {/* Header with gradient underline */}
        {title && (
          <div className="px-8 py-6 border-b" style={{ borderColor: 'rgba(248, 207, 44, 0.3)' }}>
            <h2 className="text-3xl font-bold" style={{ color: '#f8cf2c' }}>{title}</h2>
          </div>
        )}

        {/* Body */}
        <div className="px-8 py-6">{children}</div>
      </div>
    </div>
  );
}
