import React from 'react';

interface SkeletonLoaderProps {
  lines?: number;
  className?: string;
  variant?: 'text' | 'card' | 'table' | 'avatar';
}

function SkeletonPulse({ className = '' }: { className?: string }) {
  return (
    <div className={`animate-pulse rounded-lg bg-gray-200 dark:bg-white/10 ${className}`} />
  );
}

export default function SkeletonLoader({ lines = 3, className = '', variant = 'text' }: SkeletonLoaderProps) {
  if (variant === 'card') {
    return (
      <div className={`space-y-4 p-6 rounded-2xl border border-gray-200 dark:border-white/10 bg-white/50 dark:bg-white/5 ${className}`}>
        <div className="flex items-center gap-3">
          <SkeletonPulse className="w-10 h-10 rounded-full" />
          <div className="flex-1 space-y-2">
            <SkeletonPulse className="h-4 w-1/3" />
            <SkeletonPulse className="h-3 w-1/2" />
          </div>
        </div>
        <SkeletonPulse className="h-3 w-full" />
        <SkeletonPulse className="h-3 w-5/6" />
        <SkeletonPulse className="h-3 w-2/3" />
      </div>
    );
  }

  if (variant === 'table') {
    return (
      <div className={`space-y-3 ${className}`}>
        <SkeletonPulse className="h-10 w-full rounded-xl" />
        {Array.from({ length: lines }).map((_, i) => (
          <SkeletonPulse key={i} className="h-12 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  if (variant === 'avatar') {
    return (
      <div className={`flex items-center gap-4 ${className}`}>
        <SkeletonPulse className="w-12 h-12 rounded-full" />
        <div className="flex-1 space-y-2">
          <SkeletonPulse className="h-4 w-1/3" />
          <SkeletonPulse className="h-3 w-1/2" />
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-3 ${className}`}>
      {Array.from({ length: lines }).map((_, i) => (
        <SkeletonPulse
          key={i}
          className={`h-4 ${i === lines - 1 ? 'w-2/3' : 'w-full'}`}
        />
      ))}
    </div>
  );
}
