import React, { useState, useEffect } from 'react';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { FolderOpen, Plus, FileText, Video, Link, Download } from 'lucide-react';

interface Resource {
  id: string;
  familleId: string;
  titre: string;
  description?: string;
  type: string;
  url?: string;
  téléchargements: number;
  createdAt: string;
}

const TYPES = [
  { key: 'DOCUMENT', icon: FileText, color: 'text-blue-500' },
  { key: 'VIDÉO', icon: Video, color: 'text-red-500' },
  { key: 'ÉTUDE_BIBLIQUE', icon: FileText, color: 'text-green-500' },
  { key: 'AUDIO', icon: FileText, color: 'text-purple-500' },
  { key: 'LIEN', icon: Link, color: 'text-indigo-500' },
];

export default function FamilyResourcesPage() {
  const [resources, setResources] = useState<Resource[]>([]);
  const [loading, setLoading] = useState(true);
  const [familleId, setFamilleId] = useState('');

  useEffect(() => { if (familleId) loadResources(); }, [familleId]);

  const loadResources = async () => {
    try { setLoading(true); const res = await api.get(`/family-resources/family/${familleId}`); setResources(res.data || []); }
    catch { setResources([]); } finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <FolderOpen className="w-8 h-8 text-amber-500" /> Banque de Ressources
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Partagez documents, vidéos et études au sein de la famille</p>
        </div>
      </div>

      <div className="mb-6">
        <input type="text" value={familleId} onChange={e => setFamilleId(e.target.value)}
          className="w-full max-w-md px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
          placeholder="Entrez l'ID de votre famille" />
      </div>

      {!familleId ? (
        <EmptyState icon={<FolderOpen className="w-8 h-8 text-gray-400" />}
          title="Entrez l'ID de votre famille" message="Pour accéder aux ressources partagées de votre famille" />
      ) : loading ? <SkeletonLoader lines={4} variant="card" /> :
        resources.length === 0 ? (
          <EmptyState icon={<FolderOpen className="w-8 h-8 text-gray-400" />}
            title="Aucune ressource" message="Partagez des documents avec votre famille" />
        ) : (
          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
            {resources.map(r => {
              const typeInfo = TYPES.find(t => t.key === r.type) || TYPES[0];
              const TypeIcon = typeInfo.icon;
              return (
                <div key={r.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10">
                  <div className="flex items-center gap-3 mb-2">
                    <TypeIcon className={`w-5 h-5 ${typeInfo.color}`} />
                    <h3 className="font-medium text-gray-900 dark:text-white text-sm">{r.titre}</h3>
                  </div>
                  {r.description && <p className="text-xs text-gray-500 mb-2 line-clamp-2">{r.description}</p>}
                  <div className="flex items-center justify-between text-xs text-gray-400">
                    <span>{new Date(r.createdAt).toLocaleDateString('fr-FR')}</span>
                    <span className="flex items-center gap-1"><Download className="w-3 h-3" />{r.téléchargements}</span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
    </div>
  );
}
