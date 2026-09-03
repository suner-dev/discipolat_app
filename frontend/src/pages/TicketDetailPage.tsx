import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import api from '@/lib/api';
import {
  LifeBuoy, Clock, CheckCircle2, AlertCircle, MessageSquare,
  ArrowLeft, Loader2, User,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface Ticket {
  id: string;
  titre: string;
  description: string;
  categorie: string;
  priorite: string;
  statut: string;
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

export default function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: ticket, isLoading } = useQuery({
    queryKey: ['tickets', id],
    queryFn: async () => {
      const res = await api.get(`/tickets/${id}`);
      return res.data as Ticket;
    },
    enabled: !!id,
  });

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;
  if (!ticket) return <div className="p-6 text-center text-gray-400">Ticket introuvable.</div>;

  const statusColor = (s: string) => {
    switch (s) {
      case 'OUVERT': return 'bg-blue-100 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400';
      case 'EN_COURS': return 'bg-amber-100 dark:bg-amber-900/20 text-amber-700 dark:text-amber-400';
      case 'RESOLU': return 'bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-400';
      case 'FERME': return 'bg-gray-100 dark:bg-gray-900/20 text-gray-700 dark:text-gray-400';
      default: return 'bg-gray-100 dark:bg-gray-900/20 text-gray-700 dark:text-gray-400';
    }
  };

  const prioriteColor = (p: string) => {
    switch (p) {
      case 'CRITIQUE': return 'bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-400';
      case 'HAUTE': return 'bg-orange-100 dark:bg-orange-900/20 text-orange-700 dark:text-orange-400';
      case 'MOYENNE': return 'bg-blue-100 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400';
      case 'BASSE': return 'bg-gray-100 dark:bg-gray-900/20 text-gray-700 dark:text-gray-400';
      default: return 'bg-gray-100 dark:bg-gray-900/20 text-gray-700 dark:text-gray-400';
    }
  };

  return (
    <div className="page-container max-w-3xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <button onClick={() => navigate(-1)} className="btn-ghost btn-sm mb-2 -ml-2">
            <ArrowLeft className="w-4 h-4" /> Retour
          </button>
          <div className="flex items-center gap-2 mb-1">
            <LifeBuoy className="w-5 h-5 text-blue-500" />
            <h1 className="page-title">{ticket.titre}</h1>
          </div>
          <p className="page-subtitle">Détail du ticket de support</p>
        </div>
      </div>

      <div className="space-y-4 animate-slide-up">
        {/* Status & priority */}
        <div className="flex flex-wrap items-center gap-2">
          <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${statusColor(ticket.statut)}`}>{ticket.statut}</span>
          <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${prioriteColor(ticket.priorite)}`}>{ticket.priorite}</span>
          <span className="badge text-[10px]">{ticket.categorie}</span>
        </div>

        {/* Description */}
        <div className="glass-card p-6">
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-2">Description</h3>
          <p className="text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap">{ticket.description}</p>
        </div>

        {/* Info */}
        <div className="glass-card p-6">
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3">Informations</h3>
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <User className="w-4 h-4 text-gray-400 shrink-0" />
              <span className="text-xs text-gray-400 min-w-[100px]">Créé par</span>
              <span className="text-sm text-gray-700 dark:text-gray-300">
                {ticket.creePar.firstName} {ticket.creePar.lastName} ({ticket.creePar.email})
              </span>
            </div>
            {ticket.assigneA && (
              <div className="flex items-center gap-3">
                <User className="w-4 h-4 text-gray-400 shrink-0" />
                <span className="text-xs text-gray-400 min-w-[100px]">Assigné à</span>
                <span className="text-sm text-gray-700 dark:text-gray-300">
                  {ticket.assigneA.firstName} {ticket.assigneA.lastName}
                </span>
              </div>
            )}
            <div className="flex items-center gap-3">
              <Clock className="w-4 h-4 text-gray-400 shrink-0" />
              <span className="text-xs text-gray-400 min-w-[100px]">Créé le</span>
              <span className="text-sm text-gray-700 dark:text-gray-300">
                {new Date(ticket.createdAt).toLocaleString('fr-FR')}
              </span>
            </div>
            <div className="flex items-center gap-3">
              <Clock className="w-4 h-4 text-gray-400 shrink-0" />
              <span className="text-xs text-gray-400 min-w-[100px]">Mis à jour</span>
              <span className="text-sm text-gray-700 dark:text-gray-300">
                {new Date(ticket.updatedAt).toLocaleString('fr-FR')}
              </span>
            </div>
          </div>
        </div>

        {/* Messages */}
        <div className="glass-card p-6">
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
            <MessageSquare className="w-4 h-4 text-blue-500" />
            Messages ({ticket.messages?.length || 0})
          </h3>
          {!ticket.messages || ticket.messages.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-4">Aucun message</p>
          ) : (
            <div className="space-y-3">
              {ticket.messages.map((msg) => (
                <div key={msg.id} className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs font-semibold text-gray-700 dark:text-gray-300">
                      {msg.auteur.firstName} {msg.auteur.lastName}
                    </span>
                    <span className="text-[10px] text-gray-400">
                      {new Date(msg.createdAt).toLocaleString('fr-FR')}
                    </span>
                  </div>
                  <p className="text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap">{msg.contenu}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
