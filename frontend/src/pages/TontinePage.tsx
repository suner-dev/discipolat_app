import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { Loader2, HandCoins, Plus, CheckCircle2, ArrowRightCircle, Users } from 'lucide-react';
import toast from 'react-hot-toast';

interface TontineGroup {
  id: string;
  name: string;
  description: string | null;
  montantParTour: number;
  periodicite: string;
  tourActuel: number;
}

interface GroupDetail {
  group: TontineGroup;
  members: {
    id: string;
    nom: string;
    ordrePassage: number;
    aRecuTour: boolean;
    paye: boolean;
    datePaiement: string | null;
  }[];
  totalCollecte: number;
  totalAttendu: number;
  progressPercent: number;
}

/** Tontine Numérique — groupes de contribution et suivi des versements. */
export default function TontinePage() {
  const { activeRole } = useAuth();
  // Lecture ouverte à tous ; écritures (créer, marquer payé, ajouter membre,
  // tour suivant) réservées aux gestionnaires — cohérent avec l'API.
  const canManage = activeRole === 'ADMIN' || activeRole === 'PASTEUR' || activeRole === 'RESPONSABLE';
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [newGroup, setNewGroup] = useState({ name: '', montantParTour: '', periodicite: 'MENSUELLE' });
  const [newMemberName, setNewMemberName] = useState('');

  const groupsQuery = useQuery({
    queryKey: ['tontines'],
    queryFn: async () => (await api.get<TontineGroup[]>('/tontines')).data,
  });

  const detailQuery = useQuery({
    queryKey: ['tontines', selectedId],
    enabled: !!selectedId,
    queryFn: async () => (await api.get<GroupDetail>(`/tontines/${selectedId}`)).data,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['tontines'] });
  };

  const createMutation = useMutation({
    mutationFn: async () =>
      (await api.post('/tontines', {
        name: newGroup.name,
        montantParTour: Number(newGroup.montantParTour),
        periodicite: newGroup.periodicite,
      })).data,
    onSuccess: () => {
      toast.success('Tontine créée');
      setShowCreate(false);
      setNewGroup({ name: '', montantParTour: '', periodicite: 'MENSUELLE' });
      invalidate();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const addMemberMutation = useMutation({
    mutationFn: async () =>
      (
        await api.post(`/tontines/${selectedId}/members`, { nom: newMemberName })
      ).data,
    onSuccess: () => {
      toast.success('Membre ajouté');
      setNewMemberName('');
      invalidate();
      if (selectedId) queryClient.invalidateQueries({ queryKey: ['tontines', selectedId] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const payMutation = useMutation({
    mutationFn: async (memberId: string) =>
      (await api.post(`/tontines/${selectedId}/contributions/${memberId}/pay`)).data,
    onSuccess: () => {
      toast.success('Versement enregistré');
      invalidate();
      if (selectedId) queryClient.invalidateQueries({ queryKey: ['tontines', selectedId] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const nextRoundMutation = useMutation({
    mutationFn: async () => (await api.post<{ beneficiary?: string; newRound: number }>(`/tontines/${selectedId}/next-round`)).data,
    onSuccess: (r) => {
      toast.success(r.beneficiary ? `Bénéficiaire : ${r.beneficiary} — tour ${r.newRound}` : `Tour ${r.newRound}`);
      invalidate();
      if (selectedId) queryClient.invalidateQueries({ queryKey: ['tontines', selectedId] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-amber-500 to-orange-600 text-white shadow-lg">
          <HandCoins className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Tontines</h1>
          <p className="page-subtitle">Épargne solidaire & confiance — échéanciers partagés</p>
        </div>
        {canManage && (
          <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm ml-auto flex items-center gap-2">
            <Plus className="w-4 h-4" /> Nouvelle tontine
          </button>
        )}
      </div>

      {showCreate && (
        <form
          onSubmit={(e) => { e.preventDefault(); createMutation.mutate(); }}
          className="glass-card p-5 mb-6 grid gap-3 md:grid-cols-4 animate-slide-up"
        >
          <input required placeholder="Nom du groupe" className="input md:col-span-2"
            value={newGroup.name}
            onChange={(e) => setNewGroup({ ...newGroup, name: e.target.value })} />
          <input required type="number" min="0" placeholder="Montant par tour"
            className="input"
            value={newGroup.montantParTour}
            onChange={(e) => setNewGroup({ ...newGroup, montantParTour: e.target.value })} />
          <select className="input" value={newGroup.periodicite}
            onChange={(e) => setNewGroup({ ...newGroup, periodicite: e.target.value })}>
            <option value="HEBDOMADAIRE">Hebdomadaire</option>
            <option value="MENSUELLE">Mensuelle</option>
            <option value="TRIMESTRIELLE">Trimestrielle</option>
          </select>
          <button type="submit" disabled={createMutation.isPending} className="btn-primary btn-sm md:col-span-4">
            {createMutation.isPending && <Loader2 className="inline w-4 h-4 animate-spin mr-1" />} Créer
          </button>
        </form>
      )}

      {/* Liste */}
      <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3 mb-6">
        {(groupsQuery.data ?? []).map((g) => (
          <button key={g.id}
            onClick={() => setSelectedId(g.id === selectedId ? null : g.id)}
            className={`glass-card p-5 text-left transition-all hover:shadow-lg ${selectedId === g.id ? 'ring-2 ring-primary-500' : ''}`}
          >
            <p className="font-semibold text-gray-900 dark:text-gray-100">{g.name}</p>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              {Number(g.montantParTour).toLocaleString('fr-FR')} / tour · {g.periodicite.toLowerCase()}
            </p>
            <span className="badge badge-info mt-3 inline-block">Tour actuel : {g.tourActuel}</span>
          </button>
        ))}
        {!groupsQuery.isLoading && (groupsQuery.data ?? []).length === 0 && (
          <p className="text-sm text-gray-500 dark:text-gray-400 col-span-full py-8 text-center">
            Aucune tontine active. Créez la première !
          </p>
        )}
      </div>

      {/* Détail */}
      {selectedId && (
        <div className="glass-card p-6 animate-slide-up">
          {detailQuery.isLoading || !detailQuery.data ? (
            <Loader2 className="w-6 h-6 animate-spin text-primary-500 mx-auto my-6" />
          ) : (
            <>
              <div className="flex items-center justify-between mb-4">
                <h2 className="font-semibold text-gray-900 dark:text-gray-100">
                  Échéancier — tour {detailQuery.data.group.tourActuel}
                </h2>
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-500 dark:text-gray-400">
                    Collecté : {detailQuery.data.totalCollecte.toLocaleString('fr-FR')} /{' '}
                    {detailQuery.data.totalAttendu.toLocaleString('fr-FR')}
                  </span>
                  {canManage && (
                    <button
                      onClick={() => nextRoundMutation.mutate()}
                      disabled={nextRoundMutation.isPending}
                      className="btn-primary btn-sm flex items-center gap-1"
                    >
                      <ArrowRightCircle className="w-4 h-4" /> Tour suivant
                    </button>
                  )}
                </div>
              </div>
              <div className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden mb-4">
                <div className="h-full bg-gradient-to-r from-amber-500 to-orange-500 rounded-full transition-all duration-500"
                  style={{ width: `${Math.min(100, detailQuery.data.progressPercent)}%` }} />
              </div>

              <div className="divide-y divide-gray-100 dark:divide-gray-800">
                {(detailQuery.data.members ?? []).map((m, i) => (
                  <div key={m.id} className="flex items-center justify-between py-3">
                    <div className="flex items-center gap-3">
                      <span className="w-7 h-7 rounded-full bg-gray-100 dark:bg-gray-800 flex items-center justify-center text-xs font-bold text-gray-600 dark:text-gray-300">
                        {i + 1}
                      </span>
                      <div>
                        <p className="font-medium text-gray-900 dark:text-gray-100">{m.nom}</p>
                        <p className="text-xs text-gray-500 dark:text-gray-400">
                          {m.aRecuTour ? 'A déjà reçu son tour' : 'En attente de son tour'}
                        </p>
                      </div>
                    </div>
                    {m.paye ? (
                      <CheckCircle2 className="w-5 h-5 text-green-500" />
                    ) : canManage ? (
                      <button
                        onClick={() => payMutation.mutate(m.id)}
                        disabled={payMutation.isPending}
                        className="btn-success btn-sm"
                      >
                        Marquer payé
                      </button>
                    ) : (
                      <span className="text-xs text-gray-400 dark:text-gray-500">En attente</span>
                    )}
                  </div>
                ))}
              </div>

              {canManage && (
                <form
                  onSubmit={(e) => { e.preventDefault(); addMemberMutation.mutate(); }}
                  className="flex gap-2 mt-4"
                >
                  <input required placeholder="Nom du nouveau membre" className="input flex-1"
                    value={newMemberName}
                    onChange={(e) => setNewMemberName(e.target.value)} />
                  <button type="submit" disabled={addMemberMutation.isPending} className="btn-primary btn-sm flex items-center gap-1">
                    <Users className="w-4 h-4" /> Ajouter
                  </button>
                </form>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}
