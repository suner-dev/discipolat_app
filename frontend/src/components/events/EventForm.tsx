import AttachmentPicker from '@/components/shared/AttachmentPicker';
import { tText } from '@/i18n';
import type { TypeEvenement, StatutEvenement } from '@/types';
import { X, Loader2, Pencil, Plus, Paperclip } from 'lucide-react';

/**
 * Formulaire événement d'EventsPage — extrait pour tenir la garde de dette
 * frontend (ratchet « fichiers > 1000 lignes »), comportement inchangé.
 */
/** Repli (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
export const TYPE_LABELS: Record<string, string> = {
  SORTIE: 'Sortie',
  RETRAITE: 'Retraite',
  EVANGELISATION: 'Évangélisation',
  REUNION: 'Réunion',
  VISITE: 'Visite',
  CONFERENCE: 'Conférence',
  FORMATION: 'Formation',
  ANNIVERSAIRE: 'Anniversaire',
  CULTE: 'Culte',
  ETUDE_BIBLIQUE: 'Étude biblique',
  VEILLEE: 'Veillée',
  PRIERE: 'Temps de prière',
  AUTRE: 'Autre',
};

export const STATUT_LABELS: Record<string, string> = {
  PLANIFIE: 'Planifié',
  EN_COURS: 'En cours',
  TERMINE: 'Terminé',
  ANNULE: 'Annulé',
};

export interface EventFormState {
  titre: string;
  description: string;
  typeEvenement: TypeEvenement;
  dateDebut: string;
  dateFin: string;
  lieu: string;
  limitePlaces: number | undefined;
  statut?: StatutEvenement;
  fichierIds: string[];
}

export function eventForm(
  key: string,
  form: EventFormState,
  update: (partial: Partial<EventFormState>) => void,
  submitLabel: string,
  onSubmit: () => void,
  isPending: boolean,
  onCancel: () => void,
  typeOptions: { code: string; label: string }[],
  statusOptions: { code: string; label: string }[]
) {
  const isEdit = submitLabel !== 'Créer';
  return (
    <div key={key} className="card p-6 mb-6 animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
          {isEdit ? "Modifier l'événement" : 'Nouvel événement'}
        </h3>
        {onCancel && (
          <button onClick={onCancel} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
            <X className="w-5 h-5 text-gray-500" />
          </button>
        )}
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="sm:col-span-2">
          <label className="label">Titre *</label>
          <input className="input" value={form.titre} onChange={(e) => update({ titre: e.target.value })} placeholder="Ex: Retraite de prière" />
        </div>
        <div className="sm:col-span-2">
          <label className="label">Description</label>
          <textarea className="input" rows={2} value={form.description} onChange={(e) => update({ description: e.target.value })} />
        </div>
        <div>
          <label className="label">Type *</label>
          <select className="input" value={form.typeEvenement} onChange={(e) => update({ typeEvenement: e.target.value as TypeEvenement })}>
            {typeOptions.map((o) => (
              <option key={o.code} value={o.code}>{o.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="label">Lieu</label>
          <input className="input" value={form.lieu} onChange={(e) => update({ lieu: e.target.value })} placeholder="Ex: Salle paroissiale" />
        </div>
        <div>
          <label className="label">{tText('Date de début *')}</label>
          <input type="datetime-local" className="input" value={form.dateDebut} onChange={(e) => update({ dateDebut: e.target.value })} />
        </div>
        <div>
          <label className="label">Date de fin</label>
          <input type="datetime-local" className="input" value={form.dateFin} onChange={(e) => update({ dateFin: e.target.value })} />
        </div>
        <div>
          <label className="label">Limite de places</label>
          <input type="number" min={1} className="input" value={form.limitePlaces || ''} onChange={(e) => update({ limitePlaces: e.target.value ? parseInt(e.target.value) : undefined })} placeholder={tText('Illimité si vide')} />
        </div>
        {isEdit && form.statut && (
          <div>
            <label className="label">Statut</label>
            <select className="input" value={form.statut} onChange={(e) => update({ statut: e.target.value as StatutEvenement })}>
              {statusOptions.map((o) => (
                <option key={o.code} value={o.code}>{o.label}</option>
              ))}
            </select>
          </div>
        )}
      </div>
      <div className="mt-4">
        <label className="label flex items-center gap-1.5">
          <Paperclip className="w-3.5 h-3.5 text-gray-400" /> {tText('Pièces jointes')}
        </label>
        <AttachmentPicker value={form.fichierIds} onChange={(ids) => update({ fichierIds: ids })} />
      </div>
      <div className="flex justify-end gap-3 mt-4">
        {onCancel && <button onClick={onCancel} className="btn-secondary btn-sm">{tText('Annuler')}</button>}
        <button onClick={onSubmit} disabled={!form.titre || isPending} className="btn-primary btn-sm">
          {isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : isEdit ? <Pencil className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
          {submitLabel}
        </button>
      </div>
    </div>
  );
}
