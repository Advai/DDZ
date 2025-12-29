import type { ButtonHTMLAttributes, ReactNode } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Button text or child elements */
  children: ReactNode;
  /** Visual style variant */
  variant?: 'primary' | 'secondary' | 'danger';
  /** Full width button */
  fullWidth?: boolean;
}

/**
 * Reusable button component with modern, polished styling.
 * Matches the MVP aesthetic with gradients, shadows, and smooth interactions.
 */
export function Button({
  children,
  variant = 'primary',
  fullWidth = false,
  className = '',
  disabled = false,
  ...props
}: ButtonProps) {
  // Base classes for all buttons - modern and polished
  const baseClasses = 'font-bold rounded-xl transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-900 shadow-lg';

  // Variant-specific styles with gradients and shadows
  const variantClasses = {
    primary: 'bg-gradient-to-r from-green-500 to-green-600 hover:from-green-600 hover:to-green-700 text-white focus:ring-green-500 shadow-green-500/50 px-8 py-3',
    secondary: 'bg-gradient-to-r from-gray-700 to-gray-800 hover:from-gray-600 hover:to-gray-700 text-white focus:ring-gray-600 border border-gray-600 shadow-gray-700/50 px-6 py-2',
    danger: 'bg-gradient-to-r from-red-500 to-red-600 hover:from-red-600 hover:to-red-700 text-white focus:ring-red-500 shadow-red-500/50 px-6 py-2',
  };

  // Width class
  const widthClass = fullWidth ? 'w-full' : '';

  return (
    <button
      disabled={disabled}
      className={`${baseClasses} ${variantClasses[variant]} ${widthClass} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}
