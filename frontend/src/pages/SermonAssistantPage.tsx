import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import api from '@/lib/api';
import { Loader2, BookOpen, Sparkles, Copy, RefreshCw } from 'lucide-react';
import toast from 'react-hot-toast';

interface Outline {
  title: string;
  hook: string;
  structure: { heading: string; content: string }[];
  application: string;
}

const AUDIENCES = [
  { value: 'JEUNES', label: 'Jeunes' },
  { value: 'FAMILLES', label: 'Familles' },
  { value: 'FAISEURS', label: 'Faiseurs' },
];

const THEMES = [
  'La foi qui déplace les montagnes',
  'Le pardon libérateur',
  'Marcher dans la lumière',
  "L'intégrité dans le travail",
  'La puissance de la prière',
];

/** Prédicateur IA — générateur de plans de sermons par audience. */
export default function SermonAssistantPage() {
  const [theme, setTheme] = useState('');
  const [audience, setAudience] = useState('JEUNES');
  const [outlines, setOutlines] = useState<Outline[]>([]);

  const generateMutation = useMutation({
    mutationFn: async () =>
      (
        await api.post<{ theme: string; audience: string; outlines: Outline[] }>(
          '/sermon-assistant/outlines',
          { theme: theme.trim(), audience },
        )
      ).data,
    onSuccess: (r) => {
      setOutlines(r.outlines);
    },
    onError: () => toast.error("Impossible de générer les plans — vérifiez votre connexion"),
  });

  const copyOutline = (o: Outline) => {
    const text = [
      o.title,
      '',
      `ACCROCHE : ${o.hook}`,
      '',
      'STRUCTURE :',
      ...o.structure.map((s, i) => `${i + 1}. ${s.heading}\n   ${s.content}`),
      '',
      `APPLICATION : ${o.application}`,
    ].join('\n');
    navigator.clipboard?.writeText(text);
    toast.success('Plan copié');
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-indigo-500 to-blue-600 text-white shadow-lg">
          <BookOpen className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Prédicateur IA</h1>
          <p className="page-subtitle">Générez des plans de sermons adaptés à chaque audience</p>
        </div>
      </div>

      {/* Formulaire */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (theme.trim().length >= 3) generateMutation.mutate();
        }}
        className="glass-card p-6 mb-6 animate-slide-up"
      >
        <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-2 block">
          Thème du sermon
        </label>
        <input
          className="input w-full mb-4"
          placeholder="Ex. La foi qui déplace les montagnes…"
          value={theme}
          onChange={(e) => setTheme(e.target.value)}
        />

        {/* Suggestions */}
        <div className="flex flex-wrap gap-2 mb-4">
          {THEMES.map((t) => (
            <button
              key={t} type="button"
              onClick={() => setTheme(t)}
              className={`btn-sm px-3 py-1.5 rounded-full text-xs border transition-colors ${
                theme === t
                  ? 'bg-primary-500 text-white border-primary-500'
                  : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 hover:border-primary-400'
              }`}
            >
              {t}
            </button>
          ))}
        </div>

        <div className="flex flex-col md:flex-row gap-3 md:items-center">
          <select className="input md:w-48" value={audience} onChange={(e) => setAudience(e.target.value)}>
            {AUDIENCES.map((a) => (
              <option key={a.value} value={a.value}>Audience : {a.label}</option>
            ))}
          </select>
          <button
            type="submit"
            disabled={generateMutation.isPending || theme.trim().length < 3}
            className="btn-primary flex items-center justify-center gap-2 md:w-auto"
          >
            {generateMutation.isPending ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <Sparkles className="w-5 h-5" />
            )}
            Générer 3 plans
          </button>
        </div>
      </form>

      {/* Résultats */}
      <div className="grid gap-4 md:grid-cols-3">
        {outlines.map((o, i) => (
          <div key={i} className="glass-card p-6 flex flex-col animate-slide-up" style={{ animationDelay: `${i * 80}ms` }}>
            <h3 className="font-bold text-gray-900 dark:text-gray-100 mb-3">{o.title}</h3>

            <p className="text-sm italic text-primary-600 dark:text-primary-400 mb-4">« {o.hook} »</p>

            <ol className="space-y-3 flex-1">
              {o.structure.map((s, j) => (
                <li key={j} className="flex gap-3">
                  <span className="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-300 flex items-center justify-center text-xs font-bold shrink-0">
                    {j + 1}
                  </span>
                  <div>
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{s.heading}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{s.content}</p>
                  </div>
                </li>
              ))}
            </ol>

            <div className="mt-4 p-3 rounded-xl bg-emerald-50 dark:bg-emerald-900/20">
              <p className="text-xs font-semibold text-emerald-700 dark:text-emerald-400 mb-1">Application pratique</p>
              <p className="text-sm text-gray-700 dark:text-gray-300">{o.application}</p>
            </div>

            <button onClick={() => copyOutline(o)} className="btn-secondary btn-sm mt-4 flex items-center justify-center gap-2">
              <Copy className="w-4 h-4" /> Copier ce plan
            </button>
          </div>
        ))}
      </div>

      {!generateMutation.isPending && outlines.length === 0 && !generateMutation.isError && (
        <div className="glass-card p-12 text-center">
          <RefreshCw className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
          <p className="text-gray-500 dark:text-gray-400 max-w-md mx-auto">
            Entrez un thème et choisissez une audience : l'assistant propose trois structures différentes
            (exposée, narrative, interactive) avec accroche, points bibliques et applications concrètes.
          </p>
        </div>
      )}
    </div>
  );
}
