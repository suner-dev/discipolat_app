import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { History, Loader2, ChevronDown, ChevronUp } from 'lucide-react';

interface ConfigRevision {
  id: string;
  entityType: string;
  entityKey?: string;
  action: string;
  payload?: Record<string, unknown>;
  userId?: string;
  createdAt: string;
}

const ACTION_LABELS: Record<string, string> = {
  MODULE_ENABLED: 'Module activé',
  MODULE_DISABLED: 'Module désactivé',
  MODULE_CREATED: 'Module créé',
  MODULE_UPDATED: 'Module modifié',
  MODULE_DELETED: 'Module supprimé',
  MENU_CREATED: 'Menu créé',
  MENU_UPDATED: 'Menu modifié',
  MENU_DELETED: 'Menu supprimé',
  MENUS_REORDERED: 'Menus réordonnés',
};

function actionLabel(action: string): string {
  return ACTION_LABELS[action] || action;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

/**
 * Panneau « Historique » (versionnage de configuration) : liste append-only
 * des révisions d'une entité de plateforme (modules / menus), triée du plus
 * récent au plus ancien. Charge à la demande à l'ouverture.
 */
export default function ConfigRevisionHistory({
  entityType,
  title = 'Historique des modifications',
}: {
  entityType: string;
  title?: string;
}) {
  const [open, setOpen] = useState(false);

  const { data: revisions = [], isLoading } = useQuery({
    queryKey: ['platform', 'revisions', entityType],
    queryFn: async () => {
      const res = await api.get(`/platform/revisions?entityType=${entityType}&size=50`);
      return (res.data.content || []) as ConfigRevision[];
    },
    enabled: open,
  });

  return (
    <div className="glass-card p-4 animate-slide-up">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="w-full flex items-center justify-between"
        aria-expanded={open}
      >
        <span className="flex items-center gap-2 text-sm font-semibold text-gray-800 dark:text-gray-100">
          <History className="w-4 h-4 text-primary-500" />
          {title}
          <span className="text-xs font-normal text-gray-400">
            ({isLoading ? '…' : revisions.length})
          </span>
        </span>
        {open ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
      </button>

      {open && (
        <div className="mt-4">
          {isLoading ? (
            <div className="flex items-center gap-2 text-sm text-gray-400 py-4">
              <Loader2 className="w-4 h-4 animate-spin" /> Chargement de l'historique…
            </div>
          ) : revisions.length === 0 ? (
            <p className="text-sm text-gray-400 py-4">Aucune modification enregistrée pour le moment.</p>
          ) : (
            <ol className="relative border-l border-gray-200 dark:border-gray-700 ml-2 space-y-4">
              {revisions.map((r) => (
                <li key={r.id} className="ml-4">
                  <span className="absolute -left-[5px] mt-1.5 w-2 h-2 rounded-full bg-primary-400" />
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-medium text-gray-800 dark:text-gray-100">
                      {actionLabel(r.action)}
                    </span>
                    {r.entityKey && (
                      <code className="text-[11px] px-1.5 py-0.5 rounded bg-gray-100 dark:bg-gray-800 text-gray-500 font-mono">
                        {r.entityKey}
                      </code>
                    )}
                  </div>
                  <p className="text-xs text-gray-400 mt-0.5">{formatDate(r.createdAt)}</p>
                </li>
              ))}
            </ol>
          )}
        </div>
      )}
    </div>
  );
}
