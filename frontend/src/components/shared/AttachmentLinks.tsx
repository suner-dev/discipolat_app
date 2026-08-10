import { Paperclip } from 'lucide-react';
import type { TransferAttachment } from '@/types';

interface AttachmentLinksProps {
  /** Pièces jointes de l'entité (id/fileId/nom/url, contrat AttachmentItem). */
  pieces?: TransferAttachment[];
  className?: string;
  /** Clé optionnelle portant le contexte d'origine (ex: « source » sur le dossier 360). */
  sourceKey?: string;
}

/**
 * Affichage en lecture seule des pièces jointes d'une entité (rapports soumis,
 * événements, demandes membres) avec liens cliquables vers les documents du
 * module Fichiers. Réutilisé par tous les écrans de détail/liste.
 */
export default function AttachmentLinks({ pieces, className, sourceKey }: AttachmentLinksProps) {
  if (!pieces || pieces.length === 0) return null;
  return (
    <div className={`flex flex-wrap gap-1.5 ${className ?? ''}`}>
      {pieces.map((p) => {
        const source = sourceKey ? (p as any)?.[sourceKey] : undefined;
        return (
          <a
            key={p.id ?? p.fileId}
            href={p.url}
            target="_blank"
            rel="noopener noreferrer"
            title={p.nom || p.fileId}
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300 text-xs border border-emerald-200/60 dark:border-emerald-700/40 hover:bg-emerald-100 dark:hover:bg-emerald-900/50 transition-colors"
          >
            <Paperclip className="w-3 h-3 shrink-0" />
            <span className="flex flex-col leading-tight">
              <span className="max-w-[180px] truncate">{p.nom || p.fileId}</span>
              {source && <span className="text-[9px] opacity-70 truncate max-w-[180px]">{source}</span>}
            </span>
          </a>
        );
      })}
    </div>
  );
}
