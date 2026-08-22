import React from 'react';
import { useI18n } from '@/i18n';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  message?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export default function EmptyState({ icon, title, message, action }: EmptyStateProps) {
  const { t } = useI18n();

  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
      {icon && (
        <div className="w-16 h-16 rounded-full bg-gray-100 dark:bg-white/5 flex items-center justify-center mb-4">
          {icon}
        </div>
      )}
      <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-1">
        {title}
      </h3>
      {message && (
        <p className="text-sm text-gray-500 dark:text-gray-400 max-w-sm mb-4">
          {message}
        </p>
      )}
      {action && (
        <button
          onClick={action.onClick}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-green-600 to-emerald-600 text-white text-sm font-medium hover:from-green-700 hover:to-emerald-700 transition-all shadow-lg shadow-green-500/25"
        >
          {action.label}
        </button>
      )}
    </div>
  );
}
