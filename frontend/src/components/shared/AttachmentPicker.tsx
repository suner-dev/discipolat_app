import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { FileEntity } from '@/types';
import { Paperclip, X, FileText, Plus, Loader2 } from 'lucide-react';

interface AttachmentPickerProps {
  /** IDs des fichiers sélectionnés (contrôlé par le parent). */
  value: string[];
  /** Appelé avec la nouvelle liste complète à chaque changement. */
  onChange: (ids: string[]) => void;
}

const FILES_QUERY_KEY = ['attachment-picker-files'];

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const CATEGORIE_FALLBACK: Record<string, string> = {
  COMPTE_RENDU: 'Compte rendu',
  FORMATION: 'Formation',
  PHOTO: 'Photo',
  RESOURCES: 'Ressources',
  AUTRE: 'Autre',
};

/**
 * Sélecteur multi de pièces jointes (module Fichiers — références, pas d'upload
 * binaire) avec création directe d'un document. Réutilisé par tous les formulaires :
 * transferts, rapports, demandes membres, événements.
 */
export default function AttachmentPicker({ value, onChange }: AttachmentPickerProps) {
  const dictionaries = useDictionaries();
  const [open, setOpen] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [creating, setCreating] = useState(false);
  const [nom, setNom] = useState('');
  const [chemin, setChemin] = useState('');
  const [typeFichier, setTypeFichier] = useState('application/pdf');
  const [taille, setTaille] = useState('');
  const [categorie, setCategorie] = useState('AUTRE');
  const queryClient = useQueryClient();

  const { data: files } = useQuery({
    queryKey: FILES_QUERY_KEY,
    queryFn: async () => (await api.get('/files?size=100')).data.content as FileEntity[],
  });

  const toggle = (id: string, checked: boolean) => {
    onChange(checked ? [...value, id] : value.filter(x => x !== id));
  };

  const closeCreate = () => {
    setShowCreate(false);
    setNom('');
    setChemin('');
    setTypeFichier('application/pdf');
    setTaille('');
    setCategorie('AUTRE');
  };

  const handleCreate = async () => {
    if (!nom.trim() || !chemin.trim()) {
      toast.error("Le nom et l'URL du document sont requis");
      return;
    }
    setCreating(true);
    try {
      const res = await api.post('/files', {
        nom: nom.trim(),
        typeFichier,
        taille: parseInt(taille) || 0,
        chemin: chemin.trim(),
        categorie,
      });
      const id = (res.data as FileEntity).id;
      // Ajoute le document créé à la sélection courante.
      onChange(value.includes(id) ? value : [...value, id]);
      queryClient.invalidateQueries({ queryKey: FILES_QUERY_KEY });
      toast.success('Document créé et ajouté aux pièces jointes');
      closeCreate();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setCreating(false);
    }
  };

  return (
    <div>
      {value.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-3">
          {files?.filter(f => value.includes(f.id)).map(f => (
            <span key={f.id} className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300 text-xs border border-emerald-200/60 dark:border-emerald-700/40">
              <FileText className="w-3.5 h-3.5" />
              <span className="max-w-[160px] truncate">{f.nom}</span>
              <button
                type="button"
                onClick={() => onChange(value.filter(id => id !== f.id))}
                className="p-0.5 rounded hover:bg-emerald-100 dark:hover:bg-emerald-800/50 transition-colors"
                aria-label={`Retirer ${f.nom}`}
              >
                <X className="w-3 h-3" />
              </button>
            </span>
          ))}
        </div>
      )}

      {open ? (
        <div className="border border-white/20 dark:border-white/[0.06] rounded-xl p-3 bg-white/40 dark:bg-gray-900/30 animate-slide-up">
          {/* Formulaire de création directe */}
          {showCreate && (
            <div className="mb-3 p-3 rounded-xl bg-white/60 dark:bg-gray-800/50 border border-emerald-200/60 dark:border-emerald-700/30 space-y-2.5 animate-slide-up">
              <p className="text-sm font-medium text-gray-800 dark:text-gray-200 flex items-center gap-2">
                <Plus className="w-4 h-4 text-emerald-500" /> Nouveau document
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                <input
                  className="input text-sm"
                  placeholder="Nom du document *"
                  value={nom}
                  onChange={(e) => setNom(e.target.value)}
                />
                <input
                  className="input text-sm"
                  placeholder="URL / chemin du fichier *"
                  value={chemin}
                  onChange={(e) => setChemin(e.target.value)}
                />
                <input
                  className="input text-sm"
                  placeholder="Type MIME (ex: application/pdf)"
                  value={typeFichier}
                  onChange={(e) => setTypeFichier(e.target.value)}
                />
                <input
                  className="input text-sm"
                  placeholder="Taille (octets)"
                  type="number"
                  min={0}
                  value={taille}
                  onChange={(e) => setTaille(e.target.value)}
                />
                <select className="input text-sm" value={categorie} onChange={(e) => setCategorie(e.target.value)}>
                  {(dictionaries.options('DOCUMENT_CATEGORIE').length > 0
                    ? dictionaries.options('DOCUMENT_CATEGORIE')
                    : Object.entries(CATEGORIE_FALLBACK).map(([code, label]) => ({ code, label }))
                  ).map((o) => (<option key={o.code} value={o.code}>{o.label}</option>))}
                </select>
              </div>
              <div className="flex justify-end gap-2 pt-1">
                <button type="button" onClick={closeCreate} className="btn-secondary btn-sm">Annuler</button>
                <button type="button" onClick={handleCreate} disabled={creating} className="btn-primary btn-sm">
                  {creating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
                  Créer
                </button>
              </div>
            </div>
          )}

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">Documents du module Fichiers</p>
            {!showCreate && (
              <button
                type="button"
                onClick={() => setShowCreate(true)}
                className="btn-ghost btn-sm text-emerald-600 hover:text-emerald-700"
              >
                <Plus className="w-4 h-4" /> Créer un document
              </button>
            )}
          </div>

          {(files ?? []).length === 0 ? (
            <p className="text-sm text-gray-500">Aucun document disponible. Créez-en un ci-dessus.</p>
          ) : (
            <ul className="space-y-1 max-h-56 overflow-y-auto">
              {files!.map(f => {
                const checked = value.includes(f.id);
                return (
                  <li key={f.id}>
                    <label className={`flex items-center gap-2.5 px-2 py-1.5 rounded-lg cursor-pointer transition-colors ${checked ? 'bg-emerald-50 dark:bg-emerald-900/30' : 'hover:bg-gray-50 dark:hover:bg-gray-800/50'}`}>
                      <input
                        type="checkbox"
                        className="rounded accent-emerald-600"
                        checked={checked}
                        onChange={(e) => toggle(f.id, e.target.checked)}
                      />
                      <FileText className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                      <span className="text-sm text-gray-700 dark:text-gray-300 truncate">{f.nom}</span>
                      {f.taille > 0 && <span className="ml-auto text-xs text-gray-400 shrink-0">{(f.taille / 1024).toFixed(0)} Ko</span>}
                    </label>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      ) : (
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setOpen(true)}
            className="btn-secondary btn-sm"
          >
            <Paperclip className="w-4 h-4" />
            {value.length > 0 ? 'Modifier les documents' : 'Joindre des documents'}
          </button>
          <button
            type="button"
            onClick={() => { setOpen(true); setShowCreate(true); }}
            className="btn-ghost btn-sm text-emerald-600 hover:text-emerald-700"
          >
            <Plus className="w-4 h-4" /> Créer un document
          </button>
        </div>
      )}
    </div>
  );
}
