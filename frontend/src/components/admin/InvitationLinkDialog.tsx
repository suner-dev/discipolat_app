// B4 — Modale « invitation créée » : le SEUL moment où le lien d'invitation
// existe en clair. Il faut donc (1) le rendre visible, (2) permettre de le
// copier sans le sélectionner à la main, (3) dire la vérité sur l'email.
//
// `emailSent === false` n'est pas une erreur : c'est un SMTP absent. On
// l'annonce explicitement plutôt que d'afficher un « invitation envoyée » que
// personne n'a reçue.

import { useState } from 'react';
import { useI18n, tText } from '@/i18n';
import type { CreateInvitationResult } from '@/hooks/useInvitations';

interface Props {
  result: CreateInvitationResult | null;
  onClose: () => void;
}

export default function InvitationLinkDialog({ result, onClose }: Props) {
  const { t } = useI18n();
  const [copied, setCopied] = useState(false);
  if (!result) return null;

  // Membre ajouté directement : il n'y a pas de lien à envoyer.
  if (result.invitedUserId) {
    return (
      <div className="modal-overlay" role="dialog" aria-modal="true" aria-label={tText('Invitation créée')}>
        <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
          <div className="p-6">
            <p className="text-sm text-gray-700 dark:text-gray-200">{tText('Invitation créée')}</p>
            <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">{result.message ?? t('invitations.memberAdded')}</p>
            <div className="modal-footer">
              <button onClick={onClose} className="btn-primary btn-sm">{t('invitations.close')}</button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const link = result.invitationLink ?? '';

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(link);
      setCopied(true);
    } catch {
      // Presse-papiers refusé (contexte non sécurisé, permission) : le lien
      // reste affiché et sélectionnable, on ne pretend pas avoir copié.
      setCopied(false);
    }
  };

  return (
    <div
      className="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-label={tText('Invitation créée')}
      onClick={onClose}
    >
      <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
        <div className="p-6 space-y-4">
          <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">{tText('Invitation créée')}</h2>

          {result.requiresTenantSwitch && (
            <p role="status" className="rounded-lg bg-amber-50 dark:bg-amber-900/30 p-3 text-sm text-amber-800 dark:text-amber-200">
              {t('invitations.requiresSwitch')}
            </p>
          )}

          {result.emailSent === false && (
            <p role="alert" className="rounded-lg bg-red-50 dark:bg-red-900/30 p-3 text-sm text-red-800 dark:text-red-200">
              {t('invitations.emailNotSent')}
            </p>
          )}

          <div>
            <label
              htmlFor="invitation-link"
              className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-1"
            >
              {tText('Copier le lien')}
            </label>
            <div className="flex gap-2 items-center">
              <input
                id="invitation-link"
                readOnly
                value={link}
                onFocus={(e) => e.currentTarget.select()}
                className="flex-1 rounded-lg border border-gray-300 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 px-3 py-2 text-sm min-h-[44px]"
              />
              <button type="button" onClick={copy} className="btn-primary btn-sm min-h-[44px]">
                {copied ? tText('Lien copié') : tText('Copier le lien')}
              </button>
            </div>
            <p aria-live="polite" className="sr-only">
              {copied ? tText('Lien copié') : ''}
            </p>
          </div>

          <div className="modal-footer">
            <button onClick={onClose} className="btn-primary btn-sm">{t('invitations.close')}</button>
          </div>
        </div>
      </div>
    </div>
  );
}
