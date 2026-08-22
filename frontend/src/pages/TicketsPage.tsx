import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { LifeBuoy, Plus, Search, Filter, Clock, CheckCircle2, AlertCircle, MessageSquare } from 'lucide-react';

interface Ticket {
  id: string;
  titre: string;
  description: string;
  categorie: string;
  priorite: 'BASSE' | 'MOYENNE' | 'HAUTE' | 'CRITIQUE';
  statut: 'OUVERT' | 'EN_COURS' | 'RESOLU' | 'FERME';
  creePar: { id: string; firstName: string; lastName: string; email: string };
  assigneA?: { id: string; firstName: string; lastName: string };
  createdAt: string;
  updatedAt: string;
  messages: TicketMessage[];
}

interface TicketMessage {
  id: string;
  contenu: string;
  auteur: { firstName: string; lastName: string };
  createdAt: string;
}

const CATEGORIES = [
  { key: 'TECHNIQUE', label: 'Technique', color: 'blue' },
  { key: 'COMPTE', label: 'Compte', color: 'purple' },
  { key: 'FEATURE', label: 'Fonctionnalité', color: 'green' },
  { key: 'BUG', label: 'Bug', color: 'red' },
  { key: 'QUESTION', label: 'Question', color: 'amber' },
  { key: 'AUTRE', label: 'Autre', color: 'gray' },
];

const PRIORITES = [
  { key: 'BASSE', label: 'Basse', color: 'bg-gray-100 text-gray-700' },
  { key: 'MOYENNE', label: 'Moyenne', color: 'bg-blue-100 text-blue-700' },
  { key: 'HAUTE', label: 'Haute', color: 'bg-orange-100 text-orange-700' },
  { key: 'CRITIQUE', label: 'Critique', color: 'bg-red-100 text-red-700' },
];

const STATUTS = [
  { key: 'OUVERT', label: 'Ouvert', icon: AlertCircle, color: 'text-blue-500' },
  { key: 'EN_COURS', label: 'En cours', icon: Clock, color: 'text-amber-500' },
  { key: 'RESOLU', label: 'Résolu', icon: CheckCircle2, color: 'text-green-500' },
  { key: 'FERME', label: 'Fermé', icon: CheckCircle2, color: 'text-gray-400' },
];

export default function TicketsPage() {
  const { t } = useI18n();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [filterCategory, setFilterCategory] = useState<string>('');
  const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [newMessage, setNewMessage] = useState('');
  const [newTicket, setNewTicket] = useState({ titre: '', description: '', categorie: 'QUESTION', priorite: 'MOYENNE' });

  useEffect(() => { loadTickets(); }, []);

  const loadTickets = async () => {
    try {
      setLoading(true);
      const params = new URLSearchParams();
      if (filterStatus) params.append('statut', filterStatus);
      if (filterCategory) params.append('categorie', filterCategory);
      const res = await api.get(`/tickets?${params.toString()}`);
      setTickets(res.data.content || res.data || []);
    } catch {
      Toast.error('Erreur lors du chargement des tickets');
    } finally {
      setLoading(false);
    }
  };

  const createTicket = async () => {
    if (!newTicket.titre.trim() || !newTicket.description.trim()) {
      Toast.warning('Veuillez remplir tous les champs');
      return;
    }
    try {
      await api.post('/tickets', newTicket);
      Toast.success('Ticket créé avec succès');
      setShowCreate(false);
      setNewTicket({ titre: '', description: '', categorie: 'QUESTION', priorite: 'MOYENNE' });
      loadTickets();
    } catch {
      Toast.error('Erreur lors de la création du ticket');
    }
  };

  const updateStatus = async (ticketId: string, newStatus: string) => {
    try {
      await api.patch(`/tickets/${ticketId}/status`, { statut: newStatus });
      Toast.success('Statut mis à jour');
      loadTickets();
      if (selectedTicket?.id === ticketId) {
        setSelectedTicket({ ...selectedTicket!, statut: newStatus as Ticket['statut'] });
      }
    } catch {
      Toast.error('Erreur lors de la mise à jour');
    }
  };

  const sendMessage = async () => {
    if (!newMessage.trim() || !selectedTicket) return;
    try {
      await api.post(`/tickets/${selectedTicket.id}/messages`, { contenu: newMessage });
      setNewMessage('');
      Toast.success('Message envoyé');
      loadTickets();
    } catch {
      Toast.error("Erreur lors de l'envoi du message");
    }
  };

  const filtered = tickets.filter(ticket => {
    const matchesSearch = !search ||
      ticket.titre.toLowerCase().includes(search.toLowerCase()) ||
      ticket.description.toLowerCase().includes(search.toLowerCase());
    return matchesSearch;
  });

  const getStatusInfo = (statut: string) => STATUTS.find(s => s.key === statut) || STATUTS[0];
  const getPrioriteInfo = (p: string) => PRIORITES.find(pr => pr.key === p) || PRIORITES[0];
  const getCategorieInfo = (c: string) => CATEGORIES.find(cat => cat.key === c) || CATEGORIES[5];

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <LifeBuoy className="w-8 h-8 text-blue-500" />
            Tickets & Support
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Système de tickets interne — créez et suivez vos demandes
          </p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-sm font-medium hover:from-blue-700 hover:to-indigo-700 transition-all shadow-lg shadow-blue-500/25 flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          Nouveau ticket
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Rechercher un ticket..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <select
          value={filterStatus}
          onChange={e => { setFilterStatus(e.target.value); }}
          className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Tous les statuts</option>
          {STATUTS.map(s => <option key={s.key} value={s.key}>{s.label}</option>)}
        </select>
        <select
          value={filterCategory}
          onChange={e => { setFilterCategory(e.target.value); }}
          className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Toutes les catégories</option>
          {CATEGORIES.map(c => <option key={c.key} value={c.key}>{c.label}</option>)}
        </select>
      </div>

      <div className="flex gap-6">
        {/* Ticket List */}
        <div className={`${selectedTicket ? 'hidden md:block md:w-1/2' : 'w-full'} space-y-3`}>
          {loading ? (
            <SkeletonLoader lines={5} variant="table" />
          ) : filtered.length === 0 ? (
            <EmptyState
              icon={<LifeBuoy className="w-8 h-8 text-gray-400" />}
              title="Aucun ticket"
              message="Créez votre premier ticket pour obtenir de l'aide"
              action={{ label: 'Nouveau ticket', onClick: () => setShowCreate(true) }}
            />
          ) : (
            filtered.map(ticket => {
              const statusInfo = getStatusInfo(ticket.statut);
              const StatusIcon = statusInfo.icon;
              return (
                <button
                  key={ticket.id}
                  onClick={() => setSelectedTicket(ticket)}
                  className={`w-full text-left p-4 rounded-xl border transition-all ${
                    selectedTicket?.id === ticket.id
                      ? 'border-blue-500 bg-blue-50 dark:bg-blue-500/10'
                      : 'border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 hover:border-gray-300 dark:hover:border-white/20'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <StatusIcon className={`w-4 h-4 ${statusInfo.color} shrink-0`} />
                        <h3 className="text-sm font-semibold text-gray-900 dark:text-white truncate">
                          {ticket.titre}
                        </h3>
                      </div>
                      <p className="text-xs text-gray-500 dark:text-gray-400 line-clamp-2 mb-2">
                        {ticket.description}
                      </p>
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${getPrioriteInfo(ticket.priorite).color}`}>
                          {getPrioriteInfo(ticket.priorite).label}
                        </span>
                        <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-400">
                          {getCategorieInfo(ticket.categorie).label}
                        </span>
                        {ticket.messages?.length > 0 && (
                          <span className="flex items-center gap-1 text-xs text-gray-400">
                            <MessageSquare className="w-3 h-3" />
                            {ticket.messages.length}
                          </span>
                        )}
                      </div>
                    </div>
                    <span className="text-xs text-gray-400 whitespace-nowrap">
                      {new Date(ticket.createdAt).toLocaleDateString('fr-FR')}
                    </span>
                  </div>
                </button>
              );
            })
          )}
        </div>

        {/* Ticket Detail */}
        {selectedTicket && (
          <div className="w-full md:w-1/2">
            <div className="bg-white dark:bg-white/5 rounded-2xl border border-gray-200 dark:border-white/10 p-6 sticky top-4">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-gray-900 dark:text-white">{selectedTicket.titre}</h2>
                <button
                  onClick={() => setSelectedTicket(null)}
                  className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 text-sm"
                >
                  Fermer
                </button>
              </div>

              <p className="text-gray-600 dark:text-gray-400 text-sm mb-4">{selectedTicket.description}</p>

              <div className="flex items-center gap-2 mb-4 flex-wrap">
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${getPrioriteInfo(selectedTicket.priorite).color}`}>
                  {getPrioriteInfo(selectedTicket.priorite).label}
                </span>
                <span className="px-2 py-1 rounded-full text-xs font-medium bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-400">
                  {getCategorieInfo(selectedTicket.categorie).label}
                </span>
              </div>

              {/* Status actions */}
              <div className="flex gap-2 mb-4 flex-wrap">
                {STATUTS.map(s => (
                  <button
                    key={s.key}
                    onClick={() => updateStatus(selectedTicket.id, s.key)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                      selectedTicket.statut === s.key
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10'
                    }`}
                  >
                    {s.label}
                  </button>
                ))}
              </div>

              {/* Messages */}
              <div className="border-t border-gray-200 dark:border-white/10 pt-4 mb-4 max-h-64 overflow-y-auto space-y-3">
                {selectedTicket.messages?.length === 0 ? (
                  <p className="text-sm text-gray-400 text-center py-4">Aucun message</p>
                ) : (
                  selectedTicket.messages?.map(msg => (
                    <div key={msg.id} className="bg-gray-50 dark:bg-white/5 rounded-lg p-3">
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-xs font-medium text-gray-700 dark:text-gray-300">
                          {msg.auteur.firstName} {msg.auteur.lastName}
                        </span>
                        <span className="text-xs text-gray-400">
                          {new Date(msg.createdAt).toLocaleDateString('fr-FR')}
                        </span>
                      </div>
                      <p className="text-sm text-gray-600 dark:text-gray-400">{msg.contenu}</p>
                    </div>
                  ))
                )}
              </div>

              {/* New message */}
              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="Ajouter un message..."
                  value={newMessage}
                  onChange={e => setNewMessage(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && sendMessage()}
                  className="flex-1 px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <button
                  onClick={sendMessage}
                  className="px-4 py-2 rounded-xl bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 transition-all"
                >
                  Envoyer
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Create Modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouveau ticket</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Titre</label>
                <input
                  type="text"
                  value={newTicket.titre}
                  onChange={e => setNewTicket({ ...newTicket, titre: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Résumé du problème"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description</label>
                <textarea
                  value={newTicket.description}
                  onChange={e => setNewTicket({ ...newTicket, description: e.target.value })}
                  rows={4}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                  placeholder="Décrivez votre demande en détail..."
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Catégorie</label>
                  <select
                    value={newTicket.categorie}
                    onChange={e => setNewTicket({ ...newTicket, categorie: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    {CATEGORIES.map(c => <option key={c.key} value={c.key}>{c.label}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Priorité</label>
                  <select
                    value={newTicket.priorite}
                    onChange={e => setNewTicket({ ...newTicket, priorite: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    {PRIORITES.map(p => <option key={p.key} value={p.key}>{p.label}</option>)}
                  </select>
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={() => setShowCreate(false)}
                className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-white/5 transition-all text-sm"
              >
                Annuler
              </button>
              <button
                onClick={createTicket}
                className="px-4 py-2 rounded-xl bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 transition-all"
              >
                Créer le ticket
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
