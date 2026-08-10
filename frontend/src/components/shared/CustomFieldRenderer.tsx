import type { CustomFieldDefinition } from '@/types';

interface CustomFieldBundleDefinition {
  id: string;
  code: string;
  label: string;
  type: string;
  obligatoire: boolean;
  options?: string[];
  placeholder?: string;
  defaultValue?: string;
  ordre: number;
  value?: string;
}

interface Props {
  definitions: CustomFieldBundleDefinition[];
  values: Record<string, string>;
  onChange: (fieldId: string, value: string) => void;
  readOnly?: boolean;
}

/**
 * Rendu dynamique des champs personnalisés.
 * Types supportés : TEXTE, NOMBRE, DATE, DATE_HEURE, BOOLEEN, SELECTION,
 * SELECTION_MULTIPLE, FICHIER, IMAGE, TELEPHONE, EMAIL, URL, TEXTAREA.
 */
export default function CustomFieldRenderer({ definitions, values, onChange, readOnly }: Props) {
  if (definitions.length === 0) return null;

  return (
    <div className="space-y-4">
      {definitions.map((def) => {
        const val = values[def.id] ?? def.value ?? def.defaultValue ?? '';
        const required = def.obligatoire;
        const label = (
          <label className="label">
            {def.label}
            {required && <span className="text-red-500 ml-0.5">*</span>}
          </label>
        );

        const commonProps = {
          value: val,
          onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
            onChange(def.id, e.target.value),
          disabled: readOnly,
          placeholder: def.placeholder || '',
          className: 'input',
          id: `cf-${def.id}`,
        };

        return (
          <div key={def.id}>
            {label}
            {def.type === 'TEXTAREA' ? (
              <textarea {...commonProps} rows={3} />
            ) : def.type === 'BOOLEEN' ? (
              <div className="flex items-center gap-2">
                <button
                  role="switch"
                  aria-checked={val === 'true'}
                  onClick={() => onChange(def.id, val === 'true' ? 'false' : 'true')}
                  disabled={readOnly}
                  className={`relative w-11 h-6 rounded-full transition-colors duration-200 ${val === 'true' ? 'bg-primary-600' : 'bg-gray-300 dark:bg-gray-600'}`}
                >
                  <span className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform duration-200 ${val === 'true' ? 'translate-x-5' : ''}`} />
                </button>
                <span className="text-sm text-gray-500 dark:text-gray-400">{val === 'true' ? 'Oui' : 'Non'}</span>
              </div>
            ) : def.type === 'SELECTION' ? (
              <select {...commonProps} className="input">
                <option value="">—</option>
                {(def.options || []).map((opt) => <option key={opt} value={opt}>{opt}</option>)}
              </select>
            ) : def.type === 'SELECTION_MULTIPLE' ? (
              <div className="flex flex-wrap gap-2">
                {(def.options || []).map((opt) => {
                  const selected = (val || '').split(',').includes(opt);
                  return (
                    <button
                      key={opt}
                      type="button"
                      onClick={() => {
                        const current = (val || '').split(',').filter(Boolean);
                        const next = selected ? current.filter((x) => x !== opt) : [...current, opt];
                        onChange(def.id, next.join(','));
                      }}
                      disabled={readOnly}
                      className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-all border ${
                        selected
                          ? 'bg-primary-500/15 border-primary-500/30 text-primary-700 dark:text-primary-400'
                          : 'border-gray-200 dark:border-gray-700 text-gray-500 dark:text-gray-400 hover:border-gray-300'
                      }`}
                    >
                      {opt}
                    </button>
                  );
                })}
              </div>
            ) : def.type === 'NOMBRE' ? (
              <input {...commonProps} type="number" />
            ) : def.type === 'DATE' ? (
              <input {...commonProps} type="date" />
            ) : def.type === 'DATE_HEURE' ? (
              <input {...commonProps} type="datetime-local" />
            ) : def.type === 'EMAIL' ? (
              <input {...commonProps} type="email" />
            ) : def.type === 'URL' ? (
              <input {...commonProps} type="url" />
            ) : def.type === 'TELEPHONE' ? (
              <input {...commonProps} type="tel" />
            ) : def.type === 'FICHIER' || def.type === 'IMAGE' ? (
              <input {...commonProps} type="url" placeholder="https://…" />
            ) : (
              <input {...commonProps} type="text" />
            )}
            {def.placeholder && !val && <p className="text-[11px] text-gray-400 dark:text-gray-500 mt-1">{def.placeholder}</p>}
          </div>
        );
      })}
    </div>
  );
}