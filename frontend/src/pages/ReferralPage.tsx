import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import Toast from '@/components/shared/Toast';
import { Share2, Copy, Users, Gift, TrendingUp, Award, ExternalLink } from 'lucide-react';

interface ReferralStats {
  totalReferrals: number;
  convertedReferrals: number;
  pendingReferrals: number;
  referralCode: string;
  referralLink: string;
}

interface ReferralEntry {
  id: string;
  invitedName: string;
  invitedEmail: string;
  statut: 'INVITE' | 'INSCRIT' | 'ACTIF';
  dateInvitation: string;
  dateInscription?: string;
}

export default function ReferralPage() {
  const { t } = useI18n();
  const [stats, setStats] = useState<ReferralStats | null>(null);
  const [referrals, setReferrals] = useState<ReferralEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [showInvite, setShowInvite] = useState(false);
  const [inviteForm, setInviteForm] = useState({ nom: '', email: '' });
  const [copied, setCopied] = useState(false);

  useEffect(() => { loadData(); }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [statsRes, referralsRes] = await Promise.allSettled([
        api.get('/referrals/stats'),
        api.get('/referrals'),
      ]);
      if (statsRes.status === 'fulfilled') setStats(statsRes.value.data);
      if (referralsRes.status === 'fulfilled') setReferrals(referralsRes.value.data?.content || referralsRes.value.data || []);
    } catch {
      // Fallback
    } finally {
      setLoading(false);
    }
  };

  const copyCode = async () => {
    if (!stats?.referralCode) return;
    try {
      await navigator.clipboard.writeText(stats.referralCode);
      setCopied(true);
      Toast.success('Code copié !');
      setTimeout(() => setCopied(false), 2000);
    } catch {
      Toast.error('Impossible de copier');
    }
  };

  const copyLink = async () => {
    if (!stats?.referralLink) return;
    try {
      await navigator.clipboard.writeText(stats.referralLink);
      setCopied(true);
      Toast.success('Lien copié !');
      setTimeout(() => setCopied(false), 2000);
    } catch {
      Toast.error('Impossible de copier');
    }
  };

  const sendInvitation = async () => {
    if (!inviteForm.nom.trim() || !inviteForm.email.trim()) {
      Toast.warning('Veuillez remplir tous les champs');
      return;
    }
    try {
      await api.post('/referrals/invite', inviteForm);
      Toast.success('Invitation envoyée !');
      setShowInvite(false);
      setInviteForm({ nom: '', email: '' });
      loadData();
    } catch {
      Toast.error("Erreur lors de l'envoi");
    }
  };

  const conversionRate = stats && stats.totalReferrals > 0
    ? Math.round((stats.convertedReferrals / stats.totalReferrals) * 100)
    : 0;

  const STATUT_CONFIG = {
    INVITE: { label: 'Invité', color: 'bg-amber-100 text-amber-700' },
    INSCRIT: { label: 'Inscrit', color: 'bg-blue-100 text-blue-700' },
    ACTIF: { label: 'Actif', color: 'bg-green-100 text-green-700' },
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-5xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Share2 className="w-8 h-8 text-pink-500" />
            Parrainage
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Invitez vos proches à rejoindre l'église et gagnez des points
          </p>
        </div>
        <button
          onClick={() => setShowInvite(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-pink-500 to-rose-500 text-white text-sm font-medium hover:from-pink-600 hover:to-rose-600 transition-all shadow-lg shadow-pink-500/25 flex items-center gap-2"
        >
          <Gift className="w-4 h-4" />
          Inviter
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div className="p-5 rounded-2xl bg-pink-50 dark:bg-pink-500/10 border border-pink-200 dark:border-pink-500/20">
          <Users className="w-6 h-6 text-pink-500 mb-2" />
          <div className="text-2xl font-bold text-pink-900 dark:text-pink-300">{stats?.totalReferrals || 0}</div>
          <div className="text-xs text-pink-600 dark:text-pink-400">Total invitations</div>
        </div>
        <div className="p-5 rounded-2xl bg-green-50 dark:bg-green-500/10 border border-green-200 dark:border-green-500/20">
          <TrendingUp className="w-6 h-6 text-green-500 mb-2" />
          <div className="text-2xl font-bold text-green-900 dark:text-green-300">{stats?.convertedReferrals || 0}</div>
          <div className="text-xs text-green-600 dark:text-green-400">Convertis</div>
        </div>
        <div className="p-5 rounded-2xl bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20">
          <Gift className="w-6 h-6 text-amber-500 mb-2" />
          <div className="text-2xl font-bold text-amber-900 dark:text-amber-300">{stats?.pendingReferrals || 0}</div>
          <div className="text-xs text-amber-600 dark:text-amber-400">En attente</div>
        </div>
        <div className="p-5 rounded-2xl bg-purple-50 dark:bg-purple-500/10 border border-purple-200 dark:border-purple-500/20">
          <Award className="w-6 h-6 text-purple-500 mb-2" />
          <div className="text-2xl font-bold text-purple-900 dark:text-purple-300">{conversionRate}%</div>
          <div className="text-xs text-purple-600 dark:text-purple-400">Taux de conversion</div>
        </div>
      </div>

      {/* Referral Code */}
      <div className="p-6 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 mb-8">
        <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Votre code de parrainage</h2>
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
          <div className="flex-1 p-4 rounded-xl bg-gray-50 dark:bg-white/5 border border-dashed border-gray-300 dark:border-white/10">
            <div className="text-2xl font-mono font-bold text-gray-900 dark:text-white tracking-wider">
              {stats?.referralCode || '—'}
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={copyCode}
              className="flex items-center gap-2 px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 text-sm hover:bg-gray-50 dark:hover:bg-white/5 transition-all"
            >
              <Copy className="w-4 h-4" />
              {copied ? 'Copié !' : 'Copier le code'}
            </button>
            <button
              onClick={copyLink}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-pink-600 text-white text-sm font-medium hover:bg-pink-700 transition-all"
            >
              <ExternalLink className="w-4 h-4" />
              Copier le lien
            </button>
          </div>
        </div>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-3">
          Partagez ce code ou lien avec vos proches. Quand ils s'inscrivent, vous gagnez des points XP !
        </p>
      </div>

      {/* Referral History */}
      <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Historique des invitations</h2>
      {referrals.length === 0 ? (
        <div className="p-8 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-center">
          <Share2 className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
          <p className="text-gray-500 dark:text-gray-400">Aucune invitation envoyée pour le moment</p>
        </div>
      ) : (
        <div className="space-y-3">
          {referrals.map(ref => {
            const statutInfo = STATUT_CONFIG[ref.statut];
            return (
              <div key={ref.id} className="flex items-center justify-between p-4 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
                <div>
                  <div className="text-sm font-medium text-gray-900 dark:text-white">{ref.invitedName}</div>
                  <div className="text-xs text-gray-500 dark:text-gray-400">{ref.invitedEmail}</div>
                  <div className="text-xs text-gray-400 mt-1">
                    Invité le {new Date(ref.dateInvitation).toLocaleDateString('fr-FR')}
                  </div>
                </div>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${statutInfo.color}`}>
                  {statutInfo.label}
                </span>
              </div>
            );
          })}
        </div>
      )}

      {/* Invite Modal */}
      {showInvite && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowInvite(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-md w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Inviter un proche</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Nom complet</label>
                <input
                  type="text"
                  value={inviteForm.nom}
                  onChange={e => setInviteForm({ ...inviteForm, nom: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-pink-500"
                  placeholder="Jean Dupont"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Email</label>
                <input
                  type="email"
                  value={inviteForm.email}
                  onChange={e => setInviteForm({ ...inviteForm, email: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-pink-500"
                  placeholder="jean@example.com"
                />
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowInvite(false)} className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 text-sm hover:bg-gray-50 dark:hover:bg-white/5 transition-all">
                Annuler
              </button>
              <button onClick={sendInvitation} className="px-4 py-2 rounded-xl bg-pink-600 text-white text-sm font-medium hover:bg-pink-700 transition-all">
                Envoyer l'invitation
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
