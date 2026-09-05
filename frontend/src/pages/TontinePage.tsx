import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { Loader2, HandCoins, Plus, CheckCircle2, ArrowRightCircle, Users, Landmark, AlertTriangle } from 'lucide-react';
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

interface TontineStats {
  activeGroups: number;
}

interface DashboardData {
  groupId: string;
  groupName: string;
  statut: string;
  montantParTour: number;
  periodicite: string;
  totalMembers: number;
  activeMembers: number;
  nextRecipient: string;
  totalContributions: number;
  totalCollected: number;
  expectedPerPeriod: number;
  pendingPayments: number;
  overduePayments: number;
  tourActuel: number;
  toursCompleted: number;
  totalTours: number;
}

interface OverdueRow {
  membre: string;
  ordrePassage: number;
  tour: number;
  montantDu: number;
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

  const statsQuery = useQuery({
    queryKey: ['tontines', 'stats'],
    queryFn: async () => (await api.get<TontineStats>('/tontines/stats')).data,
  });

  const dashboardQuery = useQuery({
    queryKey: ['tontines', selectedId, 'dashboard'],
    enabled: !!selectedId,
    queryFn: async () => (await api.get<DashboardData>(`/tontines/${selectedId}/dashboard`)).data,
  });

  const overdueQuery = useQuery({
    queryKey: ['tontines', selectedId, 'overdue'],
    enabled: !!selectedId,
    queryFn: async () => (await api.get<OverdueRow[]>(`/tontines/${selectedId}/overdue`)).data,
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

      {/* Vue d'ensemble */}
      {statsQuery.data && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <div className="glass-card p-4">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-amber-100 dark:bg-amber-900/30">
                <Landmark className="w-5 h-5 text-amber-600 dark:text-amber-400" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-mono">{statsQuery.data.activeGroups}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">Groupes actifs</p>
              </div>
            </div>
          </div>
          {dashboardQuery.data && (
            <div className="glass-card p-4 col-span-1 sm:col-span-2">
              <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
                <span className="text-gray-500 dark:text-gray-400">Prochain bénéficiaire :</span>
                <span className="font-semibold text-gray-900 dark:text-gray-100">{dashboardQuery.data.nextRecipient}</span>
                <span className="text-xs text-gray-400">· Collecté {dashboardQuery.data.totalCollected.toLocaleString('fr-FR')} / {dashboardQuery.data.expectedPerPeriod.toLocaleString('fr-FR')}</span>
                <span className="text-xs text-gray-400">· Tour {dashboardQuery.data.tourActuel} / {dashboardQuery.data.totalTours}</span>
              </div>
            </div>
          )}
        </div>
      )}

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

              {/* Impayés */}
              {overdueQuery.isLoading ? (
                <div className="mt-4 pt-4 border-t border-gray-100 dark:border-gray-800 flex justify-center py-4">
                  <Loader2 className="w-5 h-5 animate-spin text-amber-500" />
                </div>
              ) : overdueQuery.data && overdueQuery.data.length > 0 ? (
                <div className="mt-4 pt-4 border-t border-gray-100 dark:border-gray-800">
                  <div className="flex items-center gap-2 mb-2">
                    <AlertTriangle className="w-4 h-4 text-red-500" />
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                      Impayés ({overdueQuery.data.length})
                    </p>
                  </div>
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="text-left text-[10px] uppercase text-gray-400 border-b border-gray-100 dark:border-gray-800">
                          <th className="py-2 pr-2">Membre</th>
                          <th className="py-2 pr-2">Ordre</th>
                          <th className="py-2 pr-2">Tour</th>
                          <th className="py-2">Montant dû</th>
                        </tr>
                      </thead>
                      <tbody>
                        {overdueQuery.data.map((o, i) => (
                          <tr key={i} className="border-b border-gray-50 dark:border-gray-800/60">
                            <td className="py-2 pr-2 font-medium text-gray-900 dark:text-gray-100">{o.membre}</td>
                            <td className="py-2 pr-2 text-gray-500">{o.ordrePassage}</td>
                            <td className="py-2 pr-2 text-gray-500">{o.tour}</td>
                            <td className="py-2 text-gray-700 dark:text-gray-300 font-medium">{Number(o.montantDu).toLocaleString('fr-FR')}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              ) : null}

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
