import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import api, { getErrorMessage } from '@/lib/api';
import type { Department, User } from '@/types';
import { ArrowLeft, Loader2, Save, Users, Building2 } from 'lucide-react';
import toast from 'react-hot-toast';

const createFamilySchema = z.object({
  nom: z.string().min(2, 'Le nom doit contenir au moins 2 caractères').max(100, 'Nom trop long'),
  departementId: z.string().min(1, 'Le département est requis'),
  chefFamilleId: z.string().min(1, 'Le chef de famille est requis'),
});

type CreateFamilyForm = z.infer<typeof createFamilySchema>;

export default function FamilyCreatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CreateFamilyForm>({
    resolver: zodResolver(createFamilySchema),
  });

  const departementId = watch('departementId');

  const { data: departments } = useQuery({
    queryKey: ['departments', 'all'],
    queryFn: async () => {
      const res = await api.get('/departments?size=100');
      return res.data.content as Department[];
    },
  });

  const { data: potentielsChefs } = useQuery({
    queryKey: ['users', 'faiseurs', departementId],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '100', role: 'FAISEUR' });
      const res = await api.get(`/users?${params}`);
      return res.data.content as User[];
    },
    enabled: !!departementId,
  });

  const createMutation = useMutation({
    mutationFn: async (data: CreateFamilyForm) => {
      const res = await api.post('/families', data);
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
    createMutation.mutate(data);
  };

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
              Créer une nouvelle famille et désigner son chef
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
          {errors.nom && (
            <p className="mt-1 text-xs text-red-500">{errors.nom.message}</p>
          )}
        </div>

        {/* Département */}
        <div className="card p-6">
          <label htmlFor="departementId" className="label flex items-center gap-2">
            <Building2 className="w-4 h-4 text-gray-400" />
            Département de rattachement
          </label>
          <select
            id="departementId"
            className={`input ${errors.departementId ? 'input-error' : ''}`}
            {...register('departementId')}
          >
            <option value="">Sélectionner un département...</option>
            {departments?.map((dept) => (
              <option key={dept.id} value={dept.id}>{dept.nom}</option>
            ))}
          </select>
          {errors.departementId && (
            <p className="mt-1 text-xs text-red-500">{errors.departementId.message}</p>
          )}
        </div>

        {/* Chef de famille */}
        <div className="card p-6">
          <label htmlFor="chefFamilleId" className="label flex items-center gap-2">
            <Users className="w-4 h-4 text-gray-400" />
            Chef de famille
          </label>
          {departementId ? (
            <>
              <select
                id="chefFamilleId"
                className={`input ${errors.chefFamilleId ? 'input-error' : ''}`}
                {...register('chefFamilleId')}
              >
                <option value="">Sélectionner un chef de famille...</option>
                {potentielsChefs
                  ?.filter((u) => !u.estChefDeFamille || u.familleGereeId === null)
                  .map((user) => (
                    <option key={user.id} value={user.id}>
                      {user.firstName} {user.lastName} ({user.email})
                    </option>
                  ))}
              </select>
              {potentielsChefs && potentielsChefs.filter((u) => !u.estChefDeFamille).length === 0 && (
                <p className="mt-2 text-sm text-amber-600 dark:text-amber-400">
                  Aucun faiseur disponible. Créez d'abord des comptes faiseurs.
                </p>
              )}
            </>
          ) : (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Sélectionnez d'abord un département
            </p>
          )}
          {errors.chefFamilleId && (
            <p className="mt-1 text-xs text-red-500">{errors.chefFamilleId.message}</p>
          )}
        </div>

        {/* Submit */}
        <div className="flex justify-end gap-3">
          <Link to="/families" className="btn-secondary">
            Annuler
          </Link>
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
