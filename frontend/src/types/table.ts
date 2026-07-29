import type { ReactNode } from 'react';

export interface ColumnDef<T> {
  header: string;
  accessor?: keyof T;
  cell?: (row: T) => ReactNode;
  sortable?: boolean;
  className?: string;
}
