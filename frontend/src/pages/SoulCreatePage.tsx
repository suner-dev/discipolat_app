import { useState, useRef } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import type { User, Family, Soul } from '@/types';
import { ArrowLeft, Save, Loader2, Heart, Users, ClipboardList } from 'lucide-react';
import toast from 'react-hot-toast';
import { useCustomFieldForm } from '@/hooks/useCustomFieldForm';
import CustomFieldRenderer from '@/components/shared/CustomFieldRenderer';

const soulSchema = z.object({
  nom: z.string().min(1, 'Nom requis'),
  prenom: z.string().optional(),
  email: z.string().email('Email invalide').optional().or(z.literal('')),
  telephone: z.string().optional(),
  adresse: z.string().optional(),
  dateNaissance: z.string().optional(),
  profession: z.string().optional(),
  typeDisciple: z.enum(['NOUVEL_ARRIVANT', 'NOUVEAU_CONVERTI']),
  dateIntegration: z.string().min(1, "Date d'intégration requise"),
  dateConversion: z.string().optional(),
  faiseurId: z.string().min(1, 'Faiseur requis'),
  familleId: z.string().optional(),
  situationFamiliale: z.string().optional(),
  etatSpirituel: z.string().optional(),
  niveauCroissance: z.number().min(1).max(5).optional(),
}).refine(
  (data) => data.typeDisciple !== 'NOUVEAU_CONVERTI' || (data.dateConversion && data.dateConversion.length > 0),
  {
    message: 'Date de conversion requise pour un nouveau converti',
    path: ['dateConversion'],
  }
);

type SoulForm = z.infer<typeof soulSchema>;

export default function SoulCreatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const preselectedFamilleId = searchParams.get('familleId') || '';
  const preselectedFamilleNom = searchParams.get('familleNom') || '';

  const { data: faiseurs } = useQuery({
    queryKey: ['users', 'faiseurs'],
    queryFn: async () => {
      const res = await api.get('/users?role=FAISEUR&size=100');
      return res.data.content as User[];
    },
  });

  const { data: families } = useQuery({
    queryKey: ['families', 'all'],
    queryFn: async () => {
      const res = await api.get('/families?size=100');
      return res.data.content as Family[];
    },
  });

  // Champs personnalisés configurés par l'administration (type SOUL).
  const customFields = useCustomFieldForm('SOUL');
  // Résultat de la sauvegarde des champs (non fatale si échec secondaire).
  const fieldsSavedRef = useRef(true);

  const createMutation = useMutation({
    mutationFn: async (data: SoulForm) => {
      const res = await api.post('/souls', {
        ...data,
        email: data.email || undefined,
        dateIntegration: data.dateIntegration || new Date().toISOString().split('T')[0],
      });
      const soul = res.data as Soul;
      // Sauvegarde des champs personnalisés après création de la fiche.
      fieldsSavedRef.current = await customFields.save(soul.id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['souls'] });
      if (fieldsSavedRef.current) {
        toast.success('Âme créée avec succès');
      } else {
        toast.error("Âme créée, mais les champs personnalisés n'ont pas pu être enregistrés");
      }
      navigate('/souls');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<SoulForm>({
    resolver: zodResolver(soulSchema),
    defaultValues: {
      typeDisciple: 'NOUVEL_ARRIVANT',
      dateIntegration: new Date().toISOString().split('T')[0],
      faiseurId: user?.id || '',
      familleId: preselectedFamilleId || undefined,
      niveauCroissance: 1,
    },
  });

  const typeDisciple = watch('typeDisciple');

  const onSubmit = (data: SoulForm) => {
    // Validation des champs personnalisés obligatoires avant soumission.
    if (customFields.missingRequired.length > 0) {
      toast.error(`Champs personnalisés requis : ${customFields.missingRequired.join(', ')}`);
      return;
    }
    createMutation.mutate(data);
  };

  return (
    <div className="page-container max-w-2xl">
      <div className="mb-6">
        <Link to="/souls" className="btn-ghost btn-sm mb-4">
          <ArrowLeft className="w-4 h-4" />
          Retour aux âmes
        </Link>
        {preselectedFamilleId && (
          <div className="mb-4 flex items-center gap-3 p-3.5 rounded-xl bg-primary-50/70 dark:bg-primary-900/15 border border-primary-200/60 dark:border-primary-800/40 animate-slide-down">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center flex-shrink-0">
              <Users className="w-4 h-4 text-white" />
            </div>
            <div>
              <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                Nouvelle âme dans {preselectedFamilleNom || 'la famille sélectionnée'}
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                La famille est déjà assignée — vous pouvez la modifier si besoin
              </p>
            </div>
          </div>
        )}
        <div className="page-header">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
              <Heart className="w-5 h-5 text-primary-600" />
            </div>
            <div>
              <h1 className="page-title">Nouvelle âme</h1>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Créer une nouvelle fiche de disciple
              </p>
            </div>
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Identity */}
        <div className="card p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
            Identité
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="label">Nom *</label>
              <input className={`input ${errors.nom ? 'input-error' : ''}`} {...register('nom')} />
              {errors.nom && <p className="text-xs text-red-500 mt-1">{errors.nom.message}</p>}
            </div>
            <div>
              <label className="label">Prénom</label>
              <input className="input" {...register('prenom')} />
            </div>
            <div>
              <label className="label">Email</label>
              <input type="email" className={`input ${errors.email ? 'input-error' : ''}`} {...register('email')} />
              {errors.email && <p className="text-xs text-red-500 mt-1">{errors.email.message}</p>}
            </div>
            <div>
              <label className="label">Téléphone</label>
              <input className="input" {...register('telephone')} />
            </div>
            <div className="sm:col-span-2">
              <label className="label">Adresse</label>
              <input className="input" {...register('adresse')} />
            </div>
            <div>
              <label className="label">Date de naissance</label>
              <input type="date" className="input" {...register('dateNaissance')} />
            </div>
            <div>
              <label className="label">Profession</label>
              <input className="input" {...register('profession')} />
            </div>
            <div>
              <label className="label">Situation familiale</label>
              <select className="input" {...register('situationFamiliale')}>
                <option value="">Sélectionner...</option>
                <option value="CELIBATAIRE">Célibataire</option>
                <option value="MARIE">Marié(e)</option>
                <option value="DIVORCE">Divorcé(e)</option>
                <option value="VEUF">Veuf/Veuve</option>
                <option value="AUTRE">Autre</option>
              </select>
            </div>
          </div>
        </div>

        {/* Disciple info */}
        <div className="card p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
            Information de disciple
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="label">Type *</label>
              <select className="input" {...register('typeDisciple')}>
                <option value="NOUVEL_ARRIVANT">Nouvel arrivant à l'église</option>
                <option value="NOUVEAU_CONVERTI">Nouveau converti</option>
              </select>
            </div>
            <div>
              <label className="label">Date d'intégration *</label>
              <input type="date" className={`input ${errors.dateIntegration ? 'input-error' : ''}`} {...register('dateIntegration')} />
              {errors.dateIntegration && <p className="text-xs text-red-500 mt-1">{errors.dateIntegration.message}</p>}
            </div>
            {typeDisciple === 'NOUVEAU_CONVERTI' && (
              <div>
                <label className="label">Date de conversion</label>
                <input type="date" className="input" {...register('dateConversion')} />
              </div>
            )}
            <div>
              <label className="label">État spirituel</label>
              <select className="input" {...register('etatSpirituel')}>
                <option value="">Sélectionner...</option>
                <option value="NOUVEAU_CONVERTI">Nouveau converti</option>
                <option value="EN_INTEGRATION">En intégration</option>
                <option value="ACTIF">Actif</option>
                <option value="EN_VEILLE">En veille</option>
                <option value="DECROCHE">Décroché</option>
              </select>
            </div>
            <div>
              <label className="label">Niveau de croissance (1-5)</label>
              <div className="flex items-center gap-3">
                <input
                  type="range"
                  min={1}
                  max={5}
                  className="flex-1"
                  {...register('niveauCroissance', { valueAsNumber: true })}
                />
                <span className="text-sm font-medium text-gray-700 dark:text-gray-300 w-8 text-center">
                  {watch('niveauCroissance') || 1}
                </span>
              </div>
              <div className="flex justify-between text-xs text-gray-500 mt-1">
                <span>Débutant</span>
                <span>Avancé</span>
              </div>
            </div>
            <div>
              <label className="label">Faiseur assigné *</label>
              <select className={`input ${errors.faiseurId ? 'input-error' : ''}`} {...register('faiseurId')}>
                <option value="">Sélectionner...</option>
                {faiseurs?.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.firstName} {f.lastName} {f.estChefDeFamille ? '(Chef)' : ''}
                  </option>
                ))}
              </select>
              {errors.faiseurId && <p className="text-xs text-red-500 mt-1">{errors.faiseurId.message}</p>}
            </div>
            <div>
              <label className="label">Famille (optionnel)</label>
              <select className="input" {...register('familleId')}>
                <option value="">Sélectionner...</option>
                {families?.map((fam) => (
                  <option key={fam.id} value={fam.id}>{fam.nom}</option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* Champs personnalisés (configurables par l'admin) */}
        {customFields.definitions.length > 0 && (
          <div className="card p-6">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1 flex items-center gap-2">
              <ClipboardList className="w-5 h-5 text-primary-500" />
              Informations complémentaires
            </h3>
            <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">
              Champs ajoutés par votre église
            </p>
            <CustomFieldRenderer
              definitions={customFields.definitions}
              values={customFields.values}
              onChange={customFields.setValue}
              readOnlyFieldIds={customFields.readOnlyFieldIds}
            />
            {customFields.missingRequired.length > 0 && (
              <p className="text-xs text-red-500 mt-3">
                Champs obligatoires à renseigner : {customFields.missingRequired.join(', ')}
              </p>
            )}
          </div>
        )}

        {/* Submit */}
        <div className="flex justify-end gap-3">
          <Link to="/souls" className="btn-secondary">
            Annuler
          </Link>
          <button
            type="submit"
            disabled={isSubmitting || createMutation.isPending}
            className="btn-primary"
          >
            {isSubmitting || createMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Save className="w-4 h-4" />
            )}
            Créer l'âme
          </button>
        </div>
      </form>
    </div>
  );
}
