import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import { Church, MapPin, Phone, Mail, Globe, Calendar, Heart, ExternalLink, Clock, Users, Send } from 'lucide-react';

interface ChurchSettings {
  churchName: string;
  description?: string;
  address?: string;
  phone?: string;
  email?: string;
  website?: string;
  primaryColor: string;
  logoUrl?: string;
}

interface ChurchEvent {
  id: string;
  titre: string;
  dateDebut: string;
  lieu?: string;
  typeEvenement: string;
}

export default function PublicPortalPage() {
  const { t } = useI18n();
  const [settings, setSettings] = useState<ChurchSettings | null>(null);
  const [events, setEvents] = useState<ChurchEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [contactForm, setContactForm] = useState({ nom: '', email: '', message: '' });
  const [contactSent, setContactSent] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [settingsRes, eventsRes] = await Promise.allSettled([
        api.get('/public/settings'),
        api.get('/events?upcoming=true&limit=5'),
      ]);
      if (settingsRes.status === 'fulfilled') setSettings(settingsRes.value.data);
      if (eventsRes.status === 'fulfilled') setEvents(eventsRes.value.data?.content || eventsRes.value.data || []);
    } catch {
      setSettings({ churchName: 'Discipolat', primaryColor: '#22c55e' });
    } finally {
      setLoading(false);
    }
  };

  const sendContact = async () => {
    if (!contactForm.nom.trim() || !contactForm.email.trim() || !contactForm.message.trim()) return;
    try {
      await api.post('/public/contact', contactForm);
      setContactSent(true);
      setContactForm({ nom: '', email: '', message: '' });
    } catch {
      // silently fail
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-pulse text-gray-400">Chargement...</div>
      </div>
    );
  }

  const primaryColor = settings?.primaryColor || '#22c55e';

  return (
    <div className="min-h-screen bg-white dark:bg-gray-900">
      {/* Hero */}
      <header className="relative overflow-hidden" style={{ background: `linear-gradient(135deg, ${primaryColor}15, ${primaryColor}05)` }}>
        <div className="max-w-5xl mx-auto px-4 py-16 md:py-24 text-center">
          {settings?.logoUrl && (
            <img src={settings.logoUrl} alt="Logo" className="w-20 h-20 mx-auto mb-6 rounded-2xl object-cover" />
          )}
          <Church className="w-12 h-12 mx-auto mb-4" style={{ color: primaryColor }} />
          <h1 className="text-4xl md:text-5xl font-bold text-gray-900 dark:text-white mb-4">
            {settings?.churchName || 'Discipolat'}
          </h1>
          {settings?.description && (
            <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
              {settings.description}
            </p>
          )}
          <div className="flex flex-wrap justify-center gap-6 mt-8 text-sm text-gray-500 dark:text-gray-400">
            {settings?.address && (
              <span className="flex items-center gap-2"><MapPin className="w-4 h-4" />{settings.address}</span>
            )}
            {settings?.phone && (
              <span className="flex items-center gap-2"><Phone className="w-4 h-4" />{settings.phone}</span>
            )}
            {settings?.email && (
              <span className="flex items-center gap-2"><Mail className="w-4 h-4" />{settings.email}</span>
            )}
          </div>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-4 py-12 space-y-16">
        {/* Events Section */}
        {events.length > 0 && (
          <section>
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
              <Calendar className="w-6 h-6" style={{ color: primaryColor }} />
              Événements à venir
            </h2>
            <div className="grid gap-4 md:grid-cols-2">
              {events.map(event => (
                <div key={event.id} className="p-5 rounded-2xl border border-gray-200 dark:border-white/10 bg-gray-50 dark:bg-white/5 hover:shadow-lg transition-all">
                  <div className="flex items-start justify-between mb-2">
                    <h3 className="font-semibold text-gray-900 dark:text-white">{event.titre}</h3>
                    <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-400">
                      {event.typeEvenement}
                    </span>
                  </div>
                  <div className="flex items-center gap-4 text-sm text-gray-500 dark:text-gray-400">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" />
                      {new Date(event.dateDebut).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' })}
                    </span>
                    {event.lieu && (
                      <span className="flex items-center gap-1">
                        <MapPin className="w-3.5 h-3.5" />
                        {event.lieu}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Values Section */}
        <section>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
            <Heart className="w-6 h-6" style={{ color: primaryColor }} />
            Notre vision
          </h2>
          <div className="grid md:grid-cols-3 gap-6">
            {[
              { title: 'Discipolat', desc: 'Accompagner chaque âme dans son parcours spirituel avec amour et dedication.', icon: '🕊️' },
              { title: 'Communauté', desc: 'Créer un lien fort entre les membres par la prière et le partage.', icon: '🤝' },
              { title: 'Mission', desc: 'Évangéliser et transformer les vies par la puissance de l\'Évangile.', icon: '🌍' },
            ].map((item, i) => (
              <div key={i} className="p-6 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-center hover:shadow-lg transition-all">
                <span className="text-4xl mb-3 block">{item.icon}</span>
                <h3 className="font-bold text-gray-900 dark:text-white mb-2">{item.title}</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">{item.desc}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Contact Section */}
        <section>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
            <Send className="w-6 h-6" style={{ color: primaryColor }} />
            Nous contacter
          </h2>
          {contactSent ? (
            <div className="p-8 rounded-2xl bg-green-50 dark:bg-green-500/10 border border-green-200 dark:border-green-500/20 text-center">
              <CheckCircle2 className="w-12 h-12 text-green-500 mx-auto mb-4" />
              <h3 className="text-lg font-bold text-green-800 dark:text-green-300 mb-2">Message envoyé !</h3>
              <p className="text-sm text-green-600 dark:text-green-400">
                Nous vous répondrons dans les plus brefs délais.
              </p>
            </div>
          ) : (
            <div className="p-6 rounded-2xl border border-gray-200 dark:border-white/10 bg-gray-50 dark:bg-white/5 max-w-xl">
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <input
                    type="text"
                    placeholder="Votre nom"
                    value={contactForm.nom}
                    onChange={e => setContactForm({ ...contactForm, nom: e.target.value })}
                    className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                  <input
                    type="email"
                    placeholder="Votre email"
                    value={contactForm.email}
                    onChange={e => setContactForm({ ...contactForm, email: e.target.value })}
                    className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>
                <textarea
                  placeholder="Votre message..."
                  value={contactForm.message}
                  onChange={e => setContactForm({ ...contactForm, message: e.target.value })}
                  rows={4}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-green-500 resize-none"
                />
                <button
                  onClick={sendContact}
                  className="px-6 py-2.5 rounded-xl text-white text-sm font-medium transition-all"
                  style={{ background: primaryColor }}
                >
                  Envoyer le message
                </button>
              </div>
            </div>
          )}
        </section>

        {/* Quick Links */}
        <section className="text-center">
          <a
            href="/login"
            className="inline-flex items-center gap-2 px-6 py-3 rounded-xl text-white text-sm font-medium transition-all shadow-lg"
            style={{ background: primaryColor }}
          >
            <Church className="w-4 h-4" />
            Accéder à l'espace membre
          </a>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-gray-200 dark:border-white/10 py-8 text-center text-sm text-gray-400">
        <p>© {new Date().getFullYear()} {settings?.churchName || 'Discipolat'}. Tous droits réservés.</p>
        <p className="mt-1">Propulsé par <a href="https://discipolat.com" className="underline hover:text-gray-600">Discipolat</a></p>
      </footer>
    </div>
  );
}

function CheckCircle2(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
    </svg>
  );
}
