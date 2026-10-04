import { useEffect, useState } from 'react';
import { CalendarDays, MapPin, Megaphone } from 'lucide-react';
import api from '@/lib/api';
import { tText } from '@/i18n';

/**
 * SPEC_ONBOARDING_FLOWS (FE-3) — carrousel des annonces publiques approuvées,
 * affiché sur le landing. Vitrine uniquement : aucun identifiant interne,
 * aucune PII (contrat /public/announcements). Le lien « join » utilise le
 * champ accessRef (= code ou slug) pour la rejointure directe.
 */
/**
 * Carte d'annonce publique.
 *
 * T-B6 / D8 (faille F19) : le backend ne renvoie plus `accessRef` — publier
 * le code de rejointure sur la page d'accueil ouvrait l'église à quiconque
 * lisait la page. On reçoit `invitePath` (`/j/<slug>`), qui mène à la page de
 * rejointure où le mode OPEN/APPROVAL du code continue de régner.
 */
type AnnouncementItem = {
  title: string;
  description?: string;
  churchName: string;
  city?: string;
  country?: string;
  eventAt?: string;
  imageUrl?: string;
  linkUrl?: string;
  /** Chemin interne du lien d'invitation : `/j/<slug>`. */
  invitePath?: string | null;
};

export default function SectionAnnouncements() {
  const [items, setItems] = useState<AnnouncementItem[]>([]);

  useEffect(() => {
    let active = true;
    api.get<{ content: AnnouncementItem[] }>('/public/announcements')
      .then(({ data }) => { if (active && Array.isArray(data?.content)) setItems(data.content); })
      .catch(() => { /* vitrine optionnelle : jamais bloquante */ });
    return () => { active = false; };
  }, []);

  if (items.length === 0) {
    // La section est une vitrine optionnelle : sans annonce publiée, elle
    // disparaît plutôt que d'afficher un cadre vide sur la page d'accueil.
    return null;
  }

  return (
    <section id="announcements" className="relative py-20 sm:py-24">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-2.5 text-primary-600 dark:text-primary-400 mb-3">
          <Megaphone className="w-5 h-5" />
          <span className="text-xs font-semibold uppercase tracking-[0.18em]">{tText('Annonces des églises')}</span>
        </div>
        <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white font-display tracking-tight text-balance">
          {tText('Événements annoncés sur Discipolat')}
        </h2>
        <p className="mt-3 max-w-2xl text-gray-500 dark:text-gray-400">
          {tText('Concerts, veillées, conventions… Rejoignez l’église qui annonce depuis la page d’accueil.')}
        </p>

        <div className="mt-10 flex gap-5 overflow-x-auto pb-4 snap-x snap-mandatory [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {items.map((a, i) => (
            <article
              key={`${a.title}-${i}`}
              className="snap-start shrink-0 w-[300px] sm:w-[340px] rounded-2xl overflow-hidden border border-white/40 dark:border-white/[0.06] glass-strong shadow-lg flex flex-col"
            >
              {a.imageUrl ? (
                <img src={a.imageUrl} alt="" loading="lazy" className="h-36 w-full object-cover" />
              ) : (
                <div className="h-36 w-full bg-gradient-to-br from-primary-500/15 to-gold-500/10 flex items-center justify-center">
                  <Megaphone className="w-10 h-10 text-primary-500/60" />
                </div>
              )}
              <div className="p-5 flex flex-col gap-2.5 flex-1">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-primary-600 dark:text-primary-400">
                  {a.churchName}
                </p>
                <h3 className="text-base font-bold text-gray-900 dark:text-white leading-snug">{a.title}</h3>
                {a.description && (
                  <p className="text-sm text-gray-500 dark:text-gray-400 line-clamp-3">{a.description}</p>
                )}
                <div className="mt-auto pt-2 flex flex-wrap items-center gap-3 text-xs text-gray-500 dark:text-gray-400">
                  {a.eventAt && (
                    <span className="inline-flex items-center gap-1">
                      <CalendarDays className="w-3.5 h-3.5" />
                      {new Date(a.eventAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })}
                    </span>
                  )}
                  {(a.city || a.country) && (
                    <span className="inline-flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5" />
                      {[a.city, a.country].filter(Boolean).join(', ')}
                    </span>
                  )}
                  {(a.invitePath || a.linkUrl) && (
                    <a
                      href={a.invitePath || a.linkUrl}
                      target={a.linkUrl && !a.invitePath ? '_blank' : undefined}
                      rel="noreferrer"
                      className="ml-auto font-medium text-primary-600 dark:text-primary-400 hover:underline"
                    >
                      {tText('Rejoindre')}
                    </a>
                  )}
                </div>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
