import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Globe, Clock, DollarSign, Check, Plus, Trash2 } from 'lucide-react';

interface Currency { id: string; code: string; symbol: string; timezone: string; locale: string; exchangeRateToUsd: number; isPrimary: boolean; }
const SUPPORTED_CURRENCIES = [
  { code: 'XAF', symbol: 'FCFA', name: 'Franc CFA' }, { code: 'EUR', symbol: '€', name: 'Euro' },
  { code: 'USD', symbol: '$', name: 'Dollar US' }, { code: 'KES', symbol: 'KSh', name: 'Shilling Kényan' },
  { code: 'NGN', symbol: '₦', name: 'Naira' }, { code: 'GBP', symbol: '£', name: 'Livre Sterling' },
];
const TIMEZONES = [
  { id: 'Africa/Douala', name: 'Douala (GMT+1)' }, { id: 'Africa/Lagos', name: 'Lagos (GMT+1)' },
  { id: 'Africa/Nairobi', name: 'Nairobi (GMT+3)' }, { id: 'Europe/Paris', name: 'Paris (GMT+1)' },
  { id: 'Europe/London', name: 'Londres (GMT+0)' }, { id: 'America/New_York', name: 'New York (GMT-5)' },
];

export default function CurrencySettingsPage() {
  const { t } = useI18n();
  const [currencies, setCurrencies] = useState<Currency[]>([
    { id: '1', code: 'XAF', symbol: 'FCFA', timezone: 'Africa/Douala', locale: 'fr_FR', exchangeRateToUsd: 0.0016, isPrimary: true },
  ]);
  const [showAdd, setShowAdd] = useState(false);

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Globe className="text-blue-400" /> {t('nav.currency') || 'Multi-devise & Fuseaux horaires'}</h1>

      {/* Current primary currency */}
      <div className="bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
        <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2"><DollarSign className="text-green-400" /> {t('currency.primary') || 'Devise principale'}</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {currencies.filter(c => c.isPrimary).map(c => (
            <div key={c.id} className="bg-gradient-to-br from-green-500/20 to-emerald-500/20 rounded-xl p-4 border border-green-500/30">
              <div className="text-3xl font-bold text-white">{c.symbol}</div>
              <div className="text-green-300">{c.code}</div>
              <div className="text-sm text-gray-400 mt-2">{c.timezone}</div>
            </div>
          ))}
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
                  <td className="p-3 text-white font-medium">{c.code}</td>
                  <td className="p-3 text-gray-300">{c.symbol}</td>
                  <td className="p-3 text-gray-300">{c.exchangeRateToUsd}</td>
                  <td className="p-3 text-gray-300">{c.timezone}</td>
                  <td className="p-3">{c.isPrimary ? <Check className="w-4 h-4 text-green-400" /> : '—'}</td>
                  <td className="p-3 text-right"><button className="text-red-400 hover:text-red-300"><Trash2 className="w-4 h-4" /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Timezones */}
      <div className="bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
        <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2"><Clock className="text-purple-400" /> {t('currency.timezones') || 'Fuseaux horaires'}</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {TIMEZONES.map(tz => (
            <div key={tz.id} className="bg-white/5 rounded-xl p-3 flex items-center justify-between border border-white/10 hover:border-blue-500/50 transition cursor-pointer">
              <span className="text-gray-300 text-sm">{tz.name}</span>
              <Clock className="w-4 h-4 text-gray-500" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
