import { useMemo, useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import AttachmentPicker from '@/components/shared/AttachmentPicker';
import type {
  TransferType, Soul, Family, Department, User, TransferConfiguration, PrioriteTransfert,
} from '@/types';
import { TRANSFER_TYPE_LABELS } from '@/types';
import { useDictionaries } from '@/hooks/useDictionaries';
import { ArrowLeft, ArrowLeftRight, Loader2, Send, User as UserIcon, Heart, Users, Building2, Paperclip } from 'lucide-react';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
}

/** Méta par type de transfert : entité « personne concernée » et entité « nouvelle affectation ». */
const TYPE_META: Record<TransferType, {
  personneType: 'SOUL' | 'USER';
  personneLabel: string;
  targetKind: 'FAMILLE' | 'DEPARTEMENT' | 'FAISEUR';
  targetLabel: string;
}> = {
  MEMBRE_DEPARTEMENT_TRANSFERT: { personneType: 'SOUL', personneLabel: 'Membre (âme) à transférer', targetKind: 'DEPARTEMENT', targetLabel: 'Département de destination' },
  MEMBRE_DEPARTEMENT_AJOUT: { personneType: 'SOUL', personneLabel: 'Membre (âme) à ajouter', targetKind: 'DEPARTEMENT', targetLabel: 'Département d\u2019ajout' },
  MEMBRE_DEPARTEMENT_RETRAIT: { personneType: 'SOUL', personneLabel: 'Membre (âme) à retirer', targetKind: 'DEPARTEMENT', targetLabel: 'Département de retrait' },
  DISCIPLE_FAMILLE_TRANSFERT: { personneType: 'SOUL', personneLabel: 'Disciple (âme) à transférer', targetKind: 'FAMILLE', targetLabel: 'Famille de destination' },
  FAISEUR_FAMILLE_TRANSFERT: { personneType: 'USER', personneLabel: 'Faiseur à transférer', targetKind: 'FAMILLE', targetLabel: 'Famille de destination' },
  CHEF_FAMILLE_TRANSFERT: { personneType: 'USER', personneLabel: 'Nouveau chef de famille', targetKind: 'FAMILLE', targetLabel: 'Famille concernée' },
  FAISEUR_DISCIPLE_CHANGEMENT: { personneType: 'SOUL', personneLabel: 'Disciple (âme)', targetKind: 'FAISEUR', targetLabel: 'Nouveau faiseur' },
  RESPONSABLE_DEPARTEMENT_CHANGEMENT: { personneType: 'USER', personneLabel: 'Nouveau responsable', targetKind: 'DEPARTEMENT', targetLabel: 'Département concerné' },
  CHEF_ADJOINT_CHANGEMENT: { personneType: 'USER', personneLabel: 'Nouveau chef adjoint', targetKind: 'FAMILLE', targetLabel: 'Famille concernée' },
};

export default function TransferCreatePage() {
  const navigate = useNavigate();
  const dictionaries = useDictionaries();

  const { data: configs, isLoading: loadingConfigs } = useQuery({
    queryKey: ['transfers-configurations'],
    queryFn: async () => {
      const res = await api.get('/transfers/configurations');
      return res.data as TransferConfiguration[];
    },
  });

  const [type, setType] = useState<TransferType | ''>('');
  const [personneId, setPersonneId] = useState('');
  const [targetId, setTargetId] = useState('');
  const [justification, setJustification] = useState('');
  const [priorite, setPriorite] = useState<PrioriteTransfert>('MOYENNE');
  const [commentaires, setCommentaires] = useState('');
  const [soumettreDirectement, setSoumettreDirectement] = useState(true);
  const [fichierIds, setFichierIds] = useState<string[]>([]);

  const meta = type ? TYPE_META[type] : null;

  // Données de sélection
  const { data: souls } = useQuery({
    queryKey: ['transfers', 'souls'],
    queryFn: async () => (await api.get('/souls?size=200')).data.content as Soul[],
    enabled: !!meta && meta.personneType === 'SOUL',
  });
  const { data: users } = useQuery({
    queryKey: ['transfers', 'users'],
    queryFn: async () => (await api.get('/users?size=200')).data.content as User[],
    enabled: !!meta && meta.personneType === 'USER',
  });
  const { data: families } = useQuery({
    queryKey: ['transfers', 'families'],
    queryFn: async () => (await api.get('/families?size=200')).data.content as Family[],
    enabled: !!meta && meta.targetKind === 'FAMILLE',
  });
  const { data: departments } = useQuery({
    queryKey: ['transfers', 'departments'],
    queryFn: async () => (await api.get('/departments?size=200')).data.content as Department[],
    enabled: !!meta && meta.targetKind === 'DEPARTEMENT',
  });
  const faiseurs = useMemo(() => users?.filter(u => u.roles?.includes('FAISEUR') || u.activeRole === 'FAISEUR'), [users]);

  const personnes = meta?.personneType === 'SOUL' ? souls : users;
  const cibles = meta?.targetKind === 'FAMILLE' ? families
    : meta?.targetKind === 'DEPARTEMENT' ? departments
    : faiseurs;

  const entityName = (c?: unknown): string => {
    const o = c as { nom?: string; firstName?: string; lastName?: string } | undefined;
    if (!o) return '';
    if (o.nom) return o.nom;
    return `${o.firstName ?? ''} ${o.lastName ?? ''}`.trim();
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!type || !meta || !personneId || !targetId) throw new Error('Formulaire incomplet');
      const cible = cibles?.find(c => c.id === targetId);
      const res = await api.post('/transfers', {
        type,
        personneId,
        personneType: meta.personneType,
        nouvelleAffectation: { type: meta.targetKind, id: targetId, nom: entityName(cible) },
        justification,
        priorite,
        commentaires: commentaires || undefined,
        fichierIds: fichierIds.length > 0 ? fichierIds : undefined,
      });
      const id = res.data.id;
      if (soumettreDirectement) {
        await api.post(`/transfers/${id}/submit`);
      }
      return id;
    },
    onSuccess: (id) => {
      toast.success(soumettreDirectement ? 'Demande soumise au circuit de validation' : 'Brouillon enregistré');
      navigate(`/transfers/${id}`);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const availableTypes = configs?.filter(c => c.actif && c.canInitier) ?? [];

  return (
    <div className="page-container max-w-3xl">
      <div className="page-header">
        <Link to="/transfers" className="btn-ghost btn-sm mb-2">
          <ArrowLeft className="w-4 h-4" />
          Retour aux transferts
        </Link>
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg">
            <ArrowLeftRight className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Nouvelle demande de transfert</h1>
            <p className="page-subtitle">Le circuit de validation est défini par le workflow configuré par le pasteur</p>
          </div>
        </div>
      </div>

      {loadingConfigs ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : availableTypes.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <p className="text-gray-500 mb-2">Aucun type de transfert n'est disponible pour votre rôle actif.</p>
          <Link to="/transfers" className="btn-secondary btn-sm">Retour</Link>
        </div>
      ) : (
        <div className="glass-card p-6 space-y-5">
          {/* Type */}
          <div>
            <label className="label">Type de transfert *</label>
            <select
              className="input"
              value={type}
              onChange={(e) => { setType(e.target.value as TransferType); setPersonneId(''); setTargetId(''); setFichierIds([]); }}
            >
              <option value="">Sélectionner un type...</option>
              {availableTypes.map(c => (
                <option key={c.type} value={c.type}>{dictionaries.label('TRANSFER_TYPE', c.type) || TRANSFER_TYPE_LABELS[c.type] || c.type}</option>
              ))}
            </select>
            {type && (
              <p className="text-xs text-gray-500 mt-1.5">
                Circuit : {configs?.find(c => c.type === type)?.etapes.join(' → ') || 'Aucune étape (exécution immédiate)'}
              </p>
            )}
          </div>

          {meta && (
            <>
              {/* Personne concernée */}
              <div>
                <label className="label">{meta.personneLabel} *</label>
                <div className="relative">
                  {meta.personneType === 'SOUL'
                    ? <Heart className="absolute left-3 top-3 w-4 h-4 text-gray-400" />
                    : <UserIcon className="absolute left-3 top-3 w-4 h-4 text-gray-400" />}
                  <select className="input pl-10" value={personneId} onChange={(e) => setPersonneId(e.target.value)}>
                    <option value="">Sélectionner...</option>
                    {personnes?.map((p) => {
                      const o = p as Soul | User;
                      const nom = (o as Soul).nom
                        ? `${(o as Soul).prenom ?? ''} ${(o as Soul).nom}`.trim()
                        : `${(o as User).firstName ?? ''} ${(o as User).lastName ?? ''}`.trim();
                      return <option key={o.id} value={o.id}>{nom}</option>;
                    })}
                  </select>
                </div>
              </div>

              {/* Nouvelle affectation */}
              <div>
                <label className="label">{meta.targetLabel} *</label>
                <div className="relative">
                  {meta.targetKind === 'FAMILLE'
                    ? <Users className="absolute left-3 top-3 w-4 h-4 text-gray-400" />
                    : meta.targetKind === 'DEPARTEMENT'
                      ? <Building2 className="absolute left-3 top-3 w-4 h-4 text-gray-400" />
                      : <UserIcon className="absolute left-3 top-3 w-4 h-4 text-gray-400" />}
                  <select className="input pl-10" value={targetId} onChange={(e) => setTargetId(e.target.value)}>
                    <option value="">Sélectionner...</option>
                    {cibles?.map((c) => {
                      const o = c as Family | Department | User;
                      const nom = (o as Family).nom
                        ? (o as Family | Department).nom
                        : `${(o as User).firstName ?? ''} ${(o as User).lastName ?? ''}`.trim();
                      return <option key={o.id} value={o.id}>{nom}</option>;
                    })}
                  </select>
                </div>
              </div>

              {/* Justification */}
              <div>
                <label className="label">Justification détaillée *</label>
                <textarea
                  className="input"
                  rows={3}
                  value={justification}
                  onChange={(e) => setJustification(e.target.value)}
                  placeholder="Motifs pastoraux, organisationnels, situation du disciple..."
                  required
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="label">Priorité</label>
                  <select className="input" value={priorite} onChange={(e) => setPriorite(e.target.value as PrioriteTransfert)}>
                    {(dictionaries.options('TRANSFER_PRIORITE').length > 0
                      ? dictionaries.options('TRANSFER_PRIORITE')
                      : [{ code: 'BASSE', label: 'Basse' }, { code: 'MOYENNE', label: 'Moyenne' }, { code: 'HAUTE', label: 'Haute' }, { code: 'URGENTE', label: 'Urgente' }]
                    ).map((o) => (
                      <option key={o.code} value={o.code}>{o.label}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="label">Commentaires</label>
                  <input
                    className="input"
                    value={commentaires}
                    onChange={(e) => setCommentaires(e.target.value)}
                    placeholder="Informations complémentaires"
                  />
                </div>
              </div>

              {/* Pièces jointes */}
              <div>
                <label className="label flex items-center gap-2">
                  <Paperclip className="w-3.5 h-3.5 text-gray-400" />
                  Pièces jointes {fichierIds.length > 0 && <span className="badge badge-primary">{fichierIds.length}</span>}
                </label>
                <AttachmentPicker value={fichierIds} onChange={setFichierIds} />
                <p className="text-xs text-gray-400 mt-1.5">Sélectionnez des documents du module Documents (rapports, comptes rendus...).</p>
              </div>

              <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300 cursor-pointer">
                <input
                  type="checkbox"
                  checked={soumettreDirectement}
                  onChange={(e) => setSoumettreDirectement(e.target.checked)}
                  className="rounded"
                />
                Soumettre immédiatement au circuit de validation
              </label>

              <div className="flex justify-end gap-3 pt-2">
                <Link to="/transfers" className="btn-secondary">Annuler</Link>
                <button
                  onClick={() => createMutation.mutate()}
                  disabled={createMutation.isPending || !personneId || !targetId || !justification.trim()}
                  className="btn-primary"
                >
                  {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                  {soumettreDirectement ? 'Créer et soumettre' : 'Enregistrer le brouillon'}
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
