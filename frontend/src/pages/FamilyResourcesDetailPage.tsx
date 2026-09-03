import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import api from '@/lib/api';
import {
  FolderOpen, FileText, Download, Calendar, User, ArrowLeft, Loader2,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface FamilyResource {
  id: string;
  title: string;
  description?: string;
  resourceType: string;
  url?: string;
  fileSize?: number;
  mimeType?: string;
  uploadedBy?: string;
  uploadedAt: string;
  familyId: string;
  familyName?: string;
}

export default function FamilyResourcesDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: resource, isLoading } = useQuery({
    queryKey: ['family-resources', id],
    queryFn: async () => {
      const res = await api.get(`/family-resources/${id}`);
      return res.data as FamilyResource;
    },
    enabled: !!id,
  });

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;
  if (!resource) return <div className="p-6 text-center text-gray-400">Ressource introuvable.</div>;

  const formatSize = (bytes?: number) => {
    if (!bytes) return '';
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  };

  const typeIcon = (type: string) => {
    switch (type) {
      case 'DOCUMENT': return '📄';
      case 'IMAGE': return '🖼️';
      case 'VIDEO': return '🎬';
      case 'AUDIO': return '🎵';
      default: return '📁';
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
            <FolderOpen className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">{resource.title}</h1>
          </div>
          <p className="page-subtitle">Détail de la ressource familiale</p>
        </div>
      </div>

      <div className="glass-card p-6 animate-slide-up">
        <div className="flex items-start gap-4 mb-6">
          <div className="w-14 h-14 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center text-2xl shadow-sm shrink-0">
            {typeIcon(resource.resourceType)}
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="text-lg font-bold text-gray-900 dark:text-gray-100">{resource.title}</h3>
            {resource.description && (
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">{resource.description}</p>
            )}
          </div>
        </div>

        <div className="space-y-3 border-t border-gray-200/60 dark:border-gray-700/60 pt-4">
          {([
            { Icon: FileText, label: 'Type', value: resource.resourceType },
            { Icon: Calendar, label: 'Ajouté le', value: new Date(resource.uploadedAt).toLocaleString('fr-FR') },
            { Icon: User, label: 'Ajouté par', value: resource.uploadedBy || '—' },
            resource.fileSize ? { Icon: Download, label: 'Taille', value: formatSize(resource.fileSize) } : null,
            resource.mimeType ? { Icon: FileText, label: 'Format', value: resource.mimeType } : null,
          ] as { Icon: typeof FileText; label: string; value: string }[]).filter(Boolean).map((item) => (
            <div key={item.label} className="flex items-center gap-3 py-2 border-b border-gray-100 dark:border-gray-800 last:border-0">
              <item.Icon className="w-4 h-4 text-gray-400 shrink-0" />
              <span className="text-xs text-gray-400 min-w-[100px]">{item.label}</span>
              <span className="text-sm text-gray-700 dark:text-gray-300">{item.value}</span>
            </div>
          ))}
        </div>

        {resource.url && (
          <div className="mt-6">
            <a
              href={resource.url}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary btn-sm"
            >
              <Download className="w-4 h-4" /> Ouvrir le fichier
            </a>
          </div>
        )}
      </div>
    </div>
  );
}
