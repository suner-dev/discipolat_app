import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  ListChecks, Plus, Trash2, X, CheckCircle2, Loader2,
} from 'lucide-react';

type Checklist = {
  id: string; titre: string; cibleType: string; cibleId?: string | null;
  statut: string; progression: number; createdAt?: string;
  items: { id: string; libelle: string; fait: boolean }[];
};

export function ChecklistsTab({ deptId, onChanged }: { deptId: string; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [titre, setTitre] = useState('');
  const [cibleType, setCibleType] = useState('GENERAL');
  const [items, setItems] = useState<string[]>(['', '']);

  const { data: checklists = [], isLoading } = useQuery({
    queryKey: ['department', deptId, 'checklists'],
    queryFn: async () => (await api.get(`/departments/${deptId}/checklists`)).data as Checklist[],
    enabled: !!deptId,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'checklists'] });
    onChanged();
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${deptId}/checklists`, {
        titre, cibleType,
        items: items.map((i) => i.trim()).filter(Boolean),
      });
    },
    onSuccess: () => {
      toast.success('Checklist créée ✅');
      setTitre(''); setItems(['', '']);
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const addItemMutation = useMutation({
    mutationFn: async ({ checklistId, libelle }: { checklistId: string; libelle: string }) =>
      api.post(`/departments/${deptId}/checklists/${checklistId}/items`, { libelle }),
    onSuccess: invalidate,
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const toggleItemMutation = useMutation({
    mutationFn: async ({ checklistId, itemId, fait }: { checklistId: string; itemId: string; fait: boolean }) =>
      api.put(`/departments/${deptId}/checklists/${checklistId}/items/${itemId}`, { fait }),
    onSuccess: invalidate,
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const closeMutation = useMutation({
    mutationFn: async (checklistId: string) =>
      api.put(`/departments/${deptId}/checklists/${checklistId}`, { statut: 'TERMINEE' }),
    onSuccess: () => { toast.success('Checklist terminée'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (checklistId: string) =>
      api.delete(`/departments/${deptId}/checklists/${checklistId}`),
    onSuccess: () => { toast.success('Checklist supprimée'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteItemMutation = useMutation({
    mutationFn: async ({ checklistId, itemId }: { checklistId: string; itemId: string }) =>
      api.delete(`/departments/${deptId}/checklists/${checklistId}/items/${itemId}`),
    onSuccess: invalidate,
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) {
    return <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>;
  }

  return (
    <div className="space-y-4">
      <div className="glass-card p-5">
        <div className="flex items-center gap-2 mb-4">
          <ListChecks className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Nouvelle checklist</h3>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
          <div>
            <label className="label">Titre *</label>
            <input className="input" value={titre} onChange={(e) => setTitre(e.target.value)}
              placeholder="Ex : Préparation du culte de dimanche" />
          </div>
          <div>
            <label className="label">Cible</label>
            <select className="input" value={cibleType} onChange={(e) => setCibleType(e.target.value)}>
              <option value="GENERAL">Général</option>
              <option value="TACHE">Tâche</option>
              <option value="EVENEMENT">Événement</option>
              <option value="EQUIPE">Équipe</option>
              <option value="MEMBRE">Membre</option>
            </select>
          </div>
        </div>
        <div className="space-y-2 mb-3">
          {items.map((item, idx) => (
            <div key={idx} className="flex items-center gap-2">
              <input className="input flex-1" value={item}
                onChange={(e) => setItems(items.map((v, i) => (i === idx ? e.target.value : v)))}
                placeholder={`Élément ${idx + 1} — ex : Sono testée`} />
              {items.length > 1 && (
                <button onClick={() => setItems(items.filter((_, i) => i !== idx))}
                  className="p-2 rounded-lg text-gray-400 hover:text-red-500 cursor-pointer">
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>
          ))}
          <button onClick={() => setItems([...items, ''])} className="text-xs text-primary-600 dark:text-primary-300 flex items-center gap-1 cursor-pointer">
            <Plus className="w-3.5 h-3.5" /> Ajouter un élément
          </button>
        </div>
        <button onClick={() => createMutation.mutate()} disabled={!titre.trim() || createMutation.isPending}
          className="btn-primary btn-sm cursor-pointer">
          <Plus className="w-4 h-4" /> Créer la checklist
        </button>
      </div>

      {checklists.length === 0 ? (
        <div className="text-center py-8">
          <ListChecks className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-400">Aucune checklist pour le moment</p>
        </div>
      ) : (
        checklists.map((c) => (
          <div key={c.id} className="glass-card p-4">
            <div className="flex items-center gap-2 mb-1 flex-wrap">
              <span className={`text-sm font-semibold ${c.statut === 'TERMINEE' ? 'text-gray-400 line-through' : 'text-gray-900 dark:text-gray-100'}`}>{c.titre}</span>
              <span className={`badge text-[9px] ${c.statut === 'TERMINEE' ? 'badge-success' : 'badge-info'}`}>
                {c.statut === 'TERMINEE' ? 'Terminée' : c.cibleType === 'GENERAL' ? 'Générale' : c.cibleType.toLowerCase()}
              </span>
            </div>
            <div className="flex items-center gap-2 mb-2">
              <div className="flex-1 h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                <div className="h-full rounded-full bg-gradient-to-r from-amber-500 to-orange-500 transition-all"
                  style={{ width: `${c.progression}%` }} />
              </div>
              <span className="text-[10px] text-gray-500">{c.progression}%</span>
            </div>
            <div className="space-y-1.5">
              {c.items.map((item) => (
                <div key={item.id} className="flex items-center gap-2 group">
                  <input type="checkbox" checked={item.fait}
                    onChange={() => toggleItemMutation.mutate({ checklistId: c.id, itemId: item.id, fait: !item.fait })}
                    className="w-4 h-4 accent-amber-500 cursor-pointer" />
                  <span className={`flex-1 text-sm ${item.fait ? 'line-through text-gray-400' : 'text-gray-700 dark:text-gray-200'}`}>{item.libelle}</span>
                  <button onClick={() => deleteItemMutation.mutate({ checklistId: c.id, itemId: item.id })}
                    className="p-1 rounded text-gray-300 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-all cursor-pointer">
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
            <div className="flex items-center gap-2 mt-3">
              <form onSubmit={(e) => {
                e.preventDefault();
                const input = e.currentTarget.querySelector('input') as HTMLInputElement;
                if (input.value.trim()) addItemMutation.mutate({ checklistId: c.id, libelle: input.value.trim() });
                input.value = '';
              }} className="flex-1 flex gap-2">
                <input className="input py-1.5 text-xs flex-1" placeholder="Ajouter un élément…" />
              </form>
              {c.statut !== 'TERMINEE' && (
                <button onClick={() => closeMutation.mutate(c.id)} className="btn-ghost btn-sm text-xs cursor-pointer">
                  <CheckCircle2 className="w-3.5 h-3.5" /> Terminer
                </button>
              )}
              <button onClick={() => deleteMutation.mutate(c.id)} className="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-500/10 transition-all cursor-pointer">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))
      )}
    </div>
  );
}

// ============================================================
// INVENTAIRE — matériel du département
// ============================================================

type Equipment = {
  id: string; nom: string; description?: string; quantite: number;
  etat: string; responsableId?: string | null; affecteAId?: string | null;
  localisation?: string; dateAcquisition?: string;
};

const ETAT_LABELS: Record<string, string> = {
  NEUF: 'Neuf', BON: 'Bon état', USAGE: 'Usage', REPARATION: 'En réparation', HORS_SERVICE: 'Hors service',
};
const ETAT_COLORS: Record<string, string> = {
  NEUF: 'badge-success', BON: 'badge-info', USAGE: 'badge-gray', REPARATION: 'badge-warning', HORS_SERVICE: 'badge-danger',
};

