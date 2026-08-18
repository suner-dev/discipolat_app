import { useNavigate } from 'react-router-dom';
import { Loader2, X, Users2, Network, Briefcase, ListTodo, CalendarDays } from 'lucide-react';
import { STATUT_TASK_BADGE } from './types';

function formatEventDate(value: string) {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }) +
    ' · ' + d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

/**
 * Panneau des résultats de la recherche globale du département (gestion).
 * Extrait du monolithe DepartmentManagementPage lors de la Phase 3 — réduction
 * de la dette technique.
 */
export function GlobalSearchResults({
  results, deptId, onClear,
}: { results: any; deptId: string; onClear: () => void }) {
  const navigate = useNavigate();
  if (!results) {
    return (
      <div className="glass-card p-5 mb-5 flex items-center gap-3">
        <Loader2 className="w-4 h-4 animate-spin text-primary-500" /> Recherche en cours…
      </div>
    );
  }
  const total = results.total ?? 0;
  const sections = [
    {
      key: 'membres', label: 'Membres', items: results.membres ?? [], render: (m: any) => (
        <button key={m.id} onClick={() => { navigate(`/departments/${deptId}/members/${m.id}`); onClear(); }}
          className="w-full text-left px-3 py-2 hover:bg-gray-100 dark:hover:bg-gray-700/60 rounded-lg flex items-center justify-between gap-2 cursor-pointer">
          <span className="flex items-center gap-2 text-sm">
            <Users2 className="w-4 h-4 text-primary-500" /> {m.nomComplet}
          </span>
          <span className="text-[11px] text-gray-400">{m.statut}</span>
        </button>
      ),
    },
    {
      key: 'equipes', label: 'Équipes', items: results.equipes ?? [], render: (t: any) => (
        <div key={t.id} className="px-3 py-2 flex items-center justify-between gap-2">
          <span className="flex items-center gap-2 text-sm"><Network className="w-4 h-4 text-amber-500" /> {t.nom}</span>
          <span className="badge badge-gray text-[10px]">{t.nbMembres} membre{t.nbMembres > 1 ? 's' : ''}</span>
        </div>
      ),
    },
    {
      key: 'postes', label: 'Postes', items: results.postes ?? [], render: (p: any) => (
        <div key={p.id} className="px-3 py-2 flex items-center gap-2 text-sm">
          <Briefcase className="w-4 h-4 text-sky-500" /> {p.nom}
        </div>
      ),
    },
    {
      key: 'taches', label: 'Tâches', items: results.taches ?? [], render: (t: any) => (
        <div key={t.id} className="px-3 py-2 flex items-center justify-between gap-2">
          <span className="flex items-center gap-2 text-sm"><ListTodo className="w-4 h-4 text-emerald-500" /> {t.titre}</span>
          <span className={`badge text-[10px] ${STATUT_TASK_BADGE[t.statut] || 'badge-gray'}`}>{t.statut}</span>
        </div>
      ),
    },
    {
      key: 'evenements', label: 'Événements', items: results.evenements ?? [], render: (e: any) => (
        <div key={e.id} className="px-3 py-2 flex items-center justify-between gap-2">
          <span className="flex items-center gap-2 text-sm"><CalendarDays className="w-4 h-4 text-violet-500" /> {e.titre}</span>
          {e.dateDebut && <span className="text-[11px] text-gray-400">{formatEventDate(e.dateDebut)}</span>}
        </div>
      ),
    },
  ];
  return (
    <div className="glass-card p-5 mb-5">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
          {total > 0 ? `${total} résultat${total > 1 ? 's' : ''}` : 'Aucun résultat'} pour cette recherche
        </h3>
        <button onClick={onClear} className="text-xs text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 cursor-pointer">
          <X className="w-4 h-4" />
        </button>
      </div>
      {total === 0 ? (
        <p className="text-sm text-gray-400">Essayez un nom, un poste, une équipe, une tâche ou un événement.</p>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {sections.filter((s) => s.items.length > 0).map((s) => (
            <div key={s.key}>
              <p className="text-[11px] uppercase tracking-wide text-gray-400 mb-1">{s.label} ({s.items.length})</p>
              <div className="space-y-0.5">{s.items.map(s.render)}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
