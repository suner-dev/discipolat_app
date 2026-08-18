import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  Boxes, Pencil, Trash2, Save, X, Loader2,
} from 'lucide-react';

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

export function InventoryTab({ deptId, onChanged }: { deptId: string; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [nom, setNom] = useState('');
  const [description, setDescription] = useState('');
  const [quantite, setQuantite] = useState(1);
  const [etat, setEtat] = useState('BON');
  const [localisation, setLocalisation] = useState('');
  const [editing, setEditing] = useState<Equipment | null>(null);

  const { data: equipment = [], isLoading } = useQuery({
    queryKey: ['department', deptId, 'equipment'],
    queryFn: async () => (await api.get(`/departments/${deptId}/equipment`)).data as Equipment[],
    enabled: !!deptId,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'equipment'] });
    onChanged();
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      const payload = { nom, description: description || null, quantite, etat, localisation: localisation || null };
      if (editing) {
        await api.put(`/departments/${deptId}/equipment/${editing.id}`, payload);
      } else {
        await api.post(`/departments/${deptId}/equipment`, payload);
      }
    },
    onSuccess: () => {
      toast.success(editing ? 'Équipement modifié ✅' : 'Équipement ajouté ✅');
      setNom(''); setDescription(''); setQuantite(1); setEtat('BON'); setLocalisation(''); setEditing(null);
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (equipmentId: string) => api.delete(`/departments/${deptId}/equipment/${equipmentId}`),
    onSuccess: () => { toast.success('Équipement supprimé'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) {
    return <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>;
  }

  const totalItems = equipment.reduce((sum, e) => sum + e.quantite, 0);

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Équipements</span>
          <p className="stat-value text-xl">{equipment.length}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Articles au total</span>
          <p className="stat-value text-xl">{totalItems}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">En réparation</span>
          <p className="stat-value text-xl text-amber-500">{equipment.filter((e) => e.etat === 'REPARATION').length}</p>
        </div>
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Hors service</span>
          <p className="stat-value text-xl text-red-500">{equipment.filter((e) => e.etat === 'HORS_SERVICE').length}</p>
        </div>
      </div>

      <div className="glass-card p-5">
        <div className="flex items-center gap-2 mb-4">
          <Boxes className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
            {editing ? `Modifier « ${editing.nom} »` : 'Nouvel équipement'}
          </h3>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          <div>
            <label className="label">Nom *</label>
            <input className="input" value={nom} onChange={(e) => setNom(e.target.value)} placeholder="Ex : Caméra Sony A7" />
          </div>
          <div>
            <label className="label">Quantité</label>
            <input type="number" min={1} className="input" value={quantite}
              onChange={(e) => setQuantite(Math.max(1, Number(e.target.value) || 1))} />
          </div>
          <div>
            <label className="label">État</label>
            <select className="input" value={etat} onChange={(e) => setEtat(e.target.value)}>
              {Object.entries(ETAT_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </div>
          <div className="sm:col-span-2 lg:col-span-2">
            <label className="label">Description</label>
            <input className="input" value={description} onChange={(e) => setDescription(e.target.value)}
              placeholder="Marque, modèle, caractéristiques…" />
          </div>
          <div>
            <label className="label">Localisation</label>
            <input className="input" value={localisation} onChange={(e) => setLocalisation(e.target.value)} placeholder="Ex : Salle 3, studio…" />
          </div>
        </div>
        <div className="flex items-center gap-2 mt-4">
          <button onClick={() => saveMutation.mutate()} disabled={!nom.trim() || saveMutation.isPending}
            className="btn-primary btn-sm cursor-pointer">
            <Save className="w-4 h-4" /> {editing ? 'Enregistrer' : 'Ajouter'}
          </button>
          {editing && (
            <button onClick={() => { setEditing(null); setNom(''); setDescription(''); setQuantite(1); setEtat('BON'); setLocalisation(''); }}
              className="btn-ghost btn-sm cursor-pointer">
              <X className="w-4 h-4" /> Annuler
            </button>
          )}
        </div>
      </div>

      {equipment.length === 0 ? (
        <div className="text-center py-8">
          <Boxes className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-400">Aucun équipement enregistré</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {equipment.map((e) => (
            <div key={e.id} className="glass-card p-4">
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2 min-w-0">
                  <div className="p-2 rounded-lg bg-primary-500/10 text-primary-600 dark:text-primary-300 shrink-0">
                    <Boxes className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{e.nom}</p>
                    <span className={`badge text-[9px] ${ETAT_COLORS[e.etat] || 'badge-gray'}`}>{ETAT_LABELS[e.etat] || e.etat}</span>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <button onClick={() => {
                    setEditing(e);
                    setNom(e.nom); setDescription(e.description || ''); setQuantite(e.quantite);
                    setEtat(e.etat); setLocalisation(e.localisation || '');
                  }} className="p-1.5 rounded-lg text-gray-400 hover:text-primary-500 hover:bg-primary-500/10 transition-all cursor-pointer" title="Modifier">
                    <Pencil className="w-3.5 h-3.5" />
                  </button>
                  <button onClick={() => deleteMutation.mutate(e.id)}
                    className="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-500/10 transition-all cursor-pointer" title="Supprimer">
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
              <div className="mt-2 flex items-center gap-2 text-[11px] text-gray-500">
                <span>×{e.quantite}</span>
                {e.localisation && <><span>·</span><span className="truncate">{e.localisation}</span></>}
              </div>
              {e.description && <p className="mt-1 text-[11px] text-gray-400 line-clamp-2">{e.description}</p>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// RECHERCHE GLOBALE DU DÉPARTEMENT
// ============================================================

