import { useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Download, Upload, Loader2, CheckCircle2, AlertTriangle, XCircle,
  Building2, Layers, FileJson, ShieldCheck,
} from 'lucide-react';

/** G4.5 — Format canonique versionné transporté par les exports/imports. */
const CANONICAL_FORMAT = 'space_export_v1';

interface SpaceSummary {
  id: string;
  name: string;
  code?: string;
  spaceType?: string;
  status?: string;
}

interface ImportIssue {
  section: string;
  reference: string;
  severity: 'ERROR' | 'WARNING' | 'CONFLICT';
  message: string;
}

interface ImportReport {
  valid: boolean;
  applied: boolean;
  dryRun: boolean;
  format?: string;
  version?: number;
  scope?: string;
  counters?: Record<string, number>;
  issues?: ImportIssue[];
}

const COUNTER_LABELS: Record<string, string> = {
  spacesCreated: 'Espaces créés',
  spacesUpdated: 'Espaces mis à jour',
  spacesUnchanged: 'Espaces inchangés',
  spacesConflicted: 'Espaces en conflit',
  modulesUpserted: 'Modules appliqués',
  customFieldsCreated: 'Champs créés',
  customFieldsUpdated: 'Champs mis à jour',
  customFieldsConflicted: 'Champs en conflit',
  statusesCreated: 'Statuts créés',
  statusesUpdated: 'Statuts mis à jour',
  workflowsCreated: 'Workflows créés',
  workflowsUpdated: 'Workflows mis à jour',
};

const SEVERITY_STYLE: Record<ImportIssue['severity'], string> = {
  ERROR: 'bg-rose-50 text-rose-700 border-rose-200',
  WARNING: 'bg-amber-50 text-amber-700 border-amber-200',
  CONFLICT: 'bg-orange-50 text-orange-700 border-orange-200',
};

function downloadBlob(payload: Blob | ArrayBuffer | string, filename: string) {
  const blob = payload instanceof Blob
    ? payload
    : new Blob([payload], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

export default function SpaceImportExportPage() {
  const fileInput = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [report, setReport] = useState<ImportReport | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const { data: spaces = [], isLoading } = useQuery({
    queryKey: ['spaces', 'list'],
    queryFn: async () => {
      const res = await api.get('/spaces');
      const payload = res.data as SpaceSummary[] | { content?: SpaceSummary[] };
      return (Array.isArray(payload) ? payload : payload?.content ?? []) as SpaceSummary[];
    },
  });

  const exportSpace = async (space: SpaceSummary) => {
    setBusy(`export-${space.id}`);
    try {
      const res = await api.get(`/spaces/${space.id}/export`, { responseType: 'blob' });
      downloadBlob(res.data, `space_${space.code || space.id}_${CANONICAL_FORMAT}.json`);
      toast.success(`Espace « ${space.name} » exporté`);
    } catch (e) {
      toast.error(getErrorMessage(e));
    } finally {
      setBusy(null);
    }
  };

  const exportTenant = async () => {
    setBusy('export-tenant');
    try {
      const res = await api.get('/tenant/export', { responseType: 'blob' });
      downloadBlob(res.data, `tenant_${CANONICAL_FORMAT}.json`);
      toast.success('Église exportée intégralement');
    } catch (e) {
      toast.error(getErrorMessage(e));
    } finally {
      setBusy(null);
    }
  };

  const runImport = async (dryRun: boolean) => {
    if (!file) {
      toast.error('Choisissez un fichier JSON exporté');
      return;
    }
    setBusy(dryRun ? 'dry-run' : 'import');
    try {
      const form = new FormData();
      form.append('file', file);
      const res = await api.post<ImportReport>(
        `/spaces/import?dryRun=${dryRun ? 'true' : 'false'}`,
        form,
        { headers: { 'Content-Type': 'multipart/form-data' } },
      );
      setReport(res.data);
      if (res.data.valid) {
        toast.success(dryRun ? 'Validation réussie — aucune écriture' : 'Import appliqué');
      } else {
        toast.error('Document refusé — voir le rapport');
      }
    } catch (e) {
      toast.error(getErrorMessage(e));
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="p-6 space-y-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
          <FileJson className="w-6 h-6 text-indigo-600" />
          Import / Export des espaces &amp; de l&apos;église
        </h1>
        <p className="text-sm text-slate-600">
          Format canonique versionné <code className="font-mono text-indigo-700">{CANONICAL_FORMAT}</code> :
          configuration (modules, statuts, champs, workflows) et données de l&apos;église.
          L&apos;import est <strong>idempotent</strong> (UUID préservés, aucun doublon) et
          n&apos;écrase <strong>jamais</strong> silencieusement une ligne en conflit.
        </p>
      </header>
      {/* Espaces + église */}
      <section className="grid gap-6 lg:grid-cols-2">
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
          <div className="p-4 border-b border-slate-100 flex items-center gap-2">
            <Layers className="w-5 h-5 text-indigo-600" />
            <h2 className="font-semibold text-slate-900">Exporter un espace</h2>
          </div>
          {isLoading ? (
            <div className="p-6 flex items-center gap-2 text-slate-500 text-sm">
              <Loader2 className="w-4 h-4 animate-spin" /> Chargement des espaces…
            </div>
          ) : spaces.length === 0 ? (
            <p className="p-6 text-sm text-slate-500">Aucun espace configurable pour votre rôle.</p>
          ) : (
            <ul className="divide-y divide-slate-100 max-h-96 overflow-auto">
              {spaces.map((space) => (
                <li key={space.id} className="p-4 flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="font-medium text-slate-800 truncate">{space.name}</p>
                    <p className="text-xs text-slate-500">
                      {space.spaceType || '—'}{space.code ? ` · ${space.code}` : ''}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => exportSpace(space)}
                    disabled={busy === `export-${space.id}`}
                    className="shrink-0 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-60"
                  >
                    {busy === `export-${space.id}`
                      ? <Loader2 className="w-4 h-4 animate-spin" />
                      : <Download className="w-4 h-4" />}
                    Exporter
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
          <div className="p-4 border-b border-slate-100 flex items-center gap-2">
            <Building2 className="w-5 h-5 text-emerald-600" />
            <h2 className="font-semibold text-slate-900">Exporter toute l&apos;église</h2>
          </div>
          <div className="p-4 space-y-3">
            <p className="text-sm text-slate-600">
              Export complet du tenant courant : tous les espaces, le référentiel de configuration
              et les données (âmes, événements). Réservé aux ADMIN / PASTEUR et au super admin plateforme.
            </p>
            <button
              type="button"
              onClick={exportTenant}
              disabled={busy === 'export-tenant'}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-60"
            >
              {busy === 'export-tenant'
                ? <Loader2 className="w-4 h-4 animate-spin" />
                : <Download className="w-4 h-4" />}
              Exporter l&apos;église
            </button>
          </div>
        </div>
      </section>

      <section className="bg-white rounded-xl border border-slate-200 shadow-sm">
        <div className="p-4 border-b border-slate-100 flex items-center gap-2">
          <Upload className="w-5 h-5 text-indigo-600" />
          <h2 className="font-semibold text-slate-900">Centre d&apos;import</h2>
        </div>
        <div className="p-4 space-y-4">
          <input
            ref={fileInput}
            type="file"
            accept="application/json,.json"
            onChange={(e) => { setFile(e.target.files?.[0] ?? null); setReport(null); }}
            className="block w-full text-sm text-slate-700 file:mr-3 file:rounded-lg file:border-0 file:bg-slate-100 file:px-3 file:py-2 file:text-sm file:font-medium hover:file:bg-slate-200"
          />
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => runImport(true)}
              disabled={!file || busy !== null}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold border border-indigo-200 text-indigo-700 bg-indigo-50 hover:bg-indigo-100 disabled:opacity-60"
            >
              {busy === 'dry-run' ? <Loader2 className="w-4 h-4 animate-spin" /> : <ShieldCheck className="w-4 h-4" />}
              Valider (dry-run)
            </button>
            <button
              type="button"
              onClick={() => runImport(false)}
              disabled={!file || busy !== null}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-60"
            >
              {busy === 'import' ? <Loader2 className="w-4 h-4 animate-spin" /> : <Upload className="w-4 h-4" />}
              Importer
            </button>
          </div>

          {report && (
            <div className="space-y-4 border-t border-slate-100 pt-4">
              <div className={`flex items-center gap-2 text-sm font-semibold ${report.valid ? 'text-emerald-700' : 'text-rose-700'}`}>
                {report.valid ? <CheckCircle2 className="w-4 h-4" /> : <XCircle className="w-4 h-4" />}
                {report.valid
                  ? (report.dryRun ? 'Document valide — aucune écriture effectuée' : 'Document valide — import appliqué')
                  : 'Document refusé — aucune écriture effectuée'}
              </div>

              {report.counters && (
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  {Object.entries(report.counters)
                    .filter(([, value]) => value > 0)
                    .map(([key, value]) => (
                      <div key={key} className="rounded-lg border border-slate-200 p-3">
                        <p className="text-xs text-slate-500">{COUNTER_LABELS[key] ?? key}</p>
                        <p className="text-lg font-bold text-slate-900">{value}</p>
                      </div>
                    ))}
                </div>
              )}

              {(report.issues?.length ?? 0) > 0 && (
                <ul className="space-y-2">
                  {(report.issues ?? []).map((issue, index) => (
                    <li
                      key={`${issue.section}-${issue.reference}-${index}`}
                      className={`flex items-start gap-2 rounded-lg border p-3 text-sm ${SEVERITY_STYLE[issue.severity]}`}
                    >
                      <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />
                      <span>
                        <span className="font-semibold">{issue.severity}</span>
                        {' · '}<code className="font-mono">{issue.section}</code>
                        {issue.reference ? ` · ${issue.reference}` : ''}
                        {' — '}{issue.message}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}