import { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '@/lib/api';
import { toast } from 'react-hot-toast';
import {
  Plug,
  Zap,
  Workflow,
  CalendarDays,
  CalendarClock,
  BookOpenCheck,
  FileSpreadsheet,
  RefreshCw,
  CheckCircle2,
} from 'lucide-react';

/**
 * Connecteurs tiers natifs (feature #3).
 * Configuration par église : Zapier/Make (webhooks), Google/Outlook Calendar
 * (sync iCal), QuickBooks/Xero (export comptable).
 */

interface ConnectorStatus {
  connector: string;
  enabled: boolean;
  configured: boolean;
  endpointUrl: string | null;
  icalUrl: string | null;
  hasApiKey: boolean;
  lastSyncAt: string | null;
  lastSyncStatus: string | null;
}

const CONNECTOR_META: Record<string, {
  label: string; description: string; icon: typeof Zap; needsIcal?: boolean; needsEndpoint?: boolean; needsApiKey?: boolean;
}> = {
  ZAPIER: { label: 'Zapier', description: 'Déclenche vos Zaps sur chaque événement Discipolat', icon: Zap, needsEndpoint: true },
  MAKE: { label: 'Make', description: 'Scénarios Make (ex-Integromat) déclenchés par webhook', icon: Workflow, needsEndpoint: true },
  GOOGLE_CALENDAR: { label: 'Google Agenda', description: 'Import iCal de votre agenda public', icon: CalendarDays, needsIcal: true },
  OUTLOOK_CALENDAR: { label: 'Outlook', description: 'Import iCal du calendrier Microsoft 365', icon: CalendarClock, needsIcal: true },
  QUICKBOOKS: { label: 'QuickBooks', description: 'Export des transactions financières', icon: BookOpenCheck, needsEndpoint: true, needsApiKey: true },
  XERO: { label: 'Xero', description: 'Export comptable vers Xero', icon: FileSpreadsheet, needsEndpoint: true, needsApiKey: true },
};

export default function AdminConnectorsPage() {
  const qc = useQueryClient();
  const [editing, setEditing] = useState<Record<string, Partial<ConnectorStatus> & { apiKey?: string }>>({});

  const { data: connectors } = useQuery({
    queryKey: ['connectors'],
    queryFn: async () => (await api.get<ConnectorStatus[]>('/connectors')).data,
  });

  useEffect(() => {
    if (connectors) {
      setEditing(Object.fromEntries(connectors.map((c) => [c.connector, { ...c }])));
    }
  }, [connectors]);

  const saveMutation = useMutation({
    mutationFn: async (connector: string) => {
      const e = editing[connector];
      return (await api.put('/connectors', {
        connector,
        enabled: !!e?.enabled,
        endpointUrl: e?.endpointUrl ?? null,
        apiKey: e?.apiKey ?? null,
        icalUrl: e?.icalUrl ?? null,
      })).data;
    },
    onSuccess: () => {
      toast.success('Connecteur enregistré');
      qc.invalidateQueries({ queryKey: ['connectors'] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const testMutation = useMutation({
    mutationFn: async (connector: string) =>
      (await api.post<{ success: boolean; detail?: string; error?: string }>(`/connectors/${connector}/test`)).data,
    onSuccess: (res) => {
      if (res.success) toast.success(res.detail ?? 'Connexion OK');
      else toast.error(res.error ?? 'Échec du test');
    },
  });

  const syncMutation = useMutation({
    mutationFn: async (connector: string) =>
      (await api.post<{ success: boolean; syncedEvents?: number; error?: string }>(`/connectors/${connector}/sync`)).data,
    onSuccess: (res) => {
      if (res.success) toast.success(`${res.syncedEvents ?? 0} événement(s) synchronisé(s)`);
      else toast.error(res.error ?? 'Échec de la synchronisation');
      qc.invalidateQueries({ queryKey: ['connectors'] });
    },
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-orange-500 to-red-600 text-white shadow-lg">
          <Plug size={24} />
        </div>
        <div>
          <h1 className="page-title">Connecteurs tiers</h1>
          <p className="page-subtitle">
            Intégrez Discipolat à Zapier, Make, vos agendas et votre comptabilité.
          </p>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {(Object.keys(CONNECTOR_META) as string[]).map((key) => {
          const meta = CONNECTOR_META[key];
          const state = editing[key] ?? {};
          const Icon = meta.icon;
          return (
            <div key={key} className="glass-card p-5">
              <div className="mb-3 flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <Icon size={22} className="text-primary" />
                  <div>
                    <p className="font-semibold">{meta.label}</p>
                    <p className="text-xs text-muted">{meta.description}</p>
                  </div>
                </div>
                {state.enabled && (
                  <span className="badge badge-success flex items-center gap-1">
                    <CheckCircle2 size={12} /> Actif
                  </span>
                )}
              </div>

              {meta.needsEndpoint && (
                <input
                  className="input mb-2"
                  placeholder="URL du webhook / endpoint"
                  value={state.endpointUrl ?? ''}
                  onChange={(e) =>
                    setEditing({ ...editing, [key]: { ...state, endpointUrl: e.target.value } })
                  }
                />
              )}
              {meta.needsIcal && (
                <input
                  className="input mb-2"
                  placeholder="URL iCal (.ics)"
                  value={state.icalUrl ?? ''}
                  onChange={(e) => setEditing({ ...editing, [key]: { ...state, icalUrl: e.target.value } })}
                />
              )}
              {meta.needsApiKey && (
                <input
                  type="password"
                  className="input mb-2"
                  placeholder={state.hasApiKey ? 'Clé enregistrée (chiffrée)' : 'Clé API'}
                  value={state.apiKey ?? ''}
                  onChange={(e) => setEditing({ ...editing, [key]: { ...state, apiKey: e.target.value } })}
                />
              )}

              <label className="mb-3 flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={!!state.enabled}
                  onChange={(e) => setEditing({ ...editing, [key]: { ...state, enabled: e.target.checked } })}
                />
                Activer ce connecteur
              </label>

              <div className="flex items-center gap-2">
                <button
                  className="btn-primary btn-sm"
                  disabled={saveMutation.isPending}
                  onClick={() => saveMutation.mutate(key)}
                >
                  Enregistrer
                </button>
                <button className="btn-sm btn-outline" onClick={() => testMutation.mutate(key)}>
                  Tester
                </button>
                {meta.needsIcal && (
                  <button className="btn-sm btn-outline" onClick={() => syncMutation.mutate(key)}>
                    <RefreshCw size={13} /> Synchroniser
                  </button>
                )}
                {state.lastSyncAt && (
                  <span className="ml-auto text-xs text-muted">
                    Sync : {new Date(state.lastSyncAt).toLocaleString()}
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
