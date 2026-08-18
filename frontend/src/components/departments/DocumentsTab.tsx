import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  BookOpen, Plus, Archive, CheckCircle2, Trash2, Loader2,
} from 'lucide-react';
import { DOC_TYPES, DOC_TYPE_LABELS, DOC_TYPE_BADGES } from './types';

type DeptDocument = {
  id: string; titre: string; type: string; description?: string;
  url?: string; statut: string; createdAt?: string;
};

export function DocumentsTab({ deptId, onChanged }: { deptId: string; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [titre, setTitre] = useState('');
  const [type, setType] = useState('DOCUMENT');
  const [description, setDescription] = useState('');
  const [url, setUrl] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  const { data: documents = [], isLoading } = useQuery({
    queryKey: ['department', deptId, 'documents'],
    queryFn: async () => (await api.get(`/departments/${deptId}/documents`)).data as DeptDocument[],
    enabled: !!deptId,
  });
  const { data: stats = {} } = useQuery({
    queryKey: ['department', deptId, 'documents', 'stats'],
    queryFn: async () => (await api.get(`/departments/${deptId}/documents/stats`)).data as any,
    enabled: !!deptId,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'documents'] });
    onChanged();
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${deptId}/documents`, {
        titre: titre.trim(), type, description: description || null, url: url || null,
      });
    },
    onSuccess: () => {
      toast.success('Document ajouté ✅');
      setTitre(''); setDescription(''); setUrl(''); setType('DOCUMENT'); setShowCreate(false);
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const archiveMutation = useMutation({
    mutationFn: async ({ doc, statut }: { doc: DeptDocument; statut: string }) => {
      await api.put(`/departments/${deptId}/documents/${doc.id}`, {
        titre: doc.titre, type: doc.type, description: doc.description || null,
        url: doc.url || null, statut,
      });
    },
    onSuccess: () => { toast.success('Statut mis à jour'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (docId: string) => api.delete(`/departments/${deptId}/documents/${docId}`),
    onSuccess: () => { toast.success('Document supprimé'); invalidate(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) {
    return <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>;
  }

  return (
    <div className="space-y-4">
      {/* KPIs par type */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="stat-card p-3 text-center">
          <span className="stat-label text-[10px]">Total</span>
          <p className="stat-value text-xl">{stats.total ?? documents.length}</p>
        </div>
        {DOC_TYPES.map((t) => (
          <div key={t} className="stat-card p-3 text-center">
            <span className={`badge text-[9px] ${DOC_TYPE_BADGES[t]}`}>{DOC_TYPE_LABELS[t]}</span>
            <p className="stat-value text-lg mt-1">{stats[t] ?? 0}</p>
          </div>
        ))}
      </div>

      <div className="glass-card p-5">
        <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
          <div className="flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Bibliothèque du département</h3>
          </div>
          <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm cursor-pointer">
            <Plus className="w-4 h-4" /> {showCreate ? 'Fermer' : 'Ajouter un document'}
          </button>
        </div>

        {showCreate && (
          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              <div>
                <label className="label" htmlFor="doc-titre">Titre *</label>
                <input id="doc-titre" className="input" value={titre} onChange={(e) => setTitre(e.target.value)}
                  placeholder="Ex : Procédure d'accueil, Guide son…" />
              </div>
              <div>
                <label className="label" htmlFor="doc-type">Type</label>
                <select id="doc-type" className="input" value={type} onChange={(e) => setType(e.target.value)}>
                  {DOC_TYPES.map((t) => <option key={t} value={t}>{DOC_TYPE_LABELS[t]}</option>)}
                </select>
              </div>
              <div className="sm:col-span-2">
                <label className="label" htmlFor="doc-url">Lien du document (optionnel)</label>
                <input id="doc-url" className="input" value={url} onChange={(e) => setUrl(e.target.value)}
                  placeholder="https://… (Drive, PDF, formulaire…) " />
              </div>
              <div className="sm:col-span-2 lg:col-span-4">
                <label className="label" htmlFor="doc-desc">Description</label>
                <input id="doc-desc" className="input" value={description} onChange={(e) => setDescription(e.target.value)}
                  placeholder="À quoi sert ce document, quand l'utiliser…" />
              </div>
            </div>
            <button onClick={() => createMutation.mutate()} disabled={!titre.trim() || createMutation.isPending}
              className="btn-primary btn-sm mt-3 cursor-pointer">
              <Plus className="w-4 h-4" /> Ajouter
            </button>
          </div>
        )}

        {documents.length === 0 ? (
          <div className="text-center py-8">
            <BookOpen className="w-10 h-10 text-gray-300 mx-auto mb-2" />
            <p className="text-sm text-gray-400">Aucun document — centralisez vos procédures, guides et formulaires ici.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {documents.map((d) => (
              <div key={d.id} className={`flex items-start gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 ${
                d.statut === 'ARCHIVE' ? 'opacity-50' : ''
              }`}>
                <div className={`p-2 rounded-lg shrink-0 ${DOC_TYPE_BADGES[d.type] ? 'bg-gray-100 dark:bg-gray-700' : ''}`}>
                  <BookOpen className="w-4 h-4 text-gray-500" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{d.titre}</span>
                    <span className={`badge text-[9px] ${DOC_TYPE_BADGES[d.type] || 'badge-gray'}`}>{DOC_TYPE_LABELS[d.type] || d.type}</span>
                    {d.statut === 'ARCHIVE' && <span className="badge text-[9px] badge-inactive">Archivé</span>}
                  </div>
                  {d.description && <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{d.description}</p>}
                  <div className="flex items-center gap-3 mt-1">
                    {d.url && (
                      <a href={d.url} target="_blank" rel="noreferrer"
                        className="text-[11px] font-medium text-primary-600 hover:underline inline-flex items-center gap-1">
                        Ouvrir le document ↗
                      </a>
                    )}
                    <span className="text-[10px] text-gray-400">
                      {d.createdAt ? new Date(d.createdAt).toLocaleDateString('fr-FR') : ''}
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  {d.statut === 'ACTIF' ? (
                    <button onClick={() => archiveMutation.mutate({ doc: d, statut: 'ARCHIVE' })}
                      className="p-1.5 rounded-lg text-gray-400 hover:text-amber-600 hover:bg-amber-500/10 transition-all cursor-pointer" title="Archiver">
                      <Archive className="w-3.5 h-3.5" />
                    </button>
                  ) : (
                    <button onClick={() => archiveMutation.mutate({ doc: d, statut: 'ACTIF' })}
                      className="p-1.5 rounded-lg text-gray-400 hover:text-emerald-600 hover:bg-emerald-500/10 transition-all cursor-pointer" title="Restaurer">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                  <button onClick={() => { if (confirm(`Supprimer le document « ${d.titre} » ?`)) deleteMutation.mutate(d.id); }}
                    className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-500/10 transition-all cursor-pointer" title="Supprimer">
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ============================================================
// PARAMÈTRES — seuils configurables des alertes intelligentes
// ============================================================

