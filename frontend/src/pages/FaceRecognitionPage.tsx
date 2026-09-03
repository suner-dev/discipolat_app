import { useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ScanFace, Upload, Trash2, Loader2, BarChart3, Users, Shield } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface FaceTemplate {
  id: string;
  userId: string;
  soulId?: string;
  displayName: string;
  active: boolean;
  createdAt: string;
}

interface FaceStats {
  total: number;
  active: number;
  inactive: number;
  lastEnrollment: string;
}

export default function FaceRecognitionPage() {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'templates' | 'enroll' | 'identify'>('templates');
  const [enrollForm, setEnrollForm] = useState({ userId: '', soulId: '', displayName: '', imageBase64: '' });
  const [identifyImage, setIdentifyImage] = useState('');
  const [identifyResult, setIdentifyResult] = useState<any>(null);
  const [threshold, setThreshold] = useState('0.6');
  const enrollInputRef = useRef<HTMLInputElement>(null);
  const identifyInputRef = useRef<HTMLInputElement>(null);

  const { data: templates = [], isLoading } = useQuery({
    queryKey: ['face-templates'],
    queryFn: async () => (await api.get<FaceTemplate[]>('/face/templates')).data,
  });

  const { data: stats } = useQuery({
    queryKey: ['face-stats'],
    queryFn: async () => (await api.get<FaceStats>('/face/stats')).data,
  });

  const readFileAsBase64 = (file: File): Promise<string> =>
    new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.readAsDataURL(file);
    });

  const enrollMutation = useMutation({
    mutationFn: async () => {
      if (!enrollForm.imageBase64 || !enrollForm.displayName) { toast('Remplissez tous les champs', { icon: '⚠️' }); throw new Error('empty'); }
      return api.post('/face/enroll', {
        userId: enrollForm.userId || undefined,
        soulId: enrollForm.soulId || undefined,
        displayName: enrollForm.displayName,
        imageBase64: enrollForm.imageBase64,
      });
    },
    onSuccess: () => {
      toast.success('Visage enrôlé');
      setEnrollForm({ userId: '', soulId: '', displayName: '', imageBase64: '' });
      queryClient.invalidateQueries({ queryKey: ['face-templates'] });
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  const identifyMutation = useMutation({
    mutationFn: async () => {
      if (!identifyImage) { toast('Sélectionnez une image', { icon: '⚠️' }); throw new Error('empty'); }
      return (await api.post('/face/identify-configurable', { imageBase64: identifyImage, minConfidence: parseFloat(threshold) })).data;
    },
    onSuccess: (data) => {
      setIdentifyResult(data);
      toast.success(data.matched ? 'Identifié !' : 'Non identifié');
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/face/templates/${id}`),
    onSuccess: () => { toast.success('Gabarit supprimé'); queryClient.invalidateQueries({ queryKey: ['face-templates'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const handleFile = async (e: React.ChangeEvent<HTMLInputElement>, setter: (v: string) => void) => {
    const file = e.target.files?.[0];
    if (file) setter(await readFileAsBase64(file));
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-rose-500 to-pink-600 text-white shadow-lg">
          <ScanFace className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Reconnaissance Faciale</h1>
          <p className="page-subtitle">Enrôlement et identification des membres</p>
        </div>
        <div className="ml-auto flex rounded-xl bg-white/5 border border-white/10 p-1">
          {(['templates', 'enroll', 'identify'] as const).map(t => (
            <button key={t} onClick={() => setTab(t)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${tab === t ? 'bg-rose-500 text-white' : 'text-gray-400 hover:text-white'}`}>
              {t === 'templates' ? 'Gabarits' : t === 'enroll' ? 'Enrôler' : 'Identifier'}
            </button>
          ))}
        </div>
      </div>

      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-gray-900 dark:text-white">{stats.total}</div>
            <div className="text-xs text-gray-500">Total gabarits</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-green-600">{stats.active}</div>
            <div className="text-xs text-gray-500">Actifs</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-red-500">{stats.inactive}</div>
            <div className="text-xs text-gray-500">Inactifs</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-sm font-bold text-gray-900 dark:text-white">{stats.lastEnrollment ? new Date(stats.lastEnrollment).toLocaleDateString('fr-FR') : '—'}</div>
            <div className="text-xs text-gray-500">Dernier enrôlement</div>
          </div>
        </div>
      )}

      {tab === 'templates' && (
        isLoading ? <SkeletonLoader lines={4} variant="card" /> :
          templates.length === 0 ? (
            <EmptyState icon={<ScanFace className="w-8 h-8 text-gray-400" />}
              title="Aucun gabarit"
              message="Enrôlez des visages pour activer la reconnaissance" />
          ) : (
            <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
              {templates.map(t => (
                <div key={t.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 flex items-center justify-between">
                  <div>
                    <div className="font-medium text-gray-900 dark:text-white text-sm">{t.displayName}</div>
                    <div className="text-xs text-gray-500">
                      {t.active ? <span className="text-green-600">Actif</span> : <span className="text-red-500">Inactif</span>}
                      {t.soulId && <span className="ml-2">· Âme: {t.soulId.slice(0, 8)}...</span>}
                    </div>
                  </div>
                  <button onClick={() => deleteMutation.mutate(t.id)} className="text-red-400 hover:text-red-600 p-1">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          )
      )}

      {tab === 'enroll' && (
        <div className="bg-white dark:bg-white/5 rounded-xl p-6 border border-gray-200 dark:border-white/10 max-w-lg">
          <h3 className="font-semibold text-gray-900 dark:text-white mb-4">Enrôler un visage</h3>
          <div className="space-y-4">
            <input type="text" value={enrollForm.displayName} onChange={e => setEnrollForm({ ...enrollForm, displayName: e.target.value })}
              className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
              placeholder="Nom d'affichage" />
            <div className="flex gap-3">
              <input type="text" value={enrollForm.userId} onChange={e => setEnrollForm({ ...enrollForm, userId: e.target.value })}
                className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="User ID (optionnel)" />
              <input type="text" value={enrollForm.soulId} onChange={e => setEnrollForm({ ...enrollForm, soulId: e.target.value })}
                className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="Soul ID (optionnel)" />
            </div>
            <div>
              <input ref={enrollInputRef} type="file" accept="image/*" className="hidden"
                onChange={e => handleFile(e, v => setEnrollForm({ ...enrollForm, imageBase64: v }))} />
              <button onClick={() => enrollInputRef.current?.click()}
                className="w-full px-4 py-8 rounded-xl border-2 border-dashed border-gray-200 dark:border-white/10 text-sm text-gray-400 hover:border-rose-300 hover:text-rose-500 transition flex flex-col items-center gap-2">
                <Upload className="w-6 h-6" />
                {enrollForm.imageBase64 ? 'Image sélectionnée' : 'Cliquez pour choisir une photo'}
              </button>
            </div>
            <button onClick={() => enrollMutation.mutate()} disabled={enrollMutation.isPending}
              className="w-full px-4 py-2.5 rounded-xl bg-rose-500 text-white text-sm font-medium hover:bg-rose-600 flex items-center justify-center gap-2">
              {enrollMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
              Enrôler
            </button>
          </div>
        </div>
      )}

      {tab === 'identify' && (
        <div className="bg-white dark:bg-white/5 rounded-xl p-6 border border-gray-200 dark:border-white/10 max-w-lg">
          <h3 className="font-semibold text-gray-900 dark:text-white mb-4">Identifier un visage</h3>
          <div className="space-y-4">
            <input ref={identifyInputRef} type="file" accept="image/*" className="hidden"
              onChange={e => handleFile(e, setIdentifyImage)} />
            <button onClick={() => identifyInputRef.current?.click()}
              className="w-full px-4 py-8 rounded-xl border-2 border-dashed border-gray-200 dark:border-white/10 text-sm text-gray-400 hover:border-rose-300 hover:text-rose-500 transition flex flex-col items-center gap-2">
              <Upload className="w-6 h-6" />
              {identifyImage ? 'Image sélectionnée' : 'Cliquez pour choisir une photo'}
            </button>
            <div>
              <label className="block text-xs text-gray-500 mb-1">Seuil de confiance minimum</label>
              <input type="number" step="0.05" min="0" max="1" value={threshold} onChange={e => setThreshold(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
            </div>
            <button onClick={() => identifyMutation.mutate()} disabled={identifyMutation.isPending}
              className="w-full px-4 py-2.5 rounded-xl bg-rose-500 text-white text-sm font-medium hover:bg-rose-600 flex items-center justify-center gap-2">
              {identifyMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
              Identifier
            </button>
          </div>
          {identifyResult && (
            <div className={`mt-4 p-4 rounded-xl ${identifyResult.matched ? 'bg-green-50 dark:bg-green-500/10' : 'bg-red-50 dark:bg-red-500/10'}`}>
              <div className="text-sm font-medium mb-1">{identifyResult.matched ? 'Identifié !' : 'Non identifié'}</div>
              <div className="text-xs text-gray-500">Confiance: {(identifyResult.confidence * 100).toFixed(1)}%</div>
              {identifyResult.displayName && <div className="text-xs text-gray-500 mt-1">{identifyResult.displayName}</div>}
              {identifyResult.message && <div className="text-xs text-gray-400 mt-1">{identifyResult.message}</div>}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
