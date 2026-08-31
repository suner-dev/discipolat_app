import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { QRCodeSVG } from 'qrcode.react';
import toast from 'react-hot-toast';
import {
  ShieldCheck, Loader2, Plus, Share2, RotateCcw, Ban, Baby, GraduationCap,
  HeartHandshake, BookOpen, Award, Sparkles, FileText, QrCode
} from 'lucide-react';

/* ====================================================================
 * PASSEPORT SPIRITUEL — gestion (page protégée)
 * Voir son passeport signé, son QR, ses entrées d'historique, et pour
 * les responsables : émettre / ajouter des entrées / révoquer.
 * ================================================================== */

interface PassportEntry {
  id: string;
  entryType: string;
  title: string;
  description?: string;
  occurredAt?: string;
  issuingOrganization?: string;
  verified: boolean;
}

interface Passport {
  id: string;
  passportCode: string;
  status: string;
  issuedAt: string | null;
  expiresAt: string | null;
  payloadHash: string;
  entries: PassportEntry[];
}

interface QrPayload {
  passportCode: string;
  verificationUrl: string;
  qrPngBase64: string;
}

const ENTRY_TYPES: { value: string; label: string }[] = [
  { value: 'BAPTISM', label: 'Baptême' },
  { value: 'FORMATION', label: 'Formation' },
  { value: 'CERTIFICATION', label: 'Certification' },
  { value: 'SERVICE', label: 'Service' },
  { value: 'RECOMMENDATION', label: 'Recommandation' },
  { value: 'DISCIPLESHIP_STEP', label: 'Étape de discipolat' },
  { value: 'OTHER', label: 'Autre' },
];

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  REVOKED: 'Révoqué',
  EXPIRED: 'Expiré',
};

function EntryTypeBadge({ type }: { type: string }) {
  const match = ENTRY_TYPES.find((t) => t.value === type);
  const label = match ? match.label : type;
  const colors: Record<string, string> = {
    BAPTISM: 'bg-blue-500/10 text-blue-300',
    FORMATION: 'bg-purple-500/10 text-purple-300',
    CERTIFICATION: 'bg-emerald-500/10 text-emerald-300',
    SERVICE: 'bg-amber-500/10 text-amber-300',
    RECOMMENDATION: 'bg-pink-500/10 text-pink-300',
    DISCIPLESHIP_STEP: 'bg-cyan-500/10 text-cyan-300',
  };
  return (
    <span className={`text-xs px-2 py-0.5 rounded-full ${colors[type] ?? 'bg-gray-500/10 text-gray-300'}`}>
      {label}
    </span>
  );
}

export default function PassportPage() {
  const { hasRole } = useAuth();
  const queryClient = useQueryClient();
  const [memberId, setMemberId] = useState('');
  const [showQr, setShowQr] = useState(false);
  const canManage = hasRole('ADMIN', 'PASTEUR', 'RESPONSABLE');

  const { data: passport, isPending, isError, refetch } = useQuery({
    queryKey: ['my-passport'],
    queryFn: async () => {
      const res = await api.get<Passport>('/passports/mine');
      return res.data;
    },
    retry: 0,
  });

  const { data: qr } = useQuery({
    queryKey: ['passport-qr', passport?.id],
    queryFn: async () => (await api.get<QrPayload>(`/passports/${passport!.id}/qr`)).data,
    enabled: !!passport && showQr,
  });

  const issueMutation = useMutation({
    mutationFn: async (memberIdToIssue: string) => {
      if (!memberIdToIssue.trim()) throw new Error('Identifiant du membre requis');
      return (await api.post(`/passports/member/${memberIdToIssue.trim()}`)).data as Passport;
    },
    onSuccess: () => {
      toast.success('Passeport émis avec succès');
      queryClient.invalidateQueries({ queryKey: ['my-passport'] });
    },
    onError: (err: unknown) => toast.error(err instanceof Error ? err.message : 'Émission impossible'),
  });

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white p-4 md:p-6 max-w-5xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <ShieldCheck className="w-6 h-6 text-emerald-500" /> Passeport spirituel
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Historique vérifiable et portable de votre parcours de discipolat.
          </p>
        </div>
        <div className="flex gap-2">
          {passport && (
            <button
              onClick={() => setShowQr((v) => !v)}
              className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-sm hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              <QrCode className="w-4 h-4" /> {showQr ? 'Masquer le QR' : 'Afficher le QR'}
            </button>
          )}
          <button
            onClick={() => refetch()}
            className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-sm hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <RotateCcw className="w-4 h-4" /> Actualiser
          </button>
        </div>
      </div>
  );
{isPending && (
        <div className="flex justify-center py-16 text-slate-500">
          <Loader2 className="w-8 h-8 animate-spin" />
        </div>
      )}

      {isError && (
        <div className="rounded-2xl border border-amber-500/40 bg-amber-500/10 p-6 space-y-4">
          <div className="flex items-start gap-3">
            <ShieldCheck className="w-8 h-8 text-amber-500 shrink-0" />
            <div>
              <h2 className="font-semibold text-lg">Aucun passeport émis</h2>
              <p className="text-sm text-slate-600 dark:text-slate-300 mt-1">
                Vous n'avez pas encore de passeport spirituel. Un responsable de votre église peut l'émettre et
                y ajouter votre histoire : baptême, formations, certifications, services et étapes de discipolat.
              </p>
            </div>
          </div>
          {canManage && (
            <div className="rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-4">
              <h3 className="font-semibold text-sm mb-2 flex items-center gap-2">
                <Plus className="w-4 h-4" /> Émettre un passeport pour un membre
              </h3>
              <div className="flex gap-2">
                <input
                  value={memberId}
                  onChange={(e) => setMemberId(e.target.value)}
                  placeholder="Identifiant du membre (UUID)"
                  className="flex-1 px-3 py-2 rounded-lg bg-slate-100 dark:bg-slate-950 border border-slate-200 dark:border-slate-700 text-sm"
                />
                <button
                  onClick={() => issueMutation.mutate(memberId)}
                  disabled={issueMutation.isPending}
                  className="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium inline-flex items-center gap-2 disabled:opacity-50"
                >
                  {issueMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Émettre
                </button>
              </div>
            </div>
          )}
        </div>
      )}
{passport && (
        <>
          {showQr && qr ? (
            <div className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-6 flex flex-col items-center gap-4">
              <QRCodeSVG value={qr.verificationUrl} size={180} />
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Partagez ce QR : quiconque le scanne peut vérifier l'authenticité du passeport.
              </p>
              <button
                onClick={() => {
                  navigator.clipboard?.writeText(qr.verificationUrl);
                  toast.success('Lien de vérification copié');
                }}
                className="inline-flex items-center gap-2 text-sm px-3 py-2 rounded-lg bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700"
              >
                <Share2 className="w-4 h-4" /> Copier le lien de vérification
              </button>
            </div>
          ) : (
            <PassportCard passport={passport} onChanged={() => refetch()} />
          )}
        </>
      )}
    </div>
  );
}

/* ====================================================================
 * Carte détaillée d'un passeport : infos + entrées + actions admin
 * ================================================================== */
function PassportCard({ passport, onChanged }: { passport: Passport; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const { hasRole } = useAuth();
  const canManageAll = hasRole('ADMIN', 'PASTEUR');
  const canAddEntry = hasRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR');

  const [adding, setAdding] = useState(false);
  const [entryType, setEntryType] = useState('BAPTISM');
  const [entryTitle, setEntryTitle] = useState('');
  const [entryDate, setEntryDate] = useState('');
  const [entryOrg, setEntryOrg] = useState('');
  const [entryDescription, setEntryDescription] = useState('');

  const addEntryMutation = useMutation({
    mutationFn: async (body: Record<string, string>) => {
      return (await api.post(`/passports/${passport.id}/entries`, body)).data as PassportEntry;
    },
    onSuccess: () => {
      toast.success('Entrée ajoutée et passeport re-signé');
      setAdding(false);
      setEntryTitle('');
      setEntryDate('');
      setEntryOrg('');
      setEntryDescription('');
      queryClient.invalidateQueries({ queryKey: ['my-passport'] });
      onChanged();
    },
    onError: (err: unknown) => toast.error(err instanceof Error ? err.message : 'Ajout impossible'),
  });

  const revokeMutation = useMutation({
    mutationFn: async () => {
      return (await api.post(`/passports/${passport.id}/revoke`, { reason: 'Révoqué par un responsable' })).data as Passport;
    },
    onSuccess: () => {
      toast.success('Passeport révoqué');
      queryClient.invalidateQueries({ queryKey: ['my-passport'] });
      onChanged();
    },
    onError: (err: unknown) => toast.error(err instanceof Error ? err.message : 'Révocation impossible'),
  });

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="lg:col-span-1 space-y-4">
        <div className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-6 space-y-4">
          <div className="flex items-center justify-between">
            <span className={`px-3 py-1 rounded-full text-xs font-semibold ${
              passport.status === 'ACTIVE'
                ? 'bg-emerald-500/10 text-emerald-500'
                : passport.status === 'REVOKED'
                  ? 'bg-red-500/10 text-red-500'
                  : 'bg-amber-500/10 text-amber-500'
            }`}>
              {STATUS_LABELS[passport.status] ?? passport.status}
            </span>
            <ShieldCheck className="w-5 h-5 text-emerald-500" />
          </div>
          <div>
            <p className="text-xs text-slate-500 dark:text-slate-400 uppercase tracking-wide">Code du passeport</p>
            <p className="font-mono text-sm mt-1 break-all">{passport.passportCode}</p>
          </div>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <p className="text-xs text-slate-500 dark:text-slate-400">Émis le</p>
              <p>{passport.issuedAt ? new Date(passport.issuedAt).toLocaleDateString() : '—'}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500 dark:text-slate-400">Expire le</p>
              <p>{passport.expiresAt ? new Date(passport.expiresAt).toLocaleDateString() : '—'}</p>
            </div>
          </div>
          <div className="rounded-lg bg-slate-100 dark:bg-slate-950 px-3 py-2">
            <p className="text-xs text-slate-500 dark:text-slate-400">Empreinte (SHA-256)</p>
            <p className="font-mono text-[10px] text-slate-700 dark:text-slate-300 truncate">{passport.payloadHash}</p>
          </div>
{passport.status === 'ACTIVE' && canAddEntry && (
            <div className="space-y-2">
              <button
                onClick={() => setAdding((v) => !v)}
                className="w-full inline-flex items-center justify-center gap-2 px-3 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium"
              >
                <Plus className="w-4 h-4" /> Ajouter une entrée
              </button>
              {canManageAll && (
                <button
                  onClick={() => { if (window.confirm('Révoquer ce passeport ?')) revokeMutation.mutate(); }}
                  disabled={revokeMutation.isPending}
                  className="w-full inline-flex items-center justify-center gap-2 px-3 py-2 rounded-lg bg-red-600/10 hover:bg-red-600/20 text-red-400 text-sm font-medium"
                >
                  <Ban className="w-4 h-4" /> {revokeMutation.isPending ? 'Révocation…' : 'Révoquer'}
                </button>
              )}
            </div>
          )}
          {adding && (
            <div className="rounded-xl border border-slate-200 dark:border-slate-700 p-3 space-y-2">
              <select
                value={entryType}
                onChange={(e) => setEntryType(e.target.value)}
                className="w-full px-2 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-950 border border-slate-200 dark:border-slate-700 text-sm"
              >
                {ENTRY_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
              <input
                value={entryTitle}
                onChange={(e) => setEntryTitle(e.target.value)}
                placeholder="Titre (ex. Baptême au fleuve)"
                className="w-full px-2 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-950 border border-slate-200 dark:border-slate-700 text-sm"
              />
              <input
                value={entryDate}
                onChange={(e) => setEntryDate(e.target.value)}
                type="date"
                className="w-full px-2 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-950 border border-slate-200 dark:border-slate-700 text-sm"
              />
              <input
                value={entryOrg}
                onChange={(e) => setEntryOrg(e.target.value)}
                placeholder="Organisation émettrice (optionnel)"
                className="w-full px-2 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-950 border border-slate-200 dark:border-slate-700 text-sm"
              />
              <textarea
                value={entryDescription}
                onChange={(e) => setEntryDescription(e.target.value)}
                placeholder="Description (optionnel)"
                rows={2}
                className="w-full px-2 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-950 border border-slate-200 dark:border-slate-700 text-sm"
              />
              <button
                onClick={() => addEntryMutation.mutate({
                  entryType,
                  title: entryTitle,
                  occurredAt: entryDate,
                  issuingOrganization: entryOrg,
                  description: entryDescription,
                })}
                disabled={addEntryMutation.isPending || !entryTitle.trim()}
                className="w-full px-3 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium disabled:opacity-40"
              >
                {addEntryMutation.isPending ? 'Ajout…' : 'Ajouter'}
              </button>
            </div>
          )}
        </div>
      </div>

      <div className="lg:col-span-2 space-y-3">
        <h2 className="font-semibold text-lg flex items-center gap-2">
          <BookOpen className="w-5 h-5 text-emerald-500" /> Historique du discipolat
        </h2>
        {passport.entries.length === 0 ? (
          <div className="rounded-2xl bg-white dark:bg-slate-900 border border-dashed border-slate-300 dark:border-slate-700 p-8 text-center text-slate-500 dark:text-slate-400">
            Aucune entrée pour le moment. Les jalons de votre parcours apparaîtront ici.
          </div>
        ) : (
          <div>
            {[...passport.entries].reverse().map((entry) => (
              <div key={entry.id} className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-4 mb-3">
                <div className="flex items-center justify-between gap-2 flex-wrap">
                  <div className="flex items-center gap-2">
                    <EntryTypeBadge type={entry.entryType} />
                    {entry.verified && (
                      <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400">
                        ✓ Vérifié
                      </span>
                    )}
                  </div>
                  {entry.occurredAt && (
                    <span className="text-xs text-slate-500">{new Date(entry.occurredAt).toLocaleDateString()}</span>
                  )}
                </div>
                <h3 className="font-semibold mt-2">{entry.title}</h3>
                {entry.issuingOrganization && (
                  <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
                    Émetteur : {entry.issuingOrganization}
                  </p>
                )}
                {entry.description && (
                  <p className="text-sm text-slate-600 dark:text-slate-300 mt-1">{entry.description}</p>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
