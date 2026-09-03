import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Upload, FileText, AlertTriangle, CheckCircle, Loader2, Users, Home, Heart } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';

type ImportType = 'souls' | 'families' | 'users';

interface ValidationError {
  rowNumber: number;
  field: string;
  message: string;
}

interface ValidationResult {
  success: boolean;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  errors: ValidationError[];
}

interface ImportResult {
  imported: number;
  skipped: number;
  errors?: string[];
}

const IMPORT_TYPES: { key: ImportType; label: string; icon: typeof Heart; color: string; fields: string[] }[] = [
  { key: 'souls', label: 'Âmes', icon: Heart, color: 'from-rose-500 to-pink-500', fields: ['nom', 'prenom', 'telephone', 'email', 'adresse', 'dateNaissance', 'sexe'] },
  { key: 'families', label: 'Familles', icon: Home, color: 'from-blue-500 to-indigo-500', fields: ['nomFamille', 'adresse', 'telephone', 'chefFamilleId'] },
  { key: 'users', label: 'Utilisateurs', icon: Users, color: 'from-emerald-500 to-teal-500', fields: ['email', 'firstName', 'lastName', 'role', 'password'] },
];

export default function ImportDataPage() {
  const [selectedType, setSelectedType] = useState<ImportType>('souls');
  const [csvText, setCsvText] = useState('');
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);

  const typeConfig = IMPORT_TYPES.find(t => t.key === selectedType)!;

  const parseCsv = (text: string): Record<string, string>[] => {
    const lines = text.trim().split('\n');
    if (lines.length < 2) return [];
    const headers = lines[0].split(',').map(h => h.trim().toLowerCase());
    return lines.slice(1).map(line => {
      const values = line.split(',').map(v => v.trim());
      const row: Record<string, string> = {};
      headers.forEach((h, i) => { row[h] = values[i] || ''; });
      return row;
    });
  };

  const validateMutation = useMutation({
    mutationFn: async () => {
      const rows = parseCsv(csvText);
      if (rows.length === 0) { toast('Collez des données CSV valides', { icon: '⚠️' }); throw new Error('empty'); }
      return (await api.post<ValidationResult>(`/import/validate/${selectedType}`, rows)).data;
    },
    onSuccess: (data) => {
      setValidationResult(data);
      if (data.success) toast.success(`${data.validRows} lignes valides`);
      else toast(`${data.invalidRows} erreurs trouvées`, { icon: '⚠️' });
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  const importMutation = useMutation({
    mutationFn: async () => {
      const rows = parseCsv(csvText);
      if (rows.length === 0) { toast('Rien à importer', { icon: '⚠️' }); throw new Error('empty'); }
      return (await api.post<ImportResult>(`/import/${selectedType}`, rows)).data;
    },
    onSuccess: (data) => {
      toast.success(`${data.imported} éléments importés`);
      setCsvText('');
      setValidationResult(null);
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg">
          <Upload className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Import de Données</h1>
          <p className="page-subtitle">Importez des données en masse depuis du CSV</p>
        </div>
      </div>

      {/* Type selector */}
      <div className="flex gap-3 mb-6">
        {IMPORT_TYPES.map(t => (
          <button key={t.key} onClick={() => { setSelectedType(t.key); setValidationResult(null); }}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition ${selectedType === t.key ? `bg-gradient-to-r ${t.color} text-white shadow-lg` : 'bg-white/5 text-gray-400 hover:bg-white/10 border border-gray-200 dark:border-white/10'}`}>
            <t.icon className="w-4 h-4" /> {t.label}
          </button>
        ))}
      </div>

      {/* Expected columns hint */}
      <div className="glass-card p-4 mb-6">
        <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Colonnes attendues ({typeConfig.label}):</h3>
        <div className="flex flex-wrap gap-2">
          {typeConfig.fields.map(f => (
            <span key={f} className="px-2 py-0.5 rounded-full bg-gray-100 dark:bg-white/10 text-xs text-gray-600 dark:text-gray-400">{f}</span>
          ))}
        </div>
      </div>

      {/* CSV input */}
      <div className="mb-6">
        <textarea value={csvText} onChange={e => { setCsvText(e.target.value); setValidationResult(null); }}
          rows={12}
          className="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm font-mono resize-y"
          placeholder={`nom,prenom,telephone,email\nDupont,Marie,0612345678,marie@example.com\nMartin,Jean,0698765432,jean@example.com`} />
      </div>

      {/* Actions */}
      <div className="flex gap-3 mb-6">
        <button onClick={() => validateMutation.mutate()} disabled={validateMutation.isPending || !csvText.trim()}
          className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-sm font-medium hover:bg-white/10 flex items-center gap-2 disabled:opacity-50">
          {validateMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <AlertTriangle className="w-4 h-4" />}
          Valider
        </button>
        <button onClick={() => importMutation.mutate()} disabled={importMutation.isPending || !csvText.trim()}
          className={`px-4 py-2 rounded-xl text-white text-sm font-medium flex items-center gap-2 disabled:opacity-50 bg-gradient-to-r ${typeConfig.color}`}>
          {importMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Upload className="w-4 h-4" />}
          Importer
        </button>
      </div>

      {/* Validation results */}
      {validationResult && (
        <div className={`rounded-xl p-5 border ${validationResult.success ? 'bg-green-50 dark:bg-green-500/10 border-green-200 dark:border-green-500/30' : 'bg-red-50 dark:bg-red-500/10 border-red-200 dark:border-red-500/30'}`}>
          <div className="flex items-center gap-2 mb-3">
            {validationResult.success ? <CheckCircle className="w-5 h-5 text-green-600" /> : <AlertTriangle className="w-5 h-5 text-red-500" />}
            <span className="font-medium text-sm">{validationResult.success ? 'Validation réussie' : `${validationResult.invalidRows} erreurs trouvées`}</span>
          </div>
          <div className="text-xs text-gray-600 dark:text-gray-400 mb-2">
            Total: {validationResult.totalRows} · Valides: {validationResult.validRows} · Invalides: {validationResult.invalidRows}
          </div>
          {!validationResult.success && validationResult.errors.length > 0 && (
            <div className="mt-3 space-y-1 max-h-48 overflow-y-auto">
              {validationResult.errors.map((err, i) => (
                <div key={i} className="text-xs text-red-600 dark:text-red-400">
                  Ligne {err.rowNumber}: <strong>{err.field}</strong> — {err.message}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
