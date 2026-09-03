import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { QrCode, Loader2, CheckCircle, XCircle, Camera } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';

interface CheckinResult {
  success: boolean;
  message: string;
  soulId: string;
}

export default function QrCheckinPage() {
  const [soulId, setSoulId] = useState('');
  const [lastResult, setLastResult] = useState<CheckinResult | null>(null);

  const checkinMutation = useMutation({
    mutationFn: async () => {
      if (!soulId.trim()) { toast('Entrez un ID âme', { icon: '⚠️' }); throw new Error('empty'); }
      return (await api.post<CheckinResult>('/members/qr-checkin', { soulId: soulId.trim() })).data;
    },
    onSuccess: (data) => {
      setLastResult(data);
      toast.success(data.message || 'Présence enregistrée');
      setSoulId('');
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  return (
    <div className="page-container max-w-2xl mx-auto">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-emerald-500 to-green-600 text-white shadow-lg">
          <QrCode className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Pointage QR Code</h1>
          <p className="page-subtitle">Scannez ou saisissez l'identifiant pour enregistrer la présence</p>
        </div>
      </div>

      <div className="bg-white dark:bg-white/5 rounded-2xl p-8 border border-gray-200 dark:border-white/10 text-center mb-6">
        <div className="w-20 h-20 rounded-3xl bg-gradient-to-br from-emerald-500 to-green-600 text-white flex items-center justify-center mx-auto mb-6 shadow-2xl">
          <QrCode className="w-10 h-10" />
        </div>
        <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-2">Pointage par QR Code</h2>
        <p className="text-sm text-gray-500 mb-6">Saisissez l'ID de l'âme ou scannez le code QR</p>

        <div className="flex gap-3 max-w-md mx-auto">
          <input type="text" value={soulId} onChange={e => setSoulId(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && checkinMutation.mutate()}
            className="flex-1 px-4 py-3 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
            placeholder="ID de l'âme" />
          <button onClick={() => checkinMutation.mutate()} disabled={checkinMutation.isPending}
            className="px-6 py-3 rounded-xl bg-emerald-500 text-white text-sm font-medium hover:bg-emerald-600 transition-all flex items-center gap-2 disabled:opacity-50">
            {checkinMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
            Pointer
          </button>
        </div>
      </div>

      {lastResult && (
        <div className={`rounded-2xl p-6 border text-center animate-slide-up ${lastResult.success ? 'bg-green-50 dark:bg-green-500/10 border-green-200 dark:border-green-500/30' : 'bg-red-50 dark:bg-red-500/10 border-red-200 dark:border-red-500/30'}`}>
          {lastResult.success ? (
            <CheckCircle className="w-12 h-12 text-green-500 mx-auto mb-3" />
          ) : (
            <XCircle className="w-12 h-12 text-red-500 mx-auto mb-3" />
          )}
          <h3 className={`font-bold text-lg mb-1 ${lastResult.success ? 'text-green-700 dark:text-green-400' : 'text-red-700 dark:text-red-400'}`}>
            {lastResult.success ? 'Présence enregistrée' : 'Échec du pointage'}
          </h3>
          <p className="text-sm text-gray-600 dark:text-gray-400">{lastResult.message}</p>
        </div>
      )}

      <div className="glass-card p-6 mt-6">
        <h3 className="font-semibold text-gray-900 dark:text-white mb-3 flex items-center gap-2">
          <Camera className="w-4 h-4" /> Comment ça marche
        </h3>
        <ul className="text-sm text-gray-500 dark:text-gray-400 space-y-2">
          <li className="flex items-start gap-2"><span className="text-emerald-500 mt-1">1.</span> Le responsable scanne le QR code du membre</li>
          <li className="flex items-start gap-2"><span className="text-emerald-500 mt-1">2.</span> Ou saisit manuellement l'identifiant de l'âme</li>
          <li className="flex items-start gap-2"><span className="text-emerald-500 mt-1">3.</span> La présence est enregistrée automatiquement</li>
        </ul>
      </div>
    </div>
  );
}
