import { History, Activity } from 'lucide-react';
import type { ActivityItem } from './types';
import { ACTION_LABELS } from './types';

export function ActivityTab({ activity }: { activity: ActivityItem[] }) {
  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-4">
        <History className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Journal d'activité</h3>
        <span className="badge text-[10px] badge-gray">{activity.length}</span>
      </div>
      {activity.length === 0 ? (
        <div className="text-center py-8">
          <Activity className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-400">Aucune activité enregistrée pour l'instant</p>
        </div>
      ) : (
        <div className="space-y-0">
          {activity.map((a, i) => (
            <div key={a.id} className="relative flex gap-3 pb-4">
              {i < activity.length - 1 && <span className="absolute left-[9px] top-6 bottom-0 w-px bg-gray-200 dark:bg-gray-700" />}
              <div className="w-[18px] h-[18px] rounded-full bg-gradient-to-br from-amber-500 to-orange-500 shrink-0 mt-0.5 flex items-center justify-center">
                <span className="w-2 h-2 rounded-full bg-white" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm text-gray-800 dark:text-gray-200">
                  <span className="font-semibold">{ACTION_LABELS[a.action] || a.action.replace(/_/g, ' ')}</span>
                  {a.details && <span className="text-gray-500 dark:text-gray-400"> — {a.details}</span>}
                </p>
                <p className="text-[10px] text-gray-400">
                  {a.actorNom ? `par ${a.actorNom} · ` : ''}{new Date(a.createdAt).toLocaleString('fr-FR')}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

