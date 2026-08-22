import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { CalendarOff, Plus, CheckCircle2, XCircle, Clock, AlertTriangle } from 'lucide-react';

interface LeaveRequest {
  id: string;
  type: 'MALADIE' | 'CONGE' | 'MISSION' | 'PERSONNEL' | 'AUTRE';
  dateDebut: string;
  dateFin: string;
  motif: string;
  statut: 'EN_ATTENTE' | 'APPROUVE' | 'REFUSE' | 'ANNULE';
  demandeur: { id: string; firstName: string; lastName: string; email: string };
  validePar?: { firstName: string; lastName: string };
  createdAt: string;
  commentaire?: string;
}

const LEAVE_TYPES = [
  { key: 'MALADIE', label: 'Maladie', icon: '🤒', color: 'bg-red-100 text-red-700' },
  { key: 'CONGE', label: 'Congé', icon: '🏖️', color: 'bg-blue-100 text-blue-700' },
  { key: 'MISSION', label: 'Mission', icon: '✈️', color: 'bg-purple-100 text-purple-700' },
  { key: 'PERSONNEL', label: 'Personnel', icon: '📋', color: 'bg-amber-100 text-amber-700' },
  { key: 'AUTRE', label: 'Autre', icon: '📝', color: 'bg-gray-100 text-gray-700' },
];

const STATUS_CONFIG = {
  EN_ATTENTE: { label: 'En attente', icon: Clock, color: 'text-amber-500', bg: 'bg-amber-100' },
  APPROUVE: { label: 'Approuvé', icon: CheckCircle2, color: 'text-green-500', bg: 'bg-green-100' },
  REFUSE: { label: 'Refusé', icon: XCircle, color: 'text-red-500', bg: 'bg-red-100' },
  ANNULE: { label: 'Annulé', icon: XCircle, color: 'text-gray-400', bg: 'bg-gray-100' },
};

export default function LeaveRequestsPage() {
  const { t } = useI18n();
  const [requests, setRequests] = useState<LeaveRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [filterStatus, setFilterStatus] = useState('');
  const [newRequest, setNewRequest] = useState({
    type: 'MALADIE',
    dateDebut: '',
    dateFin: '',
    motif: '',
  });

  useEffect(() => { loadRequests(); }, []);

  const loadRequests = async () => {
    try {
      setLoading(true);
      const params = filterStatus ? `?statut=${filterStatus}` : '';
      const res = await api.get(`/leave-requests${params}`);
      setRequests(res.data.content || res.data || []);
    } catch {
      setRequests([]);
    } finally {
      setLoading(false);
    }
  };

  const createRequest = async () => {
    if (!newRequest.dateDebut || !newRequest.dateFin || !newRequest.motif.trim()) {
      Toast.warning('Veuillez remplir tous les champs');
      return;
    }
    try {
      await api.post('/leave-requests', newRequest);
      Toast.success('Demande créée avec succès');
      setShowCreate(false);
      setNewRequest({ type: 'MALADIE', dateDebut: '', dateFin: '', motif: '' });
      loadRequests();
    } catch {
      Toast.error('Erreur lors de la création');
    }
  };

  const handleAction = async (id: string, action: 'approve' | 'reject') => {
    try {
      await api.patch(`/leave-requests/${id}/${action}`);
      Toast.success(action === 'approve' ? 'Demande approuvée' : 'Demande refusée');
      loadRequests();
    } catch {
      Toast.error('Erreur lors de l\'action');
    }
  };

  const cancelRequest = async (id: string) => {
    try {
      await api.patch(`/leave-requests/${id}/cancel`);
      Toast.success('Demande annulée');
      loadRequests();
    } catch {
      Toast.error('Erreur lors de l\'annulation');
    }
  };

  const getDaysCount = (start: string, end: string) => {
    const s = new Date(start);
    const e = new Date(end);
    return Math.ceil((e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24)) + 1;
  };

  const pendingCount = requests.filter(r => r.statut === 'EN_ATTENTE').length;
  const approvedCount = requests.filter(r => r.statut === 'APPROUVE').length;

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <CalendarOff className="w-8 h-8 text-orange-500" />
            Demandes d'absence
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Gérez les congés, absences et missions de vos membres
          </p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-orange-500 to-red-500 text-white text-sm font-medium hover:from-orange-600 hover:to-red-600 transition-all shadow-lg shadow-orange-500/25 flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          Nouvelle demande
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="p-4 rounded-xl bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20">
          <div className="flex items-center gap-2 mb-1">
            <Clock className="w-4 h-4 text-amber-600" />
            <span className="text-xs font-medium text-amber-700 dark:text-amber-400">En attente</span>
          </div>
          <div className="text-2xl font-bold text-amber-900 dark:text-amber-300">{pendingCount}</div>
        </div>
        <div className="p-4 rounded-xl bg-green-50 dark:bg-green-500/10 border border-green-200 dark:border-green-500/20">
          <div className="flex items-center gap-2 mb-1">
            <CheckCircle2 className="w-4 h-4 text-green-600" />
            <span className="text-xs font-medium text-green-700 dark:text-green-400">Approuvées</span>
          </div>
          <div className="text-2xl font-bold text-green-900 dark:text-green-300">{approvedCount}</div>
        </div>
        <div className="p-4 rounded-xl bg-gray-50 dark:bg-white/5 border border-gray-200 dark:border-white/10">
          <div className="flex items-center gap-2 mb-1">
            <CalendarOff className="w-4 h-4 text-gray-600" />
            <span className="text-xs font-medium text-gray-700 dark:text-gray-400">Total</span>
          </div>
          <div className="text-2xl font-bold text-gray-900 dark:text-white">{requests.length}</div>
        </div>
      </div>

      {/* Filter */}
      <div className="flex gap-2 mb-6 flex-wrap">
        {['', 'EN_ATTENTE', 'APPROUVE', 'REFUSE'].map(status => (
          <button
            key={status}
            onClick={() => { setFilterStatus(status); }}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
              filterStatus === status
                ? 'bg-orange-600 text-white'
                : 'bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10'
            }`}
          >
            {status ? STATUS_CONFIG[status as keyof typeof STATUS_CONFIG]?.label : 'Toutes'}
          </button>
        ))}
      </div>

      {loading ? (
        <SkeletonLoader lines={5} variant="table" />
      ) : requests.length === 0 ? (
        <EmptyState
          icon={<CalendarOff className="w-8 h-8 text-gray-400" />}
          title="Aucune demande d'absence"
          message="Aucune demande n'a été soumise pour le moment"
          action={{ label: 'Créer une demande', onClick: () => setShowCreate(true) }}
        />
      ) : (
        <div className="space-y-3">
          {requests.map(req => {
            const typeInfo = LEAVE_TYPES.find(lt => lt.key === req.type) || LEAVE_TYPES[4];
            const statusInfo = STATUS_CONFIG[req.statut];
            const StatusIcon = statusInfo.icon;
            return (
              <div key={req.id} className="p-4 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-start gap-3">
                    <span className="text-2xl">{typeInfo.icon}</span>
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-sm font-semibold text-gray-900 dark:text-white">
                          {typeInfo.label}
                        </span>
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${typeInfo.color}`}>
                          {typeInfo.key}
                        </span>
                      </div>
                      <p className="text-sm text-gray-600 dark:text-gray-400 mb-1">
                        {req.demandeur.firstName} {req.demandeur.lastName}
                      </p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        {new Date(req.dateDebut).toLocaleDateString('fr-FR')} → {new Date(req.dateFin).toLocaleDateString('fr-FR')}
                        ({getDaysCount(req.dateDebut, req.dateFin)} jour(s))
                      </p>
                      {req.motif && (
                        <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 italic">"{req.motif}"</p>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <span className={`flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${statusInfo.bg} ${statusInfo.color}`}>
                      <StatusIcon className="w-3 h-3" />
                      {statusInfo.label}
                    </span>
                    {req.statut === 'EN_ATTENTE' && (
                      <div className="flex gap-1">
                        <button
                          onClick={() => handleAction(req.id, 'approve')}
                          className="px-2 py-1 rounded-lg bg-green-100 text-green-700 text-xs font-medium hover:bg-green-200 transition-all"
                        >
                          ✓
                        </button>
                        <button
                          onClick={() => handleAction(req.id, 'reject')}
                          className="px-2 py-1 rounded-lg bg-red-100 text-red-700 text-xs font-medium hover:bg-red-200 transition-all"
                        >
                          ✕
                        </button>
                      </div>
                    )}
                    {req.statut === 'EN_ATTENTE' && (
                      <button
                        onClick={() => cancelRequest(req.id)}
                        className="px-2 py-1 rounded-lg bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-400 text-xs hover:bg-gray-200 dark:hover:bg-white/10 transition-all"
                      >
                        Annuler
                      </button>
                    )}
                  </div>
                </div>
                {req.validePar && (
                  <p className="text-xs text-gray-400 mt-2">
                    Validé par {req.validePar.firstName} {req.validePar.lastName}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Create Modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouvelle demande d'absence</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Type d'absence</label>
                <div className="grid grid-cols-5 gap-2">
                  {LEAVE_TYPES.map(lt => (
                    <button
                      key={lt.key}
                      onClick={() => setNewRequest({ ...newRequest, type: lt.key })}
                      className={`p-2 rounded-xl border text-center transition-all ${
                        newRequest.type === lt.key
                          ? 'border-orange-500 bg-orange-50 dark:bg-orange-500/10'
                          : 'border-gray-200 dark:border-white/10 hover:border-gray-300'
                      }`}
                    >
                      <span className="text-lg">{lt.icon}</span>
                      <div className="text-xs mt-1 text-gray-700 dark:text-gray-300">{lt.label}</div>
                    </button>
                  ))}
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Date de début *</label>
                  <input
                    type="date"
                    value={newRequest.dateDebut}
                    onChange={e => setNewRequest({ ...newRequest, dateDebut: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Date de fin *</label>
                  <input
                    type="date"
                    value={newRequest.dateFin}
                    onChange={e => setNewRequest({ ...newRequest, dateFin: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Motif *</label>
                <textarea
                  value={newRequest.motif}
                  onChange={e => setNewRequest({ ...newRequest, motif: e.target.value })}
                  rows={3}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-orange-500 resize-none"
                  placeholder="Expliquez la raison de votre absence..."
                />
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 text-sm hover:bg-gray-50 dark:hover:bg-white/5 transition-all">
                Annuler
              </button>
              <button onClick={createRequest} className="px-4 py-2 rounded-xl bg-orange-600 text-white text-sm font-medium hover:bg-orange-700 transition-all">
                Soumettre
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
