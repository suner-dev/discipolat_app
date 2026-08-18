import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import AttachmentLinks from '@/components/shared/AttachmentLinks';
import type { Pastoral360Data } from '@/types';
import {
  ArrowLeft, Heart, Activity, Clock, AlertTriangle, Star,
  MessageSquare, TrendingUp, TrendingDown, Sparkles, Users,
  Mail, Phone, Calendar, MapPin, Briefcase, Church, Shield,
  CheckCircle, XCircle, ChevronRight, BookOpen, Gavel, Paperclip,
} from 'lucide-react';
import {
  RadarChart, Radar, PolarGrid, PolarAngleAxis,
  PolarRadiusAxis, ResponsiveContainer, Tooltip,
  LineChart, Line, XAxis, YAxis, CartesianGrid, Legend,
} from 'recharts';

const INDICE_LABELS: Record<string, string> = {
  santeSpirituelle: 'Santé spirituelle',
  fidelite: 'Fidélité',
  engagement: 'Engagement',
  participation: 'Participation',
  global: 'Global',
};

const INDICE_COLORS: Record<string, string> = {
  santeSpirituelle: '#22c55e',
  fidelite: '#3b82f6',
  engagement: '#f59e0b',
  participation: '#8b5cf6',
  global: '#22c55e',
};

function IndiceGauge({ label, value, color, size = 'md' }: { label: string; value: number; color: string; size?: 'sm' | 'md' }) {
  const isSm = size === 'sm';
  return (
    <div className="flex flex-col items-center">
      <div className={`relative ${isSm ? 'w-16 h-16' : 'w-24 h-24'}`}>
        <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
          <circle cx="50" cy="50" r="42" fill="none" stroke="currentColor" strokeWidth="8"
            className="text-gray-200 dark:text-gray-700" />
          <circle cx="50" cy="50" r="42" fill="none" stroke={color} strokeWidth="8"
            strokeLinecap="round"
            strokeDasharray={`${2 * Math.PI * 42}`}
            strokeDashoffset={`${2 * Math.PI * 42 * (1 - value / 100)}`}
            className="transition-all duration-1000 ease-out"
          />
        </svg>
        <div className={`absolute inset-0 flex items-center justify-center ${isSm ? 'text-sm' : 'text-xl'} font-bold`}
          style={{ color }}>
          {value}
        </div>
      </div>
      <span className={`${isSm ? 'text-[9px]' : 'text-xs'} text-gray-400 mt-1 text-center`}>{label}</span>
    </div>
  );
}

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const STATUT_FALLBACK: Record<string, string> = {
  NOUVEAU_CONVERTI: 'Nouveau converti', NOUVEL_ARRIVANT: 'Nouvel arrivant',
  EN_INTEGRATION: 'En intégration', ACTIF: 'Actif', EN_VEILLE: 'En veille', DECROCHE: 'Décroché',
};

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const SPIRITUAL_FALLBACK: Record<string, string> = {
  NOUVEAU_CONVERTI: 'Nouveau converti', EN_CROISSANCE: 'En croissance',
  MATURE: 'Mature', EN_DIFFICULTE: 'En difficulté',
};

const EVAL_FALLBACK: Record<string, string> = {
  RESPONSABLE: 'Responsable', CHEF_FAMILLE: 'Chef de famille', FAISEUR: 'Faiseur',
};

export default function Pastoral360Page() {
  const { id } = useParams<{ id: string }>();
  const dictionaries = useDictionaries();

  const { data: dossier, isLoading } = useQuery({
    queryKey: ['soul', id, 'pastoral-360'],
    queryFn: async () => {
      const res = await api.get(`/souls/${id}/pastoral-360`);
      return res.data as Pastoral360Data;
    },
    enabled: !!id,
  });

  if (isLoading) {
    return (
      <div className="page-container">
        <div className="animate-fade-in space-y-4">
          <div className="skeleton h-8 w-64 rounded-lg" />
          <div className="skeleton h-64 w-full rounded-2xl" />
          <div className="grid grid-cols-4 gap-4">
            {[...Array(4)].map((_, i) => <div key={i} className="skeleton h-32 rounded-2xl" />)}
          </div>
        </div>
      </div>
    );
  }

  if (!dossier) {
    return (
      <div className="page-container">
        <div className="glass-card p-12 text-center animate-scale-in">
          <Heart className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-500">Membre non trouvé</p>
          <Link to="/souls" className="btn-primary btn-sm mt-4 inline-flex">Retour</Link>
        </div>
      </div>
    );
  }

  const infos = dossier.informations ?? ({} as Pastoral360Data['informations']);
  const spirituel = dossier.spirituel ?? ({} as Pastoral360Data['spirituel']);
  const indices = dossier.indices ?? ({} as Pastoral360Data['indices']);
  const alertesAuto = dossier.alertesAutomatiques ?? [];
  const encadrement = dossier.encadrement ?? ({} as Pastoral360Data['encadrement']);
  const timeline = dossier.timeline ?? [];
  const evaluations = dossier.evaluations ?? {};
  const notes = dossier.notes ?? [];
  const piecesJointes = dossier.piecesJointes ?? [];

  const radarData = Object.entries(INDICE_LABELS)
    .filter(([key]) => key !== 'global')
    .map(([key, label]) => ({
      subject: label,
      value: indices[key] ?? 0,
      fullMark: 100,
    }));

  return (
    <div className="page-container">
      {/* Back + Header */}
      <div className="page-header">
        <Link to={`/souls/${id}`} className="btn-ghost btn-sm mb-2 inline-flex animate-fade-in">
          <ArrowLeft className="w-4 h-4" /> Retour à la fiche
        </Link>
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Sparkles className="w-5 h-5 text-primary-500" />
            <span className="text-sm font-medium text-primary-600 dark:text-primary-400 uppercase tracking-wider">
              Dossier Pastoral 360°
            </span>
          </div>
          <h1 className="page-title">
            {infos.prenom ? `${infos.prenom} ${infos.nom}` : infos.nom}
          </h1>
          <p className="page-subtitle">
            Fiche complète · {new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
          </p>
        </div>
      </div>

      {/* Alertes automatiques */}
      {alertesAuto.length > 0 && (
        <div className="space-y-2 mb-6 animate-slide-up">
          {alertesAuto.map((alert, i) => (
            <div key={i} className={`p-3 rounded-xl flex items-center gap-3 border ${
              alert.priorite === 'HAUTE'
                ? 'bg-red-50/70 dark:bg-red-900/10 border-red-200/50 dark:border-red-800/30'
                : 'bg-amber-50/70 dark:bg-amber-900/10 border-amber-200/50 dark:border-amber-800/30'
            }`}>
              <AlertTriangle className={`w-5 h-5 ${alert.priorite === 'HAUTE' ? 'text-red-500' : 'text-amber-500'}`} />
              <span className={`text-sm font-medium ${alert.priorite === 'HAUTE' ? 'text-red-700 dark:text-red-300' : 'text-amber-700 dark:text-amber-300'}`}>
                {alert.message}
              </span>
              <span className={`ml-auto text-[9px] font-semibold px-1.5 py-0.5 rounded-full ${
                alert.priorite === 'HAUTE' ? 'bg-red-100 dark:bg-red-900/30 text-red-600' : 'bg-amber-100 dark:bg-amber-900/30 text-amber-600'
              }`}>
                {alert.priorite}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Indices intelligents */}
      <div className="glass-card p-6 mb-6 animate-slide-up">
        <div className="flex items-center gap-2 mb-6">
          <Activity className="w-5 h-5 text-primary-500" />
          <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 uppercase tracking-wider">Indices de santé</h2>
        </div>
        <div className="grid grid-cols-5 gap-4 mb-6">
          {Object.entries(INDICE_LABELS).map(([key, label]) => (
            <IndiceGauge
              key={key}
              label={label}
              value={indices[key] ?? 0}
              color={INDICE_COLORS[key] ?? '#22c55e'}
              size={key === 'global' ? 'md' : 'sm'}
            />
          ))}
        </div>
        {radarData.length > 0 && (
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <RadarChart data={radarData}>
                <PolarGrid stroke="rgba(128,128,128,0.15)" />
                <PolarAngleAxis dataKey="subject" tick={{ fontSize: 11, fill: 'rgba(128,128,128,0.6)' }} />
                <PolarRadiusAxis angle={90} domain={[0, 100]} tick={{ fontSize: 10, fill: 'rgba(128,128,128,0.4)' }} />
                <Radar name="Indices" dataKey="value" stroke="#22c55e" fill="#22c55e" fillOpacity={0.2} />
                <Tooltip />
              </RadarChart>
            </ResponsiveContainer>
          </div>
        )}

        {/* Parcours spirituel — graphique d'évolution */}
        {timeline.length > 1 && (
          <div className="mt-6 pt-6 border-t border-gray-100 dark:border-gray-700/30">
            <div className="flex items-center gap-2 mb-4">
              <TrendingUp className="w-4 h-4 text-primary-500" />
              <h3 className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                Évolution du parcours
              </h3>
            </div>
            <div className="h-48">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={timeline
                  .filter((e: any) => e.nouveauStatut)
                  .slice(0, 20)
                  .reverse()
                  .map((e: any, i: number) => ({
                    index: i + 1,
                    evenement: e.type?.slice(0, 15) || `#${i}`,
                    date: new Date(e.date).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }),
                    statut: e.nouveauStatut,
                  }))
                }>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.15)" vertical={false} />
                  <XAxis dataKey="date" tick={{ fontSize: 10 }} stroke="rgba(128,128,128,0.3)" />
                  <YAxis hide />
                  <Tooltip
                    content={({ active, payload }) => {
                      if (!active || !payload?.length) return null;
                      const d = payload[0].payload;
                      return (
                        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-3 border border-gray-200 dark:border-gray-700">
                          <p className="text-xs font-semibold text-gray-900 dark:text-gray-100">{d.evenement}</p>
                          <p className="text-[10px] text-gray-500">{d.date}</p>
                          {d.statut && (
                            <p className="text-[10px] text-primary-600 dark:text-primary-400 mt-1">
                              → {d.statut}
                            </p>
                          )}
                        </div>
                      );
                    }}
                  />
                  <Line type="monotone" dataKey="index" stroke="#22c55e" strokeWidth={2}
                    dot={{ r: 4, fill: '#22c55e', strokeWidth: 2, stroke: '#fff' }}
                    activeDot={{ r: 6, fill: '#22c55e' }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
            <p className="text-[9px] text-gray-400 text-center mt-2">
              {timeline.filter((e: any) => e.nouveauStatut).length} changements de statut enregistrés
            </p>
          </div>
        )}
      </div>

      {/* Coordonnées + Parcours */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div className="glass-card p-5 animate-slide-up">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-primary-500" /> Coordonnées
          </h3>
          <div className="space-y-3">
            {[
              { icon: Mail, label: 'Email', value: infos.email },
              { icon: Phone, label: 'Téléphone', value: infos.telephone },
              { icon: MapPin, label: 'Adresse', value: infos.adresse },
              { icon: Calendar, label: 'Né(e) le', value: infos.dateNaissance },
              { icon: Briefcase, label: 'Profession', value: infos.profession },
              { icon: Users, label: 'Situation', value: infos.situationFamiliale },
            ].filter(f => f.value).map(field => (
              <div key={field.label} className="flex items-center gap-3 p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                <field.icon className="w-4 h-4 text-gray-400" />
                <div>
                  <p className="text-[10px] text-gray-400">{field.label}</p>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{field.value}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '80ms' }}>
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Church className="w-4 h-4 text-primary-500" /> Parcours spirituel
          </h3>
          <div className="space-y-3">
            <div className="flex justify-between p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
              <span className="text-xs text-gray-400">Type</span>
              <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">
                {spirituel.typeDisciple === 'NOUVEAU_CONVERTI' ? 'Nouveau converti' : 'Nouvel arrivant'}
              </span>
            </div>
            <div className="flex justify-between p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
              <span className="text-xs text-gray-400">Statut</span>
              <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">
                {dictionaries.label('SOUL_STATUS', spirituel.statut) || STATUT_FALLBACK[spirituel.statut] || spirituel.statut}
              </span>
            </div>
            <div className="flex justify-between p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
              <span className="text-xs text-gray-400">État spirituel</span>
              <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">
                {dictionaries.label('SPIRITUAL_LEVEL', spirituel.etatSpirituel) || SPIRITUAL_FALLBACK[spirituel.etatSpirituel] || spirituel.etatSpirituel}
              </span>
            </div>
            <div className="flex justify-between p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
              <span className="text-xs text-gray-400">Niveau croissance</span>
              <span className="flex items-center gap-1">
                {[1,2,3,4,5].map(i => (
                  <Star key={i} className={`w-3 h-3 ${i <= (spirituel.niveauCroissance || 1) ? 'fill-amber-400 text-amber-400' : 'text-gray-300 dark:text-gray-600'}`} />
                ))}
              </span>
            </div>
            {spirituel.dateIntegration && (
              <div className="flex justify-between p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                <span className="text-xs text-gray-400">Intégré le</span>
                <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">
                  {new Date(spirituel.dateIntegration).toLocaleDateString('fr-FR')}
                </span>
              </div>
            )}
            {spirituel.dateConversion && (
              <div className="flex justify-between p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                <span className="text-xs text-gray-400">Conversion le</span>
                <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">
                  {new Date(spirituel.dateConversion).toLocaleDateString('fr-FR')}
                </span>
              </div>
            )}
            <div className="flex justify-between p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
              <span className="text-xs text-gray-400">Dernier contact</span>
              <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">
                {spirituel.dateDernierContact ? new Date(spirituel.dateDernierContact).toLocaleDateString('fr-FR') : '—'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Encadrement + Évaluations */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div className="glass-card p-5 animate-slide-up">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Shield className="w-4 h-4 text-primary-500" /> Encadrement
          </h3>
          <div className="space-y-3">
            <div className="flex justify-between p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
              <span className="text-xs text-gray-400">Faiseur</span>
              <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">{encadrement.faiseurNom || encadrement.faiseurId || '—'}</span>
            </div>
            <div className="flex justify-between p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
              <span className="text-xs text-gray-400">Famille</span>
              <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">{encadrement.familleId || 'Non assigné'}</span>
            </div>
          </div>
        </div>

        <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '80ms' }}>
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Star className="w-4 h-4 text-amber-500" /> Évaluations du faiseur
          </h3>
          {Object.keys(evaluations).length > 0 ? (
            <div className="space-y-2">
              {Object.entries(evaluations).map(([cat, data]) => (
                <div key={cat} className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <span className="text-xs text-gray-400">{dictionaries.label('EVALUATION_CATEGORIE', cat) || EVAL_FALLBACK[cat] || cat}</span>
                  <div className="flex items-center gap-2">
                    <div className="flex">
                      {[1,2,3,4,5].map(i => (
                        <Star key={i} className={`w-2.5 h-2.5 ${i <= Math.round(data.moyenne ?? 0) ? 'fill-amber-400 text-amber-400' : 'text-gray-300 dark:text-gray-600'}`} />
                      ))}
                    </div>
                    <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">
                      {data.moyenne ? `${data.moyenne.toFixed(1)}` : '—'}
                    </span>
                    <span className="text-[9px] text-gray-400">({data.total ?? 0})</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-gray-400 text-center py-4">Aucune évaluation</p>
          )}
        </div>
      </div>

      {/* Pièces jointes */}
      {piecesJointes.length > 0 && (
        <div className="glass-card p-5 mb-6 animate-slide-up">
          <div className="flex items-center gap-2 mb-4">
            <Paperclip className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 uppercase tracking-wider">
              Pièces jointes · {piecesJointes.length}
            </h3>
          </div>
          <AttachmentLinks pieces={piecesJointes} sourceKey="source" />
        </div>
      )}

      {/* Timeline */}
      <div className="glass-card p-5 mb-6 animate-slide-up">
        <div className="flex items-center gap-2 mb-4">
          <Clock className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 uppercase tracking-wider">
            Timeline · {timeline.length} événements
          </h3>
        </div>
        {timeline.length > 0 ? (
          <div className="relative">
            <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-gradient-to-b from-primary-500/30 via-primary-500/20 to-transparent" />
            <div className="space-y-3 max-h-96 overflow-y-auto">
              {timeline.slice(0, 50).map((entry, i) => (
                <div key={entry.id} className="relative pl-10 animate-slide-up" style={{ animationDelay: `${i * 30}ms` }}>
                  <div className="absolute left-2.5 top-1.5 w-3 h-3 rounded-full bg-primary-500 border-2 border-white dark:border-gray-900 shadow-[0_0_6px_rgba(22,163,74,0.4)]" />
                  <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                    <div className="flex items-center justify-between mb-0.5">
                      <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">{entry.type}</span>
                      <span className="text-[9px] text-gray-400">{new Date(entry.date).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}</span>
                    </div>
                    {entry.description && (
                      <p className="text-xs text-gray-600 dark:text-gray-400">{entry.description}</p>
                    )}
                    {(entry.ancienStatut || entry.nouveauStatut) && (
                      <div className="flex items-center gap-1 mt-1">
                        {entry.ancienStatut && <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-400">{entry.ancienStatut}</span>}
                        {entry.ancienStatut && entry.nouveauStatut && <ChevronRight className="w-2.5 h-2.5 text-gray-400" />}
                        {entry.nouveauStatut && <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400">{entry.nouveauStatut}</span>}
                      </div>
                    )}
                  </div>
                </div>
              ))}
              {timeline.length > 50 && (
                <p className="text-xs text-gray-400 text-center pt-2">+{timeline.length - 50} événements supplémentaires</p>
              )}
            </div>
          </div>
        ) : (
          <div className="text-center py-6">
            <Clock className="w-8 h-8 text-gray-300 mx-auto mb-2" />
            <p className="text-xs text-gray-400">Aucun historique</p>
          </div>
        )}
      </div>

      {/* Notes privées */}
      {notes.length > 0 && (
        <div className="glass-card p-5 animate-slide-up">
          <div className="flex items-center gap-2 mb-4">
            <MessageSquare className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 uppercase tracking-wider">
              Notes · {notes.length}
            </h3>
          </div>
          <div className="space-y-2 max-h-64 overflow-y-auto">
            {notes.map((note, i) => (
              <div key={note.id} className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 animate-fade-in" style={{ animationDelay: `${i * 40}ms` }}>
                <div className="flex items-center justify-between mb-1">
                  <span className="text-[9px] text-gray-400">{note.auteurId?.slice(0, 8)}...</span>
                  <span className="text-[9px] text-gray-400">{new Date(note.date).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}</span>
                </div>
                <p className="text-xs text-gray-700 dark:text-gray-300">{note.contenu}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
