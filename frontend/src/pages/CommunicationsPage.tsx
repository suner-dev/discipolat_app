import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import { usePlatformConfig } from '@/contexts/PlatformContext';
import { useAuth } from '@/contexts/AuthContext';
import {
  Megaphone, Plus, Pencil, Trash2, Loader2, Send, X, Users, Globe, Home, Building2,
} from 'lucide-react';
import type {
  Communication, CommunicationCible, CreateCommunicationRequest, Family, Department,
} from '@/types';

const CIBLE_LABELS: Record<string, string> = {
  TOUS: 'Toute l’église',
  ROLE: 'Par rôle',
  FAMILLE: 'Par famille',
  DEPARTEMENT: 'Par département',
};

const CIBLE_ICONS: Record<string, typeof Globe> = {
  TOUS: Globe,
  ROLE: Users,
  FAMILLE: Home,
  DEPARTEMENT: Building2,
};

const ALL_ROLES = ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'];

export default function CommunicationsPage() {
  const { moduleEnabled } = usePlatformConfig();
  const { activeRole } = useAuth();
  const queryClient = useQueryClient();
  const canManage = activeRole === 'ADMIN' || activeRole === 'PASTEUR';
  const [modal, setModal] = useState<null | { edit?: Communication }>(null);

  if (!moduleEnabled('COMMUNICATION')) {
    return (
      <div className="page-container flex flex-col items-center justify-center min-h-[60vh] text-center">
        <Megaphone className="w-10 h-10 text-gray-300 mb-3" />
        <h1 className="text-lg font-semibold text-gray-800 dark:text-gray-100">Communication désactivée</h1>
        <p className="text-sm text-gray-400 mt-1">
          L'administrateur a désactivé ce module. Réactivez-le depuis l'espace d'administration.
        </p>
        <Link to="/dashboard" className="btn-ghost btn-sm mt-4">Retour au tableau de bord</Link>
      </div>
    );
  }

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['communications'] });

  const { data: published = [], isLoading } = useQuery({
    queryKey: ['communications', 'published'],
    queryFn: async () => (await api.get('/communications')).data as Communication[],
  });

  const { data: all = [] } = useQuery({
    queryKey: ['communications', 'admin'],
    queryFn: async () => (await api.get('/communications/admin')).data as Communication[],
    enabled: canManage,
  });

  const saveMutation = useMutation({
    mutationFn: async (payload: CreateCommunicationRequest) => {
      if (modal?.edit) await api.put(`/communications/admin/${modal.edit.id}`, payload);
      else await api.post('/communications/admin', payload);
    },
    onSuccess: () => { invalidate(); setModal(null); toast.success(modal?.edit ? 'Annonce modifiée' : 'Annonce créée'); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const publishMutation = useMutation({
    mutationFn: async (id: string) => (await api.post(`/communications/admin/${id}/publish`)).data as Communication,
    onSuccess: (c) => { invalidate(); toast.success(`Annonce publiée et diffusée à ${(c as Communication & { destinataires?: number }).destinataires ?? ''} destinataire(s)`); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/communications/admin/${id}`),
    onSuccess: () => { invalidate(); toast.success('Annonce supprimée'); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const cibleBadge = (c: Communication) => {
    const Icon = CIBLE_ICONS[c.cible] || Globe;
    const detail = c.cible === 'ROLE' ? ` : ${c.roles.join(', ')}` : '';
    return (
      <span className="badge badge-gray text-[10px]">
        <Icon className="w-3 h-3" /> {CIBLE_LABELS[c.cible] || c.cible}{detail}
      </span>
    );
  };

  return (
    <div className="page-container max-w-4xl">
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <Megaphone className="w-5 h-5 text-primary-500" /> Annonces
          </h1>
          <p className="page-subtitle">
            Annonces de l'église avec diffusion ciblée (tous, rôle, famille, département).
            La publication notifie les destinataires.
          </p>
        </div>
        {canManage && (
          <div className="page-header-actions">
            <button className="btn-primary btn-sm" onClick={() => setModal({})}>
              <Plus className="w-4 h-4" /> Nouvelle annonce
            </button>
          </div>
        )}
      </div>

      {/* Gestion (ADMIN / PASTEUR) */}
      {canManage && (
        <div className="glass-card p-5 mb-6">
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
            <Megaphone className="w-4 h-4 text-primary-500" /> Gestion des annonces
          </h3>
          {all.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-4">Aucune annonce. Créez la première ci-dessus.</p>
          ) : (
            <div className="space-y-2">
              {all.map((c) => (
                <div key={c.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{c.titre}</p>
                      <span className={`badge text-[9px] ${c.statut === 'PUBLIEE' ? 'badge-success' : c.statut === 'BROUILLON' ? 'badge-gray' : 'badge-error'}`}>
                        {c.statut === 'PUBLIEE' ? 'Publiée' : c.statut === 'BROUILLON' ? 'Brouillon' : 'Archivée'}
                      </span>
                      {cibleBadge(c)}
                    </div>
                    <p className="text-xs text-gray-400 mt-0.5 line-clamp-1">{c.contenu}</p>
                  </div>
                  <div className="flex items-center gap-1 flex-shrink-0">
                    {c.statut !== 'PUBLIEE' && (
                      <button className="p-1.5 rounded-lg text-gray-400 hover:text-emerald-500 hover:bg-emerald-500/10" title="Publier et diffuser" onClick={() => { if (confirm(`Publier « ${c.titre} » et notifier les destinataires ?`)) publishMutation.mutate(c.id); }}>
                        <Send className="w-4 h-4" />
                      </button>
                    )}
                    <button className="p-1.5 rounded-lg text-gray-400 hover:text-amber-500 hover:bg-amber-500/10" title="Modifier" onClick={() => setModal({ edit: c })}>
                      <Pencil className="w-4 h-4" />
                    </button>
                    <button className="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-500/10" title="Supprimer" onClick={() => { if (confirm(`Supprimer « ${c.titre} » ?`)) deleteMutation.mutate(c.id); }}>
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Annonces publiées pour l'utilisateur courant */}
      {isLoading ? (
        <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>
      ) : published.length === 0 ? (
        <div className="empty-state glass-card">
          <Megaphone className="empty-state-icon" />
          <p className="text-gray-500 dark:text-gray-400">Aucune annonce publiée pour vous pour le moment.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {published.map((c) => (
            <div key={c.id} className="glass-card p-5">
              <div className="flex items-center gap-2 flex-wrap mb-1.5">
                <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{c.titre}</h3>
                {cibleBadge(c)}
              </div>
              {c.datePublication && (
                <p className="text-[11px] text-gray-400 mb-2">{new Date(c.datePublication).toLocaleDateString('fr-FR')}</p>
              )}
              <p className="text-sm text-gray-600 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">{c.contenu}</p>
            </div>
          ))}
        </div>
      )}

      {modal && (
        <CommunicationModal
          edit={modal.edit}
          onClose={() => setModal(null)}
          onSave={(payload) => saveMutation.mutate(payload)}
          pending={saveMutation.isPending}
        />
      )}
    </div>
  );
}

function CommunicationModal({ edit, onClose, onSave, pending }: {
  edit?: Communication;
  onClose: () => void;
  onSave: (p: CreateCommunicationRequest) => void;
  pending: boolean;
}) {
  const [titre, setTitre] = useState(edit?.titre || '');
  const [contenu, setContenu] = useState(edit?.contenu || '');
  const [cible, setCible] = useState<CommunicationCible>(edit?.cible || 'TOUS');
  const [roles, setRoles] = useState<string[]>(edit?.roles || []);
  const [familleId, setFamilleId] = useState(edit?.familleId || '');
  const [departmentId, setDepartmentId] = useState(edit?.departmentId || '');

  const { data: families = [] } = useQuery({
    queryKey: ['families', 'options'],
    queryFn: async () => (await api.get('/families')).data as Family[],
    enabled: cible === 'FAMILLE',
  });
  const { data: departments = [] } = useQuery({
    queryKey: ['departments', 'options'],
    queryFn: async () => (await api.get('/departments')).data as Department[],
    enabled: cible === 'DEPARTEMENT',
  });

  const toggleRole = (r: string) => setRoles((prev) => prev.includes(r) ? prev.filter((x) => x !== r) : [...prev, r]);

  const submit = () => {
    if (!titre.trim() || !contenu.trim()) return;
    onSave({
      titre: titre.trim(),
      contenu: contenu.trim(),
      cible,
      roles: cible === 'ROLE' ? roles : undefined,
      familleId: cible === 'FAMILLE' && familleId ? familleId : undefined,
      departmentId: cible === 'DEPARTEMENT' && departmentId ? departmentId : undefined,
    });
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div className="glass-card p-5 w-full max-w-lg max-h-[85vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
            {edit ? 'Modifier l’annonce' : 'Nouvelle annonce'}
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg text-gray-400 hover:text-gray-600 cursor-pointer">
            <X className="w-4 h-4" />
          </button>
        </div>
        <div>
          <label className="label" htmlFor="comm-titre">Titre</label>
          <input id="comm-titre" className="input mb-3" value={titre} onChange={(e) => setTitre(e.target.value)} />
        </div>
        <div>
          <label className="label" htmlFor="comm-contenu">Contenu</label>
          <textarea id="comm-contenu" className="input mb-3 min-h-[110px]" value={contenu} onChange={(e) => setContenu(e.target.value)} />
        </div>
        <div>
          <label className="label" htmlFor="comm-cible">Cible de diffusion</label>
          <select id="comm-cible" className="input mb-3" value={cible} onChange={(e) => setCible(e.target.value as CommunicationCible)}>
            {Object.entries(CIBLE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </div>
        {cible === 'ROLE' && (
          <div className="mb-3">
            <label className="label">Rôles destinataires</label>
            <div className="flex flex-wrap gap-2">
              {ALL_ROLES.map((r) => (
                <button key={r} type="button" onClick={() => toggleRole(r)}
                  className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors border ${roles.includes(r) ? 'bg-primary-600 border-primary-600 text-white' : 'bg-gray-50 dark:bg-gray-800 border-gray-200 dark:border-gray-700 text-gray-500 dark:text-gray-400'}`}>
                  {r}
                </button>
              ))}
            </div>
          </div>
        )}
        {cible === 'FAMILLE' && (
          <div className="mb-3">
            <label className="label" htmlFor="comm-famille">Famille</label>
            <select id="comm-famille" className="input" value={familleId} onChange={(e) => setFamilleId(e.target.value)}>
              <option value="">— Choisir une famille —</option>
              {families.map((f) => <option key={f.id} value={f.id}>{f.nom}</option>)}
            </select>
          </div>
        )}
        {cible === 'DEPARTEMENT' && (
          <div className="mb-3">
            <label className="label" htmlFor="comm-departement">Département</label>
            <select id="comm-departement" className="input" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
              <option value="">— Choisir un département —</option>
              {departments.map((d) => <option key={d.id} value={d.id}>{d.nom}</option>)}
            </select>
          </div>
        )}
        <button onClick={submit} disabled={pending || !titre.trim() || !contenu.trim() || (cible === 'ROLE' && roles.length === 0)}
          className="btn-primary btn-sm mt-4 w-full justify-center cursor-pointer">
          {pending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
          {edit ? 'Enregistrer' : 'Créer'}
        </button>
      </div>
    </div>
  );
}
