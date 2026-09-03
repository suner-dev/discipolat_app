import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Award, Download, Star, Loader2, Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface Certificate {
  id: string;
  memberId: string;
  memberName?: string;
  titre: string;
  description: string;
  category: string;
  issuedAt: string;
  issuedBy?: string;
}

interface EligibleReward {
  titre: string;
  description: string;
  category: string;
  reason: string;
}

export default function RewardCertificatesPage() {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'all' | 'eligible'>('all');
  const [showIssue, setShowIssue] = useState(false);
  const [newCert, setNewCert] = useState({ memberId: '', titre: '', description: '', category: 'PARTICIPATION' });

  const { data: certificates = [], isLoading } = useQuery({
    queryKey: ['reward-certificates'],
    queryFn: async () => (await api.get<Certificate[]>('/reward-certificates')).data,
  });

  const { data: eligible = [], isLoading: eligibleLoading } = useQuery({
    queryKey: ['reward-certificates-eligible'],
    queryFn: async () => (await api.get<EligibleReward[]>('/reward-certificates/eligible')).data,
  });

  const issueMutation = useMutation({
    mutationFn: async () => {
      if (!newCert.memberId.trim() || !newCert.titre.trim()) { toast('Remplissez les champs requis', { icon: '⚠️' }); throw new Error('empty'); }
      return api.post('/reward-certificates', newCert);
    },
    onSuccess: () => {
      toast.success('Certificat émis');
      setShowIssue(false);
      setNewCert({ memberId: '', titre: '', description: '', category: 'PARTICIPATION' });
      queryClient.invalidateQueries({ queryKey: ['reward-certificates'] });
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  const CATEGORY_COLORS: Record<string, string> = {
    PARTICIPATION: 'bg-blue-100 text-blue-700', EXCELLENCE: 'bg-yellow-100 text-yellow-700',
    FIDÉLITÉ: 'bg-green-100 text-green-700', SERVICE: 'bg-purple-100 text-purple-700',
    ÉVANGÉLISATION: 'bg-orange-100 text-orange-700',
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-yellow-500 to-amber-600 text-white shadow-lg">
          <Award className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Certificats de Récompense</h1>
          <p className="page-subtitle">Récompensez l'engagement de vos membres</p>
        </div>
        <div className="ml-auto flex items-center gap-3">
          <div className="flex rounded-xl bg-white/5 border border-white/10 p-1">
            <button onClick={() => setTab('all')}
              className={`px-4 py-1.5 rounded-lg text-sm font-medium transition ${tab === 'all' ? 'bg-yellow-500 text-white' : 'text-gray-400 hover:text-white'}`}>
              Tous ({certificates.length})
            </button>
            <button onClick={() => setTab('eligible')}
              className={`px-4 py-1.5 rounded-lg text-sm font-medium transition ${tab === 'eligible' ? 'bg-yellow-500 text-white' : 'text-gray-400 hover:text-white'}`}>
              Éligibles ({eligible.length})
            </button>
          </div>
          <button onClick={() => setShowIssue(true)}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-yellow-500 to-amber-500 text-white text-sm font-medium hover:from-yellow-600 hover:to-amber-600 transition-all shadow-lg flex items-center gap-2">
            <Plus className="w-4 h-4" /> Émettre
          </button>
        </div>
      </div>

      {tab === 'all' && (
        isLoading ? <SkeletonLoader lines={4} variant="card" /> :
          certificates.length === 0 ? (
            <EmptyState icon={<Award className="w-8 h-8 text-gray-400" />}
              title="Aucun certificat"
              message="Émettez des certificats pour récompenser vos membres"
              action={{ label: 'Émettre un certificat', onClick: () => setShowIssue(true) }} />
          ) : (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {certificates.map(c => (
                <div key={c.id} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                  <div className="flex items-center justify-between mb-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${CATEGORY_COLORS[c.category] || 'bg-gray-100 text-gray-700'}`}>
                      {c.category}
                    </span>
                    <Star className="w-4 h-4 text-yellow-400 fill-yellow-400" />
                  </div>
                  <h3 className="font-semibold text-gray-900 dark:text-white text-sm mb-1">{c.titre}</h3>
                  <p className="text-xs text-gray-500 mb-2">{c.description}</p>
                  <div className="text-xs text-gray-400">
                    <div>{c.memberName || c.memberId.slice(0, 8)}...</div>
                    <div>{new Date(c.issuedAt).toLocaleDateString('fr-FR')}</div>
                  </div>
                </div>
              ))}
            </div>
          )
      )}

      {tab === 'eligible' && (
        eligibleLoading ? <SkeletonLoader lines={3} variant="card" /> :
          eligible.length === 0 ? (
            <EmptyState icon={<Award className="w-8 h-8 text-gray-400" />}
              title="Aucune récompense éligible"
              message="Les récompenses éligibles apparaîtront ici" />
          ) : (
            <div className="grid gap-4 md:grid-cols-2">
              {eligible.map((e, i) => (
                <div key={i} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                  <div className="flex items-center gap-2 mb-2">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${CATEGORY_COLORS[e.category] || 'bg-gray-100 text-gray-700'}`}>
                      {e.category}
                    </span>
                  </div>
                  <h3 className="font-semibold text-gray-900 dark:text-white text-sm mb-1">{e.titre}</h3>
                  <p className="text-xs text-gray-500 mb-1">{e.description}</p>
                  <p className="text-xs text-gray-400 italic">{e.reason}</p>
                </div>
              ))}
            </div>
          )
      )}

      {showIssue && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowIssue(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Émettre un certificat</h2>
            <div className="space-y-4">
              <input type="text" value={newCert.memberId} onChange={e => setNewCert({ ...newCert, memberId: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="ID du membre" />
              <input type="text" value={newCert.titre} onChange={e => setNewCert({ ...newCert, titre: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="Titre du certificat" />
              <textarea value={newCert.description} onChange={e => setNewCert({ ...newCert, description: e.target.value })}
                rows={2} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none"
                placeholder="Description" />
              <select value={newCert.category} onChange={e => setNewCert({ ...newCert, category: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
                <option value="PARTICIPATION">Participation</option>
                <option value="EXCELLENCE">Excellence</option>
                <option value="FIDÉLITÉ">Fidélité</option>
                <option value="SERVICE">Service</option>
                <option value="ÉVANGÉLISATION">Évangélisation</option>
              </select>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowIssue(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={() => issueMutation.mutate()} disabled={issueMutation.isPending}
                className="px-4 py-2 rounded-xl bg-yellow-500 text-white text-sm font-medium hover:bg-yellow-600 flex items-center gap-2">
                {issueMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                Émettre
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
