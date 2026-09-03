import { useState, useRef } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Mic, Upload, Loader2, CheckCircle, Activity, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';

interface HealthStatus {
  status: string;
  sttProvider: string;
  available: boolean;
  timestamp: string;
}

interface TranscribeResult {
  transcription?: string;
  reply?: string;
  intent?: string;
  error?: string;
}

export default function VoicesPage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [language, setLanguage] = useState('fr');
  const [transcribeResult, setTranscribeResult] = useState<TranscribeResult | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { data: health, isLoading: healthLoading } = useQuery({
    queryKey: ['voice-health'],
    queryFn: async () => (await api.get<HealthStatus>('/voice/health')).data,
  });

  const { data: sttStatus } = useQuery({
    queryKey: ['voice-stt-status'],
    queryFn: async () => (await api.get('/voice/stt-status')).data,
  });

  const transcribeMutation = useMutation({
    mutationFn: async () => {
      if (!selectedFile) { toast('Sélectionnez un fichier audio', { icon: '⚠️' }); throw new Error('empty'); }
      const formData = new FormData();
      formData.append('file', selectedFile);
      formData.append('language', language);
      return (await api.post('/voice/transcribe', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })).data as TranscribeResult;
    },
    onSuccess: (data) => {
      setTranscribeResult(data);
      toast.success('Transcription terminée');
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-sound-500 to-indigo-600 text-white shadow-lg">
          <Mic className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Voice Assistant</h1>
          <p className="page-subtitle">Transcription et santé du système vocal</p>
        </div>
      </div>

      {/* Health status */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        <div className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
          <h3 className="font-semibold text-gray-900 dark:text-white mb-3 flex items-center gap-2">
            <Activity className="w-4 h-4" /> État du service
          </h3>
          {healthLoading ? (
            <Loader2 className="w-5 h-5 animate-spin text-gray-400" />
          ) : health ? (
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">Statut</span>
                <span className={`font-medium ${health.status === 'UP' ? 'text-green-600' : 'text-red-500'}`}>
                  {health.status === 'UP' ? 'Opérationnel' : 'Indisponible'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Fournisseur STT</span>
                <span className="font-medium text-gray-900 dark:text-white">{health.sttProvider || 'N/A'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Disponible</span>
                {health.available ? <CheckCircle className="w-4 h-4 text-green-500" /> : <AlertCircle className="w-4 h-4 text-red-500" />}
              </div>
            </div>
          ) : (
            <p className="text-sm text-gray-400">Impossible de vérifier le statut</p>
          )}
        </div>

        <div className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
          <h3 className="font-semibold text-gray-900 dark:text-white mb-3 flex items-center gap-2">
            <Mic className="w-4 h-4" /> Configuration STT
          </h3>
          {sttStatus ? (
            <pre className="text-xs text-gray-600 dark:text-gray-300 bg-black/5 dark:bg-white/5 rounded-lg p-3 overflow-auto max-h-32">
              {JSON.stringify(sttStatus, null, 2)}
            </pre>
          ) : (
            <p className="text-sm text-gray-400">Chargement...</p>
          )}
        </div>
      </div>

      {/* Transcription */}
      <div className="bg-white dark:bg-white/5 rounded-xl p-6 border border-gray-200 dark:border-white/10">
        <h3 className="font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
          <Upload className="w-4 h-4" /> Transcrire un fichier audio
        </h3>
        <div className="flex flex-col gap-4">
          <div className="flex gap-3 items-center">
            <input ref={fileInputRef} type="file" accept="audio/*" onChange={e => setSelectedFile(e.target.files?.[0] || null)}
              className="hidden" />
            <button onClick={() => fileInputRef.current?.click()}
              className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-sm hover:bg-white/10 flex items-center gap-2">
              <Mic className="w-4 h-4" />
              {selectedFile ? selectedFile.name : 'Choisir un fichier audio'}
            </button>
            <select value={language} onChange={e => setLanguage(e.target.value)}
              className="px-3 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
              <option value="fr">Français</option>
              <option value="en">English</option>
              <option value="es">Español</option>
            </select>
            <button onClick={() => transcribeMutation.mutate()} disabled={transcribeMutation.isPending || !selectedFile}
              className="px-4 py-2 rounded-xl bg-indigo-500 text-white text-sm font-medium hover:bg-indigo-600 flex items-center gap-2 disabled:opacity-50">
              {transcribeMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Mic className="w-4 h-4" />}
              Transcrire
            </button>
          </div>

          {transcribeResult && (
            <div className="bg-gray-50 dark:bg-white/5 rounded-xl p-4 space-y-2">
              {transcribeResult.transcription && (
                <div><span className="text-xs text-gray-500 font-medium">Transcription:</span>
                  <p className="text-sm text-gray-700 dark:text-gray-300">{transcribeResult.transcription}</p></div>
              )}
              {transcribeResult.intent && (
                <div><span className="text-xs text-gray-500 font-medium">Intent:</span>
                  <span className="text-sm text-indigo-600 dark:text-indigo-400 ml-1">{transcribeResult.intent}</span></div>
              )}
              {transcribeResult.reply && (
                <div><span className="text-xs text-gray-500 font-medium">Réponse:</span>
                  <p className="text-sm text-gray-700 dark:text-gray-300">{transcribeResult.reply}</p></div>
              )}
              {transcribeResult.error && (
                <div className="text-sm text-red-500">{transcribeResult.error}</div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
