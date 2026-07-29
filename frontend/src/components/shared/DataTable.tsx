import { useState } from 'react';
import type { ColumnDef } from '@/types/table';

interface DataTableProps<T> {
  columns: ColumnDef<T>[];
  data: T[];
  isLoading?: boolean;
  emptyMessage?: string;
  emptyIcon?: React.ReactNode;
  sortable?: boolean;
  onRowClick?: (row: T) => void;
}

function SkeletonRow({ cells, index }: { cells: number; index: number }) {
  return (
    <tr
      className="animate-fade-in"
      style={{ animationDelay: `${index * 50}ms` }}
    >
      {Array.from({ length: cells }).map((_, i) => (
        <td key={i} className="px-4 sm:px-6 py-4">
          <div className="skeleton h-4 w-full max-w-[150px]" />
        </td>
      ))}
    </tr>
  );
}

function EmptyIcon() {
  return (
    <svg className="w-16 h-16 text-gray-300 dark:text-gray-600 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
    </svg>
  );
}

export default function DataTable<T extends { id: string }>({
  columns,
  data,
  isLoading,
  emptyMessage = "Aucune donnée trouvée",
  emptyIcon,
  sortable,
  onRowClick,
}: DataTableProps<T>) {
  const [sortField, setSortField] = useState<string | null>(null);
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');

  const handleSort = (accessor: keyof T | undefined) => {
    if (!sortable || !accessor) return;
    const acc = String(accessor);
    if (sortField === acc) {
      setSortDir((prev) => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(acc);
      setSortDir('asc');
    }
  };

  const sortedData = [...data].sort((a, b) => {
    if (!sortField) return 0;
    const aVal = (a as Record<string, unknown>)[sortField];
    const bVal = (b as Record<string, unknown>)[sortField];
    if (aVal == null) return 1;
    if (bVal == null) return -1;
    const cmp = String(aVal).localeCompare(String(bVal));
    return sortDir === 'asc' ? cmp : -cmp;
  });

  if (isLoading) {
    return (
      <div className="table-container overflow-hidden">
        <div className="overflow-x-auto">
          <table className="table">
            <thead>
              <tr>
                {columns.map((col, i) => (
                  <th key={i} className="px-4 sm:px-6 py-3.5">{col.header}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100/50 dark:divide-gray-800/30">
              {Array.from({ length: 5 }).map((_, rowIdx) => (
                <SkeletonRow key={rowIdx} cells={columns.length} index={rowIdx} />
              ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className="glass-card p-12 sm:p-16 animate-scale-in">
        <div className="empty-state">
          {emptyIcon || <EmptyIcon />}
          <p className="text-gray-500 dark:text-gray-400 text-sm">{emptyMessage}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="table-container overflow-hidden">
      <div className="overflow-x-auto hide-scrollbar">
        <table className="table">
          <thead>
            <tr>
              {columns.map((col, i) => (
                <th
                  key={i}
                  className={`px-4 sm:px-6 py-3.5 ${sortable && col.accessor ? 'cursor-pointer select-none hover:text-gray-700 dark:hover:text-gray-300 transition-colors' : ''}`}
                  onClick={() => handleSort(col.accessor)}
                >
                  <div className="flex items-center gap-2">
                    <span>{col.header}</span>
                    {sortable && col.accessor && sortField === col.accessor && (
                      <span className="text-primary-500 transition-transform duration-200">
                        {sortDir === 'asc' ? '↑' : '↓'}
                      </span>
                    )}
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100/50 dark:divide-gray-800/30">
            {sortedData.map((row, idx) => (
              <tr
                key={row.id}
                onClick={() => onRowClick?.(row)}
                className={`${onRowClick ? 'cursor-pointer' : ''} animate-fade-in`}
                style={{ animationDelay: `${idx * 30}ms` }}
              >
                {columns.map((col, i) => (
                  <td key={i} className="px-4 sm:px-6 py-4">
                    {col.cell ? col.cell(row) : String((row as Record<string, unknown>)[col.accessor as string] ?? '')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
