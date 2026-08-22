import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { Award, Star, TrendingUp, Users, Search, Filter } from 'lucide-react';

interface SkillDefinition {
  id: string;
  nom: string;
  categorie: string;
  description: string;
}

interface MemberSkill {
  id: string;
  memberId: string;
  memberName: string;
  skillId: string;
  skillName: string;
  niveau: 'DEBUTANT' | 'INTERMEDIAIRE' | 'AVANCE' | 'EXPERT';
  dateEvaluation: string;
  evaluatedBy: string;
}

const NIVEAU_CONFIG = {
  DEBUTANT: { label: 'Débutant', color: 'bg-gray-100 text-gray-700', stars: 1 },
  INTERMEDIAIRE: { label: 'Intermédiaire', color: 'bg-blue-100 text-blue-700', stars: 2 },
  AVANCE: { label: 'Avancé', color: 'bg-green-100 text-green-700', stars: 3 },
  EXPERT: { label: 'Expert', color: 'bg-purple-100 text-purple-700', stars: 4 },
};

const CATEGORIES = [
  { key: 'MUSIQUE', label: 'Musique', icon: '🎵' },
  { key: 'ACCUEIL', label: 'Accueil', icon: '🫂' },
  { key: 'TECHNIQUE', label: 'Technique', icon: '🔧' },
  { key: 'FORMATION', label: 'Formation', icon: '📚' },
  { key: 'COMMUNICATION', label: 'Communication', icon: '📢' },
  { key: 'PRIERE', label: 'Prière', icon: '🙏' },
  { key: 'EVANGELISATION', label: 'Évangélisation', icon: '🌍' },
  { key: 'AUTRE', label: 'Autre', icon: '📋' },
];

const DEFAULT_SKILLS: SkillDefinition[] = [
  { id: 'skill-1', nom: 'Chant', categorie: 'MUSIQUE', description: 'Capacité à chanter en solo ou en groupe' },
  { id: 'skill-2', nom: 'Instrument', categorie: 'MUSIQUE', description: 'Jouer d\'un instrument de musique' },
  { id: 'skill-3', nom: 'Sonorisation', categorie: 'TECHNIQUE', description: 'Gérer le matériel son et lumière' },
  { id: 'skill-4', nom: 'Accueil visiteurs', categorie: 'ACCUEIL', description: 'Accueillir et orienter les visiteurs' },
  { id: 'skill-5', nom: 'Animation', categorie: 'FORMATION', description: 'Animer un groupe ou une session' },
  { id: 'skill-6', nom: 'Prédication', categorie: 'FORMATION', description: 'Prêcher ou enseigner' },
  { id: 'skill-7', nom: 'Évangélisation terrain', categorie: 'EVANGELISATION', description: 'Partager l\'évangile en extérieur' },
  { id: 'skill-8', nom: 'Intercession', categorie: 'PRIERE', description: 'Ministère de prière d\'intercession' },
  { id: 'skill-9', nom: 'Rédaction', categorie: 'COMMUNICATION', description: 'Rédiger des contenus (posts, articles)' },
  { id: 'skill-10', nom: 'Gestion d\'équipe', categorie: 'AUTRE', description: 'Coordonner et manager une équipe' },
];

export default function SkillsMatrixPage() {
  const { t } = useI18n();
  const [skills, setSkills] = useState<SkillDefinition[]>([]);
  const [memberSkills, setMemberSkills] = useState<MemberSkill[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterCategory, setFilterCategory] = useState('');
  const [selectedMember, setSelectedMember] = useState<string | null>(null);

  useEffect(() => { loadData(); }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [skillsRes, memberSkillsRes] = await Promise.allSettled([
        api.get('/skills'),
        api.get('/skills/members'),
      ]);
      setSkills(skillsRes.status === 'fulfilled' ? (skillsRes.value.data.content || skillsRes.value.data || []) : DEFAULT_SKILLS);
      setMemberSkills(memberSkillsRes.status === 'fulfilled' ? (memberSkillsRes.value.data.content || memberSkillsRes.value.data || []) : []);
    } catch {
      setSkills(DEFAULT_SKILLS);
    } finally {
      setLoading(false);
    }
  };

  const updateSkillLevel = async (memberId: string, skillId: string, niveau: string) => {
    try {
      await api.put(`/skills/members`, { memberId, skillId, niveau });
      Toast.success('Compétence mise à jour');
      loadData();
    } catch {
      Toast.error('Erreur lors de la mise à jour');
    }
  };

  const filteredSkills = skills.filter(s => {
    const matchesSearch = !search || s.nom.toLowerCase().includes(search.toLowerCase());
    const matchesCategory = !filterCategory || s.categorie === filterCategory;
    return matchesSearch && matchesCategory;
  });

  // Group member skills by member
  const memberMap = new Map<string, { name: string; skills: Map<string, string> }>();
  memberSkills.forEach(ms => {
    if (!memberMap.has(ms.memberId)) {
      memberMap.set(ms.memberId, { name: ms.memberName, skills: new Map() });
    }
    memberMap.get(ms.memberId)!.skills.set(ms.skillId, ms.niveau);
  });

  const filteredMembers = Array.from(memberMap.entries()).filter(([_, data]) =>
    !selectedMember || data.name.toLowerCase().includes(selectedMember.toLowerCase())
  );

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Award className="w-8 h-8 text-amber-500" />
            Matrice de compétences
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Évaluez et suivez les compétences de vos membres
          </p>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="p-4 rounded-xl bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20">
          <div className="text-xs text-amber-600 mb-1">Compétences</div>
          <div className="text-2xl font-bold text-amber-900 dark:text-amber-300">{skills.length}</div>
        </div>
        <div className="p-4 rounded-xl bg-blue-50 dark:bg-blue-500/10 border border-blue-200 dark:border-blue-500/20">
          <div className="text-xs text-blue-600 mb-1">Membres évalués</div>
          <div className="text-2xl font-bold text-blue-900 dark:text-blue-300">{memberMap.size}</div>
        </div>
        <div className="p-4 rounded-xl bg-green-50 dark:bg-green-500/10 border border-green-200 dark:border-green-500/20">
          <div className="text-xs text-green-600 mb-1">Évaluations totales</div>
          <div className="text-2xl font-bold text-green-900 dark:text-green-300">{memberSkills.length}</div>
        </div>
        <div className="p-4 rounded-xl bg-purple-50 dark:bg-purple-500/10 border border-purple-200 dark:border-purple-500/20">
          <div className="text-xs text-purple-600 mb-1">Experts</div>
          <div className="text-2xl font-bold text-purple-900 dark:text-purple-300">
            {memberSkills.filter(ms => ms.niveau === 'EXPERT').length}
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Rechercher une compétence..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
          />
        </div>
        <select
          value={filterCategory}
          onChange={e => setFilterCategory(e.target.value)}
          className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
        >
          <option value="">Toutes les catégories</option>
          {CATEGORIES.map(c => <option key={c.key} value={c.key}>{c.icon} {c.label}</option>)}
        </select>
      </div>

      {loading ? (
        <SkeletonLoader lines={6} variant="table" />
      ) : filteredSkills.length === 0 ? (
        <EmptyState
          icon={<Award className="w-8 h-8 text-gray-400" />}
          title="Aucune compétence"
          message="Les compétences par défaut seront chargées automatiquement"
        />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200 dark:border-white/10">
                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Compétence</th>
                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Catégorie</th>
                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Membres qualifiés</th>
                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Niveau moyen</th>
              </tr>
            </thead>
            <tbody>
              {filteredSkills.map(skill => {
                const catInfo = CATEGORIES.find(c => c.key === skill.categorie) || CATEGORIES[7];
                const skillMembers = memberSkills.filter(ms => ms.skillId === skill.id);
                const avgLevel = skillMembers.length > 0
                  ? Math.round(skillMembers.reduce((acc, ms) => acc + (NIVEAU_CONFIG[ms.niveau as keyof typeof NIVEAU_CONFIG]?.stars || 1), 0) / skillMembers.length)
                  : 0;

                return (
                  <tr key={skill.id} className="border-b border-gray-100 dark:border-white/5 hover:bg-gray-50 dark:hover:bg-white/5 transition-colors">
                    <td className="py-3 px-4">
                      <div className="text-sm font-medium text-gray-900 dark:text-white">{skill.nom}</div>
                      {skill.description && (
                        <div className="text-xs text-gray-500 dark:text-gray-400">{skill.description}</div>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-400">
                        {catInfo.icon} {catInfo.label}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-1">
                        <Users className="w-3.5 h-3.5 text-gray-400" />
                        <span className="text-sm text-gray-700 dark:text-gray-300">{skillMembers.length}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-1">
                        {Array.from({ length: 4 }).map((_, i) => (
                          <Star
                            key={i}
                            className={`w-4 h-4 ${i < avgLevel ? 'text-amber-400 fill-amber-400' : 'text-gray-200 dark:text-gray-700'}`}
                          />
                        ))}
                        <span className="text-xs text-gray-400 ml-1">{avgLevel}/4</span>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
