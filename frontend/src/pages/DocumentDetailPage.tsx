import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import api from '@/lib/api';
import {
  FileText, Download, Calendar, User, Tag, ArrowLeft, Loader2,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface Document {
  id: string;
  title: string;
  description?: string;
  content?: string;
  fileType: string;
  fileSize?: number;
  uploadedBy?: string;
  uploadedByName?: string;
  uploadedAt: string;
  category?: string;
  tags?: string[];
}

export default function DocumentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: document, isLoading } = useQuery({
    queryKey: ['documents', id],
    queryFn: async () => {
      const res = await api.get(`/documents/${id}`);
      return res.data as Document;
    },
    enabled: !!id,
  });

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;
  if (!document) return <div className="p-6 text-center text-gray-400">Document introuvable.</div>;

  const formatSize = (bytes?: number) => {
    if (!bytes) return '';
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  };

  return (
    <div className="page-container max-w-3xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <button onClick={() => navigate(-1)} className="btn-ghost btn-sm mb-2 -ml-2">
            <ArrowLeft className="w-4 h-4" /> Retour
          </button>
          <div className="flex items-center gap-2 mb-1">
            <FileText className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">{document.title}</h1>
          </div>
          {document.description && <p className="page-subtitle">{document.description}</p>}
        </div>
      </div>

      <div className="space-y-4 animate-slide-up">
        {/* Meta card */}
        <div className="glass-card p-6">
          <div className="space-y-3">
            {[
              { icon: Tag, label: 'Type', value: document.fileType },
              { icon: Tag, label: 'Catégorie', value: document.category || '—' },
              { icon: Download, label: 'Taille', value: formatSize(document.fileSize) },
              { icon: User, label: 'Ajouté par', value: document.uploadedByName || document.uploadedBy || '—' },
              { icon: Calendar, label: 'Ajouté le', value: new Date(document.uploadedAt).toLocaleString('fr-FR') },
            ].filter((item) => item.value && item.value !== '—').map((item) => (
              <div key={item.label} className="flex items-center gap-3 py-2 border-b border-gray-100 dark:border-gray-800 last:border-0">
                <item.icon className="w-4 h-4 text-gray-400 shrink-0" />
                <span className="text-xs text-gray-400 min-w-[100px]">{item.label}</span>
                <span className="text-sm text-gray-700 dark:text-gray-300">{item.value}</span>
              </div>
            ))}
          </div>

          {/* Tags */}
          {document.tags && document.tags.length > 0 && (
            <div className="mt-4 pt-4 border-t border-gray-200/60 dark:border-gray-700/60">
              <div className="flex flex-wrap gap-1.5">
                {document.tags.map((tag) => (
                  <span key={tag} className="badge text-[10px]">{tag}</span>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Content */}
        {document.content && (
          <div className="glass-card p-6">
            <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3">Contenu</h3>
            <div className="text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap leading-relaxed">
              {document.content}
            </div>
          </div>
        )}

        {/* Download action */}
        <div className="flex gap-3">
          <button className="btn-primary btn-sm">
            <Download className="w-4 h-4" /> Télécharger
          </button>
        </div>
      </div>
    </div>
  );
}
