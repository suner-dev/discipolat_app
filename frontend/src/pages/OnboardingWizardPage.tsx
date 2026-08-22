import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import Toast from '@/components/shared/Toast';
import { Church, Users, Settings, Rocket, CheckCircle2, ArrowRight, ArrowLeft, Upload } from 'lucide-react';

const STEPS = [
  { key: 'identity', icon: Church, title: 'Identité de l\'église', description: 'Configurez le nom, le logo et les couleurs' },
  { key: 'import', icon: Upload, title: 'Import des membres', description: 'Importez votre liste de membres depuis un fichier CSV' },
  { key: 'structure', icon: Users, title: 'Structure', description: 'Créez vos départements et familles' },
  { key: 'roles', icon: Settings, title: 'Rôles & Permissions', description: 'Configurez les rôles de votre église' },
  { key: 'launch', icon: Rocket, title: 'Lancement', description: 'Vous êtes prêt à commencer !' },
];

const CHURCH_SIZES = [
  { key: 'small', label: 'Petite église', description: 'Moins de 50 membres', members: '20-50' },
  { key: 'medium', label: 'Église moyenne', description: '50 à 200 membres', members: '50-200' },
  { key: 'large', label: 'Grande église', description: 'Plus de 200 membres', members: '200+' },
];

export default function OnboardingWizardPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [churchSize, setChurchSize] = useState('medium');
  const [formData, setFormData] = useState({
    churchName: '',
    primaryColor: '#22c55e',
    accentColor: '#f59e0b',
    departments: [
      { name: 'Jeunesse', description: 'Département des jeunes' },
      { name: 'Louange', description: 'Équipe de louange' },
      { name: 'Accueil', description: 'Accueil des visiteurs' },
      { name: 'Prières', description: 'Ministère de prière' },
    ],
    importFile: null as File | null,
  });

  const handleNext = () => {
    if (step === 0 && !formData.churchName.trim()) {
      Toast.warning('Veuillez entrer le nom de votre église');
      return;
    }
    if (step < STEPS.length - 1) setStep(step + 1);
  };

  const handleBack = () => {
    if (step > 0) setStep(step - 1);
  };

  const handleFinish = async () => {
    try {
      // Save church settings
      await api.put('/settings', {
        churchName: formData.churchName,
        primaryColor: formData.primaryColor,
        accentColor: formData.accentColor,
      });
      // Create departments
      for (const dept of formData.departments) {
        if (dept.name.trim()) {
          await api.post('/departments', { nom: dept.name, description: dept.description });
        }
      }
      Toast.success('Configuration sauvegardée avec succès !');
      navigate('/dashboard');
    } catch {
      Toast.error('Erreur lors de la sauvegarde');
    }
  };

  const addDepartment = () => {
    setFormData({
      ...formData,
      departments: [...formData.departments, { name: '', description: '' }],
    });
  };

  const updateDepartment = (index: number, field: string, value: string) => {
    const updated = [...formData.departments];
    updated[index] = { ...updated[index], [field]: value };
    setFormData({ ...formData, departments: updated });
  };

  const removeDepartment = (index: number) => {
    setFormData({
      ...formData,
      departments: formData.departments.filter((_, i) => i !== index),
    });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 via-white to-emerald-50 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800 p-4 md:p-8">
      <div className="max-w-3xl mx-auto">
        {/* Progress */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            {STEPS.map((s, i) => {
              const StepIcon = s.icon;
              return (
                <React.Fragment key={s.key}>
                  <div className="flex flex-col items-center">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center transition-all ${
                      i <= step
                        ? 'bg-green-600 text-white'
                        : 'bg-gray-200 dark:bg-white/10 text-gray-400'
                    }`}>
                      {i < step ? <CheckCircle2 className="w-5 h-5" /> : <StepIcon className="w-5 h-5" />}
                    </div>
                    <span className="text-xs mt-1 text-gray-500 dark:text-gray-400 hidden sm:block">{s.title}</span>
                  </div>
                  {i < STEPS.length - 1 && (
                    <div className={`flex-1 h-0.5 mx-2 transition-all ${
                      i < step ? 'bg-green-600' : 'bg-gray-200 dark:bg-white/10'
                    }`} />
                  )}
                </React.Fragment>
              );
            })}
          </div>
        </div>

        {/* Step Content */}
        <div className="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-white/10 p-8 shadow-xl">
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            {STEPS[step].title}
          </h2>
          <p className="text-gray-500 dark:text-gray-400 mb-6">
            {STEPS[step].description}
          </p>

          {/* Step 0: Identity */}
          {step === 0 && (
            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Nom de l'église *
                </label>
                <input
                  type="text"
                  value={formData.churchName}
                  onChange={e => setFormData({ ...formData, churchName: e.target.value })}
                  className="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-green-500 text-lg"
                  placeholder="Ex: Église de la Grâce"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Taille de l'église
                </label>
                <div className="grid grid-cols-3 gap-3">
                  {CHURCH_SIZES.map(size => (
                    <button
                      key={size.key}
                      onClick={() => setChurchSize(size.key)}
                      className={`p-4 rounded-xl border text-left transition-all ${
                        churchSize === size.key
                          ? 'border-green-500 bg-green-50 dark:bg-green-500/10'
                          : 'border-gray-200 dark:border-white/10 hover:border-gray-300'
                      }`}
                    >
                      <div className="font-medium text-gray-900 dark:text-white text-sm">{size.label}</div>
                      <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">{size.members} membres</div>
                    </button>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Couleur principale
                  </label>
                  <div className="flex items-center gap-3">
                    <input
                      type="color"
                      value={formData.primaryColor}
                      onChange={e => setFormData({ ...formData, primaryColor: e.target.value })}
                      className="w-10 h-10 rounded-lg cursor-pointer"
                    />
                    <span className="text-sm text-gray-500">{formData.primaryColor}</span>
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Couleur d'accent
                  </label>
                  <div className="flex items-center gap-3">
                    <input
                      type="color"
                      value={formData.accentColor}
                      onChange={e => setFormData({ ...formData, accentColor: e.target.value })}
                      className="w-10 h-10 rounded-lg cursor-pointer"
                    />
                    <span className="text-sm text-gray-500">{formData.accentColor}</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Step 1: Import */}
          {step === 1 && (
            <div className="space-y-6">
              <div className="border-2 border-dashed border-gray-300 dark:border-white/10 rounded-xl p-8 text-center">
                <Upload className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
                  Importez votre liste de membres
                </h3>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                  Format CSV accepté : nom, prénom, email, téléphone, famille, département
                </p>
                <label className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-green-600 text-white text-sm font-medium hover:bg-green-700 cursor-pointer transition-all">
                  <Upload className="w-4 h-4" />
                  Choisir un fichier
                  <input
                    type="file"
                    accept=".csv"
                    className="hidden"
                    onChange={e => setFormData({ ...formData, importFile: e.target.files?.[0] || null })}
                  />
                </label>
                {formData.importFile && (
                  <p className="text-sm text-green-600 mt-3">✓ {formData.importFile.name}</p>
                )}
              </div>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                💡 Vous pouvez ignorer cette étape et ajouter vos membres plus tard depuis l'administration.
              </p>
            </div>
          )}

          {/* Step 2: Structure */}
          {step === 2 && (
            <div className="space-y-4">
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Départements suggérés pour une église {churchSize === 'small' ? 'petite' : churchSize === 'medium' ? 'moyenne' : 'grande'} :
              </p>
              {formData.departments.map((dept, i) => (
                <div key={i} className="flex gap-3 items-start">
                  <div className="flex-1 grid grid-cols-2 gap-3">
                    <input
                      type="text"
                      value={dept.name}
                      onChange={e => updateDepartment(i, 'name', e.target.value)}
                      className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                      placeholder="Nom du département"
                    />
                    <input
                      type="text"
                      value={dept.description}
                      onChange={e => updateDepartment(i, 'description', e.target.value)}
                      className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                      placeholder="Description"
                    />
                  </div>
                  <button
                    onClick={() => removeDepartment(i)}
                    className="px-2 py-2 text-red-400 hover:text-red-600 transition-colors"
                  >
                    ✕
                  </button>
                </div>
              ))}
              <button
                onClick={addDepartment}
                className="w-full py-3 rounded-xl border-2 border-dashed border-gray-300 dark:border-white/10 text-gray-500 dark:text-gray-400 text-sm hover:border-green-400 hover:text-green-600 transition-all"
              >
                + Ajouter un département
              </button>
            </div>
          )}

          {/* Step 3: Roles */}
          {step === 3 && (
            <div className="space-y-4">
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Les rôles par défaut ont été configurés. Vous pourrez les personnaliser plus tard dans Administration → Permissions.
              </p>
              {[
                { role: 'ADMIN', desc: 'Super-utilisateur — accès complet', color: 'bg-red-100 text-red-700' },
                { role: 'PASTEUR', desc: 'Centre de commandement — vue globale', color: 'bg-purple-100 text-purple-700' },
                { role: 'RESPONSABLE', desc: 'Gestionnaire RH de département', color: 'bg-blue-100 text-blue-700' },
                { role: 'CHEF_DE_FAMILLE', desc: 'Leader spirituel de famille', color: 'bg-green-100 text-green-700' },
                { role: 'FAISEUR', desc: 'Discipleur — suivi des âmes', color: 'bg-amber-100 text-amber-700' },
                { role: 'MEMBRE', desc: 'Espace personnel du disciple', color: 'bg-gray-100 text-gray-700' },
              ].map(r => (
                <div key={r.role} className="flex items-center gap-4 p-4 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
                  <span className={`px-3 py-1 rounded-full text-xs font-semibold ${r.color}`}>{r.role}</span>
                  <div>
                    <div className="text-sm font-medium text-gray-900 dark:text-white">{r.desc}</div>
                  </div>
                  <CheckCircle2 className="w-5 h-5 text-green-500 ml-auto" />
                </div>
              ))}
            </div>
          )}

          {/* Step 4: Launch */}
          {step === 4 && (
            <div className="text-center py-8">
              <div className="w-20 h-20 rounded-full bg-green-100 dark:bg-green-500/20 flex items-center justify-center mx-auto mb-6">
                <Rocket className="w-10 h-10 text-green-600" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-2">
                🎉 Vous êtes prêt !
              </h3>
              <p className="text-gray-500 dark:text-gray-400 max-w-md mx-auto mb-6">
                Votre église <strong>{formData.churchName || 'Discipolat'}</strong> est configurée avec {formData.departments.length} départements.
                Vous pouvez commencer à ajouter des membres et à explorer les fonctionnalités.
              </p>
              <div className="grid grid-cols-2 gap-3 max-w-sm mx-auto text-left">
                {[
                  { label: 'Ajouter des membres', icon: Users },
                  { label: 'Créer un événement', icon: Rocket },
                  { label: 'Configurer les modules', icon: Settings },
                  { label: 'Inviter des utilisateurs', icon: Church },
                ].map((item, i) => (
                  <div key={i} className="flex items-center gap-2 p-3 rounded-xl bg-gray-50 dark:bg-white/5 text-sm text-gray-700 dark:text-gray-300">
                    <item.icon className="w-4 h-4 text-green-600" />
                    {item.label}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Navigation */}
        <div className="flex justify-between mt-6">
          <button
            onClick={handleBack}
            disabled={step === 0}
            className="flex items-center gap-2 px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-white/5 transition-all text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <ArrowLeft className="w-4 h-4" />
            Précédent
          </button>
          {step < STEPS.length - 1 ? (
            <button
              onClick={handleNext}
              className="flex items-center gap-2 px-6 py-2 rounded-xl bg-green-600 text-white text-sm font-medium hover:bg-green-700 transition-all"
            >
              Suivant
              <ArrowRight className="w-4 h-4" />
            </button>
          ) : (
            <button
              onClick={handleFinish}
              className="flex items-center gap-2 px-6 py-2 rounded-xl bg-gradient-to-r from-green-600 to-emerald-600 text-white text-sm font-medium hover:from-green-700 hover:to-emerald-700 transition-all shadow-lg shadow-green-500/25"
            >
              <Rocket className="w-4 h-4" />
              Lancer Discipolat
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
