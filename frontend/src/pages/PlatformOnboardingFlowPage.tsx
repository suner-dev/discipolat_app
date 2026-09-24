import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  Rocket, Building2, Users, Home as HomeIcon, ArrowLeft, ArrowRight,
  CheckCircle2, Circle, Loader2, Sparkles, ClipboardList,
} from 'lucide-react';

/* ============================================================================
 * Super Admin — Flux de provisionnement guidé (pas à pas, 100 % cliquable) :
 *   1. Tenant → 2. Église → 3. Département → 4. Famille → Récapitulatif
 * Les quatre formulaires sont regroupés et envoyés dans une transaction serveur unique.
 * ========================================================================== */

const STEPS = [
  { id: 'tenant', title: 'Organisation', subtitle: 'Tenant / plan', icon: Rocket },
  { id: 'church', title: 'Église', subtitle: 'Église racine', icon: Building2 },
  { id: 'department', title: 'Département', subtitle: 'Structure', icon: Users },
  { id: 'family', title: 'Famille', subtitle: 'Unité de base', icon: HomeIcon },
] as const;

interface TenantResult { id: string; name: string; slug: string; plan: string; status: string }
interface ChurchResult { id: string; tenantId: string; name: string; code: string; path: string }
interface DepartmentResult { id: string; tenantId: string; nom: string; responsableId: string | null }
interface FamilyResult { id: string; tenantId: string; nom: string; chefFamilleId: string | null }
interface PlanOption { key: string; name: string }

const slugify = (value: string) =>
  value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '').slice(0, 50);

export default function PlatformOnboardingFlowPage() {
  const navigate = useNavigate();

  // Étape courante (0..3 = formulaires, 4 = récapitulatif)
  const [step, setStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  // Résultats des étapes déjà créées (conditionnent l'accès aux étapes suivantes)
  const [tenant, setTenant] = useState<TenantResult | null>(null);
  const [church, setChurch] = useState<ChurchResult | null>(null);
  const [department, setDepartment] = useState<DepartmentResult | null>(null);
  const [family, setFamily] = useState<FamilyResult | null>(null);

  // Formulaires
  const [tenantForm, setTenantForm] = useState({
    name: '', slug: '', plan: 'free', country: 'CM', currency: 'XAF',
    timezone: 'Africa/Douala', locale: 'fr',
  });
  const [slugTouched, setSlugTouched] = useState(false);
  const [churchForm, setChurchForm] = useState({ name: '' });
  const [deptForm, setDeptForm] = useState({
    nom: '', description: '', mode: 'new' as 'new' | 'existing',
    newRespFirstName: '', newRespLastName: '', newRespEmail: '', newRespPhone: '',
    responsableId: '',
  });
  const [famForm, setFamForm] = useState({
    nom: '', mode: 'new' as 'new' | 'existing',
    newChefFirstName: '', newChefLastName: '', newChefEmail: '', newChefPhone: '',
    newChefSexe: '', newChefDateNaissance: '', newChefAdresse: '',
    chefFamilleId: '',
  });

  const { data: plans = [] } = useQuery({
    queryKey: ['platform-plans'],
    queryFn: async () => (await api.get('/platform/admin/plans')).data as PlanOption[],
    retry: 1,
  });
  const planOptions = plans.length > 0 ? plans : [{ key: 'free', name: 'Free' }];

  // Slug auto-tant que l'utilisateur n'a pas édité manuellement
  const effectiveSlug = slugTouched ? tenantForm.slug : slugify(tenantForm.name);

  const setTenantName = (value: string) => {
    setTenantForm((prev) => ({ ...prev, name: value, slug: slugTouched ? prev.slug : slugify(value) }));
  };

  const validateStep = (index: number): string | null => {
    if (index === 0) {
      if (!tenantForm.name.trim()) return "Le nom de l'organisation est requis";
      if (!/^[a-z0-9-]+$/.test(effectiveSlug)) return 'Slug invalide (lettres minuscules, chiffres, tirets)';
      return null;
    }
    if (index === 1) {
      if (!churchForm.name.trim()) return "Le nom de l'église est requis";
      return null;
    }
    if (index === 2) {
      if (!deptForm.nom.trim()) return 'Le nom du département est requis';
      if (deptForm.mode === 'existing' && !deptForm.responsableId.trim())
        return "L'ID du responsable existant est requis";
      if (deptForm.mode === 'new') {
        if (!deptForm.newRespFirstName.trim() || !deptForm.newRespLastName.trim())
          return 'Prénom et nom du responsable requis';
        if (!/^\S+@\S+\.\S+$/.test(deptForm.newRespEmail)) return 'Email du responsable invalide';
      }
      return null;
    }
    if (index === 3) {
      if (!famForm.nom.trim()) return 'Le nom de la famille est requis';
      if (famForm.mode === 'existing' && !famForm.chefFamilleId.trim())
        return "L'ID du chef de famille existant est requis";
      if (famForm.mode === 'new') {
        if (!famForm.newChefFirstName.trim() || !famForm.newChefLastName.trim())
          return 'Prénom et nom du chef requis';
        if (!/^\S+@\S+\.\S+$/.test(famForm.newChefEmail)) return 'Email du chef invalide';
      }
      return null;
    }
    return null;
  };

  const canReachStep = (index: number): boolean => {
    if (index === 0) return true;
    if (index === 4) return step === 4;
    for (let prerequisite = 0; prerequisite < index; prerequisite += 1) {
      if (validateStep(prerequisite) !== null) return false;
    }
    return true;
  };

  const reachable = [canReachStep(0), canReachStep(1), canReachStep(2), canReachStep(3), canReachStep(4)];
  const completedCount = step === 4 ? 4 : step;

  const submitStep = async () => {
    const error = validateStep(step);
    if (error) { toast.error(error); return; }
    setSubmitting(true);
    try {
      if (step < 3) {
        if (step === 0 && !churchForm.name) {
          setChurchForm({ name: `${tenantForm.name.trim()} — Église principale` });
        }
        if (step === 2 && !famForm.nom) {
          setFamForm((form) => ({ ...form, nom: `${tenantForm.name.trim()} — Famille modèle` }));
        }
        setStep((current) => Math.min(current + 1, 4));
        return;
      }

      const { data } = await api.post('/platform/admin/provisioning', {
        ...tenantForm,
        slug: effectiveSlug,
        churchName: churchForm.name.trim(),
        departmentName: deptForm.nom.trim(),
        departmentDescription: deptForm.description.trim() || null,
        ...(deptForm.mode === 'existing'
          ? { responsableId: deptForm.responsableId.trim() }
          : {
              createNewResponsable: true,
              newResponsableFirstName: deptForm.newRespFirstName.trim(),
              newResponsableLastName: deptForm.newRespLastName.trim(),
              newResponsableEmail: deptForm.newRespEmail.trim(),
              newResponsablePhone: deptForm.newRespPhone.trim() || null,
            }),
        familyName: famForm.nom.trim(),
        ...(famForm.mode === 'existing'
          ? { chefFamilleId: famForm.chefFamilleId.trim() }
          : {
              createNewChef: true,
              newChefFirstName: famForm.newChefFirstName.trim(),
              newChefLastName: famForm.newChefLastName.trim(),
              newChefEmail: famForm.newChefEmail.trim(),
              newChefPhone: famForm.newChefPhone.trim() || null,
              newChefSexe: famForm.newChefSexe || null,
              newChefDateNaissance: famForm.newChefDateNaissance || null,
              newChefAdresse: famForm.newChefAdresse.trim() || null,
            }),
      });
      setTenant(data.tenant as TenantResult);
      setChurch(data.church as ChurchResult);
      setDepartment(data.department as DepartmentResult);
      setFamily(data.family as FamilyResult);
      toast.success('Organisation provisionnée !');
      setStep(4);
    } catch (e) {
      toast.error(getErrorMessage(e));
    } finally {
      setSubmitting(false);
    }
  };

  const resetAll = () => {
    setStep(0); setTenant(null); setChurch(null); setDepartment(null); setFamily(null);
    setTenantForm({ name: '', slug: '', plan: 'free', country: 'CM', currency: 'XAF', timezone: 'Africa/Douala', locale: 'fr' });
    setSlugTouched(false);
    setChurchForm({ name: '' });
    setDeptForm({ nom: '', description: '', mode: 'new', newRespFirstName: '', newRespLastName: '', newRespEmail: '', newRespPhone: '', responsableId: '' });
    setFamForm({ nom: '', mode: 'new', newChefFirstName: '', newChefLastName: '', newChefEmail: '', newChefPhone: '', newChefSexe: '', newChefDateNaissance: '', newChefAdresse: '', chefFamilleId: '' });
  };


  const inputCls =
    'w-full px-3 py-2.5 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-100 text-sm focus:outline-none focus:ring-2 focus:ring-violet-500';
  const labelCls = 'block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1';

  return (
    <div className="page-container max-w-4xl mx-auto">
      {/* En-tête */}
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg">
          <Rocket className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Provisionnement guidé</h1>
          <p className="page-subtitle">
            Créez une organisation complète en 4 étapes : tenant, église, département, famille.
          </p>
        </div>
      </div>

      {/* Stepper — scrollable sur mobile, aligné sur desktop */}
      <div className="glass-card p-4 mb-6 overflow-x-auto">
        <ol className="flex items-center gap-1 sm:gap-2 min-w-[520px] sm:min-w-0">
          {STEPS.map((s, idx) => {
             const done = step > idx;

            const active = step === idx;
            const clickable = reachable[idx];
            const Icon = s.icon;
            return (
              <li key={s.id} className="flex items-center flex-1">
                <button
                  type="button"
                  disabled={!clickable}
                  onClick={() => clickable && setStep(idx)}
                  className={`flex items-center gap-2 px-2 sm:px-3 py-2 rounded-xl text-sm transition
                    ${active ? 'bg-violet-600 text-white shadow' :
                      done ? 'bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300' :
                      clickable ? 'hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-600 dark:text-gray-300' :
                      'text-gray-400 dark:text-gray-600 cursor-not-allowed'}`}
                >
                  {done && !active ? <CheckCircle2 className="w-5 h-5 shrink-0" />
                    : active ? <Icon className="w-5 h-5 shrink-0" />
                    : <Circle className="w-5 h-5 shrink-0" />}
                  <span className="hidden sm:inline font-medium whitespace-nowrap">
                    {idx + 1}. {s.title}
                  </span>
                </button>
                {idx < STEPS.length - 1 && (
                  <ArrowRight className="w-4 h-4 mx-1 text-gray-300 dark:text-gray-600 shrink-0" />
                )}
              </li>
            );
          })}
          <li className="flex items-center">
            <span className={`px-3 py-2 rounded-xl text-sm font-medium whitespace-nowrap
              ${step === 4 ? 'bg-gradient-to-r from-violet-600 to-purple-600 text-white' :
                'bg-gray-100 dark:bg-gray-800 text-gray-500'}`}>
              Récap
            </span>
          </li>
        </ol>
      </div>

      {/* Progression */}
      <div className="glass-card p-4 mb-6">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs text-gray-500">Progression</span>
          <span className="text-xs font-medium text-gray-700 dark:text-gray-300">
            {completedCount}/4 étapes créées
          </span>
        </div>
        <div className="h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-violet-500 to-purple-500 rounded-full transition-all duration-500"
            style={{ width: `${(completedCount / 4) * 100}%` }}
          />
        </div>
      </div>


      {/* ============================ ÉTAPE 1 : TENANT ============================ */}
      {step === 0 && (
        <div className="glass-card p-6">
          <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-1">
            1. Organisation (tenant)
          </h2>
          <p className="text-sm text-gray-500 mb-5">
            Identité de l'organisation sur la plateforme : nom, plan et localisation.
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className={labelCls}>Nom de l'organisation *</label>
              <input className={inputCls} value={tenantForm.name}
                onChange={(e) => setTenantName(e.target.value)}
                placeholder="Ex. : Église Évangélique Bethel" />
            </div>
            <div>
              <label className={labelCls}>Slug (URL)</label>
              <input className={inputCls} value={effectiveSlug}
                onChange={(e) => { setSlugTouched(true); setTenantForm((p) => ({ ...p, slug: e.target.value })); }}
                placeholder="ex. : eglise-bethel" />
            </div>
            <div>
              <label className={labelCls}>Plan</label>
              <select className={inputCls} value={tenantForm.plan}
                onChange={(e) => setTenantForm((p) => ({ ...p, plan: e.target.value }))}>
                {planOptions.map((p) => (
                  <option key={p.key} value={p.key}>{p.name} ({p.key})</option>
                ))}
              </select>
            </div>
            <div>
              <label className={labelCls}>Pays</label>
              <input className={inputCls} value={tenantForm.country}
                onChange={(e) => setTenantForm((p) => ({ ...p, country: e.target.value }))} />
            </div>
            <div>
              <label className={labelCls}>Devise</label>
              <input className={inputCls} value={tenantForm.currency}
                onChange={(e) => setTenantForm((p) => ({ ...p, currency: e.target.value }))} />
            </div>
            <div>
              <label className={labelCls}>Fuseau horaire</label>
              <input className={inputCls} value={tenantForm.timezone}
                onChange={(e) => setTenantForm((p) => ({ ...p, timezone: e.target.value }))} />
            </div>
            <div>
              <label className={labelCls}>Langue</label>
              <select className={inputCls} value={tenantForm.locale}
                onChange={(e) => setTenantForm((p) => ({ ...p, locale: e.target.value }))}>
                <option value="fr">Français</option>
                <option value="en">English</option>
                <option value="pt">Português</option>
                <option value="es">Español</option>
              </select>
            </div>
          </div>
          <div className="flex justify-end mt-6">
            <button onClick={submitStep} disabled={submitting} className="btn-primary inline-flex items-center gap-2">
              {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
              Continuer
            </button>
          </div>
        </div>
      )}


      {/* ============================ ÉTAPE 2 : ÉGLISE ============================ */}
      {step === 1 && (
        <div className="glass-card p-6">
          <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-1">
            2. Église racine
          </h2>
          <p className="text-sm text-gray-500 mb-5">
             Organisationne l'arborescence de <strong>{tenantForm.name.trim() || 'votre organisation'}</strong> autour de son église racine.

          </p>
          <div>
            <label className={labelCls}>Nom de l'église *</label>
            <input className={inputCls} value={churchForm.name}
              onChange={(e) => setChurchForm({ name: e.target.value })}
              placeholder="Ex. : Bethel — Église principale" />
          </div>
          <div className="flex justify-between mt-6">
            <button onClick={() => setStep(0)} disabled={submitting}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800">
              <ArrowLeft className="w-4 h-4" /> Retour
            </button>
            <button onClick={submitStep} disabled={submitting} className="btn-primary inline-flex items-center gap-2">
              {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
              Continuer
            </button>
          </div>
        </div>
      )}

      {/* ========================= ÉTAPE 3 : DÉPARTEMENT ========================= */}
      {step === 2 && (
        <div className="glass-card p-6">
          <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-1">
            3. Département
          </h2>
          <p className="text-sm text-gray-500 mb-5">
             Première structure de <strong>{tenantForm.name.trim() || 'votre organisation'}</strong> — un responsable est obligatoire.

          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className={labelCls}>Nom du département *</label>
              <input className={inputCls} value={deptForm.nom}
                onChange={(e) => setDeptForm((p) => ({ ...p, nom: e.target.value }))}
                placeholder="Ex. : Accueil & Louange" />
            </div>
            <div className="sm:col-span-2">
              <label className={labelCls}>Description</label>
              <textarea className={inputCls} rows={2} value={deptForm.description}
                onChange={(e) => setDeptForm((p) => ({ ...p, description: e.target.value }))} />
            </div>
            <div className="sm:col-span-2">
              <label className={labelCls}>Responsable *</label>
              <div className="flex gap-2 mb-3">
                <button type="button"
                  onClick={() => setDeptForm((p) => ({ ...p, mode: 'new' }))}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition
                    ${deptForm.mode === 'new' ? 'bg-violet-600 text-white border-violet-600'
                      : 'border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300'}`}>
                  Créer un nouveau responsable
                </button>
                <button type="button"
                  onClick={() => setDeptForm((p) => ({ ...p, mode: 'existing' }))}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition
                    ${deptForm.mode === 'existing' ? 'bg-violet-600 text-white border-violet-600'
                      : 'border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300'}`}>
                  Responsable existant
                </button>
              </div>
              {deptForm.mode === 'existing' ? (
                <div>
                  <label className={labelCls}>ID du responsable (UUID) *</label>
                  <input className={inputCls} value={deptForm.responsableId}
                    onChange={(e) => setDeptForm((p) => ({ ...p, responsableId: e.target.value }))}
                    placeholder="uuid-existant" />
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <input className={inputCls} placeholder="Prénom *"
                    value={deptForm.newRespFirstName}
                    onChange={(e) => setDeptForm((p) => ({ ...p, newRespFirstName: e.target.value }))} />
                  <input className={inputCls} placeholder="Nom *"
                    value={deptForm.newRespLastName}
                    onChange={(e) => setDeptForm((p) => ({ ...p, newRespLastName: e.target.value }))} />
                  <input className={inputCls} placeholder="Email *" type="email"
                    value={deptForm.newRespEmail}
                    onChange={(e) => setDeptForm((p) => ({ ...p, newRespEmail: e.target.value }))} />
                  <input className={inputCls} placeholder="Téléphone"
                    value={deptForm.newRespPhone}
                    onChange={(e) => setDeptForm((p) => ({ ...p, newRespPhone: e.target.value }))} />
                </div>
              )}
            </div>
          </div>
          <div className="flex justify-between mt-6">
            <button onClick={() => setStep(1)} disabled={submitting}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800">
              <ArrowLeft className="w-4 h-4" /> Retour
            </button>
            <button onClick={submitStep} disabled={submitting} className="btn-primary inline-flex items-center gap-2">
              {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
              Continuer
            </button>
          </div>
        </div>
      )}


      {/* ============================ ÉTAPE 4 : FAMILLE ============================ */}
      {step === 3 && (
        <div className="glass-card p-6">
          <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-1">
            4. Famille
          </h2>
          <p className="text-sm text-gray-500 mb-5">
             Première famille de <strong>{tenantForm.name.trim() || 'votre organisation'}</strong> — un chef de famille est obligatoire.

          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className={labelCls}>Nom de la famille *</label>
              <input className={inputCls} value={famForm.nom}
                onChange={(e) => setFamForm((p) => ({ ...p, nom: e.target.value }))}
                placeholder="Ex. : Famille Mbarga" />
            </div>
            <div className="sm:col-span-2">
              <label className={labelCls}>Chef de famille *</label>
              <div className="flex gap-2 mb-3">
                <button type="button"
                  onClick={() => setFamForm((p) => ({ ...p, mode: 'new' }))}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition
                    ${famForm.mode === 'new' ? 'bg-violet-600 text-white border-violet-600'
                      : 'border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300'}`}>
                  Créer un nouveau chef
                </button>
                <button type="button"
                  onClick={() => setFamForm((p) => ({ ...p, mode: 'existing' }))}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition
                    ${famForm.mode === 'existing' ? 'bg-violet-600 text-white border-violet-600'
                      : 'border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300'}`}>
                  Chef existant
                </button>
              </div>
              {famForm.mode === 'existing' ? (
                <div>
                  <label className={labelCls}>ID du chef de famille (UUID) *</label>
                  <input className={inputCls} value={famForm.chefFamilleId}
                    onChange={(e) => setFamForm((p) => ({ ...p, chefFamilleId: e.target.value }))}
                    placeholder="uuid-existant" />
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <input className={inputCls} placeholder="Prénom *"
                    value={famForm.newChefFirstName}
                    onChange={(e) => setFamForm((p) => ({ ...p, newChefFirstName: e.target.value }))} />
                  <input className={inputCls} placeholder="Nom *"
                    value={famForm.newChefLastName}
                    onChange={(e) => setFamForm((p) => ({ ...p, newChefLastName: e.target.value }))} />
                  <input className={inputCls} placeholder="Email *" type="email"
                    value={famForm.newChefEmail}
                    onChange={(e) => setFamForm((p) => ({ ...p, newChefEmail: e.target.value }))} />
                  <input className={inputCls} placeholder="Téléphone"
                    value={famForm.newChefPhone}
                    onChange={(e) => setFamForm((p) => ({ ...p, newChefPhone: e.target.value }))} />
                  <select className={inputCls} value={famForm.newChefSexe}
                    onChange={(e) => setFamForm((p) => ({ ...p, newChefSexe: e.target.value }))}>
                    <option value="">Sexe (optionnel)</option>
                    <option value="M">Masculin</option>
                    <option value="F">Féminin</option>
                  </select>
                  <input className={inputCls} type="date" value={famForm.newChefDateNaissance}
                    onChange={(e) => setFamForm((p) => ({ ...p, newChefDateNaissance: e.target.value }))} />
                  <input className={inputCls + ' sm:col-span-2'} placeholder="Adresse"
                    value={famForm.newChefAdresse}
                    onChange={(e) => setFamForm((p) => ({ ...p, newChefAdresse: e.target.value }))} />
                </div>
              )}
            </div>
          </div>
          <div className="flex justify-between mt-6">
            <button onClick={() => setStep(2)} disabled={submitting}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800">
              <ArrowLeft className="w-4 h-4" /> Retour
            </button>
            <button onClick={submitStep} disabled={submitting} className="btn-primary inline-flex items-center gap-2">
              {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
              Provisionner l'organisation
            </button>
          </div>
        </div>
      )}


      {/* ============================ RÉCAPITULATIF ============================ */}
      {step === 4 && (
        <div className="glass-card p-6">
          <div className="text-center mb-6">
            <CheckCircle2 className="w-12 h-12 text-green-500 mx-auto mb-3" />
            <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100">
              Organisation provisionnée !
            </h2>
            <p className="text-sm text-gray-500 mt-1">
              Les 4 étapes ont été créées avec succès — chaque action est journalisée en audit.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="rounded-xl border border-gray-200 dark:border-gray-700 p-4">
              <div className="flex items-center gap-2 text-violet-600 dark:text-violet-400 mb-2">
                <Rocket className="w-4 h-4" />
                <span className="text-xs font-semibold uppercase tracking-wide">Tenant</span>
              </div>
              <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{tenant?.name}</p>
              <p className="text-xs text-gray-500 mt-1">slug : {tenant?.slug} · plan : {tenant?.plan}</p>
              <p className="text-[11px] text-gray-400 mt-1 font-mono break-all">id : {tenant?.id}</p>
            </div>
            <div className="rounded-xl border border-gray-200 dark:border-gray-700 p-4">
              <div className="flex items-center gap-2 text-violet-600 dark:text-violet-400 mb-2">
                <Building2 className="w-4 h-4" />
                <span className="text-xs font-semibold uppercase tracking-wide">Église</span>
              </div>
              <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{church?.name}</p>
              <p className="text-xs text-gray-500 mt-1">code : {church?.code}</p>
              <p className="text-[11px] text-gray-400 mt-1 font-mono break-all">id : {church?.id}</p>
            </div>
            <div className="rounded-xl border border-gray-200 dark:border-gray-700 p-4">
              <div className="flex items-center gap-2 text-violet-600 dark:text-violet-400 mb-2">
                <Users className="w-4 h-4" />
                <span className="text-xs font-semibold uppercase tracking-wide">Département</span>
              </div>
              <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{department?.nom}</p>
              <p className="text-[11px] text-gray-400 mt-1 font-mono break-all">id : {department?.id}</p>
            </div>
            <div className="rounded-xl border border-gray-200 dark:border-gray-700 p-4">
              <div className="flex items-center gap-2 text-violet-600 dark:text-violet-400 mb-2">
                <HomeIcon className="w-4 h-4" />
                <span className="text-xs font-semibold uppercase tracking-wide">Famille</span>
              </div>
              <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{family?.nom}</p>
              <p className="text-[11px] text-gray-400 mt-1 font-mono break-all">id : {family?.id}</p>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row justify-between gap-3 mt-6">
            <button onClick={resetAll}
              className="inline-flex items-center justify-center gap-2 px-4 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800">
              <ClipboardList className="w-4 h-4" /> Nouveau provisionnement
            </button>
            <div className="flex gap-3">
              <button onClick={() => navigate('/platform/tenants')}
                className="inline-flex items-center justify-center gap-2 px-4 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800">
                Voir les organisations
              </button>
              <button onClick={() => navigate('/platform/dashboard')} className="btn-primary inline-flex items-center gap-2">
                <ArrowRight className="w-4 h-4" /> Tableau de bord plateforme
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

