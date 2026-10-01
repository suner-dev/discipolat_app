import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Undo2 as ArrowUturnLeft, CircleCheck, CircleAlert, DatabaseZap, FlaskConical,
  History, Loader2, Play, ShieldQuestion, Table2,
} from 'lucide-react';

/**
 * G4.6 — Section Migration legacy (écran admin tenant).
 *
 * Pilotée par le toggle tenant §G1.2 `legacy_migration_enabled` : toute exécution réelle est
 * refusée côté serveur tant que le toggle est désactivé. Le dry-run reste disponible
 * (aucune écriture) pour préparer la bascule.
 */

interface LegacyMap {
  moduleCode: string;
  name: string;
  description: string;
  sourceTables: string[];
  targetTables: string;
  unmappableFields: string[];
}

interface MigrationJob {
  id: string;
  moduleCode: string;
  mode: 'DRY_RUN' | 'MIGRATE' | 'ROLLBACK';
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  rowsSeen: number;
  rowsMigrated: number;
  rowsMerged: number;
  rowsSkipped: number;
  rowsConflicts: number;
  createdAt: string;
  completedAt?: string;
}

interface MigrationAudit {
  id: string;
  sourceTable: string;
  sourceId: string;
  targetTable?: string;
  targetId?: string;
  status: 'MIGRATED' | 'MERGED' | 'SKIPPED' | 'CONFLICT';
  message?: string;
}

interface MigrationStatus {
  tenantId: string;
  enabled: boolean;
  modules: string[];
}

const MODE_LABEL: Record<MigrationJob['mode'], string> = {
  DRY_RUN: 'Simulation',
  MIGRATE: 'Migration',
  ROLLBACK: 'Annulation',
};

const STATUS_STYLE: Record<MigrationJob['status'], string> = {
  PENDING: 'bg-slate-100 text-slate-600',
  RUNNING: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-emerald-100 text-emerald-700',
  FAILED: 'bg-rose-100 text-rose-700',
  CANCELLED: 'bg-amber-100 text-amber-700',
};

const AUDIT_STYLE: Record<MigrationAudit['status'], string> = {
  MIGRATED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  MERGED: 'bg-sky-50 text-sky-700 border-sky-200',
  SKIPPED: 'bg-slate-50 text-slate-600 border-slate-200',
  CONFLICT: 'bg-orange-50 text-orange-700 border-orange-200',
};

export default function LegacyMigrationPage() {
  const queryClient = useQueryClient();
  const [selectedJob, setSelectedJob] = useState<string | null>(null);
  const [lastReport, setLastReport] = useState<MigrationJob | null>(null);

  const { data: status } = useQuery<MigrationStatus>({
    queryKey: ['legacy-migration', 'status'],
    queryFn: async () => (await api.get('/legacy-migration/status')).data,
  });

  const { data: maps = [] } = useQuery<LegacyMap[]>({
    queryKey: ['legacy-migration', 'maps'],
    queryFn: async () => (await api.get('/legacy-migration/maps')).data,
  });

  const { data: jobs = [] } = useQuery<MigrationJob[]>({
    queryKey: ['legacy-migration', 'jobs'],
    queryFn: async () => (await api.get('/legacy-migration/jobs')).data,
  });

  const { data: audit = [] } = useQuery<MigrationAudit[]>({
    queryKey: ['legacy-migration', 'audit', selectedJob],
    enabled: !!selectedJob,
    queryFn: async () => (await api.get(`/legacy-migration/jobs/${selectedJob}/audit`)).data,
  });

  const dryRun = useMutation({
    mutationFn: async (moduleCode: string) =>
      (await api.post<MigrationJob>(`/legacy-migration/modules/${moduleCode}/dry-run`)).data,
    onSuccess: (job) => {
      setLastReport(job);
      toast.success(`Simulation « ${job.moduleCode} » terminée — ${job.rowsSeen} ligne(s) analysée(s)`);
      queryClient.invalidateQueries({ queryKey: ['legacy-migration', 'jobs'] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const migrate = useMutation({
    mutationFn: async (moduleCode: string) =>
      (await api.post<MigrationJob>(`/legacy-migration/modules/${moduleCode}/migrate`)).data,
    onSuccess: (job) => {
      setLastReport(job);
      toast.success(`Migration « ${job.moduleCode} » : ${job.rowsMigrated} migrée(s), ${job.rowsConflicts} conflit(s)`);
      queryClient.invalidateQueries({ queryKey: ['legacy-migration', 'jobs'] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const rollback = useMutation({
    mutationFn: async (jobId: string) =>
      (await api.post<MigrationJob>(`/legacy-migration/jobs/${jobId}/rollback`)).data,
    onSuccess: (job) => {
      toast.success(`Annulation appliquée — ${job.rowsMigrated} ligne(s) retirée(s)`);
      setSelectedJob(null);
      queryClient.invalidateQueries({ queryKey: ['legacy-migration', 'jobs'] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const enabled = status?.enabled ?? false;

  return (
    <div className="p-6 space-y-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
          <DatabaseZap className="w-6 h-6 text-indigo-600" />
          Migration des données legacy
        </h1>
        <p className="text-sm text-slate-600 max-w-3xl">
          Basculez les données d&apos;origine (âmes, départements, événements) vers les moteurs Church OS.
          Le moteur est <strong>non destructeur</strong> : aucune donnée source n&apos;est effacée,
          la migration est <strong>idempotente</strong> (rejeu sans doublon) et
          <strong> annulable</strong> pendant 30&nbsp;jours.
        </p>
      </header>

      <section
        className={`rounded-xl border p-4 flex items-start gap-3 ${
          enabled ? 'border-emerald-200 bg-emerald-50' : 'border-amber-200 bg-amber-50'
        }`}
      >
        {enabled
          ? <CircleCheck className="w-5 h-5 text-emerald-600 mt-0.5" />
          : <ShieldQuestion className="w-5 h-5 text-amber-600 mt-0.5" />}
        <div className="text-sm">
          <p className="font-semibold text-slate-900">
            {enabled ? 'Migration activée pour cette église' : 'Migration désactivée (toggle legacy_migration_enabled)'}
          </p>
          <p className="text-slate-600">
            {enabled
              ? 'Vous pouvez exécuter une migration réelle. Commencez toujours par une simulation.'
              : 'La simulation reste disponible ; l&apos;exécution réelle est bloquée jusqu&apos;à activation du toggle dans les paramètres du tenant.'}
          </p>
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-3">
        {maps.map((map) => (
          <div key={map.moduleCode} className="bg-white rounded-xl border border-slate-200 shadow-sm p-4 flex flex-col gap-3">
            <div className="flex items-center gap-2">
              <Table2 className="w-5 h-5 text-indigo-600" />
              <h2 className="font-semibold text-slate-900">{map.name}</h2>
            </div>
            <p className="text-sm text-slate-600 flex-1">{map.description}</p>
            <div className="text-xs text-slate-500 space-y-1">
              <p><span className="font-medium">Sources :</span> {map.sourceTables.length ? map.sourceTables.join(', ') : 'absentes'}</p>
              <p><span className="font-medium">Cible :</span> {map.targetTables}</p>
              {map.unmappableFields.length > 0 && (
                <p><span className="font-medium">Non mappé :</span> {map.unmappableFields.join(', ')}</p>
              )}
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => dryRun.mutate(map.moduleCode)}
                disabled={dryRun.isPending}
                className="flex-1 inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium border border-indigo-200 text-indigo-700 bg-indigo-50 hover:bg-indigo-100 disabled:opacity-60"
              >
                {dryRun.isPending && dryRun.variables === map.moduleCode
                  ? <Loader2 className="w-4 h-4 animate-spin" />
                  : <FlaskConical className="w-4 h-4" />}
                Simuler
              </button>
              <button
                type="button"
                onClick={() => {
                  if (!enabled) { toast.error('Activez le toggle legacy_migration_enabled pour migrer'); return; }
                  if (window.confirm(`Migrer « ${map.name} » ? Cette écriture est annulable sous 30 jours.`)) {
                    migrate.mutate(map.moduleCode);
                  }
                }}
                disabled={migrate.isPending}
                className="flex-1 inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg text-sm font-semibold bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-60"
              >
                {migrate.isPending && migrate.variables === map.moduleCode
                  ? <Loader2 className="w-4 h-4 animate-spin" />
                  : <Play className="w-4 h-4" />}
                Migrer
              </button>
            </div>
          </div>
        ))}
      </section>

      {lastReport && (
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-4">
          <h2 className="font-semibold text-slate-900 mb-3">
            Rapport — {MODE_LABEL[lastReport.mode]} « {lastReport.moduleCode} »
          </h2>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-3 text-center">
            {[
              ['Vu', lastReport.rowsSeen], ['Migrées', lastReport.rowsMigrated],
              ['Fusionnées', lastReport.rowsMerged], ['Ignorées', lastReport.rowsSkipped],
              ['Conflits', lastReport.rowsConflicts],
            ].map(([label, value]) => (
              <div key={label as string} className="rounded-lg border border-slate-200 p-3">
                <p className="text-xs text-slate-500">{label}</p>
                <p className="text-xl font-bold text-slate-900">{value as number}</p>
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="bg-white rounded-xl border border-slate-200 shadow-sm">
        <div className="p-4 border-b border-slate-100 flex items-center gap-2">
          <History className="w-5 h-5 text-indigo-600" />
          <h2 className="font-semibold text-slate-900">Historique des jobs</h2>
        </div>
        {jobs.length === 0 ? (
          <p className="p-6 text-sm text-slate-500">Aucune migration exécutée pour le moment.</p>
        ) : (
          <div className="overflow-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-slate-600">
                <tr>
                  <th className="text-left px-4 py-2 font-medium">Module</th>
                  <th className="text-left px-4 py-2 font-medium">Mode</th>
                  <th className="text-left px-4 py-2 font-medium">Statut</th>
                  <th className="text-right px-4 py-2 font-medium">Vu</th>
                  <th className="text-right px-4 py-2 font-medium">Migrées</th>
                  <th className="text-right px-4 py-2 font-medium">Conflits</th>
                  <th className="text-left px-4 py-2 font-medium">Date</th>
                  <th className="px-4 py-2" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {jobs.map((job) => (
                  <tr key={job.id} className="hover:bg-slate-50">
                    <td className="px-4 py-2 font-medium text-slate-800">{job.moduleCode}</td>
                    <td className="px-4 py-2 text-slate-600">{MODE_LABEL[job.mode]}</td>
                    <td className="px-4 py-2">
                      <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLE[job.status]}`}>
                        {job.status}
                      </span>
                    </td>
                    <td className="px-4 py-2 text-right">{job.rowsSeen}</td>
                    <td className="px-4 py-2 text-right">{job.rowsMigrated}</td>
                    <td className="px-4 py-2 text-right">{job.rowsConflicts}</td>
                    <td className="px-4 py-2 text-slate-500">{new Date(job.createdAt).toLocaleString()}</td>
                    <td className="px-4 py-2 text-right whitespace-nowrap">
                      <button
                        type="button"
                        onClick={() => setSelectedJob(selectedJob === job.id ? null : job.id)}
                        className="text-indigo-600 hover:underline text-xs font-medium mr-3"
                      >
                        Détail
                      </button>
                      {job.mode === 'MIGRATE' && job.status === 'COMPLETED' && (
                        <button
                          type="button"
                          onClick={() => {
                            if (window.confirm('Annuler cette migration ? Seules les lignes créées par ce job seront retirées.')) {
                              rollback.mutate(job.id);
                            }
                          }}
                          disabled={rollback.isPending}
                          className="inline-flex items-center gap-1 text-rose-600 hover:underline text-xs font-medium disabled:opacity-50"
                        >
                          <ArrowUturnLeft className="w-3.5 h-3.5" /> Annuler
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {selectedJob && (
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm">
          <div className="p-4 border-b border-slate-100 flex items-center gap-2">
            <CircleAlert className="w-5 h-5 text-indigo-600" />
            <h2 className="font-semibold text-slate-900">Traçabilité ligne à ligne</h2>
          </div>
          {audit.length === 0 ? (
            <p className="p-6 text-sm text-slate-500">Aucune trace pour ce job (simulation ou job vide).</p>
          ) : (
            <ul className="divide-y divide-slate-100 max-h-96 overflow-auto">
              {audit.map((a) => (
                <li key={a.id} className="p-3 flex items-start gap-3">
                  <span className={`shrink-0 inline-block px-2 py-0.5 rounded-full text-xs font-medium border ${AUDIT_STYLE[a.status]}`}>
                    {a.status}
                  </span>
                  <div className="text-sm min-w-0">
                    <p className="font-mono text-xs text-slate-500 truncate">
                      {a.sourceTable}:{a.sourceId}
                      {a.targetTable ? ` → ${a.targetTable}:${a.targetId ?? '—'}` : ''}
                    </p>
                    {a.message && <p className="text-slate-600">{a.message}</p>}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  );
}
