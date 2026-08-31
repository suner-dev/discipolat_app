import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import api from '@/lib/api';
import {
  ShieldCheck, ShieldX, Clock, Building2, User, Loader2, CalendarDays,
  CheckCircle2, AlertTriangle, HelpCircle
} from 'lucide-react';

/* ====================================================================
 * VÉRIFICATION PUBLIQUE D'UN PASSEPORT SPIRITUEL
 * Route publique : /verify/passport/:code
 * Aucune authentification requise — endpoint GET /api/v1/public/passports/{code}
 * Exposure minimale : verdict + titulaire + émetteur + dates + nb d'entrées.
 * ================================================================== */

interface VerificationResult {
  status: string;        // VALID | REVOKED | EXPIRED | INVALID | NOT_FOUND
  holderName: string | null;
  issuedBy: string | null;
  issuedAt: string | null;
  expiresAt: string | null;
  entryCount: number;
  signatureValid: boolean;
}

function formatDate(value: string | null): string {
  if (!value) return '—';
  return new Date(value).toLocaleDateString();
}

function VerdictBanner({ result }: { result: VerificationResult }) {
  const config: Record<string, { color: string; icon: React.ReactNode; title: string; message: string }> = {
    VALID: {
      color: 'border-emerald-500/50 bg-emerald-500/10 text-emerald-300',
      icon: <ShieldCheck className="w-10 h-10 text-emerald-400" />,
      title: 'Passeport valide',
      message: 'Ce passeport est authentique, actif et signé cryptographiquement par la plateforme.',
    },
    REVOKED: {
      color: 'border-red-500/50 bg-red-500/10 text-red-300',
      icon: <ShieldX className="w-10 h-10 text-red-400" />,
      title: 'Passeport révoqué',
      message: 'Ce passeport a été révoqué par l\'organisation émettrice.',
    },
    EXPIRED: {
      color: 'border-amber-500/50 bg-amber-500/10 text-amber-300',
      icon: <Clock className="w-10 h-10 text-amber-400" />,
      title: 'Passeport expiré',
      message: 'La période de validité de ce passeport est dépassée.',
    },
    INVALID: {
      color: 'border-red-500/50 bg-red-500/10 text-red-300',
      icon: <AlertTriangle className="w-10 h-10 text-red-400" />,
      title: 'Signature invalide',
      message: 'Le contenu de ce passeport ne correspond plus à sa signature. Falsification suspectée.',
    },
    NOT_FOUND: {
      color: 'border-gray-500/50 bg-gray-500/10 text-gray-300',
      icon: <HelpCircle className="w-10 h-10 text-gray-400" />,
      title: 'Passeport introuvable',
      message: 'Aucun passeport ne correspond à ce code.',
    },
  };

  const c = config[result.status] ?? config.NOT_FOUND;
  return (
    <div className={`w-full max-w-xl rounded-2xl border p-6 text-center ${c.color}`}>
      <div className="flex justify-center mb-3">{c.icon}</div>
      <h1 className="text-2xl font-bold mb-2">{c.title}</h1>
      <p className="opacity-80 text-sm">{c.message}</p>
    </div>
  );
}
export default function PassportVerifyPage() {
  const { code } = useParams<{ code: string }>();

  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ['passport-verify', code],
    queryFn: async () => {
      const res = await api.get<VerificationResult>(`/public/passports/${code}`);
      return res.data;
    },
  });

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-emerald-950 text-white flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-xl mb-6 text-center">
        <div className="inline-flex items-center gap-2 bg-emerald-500/10 border border-emerald-500/30 rounded-full px-4 py-1.5 text-sm text-emerald-300 mb-6">
          <ShieldCheck className="w-4 h-4" /> Vérification officielle Discipolat
        </div>

        {isPending && (
          <div className="flex flex-col items-center gap-3 text-white/60">
            <Loader2 className="w-8 h-8 animate-spin" />
            <p>Vérification en cours…</p>
          </div>
        )}

        {isError && (
          <div className="w-full max-w-xl rounded-2xl border border-red-500/50 bg-red-500/10 text-red-300 p-6 text-center">
            <p>Impossible de contacter le service de vérification.</p>
            <button
              onClick={() => refetch()}
              className="mt-3 px-4 py-2 rounded-lg bg-white/10 hover:bg-white/20 text-white text-sm"
            >
              Réessayer
            </button>
          </div>
        )}

        {data && (
          <div className="space-y-6">
            <VerdictBanner result={data} />

            <div className="w-full max-w-xl rounded-2xl bg-white/5 border border-white/10 p-6 space-y-4">
              <h2 className="text-lg font-semibold text-white/90">Détails du passeport</h2>
              <div className="grid gap-3 sm:grid-cols-2 text-sm">
                <div className="flex items-center gap-2 text-white/70">
                  <User className="w-4 h-4 text-emerald-400" />
                  <span>Titulaire :</span>
                  <span className="text-white font-medium">{data.holderName ?? '—'}</span>
                </div>
                <div className="flex items-center gap-2 text-white/70">
                  <Building2 className="w-4 h-4 text-emerald-400" />
                  <span>Émetteur :</span>
                  <span className="text-white font-medium">{data.issuedBy ?? '—'}</span>
                </div>
                <div className="flex items-center gap-2 text-white/70">
                  <CalendarDays className="w-4 h-4 text-emerald-400" />
                  <span>Émis le :</span>
                  <span className="text-white font-medium">{formatDate(data.issuedAt)}</span>
                </div>
                <div className="flex items-center gap-2 text-white/70">
                  <Clock className="w-4 h-4 text-emerald-400" />
                  <span>Expire le :</span>
                  <span className="text-white font-medium">{formatDate(data.expiresAt)}</span>
                </div>
              </div>
              <div className="pt-3 border-t border-white/10 flex items-center justify-between text-sm text-white/60">
                <span>Entrées d'historique :</span>
                <span className="text-white font-semibold">{data.entryCount}</span>
              </div>
              {data.status !== 'NOT_FOUND' && (
                <div className="flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-xs bg-white/5">
                  {data.signatureValid ? (
                    <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  ) : (
                    <AlertTriangle className="w-4 h-4 text-red-400" />
                  )}
                  <span className={data.signatureValid ? 'text-emerald-300' : 'text-red-300'}>
                    {data.signatureValid
                      ? 'Signature cryptographique vérifiée'
                      : 'Signature cryptographique non vérifiée'}
                  </span>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}