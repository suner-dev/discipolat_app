import { useCallback, useEffect, useState } from 'react';
import { useI18n } from '@/i18n';
import { Globe, Clock, DollarSign, Check, Plus, Trash2, Star, RefreshCw } from 'lucide-react';
import api from '@/lib/api';
import { getErrorMessage } from '@/lib/api';

interface CurrencyConfig {
  id: string;
  tenantId: string;
  currencyCode: string;
  currencySymbol: string;
  timezone: string | null;
  locale: string | null;
  exchangeRateToUsd: number;
  isPrimary: boolean;
  isActive: boolean;
}

interface SupportedCurrency { code: string; name: string; symbol: string; }
interface SupportedTimezone { id: string; name: string; }

interface NewCurrency {
  currencyCode: string;
  currencySymbol: string;
  timezone: string;
  locale: string;
  exchangeRateToUsd: number;
  isPrimary: boolean;
}

export default function CurrencySettingsPage() {
  const { t } = useI18n();
  const [currencies, setCurrencies] = useState<CurrencyConfig[]>([]);
  const [supported, setSupported] = useState<SupportedCurrency[]>([]);
  const [timezones, setTimezones] = useState<SupportedTimezone[]>([]);
  const [stats, setStats] = useState<Record<string, unknown> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [showAdd, setShowAdd] = useState(false);
  const [newCurrency, setNewCurrency] = useState<NewCurrency>({
    currencyCode: 'XAF',
    currencySymbol: 'FCFA',
    timezone: 'Africa/Douala',
    locale: 'fr_FR',
    exchangeRateToUsd: 1,
    isPrimary: false,
  });
  const [converting, setConverting] = useState<{ from: string; to: string; amount: number; result: number | null } | null>(null);
  const [convertAmount, setConvertAmount] = useState(100);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [cur, prim, supp, tz, statsRes] = await Promise.all([
        api.get('/currencies'),
        api.get('/currencies/primary'),
        api.get('/currencies/supported'),
        api.get('/currencies/timezones'),
        api.get('/currencies/stats'),
      ]);
      const curList = Array.isArray(cur.data) ? cur.data : cur.data?.currencies ?? [];
      setCurrencies(curList);
      setSupported(Array.isArray(supp.data) ? supp.data : []);
      setTimezones(Array.isArray(tz.data) ? tz.data : []);
      setStats(statsRes.data ?? null);
      const primary = prim.data;
      if (primary && !curList.some((c: CurrencyConfig) => c.isPrimary)) {
        setCurrencies((prev) =>
          prev.some((c) => c.id === primary.id) ? prev : [primary, ...prev]
        );
      }
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (id: string) => {
    try {
      await api.delete(`/currencies/${id}`);
      setCurrencies((prev) => prev.filter((c) => c.id !== id));
    } catch (e) {
      setError(getErrorMessage(e));
    }
  };

  const handlePrimary = async (id: string) => {
    const target = currencies.find((c) => c.id === id);
    if (!target) return;
    try {
      const updated = await api.put(`/currencies/${id}`, {
        ...target,
        isPrimary: true,
      });
      await load();
      return updated;
    } catch (e) {
      setError(getErrorMessage(e));
    }
  };

  const handleAdd = async () => {
    try {
      await api.post('/currencies', newCurrency);
      setShowAdd(false);
      setNewCurrency({
        currencyCode: 'XAF',
        currencySymbol: 'FCFA',
        timezone: 'Africa/Douala',
        locale: 'fr_FR',
        exchangeRateToUsd: 1,
        isPrimary: false,
      });
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  };

  const handleConvert = async () => {
    if (!converting) return;
    try {
      const res = await api.post('/currencies/convert', {
        amount: convertAmount,
        from: converting.from,
        to: converting.to,
      });
      setConverting({ ...converting, result: res.data?.result ?? null });
    } catch (e) {
      setError(getErrorMessage(e));
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[40vh]">
        <RefreshCw className="w-8 h-8 text-blue-400 animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Globe className="text-blue-400" /> {t('nav.currency') || 'Multi-devise & Fuseaux horaires'}
        </h1>
        <button onClick={load} className="flex items-center gap-2 px-3 py-2 bg-white/5 hover:bg-white/10 rounded-xl text-white/70 text-sm">
          <RefreshCw className="w-4 h-4" /> Actualiser
        </button>
      </div>

      {error && (
        <div className="bg-red-500/15 border border-red-500/40 text-red-200 rounded-xl p-4 text-sm">{error}</div>
      )}

      {stats && (
        <div className="bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
          <h2 className="text-lg font-semibold text-white mb-2">Statistiques</h2>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
            <div className="text-gray-300">Devises actives: <span className="text-white font-medium">{String(stats.primaryCurrency ?? '-')}</span></div>
            <div className="text-gray-300">Total configurées: <span className="text-white font-medium">{Array.isArray(currencies) ? currencies.length : '-'}</span></div>
          </div>
        </div>
      )}

      {/* Current primary currency */}
      <div className="bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
        <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
          <DollarSign className="text-green-400" /> {t('currency.primary') || 'Devise principale'}
        </h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {currencies.filter(c => c.isPrimary).map(c => (
            <div key={c.id} className="bg-gradient-to-br from-green-500/20 to-emerald-500/20 rounded-xl p-4 border border-green-500/30">
              <div className="text-3xl font-bold text-white">{c.currencySymbol}</div>
              <div className="text-green-300">{c.currencyCode}</div>
              <div className="text-sm text-gray-400 mt-2">{c.timezone || '-'}</div>
            </div>
          ))}
          {currencies.filter(c => c.isPrimary).length === 0 && (
            <div className="text-gray-500 text-sm col-span-full">Aucune devise principale définie</div>
          )}
        </div>
      </div>

      {/* All currencies */}
      <div className="bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-semibold text-white">{t('currency.all') || 'Devises configurées'}</h2>
          <button onClick={() => setShowAdd(!showAdd)} className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-xl text-white text-sm font-medium transition">
            <Plus className="w-4 h-4" /> {t('currency.add') || 'Ajouter'}
          </button>
        </div>
        {currencies.length === 0 && !showAdd && (
          <div className="text-gray-500 text-sm">Aucune devise configurée. Ajoutez-en une.</div>
        )}
        {showAdd && (
          <div className="bg-blue-500/10 border border-blue-500/30 rounded-xl p-4 mb-4 space-y-3">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div>
                <label className="text-xs text-gray-400">Code</label>
                <select
                  value={newCurrency.currencyCode}
                  onChange={(e) => {
                    const sel = supported.find((s) => s.code === e.target.value);
                    setNewCurrency((p) => ({
                      ...p,
                      currencyCode: e.target.value,
                      currencySymbol: sel?.symbol ?? p.currencySymbol,
                      locale: sel?.name ? p.locale : p.locale,
                    }));
                  }}
                  className="w-full bg-white/5 rounded-lg px-3 py-2 text-white text-sm border border-white/10"
                >
                  {supported.map((s) => <option key={s.code} value={s.code}>{s.code}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400">Symbole</label>
                <input
                  value={newCurrency.currencySymbol}
                  onChange={(e) => setNewCurrency((p) => ({ ...p, currencySymbol: e.target.value }))}
                  className="w-full bg-white/5 rounded-lg px-3 py-2 text-white text-sm border border-white/10"
                />
              </div>
              <div>
                <label className="text-xs text-gray-400">Taux/USD</label>
                <input
                  type="number"
                  value={newCurrency.exchangeRateToUsd}
                  onChange={(e) => setNewCurrency((p) => ({ ...p, exchangeRateToUsd: Number(e.target.value) }))}
                  className="w-full bg-white/5 rounded-lg px-3 py-2 text-white text-sm border border-white/10"
                />
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="text-xs text-gray-400">Fuseau horaire</label>
                <select
                  value={newCurrency.timezone}
                  onChange={(e) => setNewCurrency((p) => ({ ...p, timezone: e.target.value }))}
                  className="w-full bg-white/5 rounded-lg px-3 py-2 text-white text-sm border border-white/10"
                >
                  {timezones.map((tz) => <option key={tz.id} value={tz.id}>{tz.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400">Locale</label>
                <input
                  value={newCurrency.locale}
                  onChange={(e) => setNewCurrency((p) => ({ ...p, locale: e.target.value }))}
                  className="w-full bg-white/5 rounded-lg px-3 py-2 text-white text-sm border border-white/10"
                />
              </div>
            </div>
            <label className="flex items-center gap-2 text-sm text-gray-300">
              <input
                type="checkbox"
                checked={newCurrency.isPrimary}
                onChange={(e) => setNewCurrency((p) => ({ ...p, isPrimary: e.target.checked }))}
                className="accent-blue-500"
              />
              Définir comme devise principale
            </label>
            <div className="flex gap-2">
              <button onClick={handleAdd} className="px-4 py-2 bg-green-600 hover:bg-green-700 rounded-xl text-white text-sm">Créer</button>
              <button onClick={() => setShowAdd(false)} className="px-4 py-2 bg-white/10 hover:bg-white/20 rounded-xl text-white text-sm">Annuler</button>
            </div>
          </div>
        )}
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead><tr className="text-gray-400 border-b border-white/10">
              <th className="text-left p-3">Code</th><th className="text-left p-3">Symbole</th>
              <th className="text-left p-3">Taux/USD</th><th className="text-left p-3">Fuseau</th>
              <th className="text-left p-3">Principal</th><th className="text-right p-3">Actions</th>
            </tr></thead>
            <tbody>
              {currencies.map(c => (
                <tr key={c.id} className="border-b border-white/5 hover:bg-white/5">
                  <td className="p-3 text-white font-medium">{c.currencyCode}</td>
                  <td className="p-3 text-gray-300">{c.currencySymbol}</td>
                  <td className="p-3 text-gray-300">{c.exchangeRateToUsd}</td>
                  <td className="p-3 text-gray-300">{c.timezone || '-'}</td>
                  <td className="p-3">
                    {c.isPrimary ? <Check className="w-4 h-4 text-green-400" /> : (
                      <button onClick={() => handlePrimary(c.id)} title="Définir principale" className="text-gray-500 hover:text-yellow-400">
                        <Star className="w-4 h-4" />
                      </button>
                    )}
                  </td>
                  <td className="p-3 text-right">
                    <button onClick={() => handleDelete(c.id)} className="text-red-400 hover:text-red-300" title="Supprimer">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Converter */}
      <div className="bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
        <h2 className="text-lg font-semibold text-white mb-4">Convertisseur de devises</h2>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          <select
            value={converting?.from ?? currencies[0]?.currencyCode ?? 'XAF'}
            onChange={(e) => setConverting((p) => ({ from: e.target.value, to: p?.to ?? 'USD', amount: p?.amount ?? 0, result: null }))}
            className="bg-white/5 rounded-lg px-3 py-2 text-white text-sm border border-white/10"
          >
            {currencies.map((c) => <option key={c.id} value={c.currencyCode}>{c.currencyCode}</option>)}
          </select>
          <input
            type="number"
            value={convertAmount}
            onChange={(e) => setConvertAmount(Number(e.target.value))}
            className="bg-white/5 rounded-lg px-3 py-2 text-white text-sm border border-white/10"
            placeholder="Montant"
          />
          <select
            value={converting?.to ?? 'USD'}
            onChange={(e) => setConverting((p) => ({ from: p?.from ?? currencies[0]?.currencyCode ?? 'XAF', to: e.target.value, amount: p?.amount ?? 0, result: null }))}
            className="bg-white/5 rounded-lg px-3 py-2 text-white text-sm border border-white/10"
          >
            {currencies.map((c) => <option key={c.id} value={c.currencyCode}>{c.currencyCode}</option>)}
            {!currencies.some((c) => c.currencyCode === 'USD') && <option value="USD">USD</option>}
          </select>
          <button onClick={handleConvert} className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-xl text-white text-sm">Convertir</button>
        </div>
        {converting?.result != null && (
          <div className="mt-4 text-white">
            {converting.amount} {converting.from} = {' '}
            <span className="text-green-400 font-bold">{converting.result.toFixed(2)} {converting.to}</span>
          </div>
        )}
      </div>

      {/* Timezones */}
      <div className="bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
        <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
          <Clock className="text-purple-400" /> {t('currency.timezones') || 'Fuseaux horaires'}
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {timezones.map(tz => (
            <div key={tz.id} className="bg-white/5 rounded-xl p-3 flex items-center justify-between border border-white/10 hover:border-blue-500/50 transition cursor-pointer">
              <span className="text-gray-300 text-sm">{tz.name}</span>
              <Clock className="w-4 h-4 text-gray-500" />
            </div>
          ))}
          {timezones.length === 0 && <div className="text-gray-500 text-sm col-span-full">Aucun fuseau horaire disponible</div>}
        </div>
      </div>
    </div>
  );
}
