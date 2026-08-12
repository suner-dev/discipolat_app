import { useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import api, { getErrorMessage } from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { Soul, TypeDisciple, StatutAme } from '@/types';
import { ArrowLeft, Loader2, Save, Heart } from 'lucide-react';
import toast from 'react-hot-toast';

const editSoulSchema = z.object({
  nom: z.string().min(1, 'Le nom est requis'),
  prenom: z.string().optional(),
  email: z.string().email('Email invalide').optional().or(z.literal('')),
  telephone: z.string().optional(),
  adresse: z.string().optional(),
  dateNaissance: z.string().optional(),
  profession: z.string().optional(),
  typeDisciple: z.enum(['NOUVEL_ARRIVANT', 'NOUVEAU_CONVERTI']),
  statut: z.enum(['NOUVEAU_CONVERTI', 'NOUVEL_ARRIVANT', 'EN_INTEGRATION', 'ACTIF', 'EN_VEILLE', 'DECROCHE']),
  dateConversion: z.string().optional(),
  notesPasteur: z.string().optional(),
});

type EditSoulForm = z.infer<typeof editSoulSchema>;

/** Repli (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const STATUT_FALLBACK: { value: StatutAme; label: string }[] = [
  { value: 'NOUVEAU_CONVERTI', label: 'Nouveau converti' },
  { value: 'NOUVEL_ARRIVANT', label: 'Nouvel arrivant' },
  { value: 'EN_INTEGRATION', label: 'En intégration' },
  { value: 'ACTIF', label: 'Actif' },
  { value: 'EN_VEILLE', label: 'En veille' },
  { value: 'DECROCHE', label: 'Décroché' },
];

export default function SoulEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const dictionaries = useDictionaries();

  const typeEntries = useMemo(() => {
    const configured = dictionaries.options('SOUL_TYPE');
    return configured.length > 0 ? configured.map((e) => ({ code: e.code, label: e.label })) : [{ code: 'NOUVEL_ARRIVANT', label: 'Nouvel arrivant' }, { code: 'NOUVEAU_CONVERTI', label: 'Nouveau converti' }];
  }, [dictionaries]);

  const statutEntries = useMemo(() => {
    const configured = dictionaries.options('SOUL_STATUS');
    return configured.length > 0 ? configured.map((e) => ({ value: e.code as StatutAme, label: e.label })) : STATUT_FALLBACK;
  }, [dictionaries]);

  const { data: soul, isLoading } = useQuery({
    queryKey: ['soul', id],
    queryFn: async () => {
      const res = await api.get(`/souls/${id}`);
      return res.data as Soul;
    },
    enabled: !!id,
  });

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<EditSoulForm>({
    resolver: zodResolver(editSoulSchema),
    values: soul ? {
      nom: soul.nom,
      prenom: soul.prenom || '',
      email: soul.email || '',
      telephone: soul.telephone || '',
      adresse: soul.adresse || '',
      dateNaissance: soul.dateNaissance ? soul.dateNaissance.split('T')[0] : '',
      profession: soul.profession || '',
      typeDisciple: soul.typeDisciple,
      statut: soul.statut,
      dateConversion: soul.dateConversion ? soul.dateConversion.split('T')[0] : '',
      notesPasteur: soul.notesPasteur || '',
    } : undefined,
  });

  const updateMutation = useMutation({
    mutationFn: async (data: EditSoulForm) => {
      await api.put(`/souls/${id}`, {
        ...data,
        dateNaissance: data.dateNaissance || undefined,
        dateConversion: data.dateConversion || undefined,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['soul', id] });
      queryClient.invalidateQueries({ queryKey: ['souls'] });
      toast.success('Âme mise à jour avec succès');
      navigate(`/souls/${id}`);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const onSubmit = async (data: EditSoulForm) => {
    updateMutation.mutate(data);
  };

  if (isLoading) {
    return (
      <div className="page-container">
        <div className="animate-pulse space-y-4">
          <div className="skeleton h-8 w-48" />
          <div className="skeleton h-96 w-full" />
        </div>
      </div>
    );
  }

  if (!soul) {
    return (
      <div className="page-container">
        <p className="text-gray-500">Âme non trouvée</p>
      </div>
    );
  }

  return (
    <div className="page-container max-w-2xl mx-auto">
      <Link to={`/souls/${id}`} className="btn-ghost btn-sm mb-4">
        <ArrowLeft className="w-4 h-4" />
        Retour au détail
      </Link>

      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
            <Heart className="w-6 h-6 text-primary-600" />
          </div>
          <div>
            <h1 className="page-title">Modifier l'âme</h1>
            <p className="text-gray-500 dark:text-gray-400 text-sm">
              {soul.prenom ? `${soul.prenom} ${soul.nom}` : soul.nom}
            </p>
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Identité */}
        <div className="card p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Identité</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="label">Nom *</label>
              <input className={`input ${errors.nom ? 'input-error' : ''}`} {...register('nom')} />
              {errors.nom && <p className="mt-1 text-xs text-red-500">{errors.nom.message}</p>}
            </div>
            <div>
              <label className="label">Prénom</label>
              <input className="input" {...register('prenom')} />
            </div>
          </div>
        </div>

        {/* Contact */}
        <div className="card p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Contact</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="label">Email</label>
              <input type="email" className={`input ${errors.email ? 'input-error' : ''}`} {...register('email')} />
              {errors.email && <p className="mt-1 text-xs text-red-500">{errors.email.message}</p>}
            </div>
            <div>
              <label className="label">Téléphone</label>
              <input className="input" {...register('telephone')} />
            </div>
            <div className="sm:col-span-2">
              <label className="label">Adresse</label>
              <input className="input" {...register('adresse')} />
            </div>
          </div>
        </div>

        {/* Infos supplémentaires */}
        <div className="card p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Informations supplémentaires</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="label">Date de naissance</label>
              <input type="date" className="input" {...register('dateNaissance')} />
            </div>
            <div>
              <label className="label">Profession</label>
              <input className="input" {...register('profession')} />
            </div>
          </div>
        </div>

        {/* Parcours spirituel */}
        <div className="card p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Parcours spirituel</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="label">Type de disciple</label>
              <select className="input" {...register('typeDisciple')}>
                {typeEntries.map((o) => (<option key={o.code} value={o.code}>{o.label}</option>))}
              </select>
            </div>
            <div>
              <label className="label">État spirituel</label>
              <select className="input" {...register('statut')}>
                {statutEntries.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Date de conversion</label>
              <input type="date" className="input" {...register('dateConversion')} />
            </div>
          </div>
        </div>

        {/* Notes du pasteur */}
        <div className="card p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Notes pastorales</h3>
          <textarea
            className="input"
            rows={4}
            {...register('notesPasteur')}
            placeholder="Notes visibles par le pasteur uniquement..."
          />
        </div>

        {/* Submit */}
        <div className="flex justify-end gap-3">
          <Link to={`/souls/${id}`} className="btn-secondary">
            Annuler
          </Link>
          <button
            type="submit"
            disabled={updateMutation.isPending || isSubmitting}
            className="btn-primary"
          >
            {updateMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Save className="w-4 h-4" />
            )}
            {updateMutation.isPending ? 'Enregistrement...' : 'Enregistrer les modifications'}
          </button>
        </div>
      </form>
    </div>
  );
}
