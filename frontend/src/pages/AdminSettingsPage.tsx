import { useEffect, useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import {
  Church,
  Palette,
  Type,
  Phone,
  Globe,
  Loader2,
  Save,
  RotateCcw,
  Plus,
  Trash2,
  Image as ImageIcon,
  Check,
  Building2,
  Eye,
} from 'lucide-react';
import type { ChurchSettings } from '@/types';
import { useSettings } from '@/contexts/SettingsContext';
import { shadeScale } from '@/lib/branding';

const FONT_OPTIONS = ['Inter', 'Poppins', 'Roboto', 'Open Sans', 'Montserrat', 'Playfair Display', 'Georgia'];

const SOCIAL_PRESETS = [
  { key: 'facebook', placeholder: 'https://facebook.com/votre-eglise' },
  { key: 'instagram', placeholder: 'https://instagram.com/votre-eglise' },
  { key: 'youtube', placeholder: 'https://youtube.com/@votre-eglise' },
  { key: 'whatsapp', placeholder: 'https://wa.me/243800000000' },
];

interface SettingsForm {
  churchName: string;
  platformName: string;
  slogan: string;
  description: string;
  logoUrl: string;
  faviconUrl: string;
  bannerUrl: string;
  primaryColor: string;
  accentColor: string;
  buttonColor: string;
  fontFamily: string;
  allowDarkMode: boolean;
  address: string;
  phone: string;
  email: string;
  website: string;
  contactNotes: string;
  socialLinks: Record<string, string>;
}

function ColorField({
  label, value, onChange, hint,
}: { label: string; value: string; onChange: (v: string) => void; hint?: string }) {
  const shades = useMemo(() => {
    try { return shadeScale(value); } catch { return {}; }
  }, [value]);
  return (
    <div>
      <div className="flex items-center justify-between mb-2">
        <label className="label !mb-0">{label}</label>
        <div className="flex items-center gap-2">
          <input
            type="color"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            className="w-9 h-9 rounded-lg border border-gray-200 dark:border-gray-700 bg-transparent cursor-pointer p-0.5"
            aria-label={`${label} — sélecteur`}
          />
          <input
            type="text"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            className="input !w-28 !py-1.5 font-mono text-xs uppercase"
          />
        </div>
      </div>
      <div className="flex rounded-xl overflow-hidden border border-gray-200 dark:border-gray-700 h-8">
        {[50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950].map((s) => (
          <div
            key={s}
            className="flex-1 h-full"
            style={{ backgroundColor: `rgb(${shades[String(s)] || '0 0 0'})` }}
            title={`${label} ${s}`}
          />
        ))}
      </div>
      {hint && <p className="text-[11px] text-gray-400 dark:text-gray-500 mt-1.5">{hint}</p>}
    </div>
  );
}

function Section({ icon: Icon, title, subtitle, children }: {
  icon: React.ElementType; title: string; subtitle: string; children: React.ReactNode;
}) {
  return (
    <section className="glass-card p-6">
      <div className="flex items-start gap-3 mb-5">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-sm flex-shrink-0">
          <Icon className="w-5 h-5 text-white" />
        </div>
        <div>
          <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{title}</h3>
          <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{subtitle}</p>
        </div>
      </div>
      <div className="space-y-4">{children}</div>
    </section>
  );
}

export default function AdminSettingsPage() {
  const { apply } = useSettings();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<SettingsForm | null>(null);

  const { data: settings, isLoading } = useQuery({
    queryKey: ['settings'],
    queryFn: async () => {
      const res = await api.get('/settings');
      return res.data as ChurchSettings;
    },
  });

  useEffect(() => {
    if (settings && !form) {
      setForm({
        churchName: settings.churchName,
        platformName: settings.platformName,
        slogan: settings.slogan || '',
        description: settings.description || '',
        logoUrl: settings.logoUrl || '',
        faviconUrl: settings.faviconUrl || '',
        bannerUrl: settings.bannerUrl || '',
        primaryColor: settings.primaryColor,
        accentColor: settings.accentColor,
        buttonColor: settings.buttonColor,
        fontFamily: settings.fontFamily,
        allowDarkMode: settings.allowDarkMode,
        address: settings.address || '',
        phone: settings.phone || '',
        email: settings.email || '',
        website: settings.website || '',
        contactNotes: settings.contactNotes || '',
        socialLinks: { ...(settings.socialLinks || {}) },
      });
    }
  }, [settings, form]);

  const set = <K extends keyof SettingsForm>(key: K, value: SettingsForm[K]) =>
    setForm((f) => (f ? { ...f, [key]: value } : f));

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (!form) throw new Error('Formulaire non initialisé');
      const res = await api.put('/settings', form);
      return res.data as ChurchSettings;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['settings'] });
      apply(data);
      toast.success('Identité & marque enregistrées — thème appliqué partout');
    },
    onError: () => toast.error("Erreur lors de l'enregistrement"),
  });

  const resetMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/settings/reset');
      return res.data as ChurchSettings;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['settings'] });
      setForm(null);
      apply(data);
      toast.success('Paramètres réinitialisés aux valeurs par défaut');
    },
    onError: () => toast.error('Erreur lors de la réinitialisation'),
  });

  if (isLoading || !form) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }

  const socialKeys = Object.keys(form.socialLinks);
  const missingPresets = SOCIAL_PRESETS.filter((p) => !(p.key in form.socialLinks));

  return (
    <div className="page-container max-w-4xl">
      <div className="page-header">
        <div>
          <h1 className="page-title">Identité & marque</h1>
          <p className="page-subtitle">
            Personnalisez le nom, le logo, les couleurs et les coordonnées de votre église.
            Les changements s'appliquent immédiatement à toute la plateforme — sans code.
          </p>
        </div>
        <div className="page-header-actions">
          <button
            className="btn-ghost btn-sm"
            onClick={() => resetMutation.mutate()}
            disabled={resetMutation.isPending}
          >
            <RotateCcw className="w-4 h-4" /> Réinitialiser
          </button>
          <button
            className="btn-primary btn-sm"
            onClick={() => saveMutation.mutate()}
            disabled={saveMutation.isPending}
          >
            {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
            Enregistrer
          </button>
        </div>
      </div>

      {/* Aperçu en direct */}
      <div className="glass-card p-6 mb-6">
        <div className="flex items-center gap-2 mb-4 text-xs font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500">
          <Eye className="w-4 h-4" /> Aperçu de la marque
        </div>
        <div className="flex items-center gap-4 p-4 rounded-xl border border-gray-200/60 dark:border-gray-700/60 bg-white/50 dark:bg-gray-900/40">
          <div className="w-12 h-12 rounded-xl flex items-center justify-center shadow-md" style={{ backgroundColor: `rgb(${shadeScale(form.primaryColor)[500]})` }}>
            {form.logoUrl ? (
              <img src={form.logoUrl} alt="" className="w-6 h-6 object-contain" />
            ) : (
              <Church className="w-6 h-6 text-white" />
            )}
          </div>
          <div className="min-w-0">
            <p className="font-display font-bold text-gray-900 dark:text-gray-100 truncate">
              {form.platformName || 'Discipolat'}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
              {form.slogan || form.churchName || 'Slogan de votre église'}
            </p>
          </div>
          <button className="btn-primary btn-sm ml-auto flex-shrink-0" onClick={() => saveMutation.mutate()}>
            Exemple de bouton
          </button>
        </div>
      </div>

      <div className="space-y-6">
        <Section icon={Building2} title="Identité" subtitle="Nom de l'église, de la plateforme et message public">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="label">Nom de l'église</label>
              <input className="input" value={form.churchName} onChange={(e) => set('churchName', e.target.value)} />
            </div>
            <div>
              <label className="label">Nom de la plateforme</label>
              <input className="input" value={form.platformName} onChange={(e) => set('platformName', e.target.value)} />
            </div>
          </div>
          <div>
            <label className="label">Slogan</label>
            <input className="input" value={form.slogan} onChange={(e) => set('slogan', e.target.value)} placeholder="Former des disciples de Jésus-Christ" />
          </div>
          <div>
            <label className="label">Description</label>
            <textarea className="input" rows={2} value={form.description} onChange={(e) => set('description', e.target.value)} />
          </div>
        </Section>

        <Section icon={ImageIcon} title="Logo & images" subtitle="Logo, favicon et bannière (URL hébergées)">
          <div>
            <label className="label">Logo (URL)</label>
            <input className="input" value={form.logoUrl} onChange={(e) => set('logoUrl', e.target.value)} placeholder="https://…/logo.png" />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="label">Favicon (URL)</label>
              <input className="input" value={form.faviconUrl} onChange={(e) => set('faviconUrl', e.target.value)} placeholder="https://…/favicon.ico" />
            </div>
            <div>
              <label className="label">Bannière (URL)</label>
              <input className="input" value={form.bannerUrl} onChange={(e) => set('bannerUrl', e.target.value)} placeholder="https://…/banniere.png" />
            </div>
          </div>
        </Section>

        <Section icon={Palette} title="Couleurs & thème" subtitle="La palette est générée automatiquement depuis chaque couleur de base">
          <ColorField
            label="Couleur principale"
            value={form.primaryColor}
            onChange={(v) => set('primaryColor', v)}
            hint="Utilisée pour les accents, titres et éléments actifs."
          />
          <ColorField
            label="Couleur d'accent"
            value={form.accentColor}
            onChange={(v) => set('accentColor', v)}
            hint="Utilisée pour les mises en avant et décorations."
          />
          <ColorField
            label="Couleur des boutons"
            value={form.buttonColor}
            onChange={(v) => set('buttonColor', v)}
            hint="Couleur des actions principales."
          />
          <div className="flex items-center justify-between p-3.5 rounded-xl border border-gray-200/60 dark:border-gray-700/60 bg-white/40 dark:bg-gray-900/30">
            <div>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Mode sombre autorisé</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Les utilisateurs peuvent basculer en thème sombre.</p>
            </div>
            <button
              role="switch"
              aria-checked={form.allowDarkMode}
              onClick={() => set('allowDarkMode', !form.allowDarkMode)}
              className={`relative w-11 h-6 rounded-full transition-colors duration-200 ${form.allowDarkMode ? 'bg-primary-600' : 'bg-gray-300 dark:bg-gray-600'}`}
            >
              <span className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform duration-200 ${form.allowDarkMode ? 'translate-x-5' : ''}`} />
            </button>
          </div>
        </Section>

        <Section icon={Type} title="Typographie" subtitle="Police d'écriture de toute l'application">
          <div>
            <label className="label">Police de caractères</label>
            <select className="input" value={form.fontFamily} onChange={(e) => set('fontFamily', e.target.value)}>
              {FONT_OPTIONS.map((f) => <option key={f} value={f}>{f}</option>)}
            </select>
          </div>
        </Section>

        <Section icon={Phone} title="Coordonnées" subtitle="Affichées sur la page d'accueil et les pages publiques">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="label">Adresse</label>
              <input className="input" value={form.address} onChange={(e) => set('address', e.target.value)} />
            </div>
            <div>
              <label className="label">Téléphone</label>
              <input className="input" value={form.phone} onChange={(e) => set('phone', e.target.value)} />
            </div>
            <div>
              <label className="label">Email</label>
              <input className="input" type="email" value={form.email} onChange={(e) => set('email', e.target.value)} />
            </div>
            <div>
              <label className="label">Site web</label>
              <input className="input" value={form.website} onChange={(e) => set('website', e.target.value)} />
            </div>
          </div>
          <div>
            <label className="label">Notes de contact</label>
            <textarea className="input" rows={2} value={form.contactNotes} onChange={(e) => set('contactNotes', e.target.value)} />
          </div>
        </Section>

        <Section icon={Globe} title="Réseaux sociaux" subtitle="Liens affichés sur les pages publiques">
          {socialKeys.length === 0 && (
            <p className="text-sm text-gray-400 dark:text-gray-500">
              Aucun lien. Ajoutez un réseau social ci-dessous.
            </p>
          )}
          {socialKeys.map((key) => (
            <div key={key} className="flex items-center gap-2">
              <span className="w-32 flex-shrink-0 text-sm font-medium text-gray-600 dark:text-gray-300 capitalize">{key}</span>
              <input
                className="input"
                value={form.socialLinks[key]}
                onChange={(e) => set('socialLinks', { ...form.socialLinks, [key]: e.target.value })}
                placeholder="https://…"
              />
              <button
                className="p-2 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                onClick={() => {
                  const next = { ...form.socialLinks };
                  delete next[key];
                  set('socialLinks', next);
                }}
                aria-label={`Supprimer ${key}`}
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
          {missingPresets.length > 0 && (
            <div className="flex flex-wrap gap-2 pt-1">
              {missingPresets.map((p) => (
                <button
                  key={p.key}
                  className="btn-secondary btn-sm"
                  onClick={() => set('socialLinks', { ...form.socialLinks, [p.key]: '' })}
                >
                  <Plus className="w-3.5 h-3.5" /> {p.key}
                </button>
              ))}
            </div>
          )}
        </Section>
      </div>

      <div className="sticky bottom-4 mt-6 flex justify-end">
        <button
          className="btn-primary"
          onClick={() => saveMutation.mutate()}
          disabled={saveMutation.isPending}
        >
          {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
          Appliquer les changements
        </button>
      </div>
    </div>
  );
}
