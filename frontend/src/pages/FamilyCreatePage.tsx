import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import api, { getErrorMessage } from '@/lib/api';
import type { User } from '@/types';
import { ArrowLeft, Loader2, Save, Users, UserPlus, CheckCircle2 } from 'lucide-react';
import toast from 'react-hot-toast';

const createFamilySchema = z.object({
  nom: z.string().min(2, 'Le nom doit contenir au moins 2 caractères').max(100, 'Nom trop long'),
  // Cas 1 : chef existant
  chefFamilleId: z.string().optional(),
  // Cas 2 : nouveau chef
  newChefFirstName: z.string().optional(),
  newChefLastName: z.string().optional(),
  newChefEmail: z.string().email('Email invalide').optional().or(z.literal('')),
  newChefPhone: z.string().optional(),
  newChefSexe: z.string().optional(),
});

type CreateFamilyForm = z.infer<typeof createFamilySchema>;

export default function FamilyCreatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<'existing' | 'new'>('existing');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CreateFamilyForm>({
    resolver: zodResolver(createFamilySchema),
  });

  const { data: potentielsChefs } = useQuery({
    queryKey: ['users', 'chefs-disponibles'],
    queryFn: async () => {
      const res = await api.get('/users?size=100');
      return res.data.content as User[];
    },
  });

  const createMutation = useMutation({
    mutationFn: async (data: CreateFamilyForm) => {
      const payload: any = { nom: data.nom };
      if (mode === 'existing') {
        payload.chefFamilleId = data.chefFamilleId;
      } else {
        payload.createNewChef = true;
        payload.newChefFirstName = data.newChefFirstName;
        payload.newChefLastName = data.newChefLastName;
        payload.newChefEmail = data.newChefEmail;
        payload.newChefPhone = data.newChefPhone;
        payload.newChefSexe = data.newChefSexe;
      }
      const res = await api.post('/families', payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['families'] });
      toast.success('Famille créée avec succès');
      navigate('/families');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const onSubmit = async (data: CreateFamilyForm) => {
    // Validation selon le mode
    if (mode === 'existing' && !data.chefFamilleId) {
      toast.error('Sélectionnez un chef existant ou basculez en mode « créer un chef »');
      return;
    }
    if (mode === 'new' && (!data.newChefFirstName || !data.newChefLastName || !data.newChefEmail)) {
      toast.error('Renseignez nom, prénom et email du nouveau chef');
      return;
    }
    createMutation.mutate(data);
  };

  const chefsDisponibles = (potentielsChefs ?? []).filter(
    (u) => !u.estChefDeFamille || u.familleGereeId === null
  );

  return (
    <div className="page-container max-w-2xl mx-auto">
      <Link to="/families" className="btn-ghost btn-sm mb-4">
        <ArrowLeft className="w-4 h-4" />
        Retour aux familles
      </Link>

      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
            <Users className="w-6 h-6 text-primary-600" />
          </div>
          <div>
            <h1 className="page-title">Nouvelle famille de disciples</h1>
            <p className="text-gray-500 dark:text-gray-400 text-sm">
              Créer une famille avec un chef existant ou créer directement un nouveau chef
            </p>
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Nom */}
        <div className="card p-6">
          <label htmlFor="nom" className="label">Nom de la famille</label>
          <input
            id="nom"
            className={`input ${errors.nom ? 'input-error' : ''}`}
            placeholder="Ex: Famille Emmanuel"
            {...register('nom')}
          />
          {errors.nom && <p className="mt-1 text-xs text-red-500">{errors.nom.message}</p>}
        </div>

        {/* Mode selection */}
        <div className="card p-6">
          <label className="label">Chef de famille</label>
          <div className="grid grid-cols-2 gap-2 mb-4">
            <button
              type="button"
              onClick={() => setMode('existing')}
              className={`p-3 rounded-xl text-sm font-medium transition-all border-2
                ${mode === 'existing'
                  ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300'
                  : 'border-gray-200 dark:border-gray-700 text-gray-500 hover:border-gray-300'}`}
            >
              <CheckCircle2 className="w-4 h-4 inline mr-1.5 -mt-0.5" />
              Sélectionner un chef
            </button>
            <button
              type="button"
              onClick={() => setMode('new')}
              className={`p-3 rounded-xl text-sm font-medium transition-all border-2
                ${mode === 'new'
                  ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300'
                  : 'border-gray-200 dark:border-gray-700 text-gray-500 hover:border-gray-300'}`}
            >
              <UserPlus className="w-4 h-4 inline mr-1.5 -mt-0.5" />
              Créer un nouveau chef
            </button>
          </div>

          {mode === 'existing' ? (
            <>
              <select
                className={`input ${errors.chefFamilleId ? 'input-error' : ''}`}
                {...register('chefFamilleId')}
                defaultValue=""
              >
                <option value="">Sélectionner un chef de famille...</option>
                {chefsDisponibles.map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.firstName} {user.lastName} ({user.email})
                  </option>
                ))}
              </select>
              {chefsDisponibles.length === 0 && (
                <p className="mt-2 text-sm text-amber-600 dark:text-amber-400">
                  Aucun chef disponible. Basculez sur « Créer un nouveau chef » pour créer le compte en même temps que la famille.
                </p>
              )}
            </>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="label">Prénom *</label>
                <input className={`input ${errors.newChefFirstName ? 'input-error' : ''}`} placeholder="Prénom du chef"
                  {...register('newChefFirstName')} />
              </div>
              <div>
                <label className="label">Nom *</label>
                <input className={`input ${errors.newChefLastName ? 'input-error' : ''}`} placeholder="Nom du chef"
                  {...register('newChefLastName')} />
              </div>
              <div>
                <label className="label">Email *</label>
                <input type="email" className={`input ${errors.newChefEmail ? 'input-error' : ''}`} placeholder="chef@eglise.com"
                  {...register('newChefEmail')} />
                {errors.newChefEmail && <p className="mt-1 text-xs text-red-500">{errors.newChefEmail.message}</p>}
              </div>
              <div>
                <label className="label">Téléphone</label>
                <input className="input" placeholder="+241 ..." {...register('newChefPhone')} />
              </div>
              <div>
                <label className="label">Sexe</label>
                <select className="input" {...register('newChefSexe')}>
                  <option value="">Non précisé</option>
                  <option value="M">Masculin</option>
                  <option value="F">Féminin</option>
                </select>
              </div>
              <div className="flex items-end">
                <p className="text-[11px] text-gray-400 pb-2">
                  Le compte du chef sera créé automatiquement avec le rôle CHEF_DE_FAMILLE et affecté à cette famille.
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Submit */}
        <div className="flex justify-end gap-3">
          <Link to="/families" className="btn-secondary">Annuler</Link>
          <button
            type="submit"
            disabled={createMutation.isPending || isSubmitting}
            className="btn-primary"
          >
            {createMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Save className="w-4 h-4" />
            )}
            {createMutation.isPending ? 'Création...' : 'Créer la famille'}
          </button>
        </div>
      </form>
    </div>
  );
}
