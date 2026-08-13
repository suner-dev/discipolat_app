import { useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  Building2, Users, Heart, ArrowLeft, Mail, UserCog,
  ChevronRight, Loader2, AlertTriangle, BarChart3, FileText,
  CheckCircle2, XCircle, Clock, AlertCircle, Search, Download, Upload, FolderOpen,
} from 'lucide-react';

export default function DepartmentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [memberSearch, setMemberSearch] = useState('');
  const [importPreview, setImportPreview] = useState<any[] | null>(null);
  const [importCounts, setImportCounts] = useState<any>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // ==================== EXPORT CSV ====================
  const exportMutation = useMutation({
    mutationFn: async () => {
      const res = await api.get(`/departments/${id}/members/export`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      const disposition = res.headers['content-disposition'] || '';
      const match = disposition.match(/filename=([^;]+)/);
      link.download = match ? match[1].replace(/"/g, '') : `membres-${id}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // ==================== IMPORT CSV ====================
  const importMutation = useMutation({
    mutationFn: async ({ rows, preview }: { rows: any[]; preview: boolean }) => {
      const res = await api.post(`/departments/${id}/members/import?preview=${preview}`, { rows });
      return res.data as any;
    },
    onSuccess: (data, vars) => {
      if (vars.preview) {
        setImportCounts(data);
        setImportPreview(data.resultats || []);
      } else {
        toast.success(`Import terminé : ${data.cree} créés, ${data.doublon} doublons, ${data.erreur} erreurs ✅`);
        setImportPreview(null);
        setImportCounts(null);
        queryClient.invalidateQueries({ queryKey: ['department', id, 'members'] });
        queryClient.invalidateQueries({ queryKey: ['department', id, 'detail'] });
      }
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const handleCsvFile = (file: File) => {
    const reader = new FileReader();
    reader.onload = () => {
      const text = String(reader.result || '');
      const lines = text.split(/\r?\n/).filter((l) => l.trim().length > 0);
      if (lines.length === 0) {
        toast.error('Fichier vide');
        return;
      }
      const header = lines[0].split(';').map((h) => h.trim().toLowerCase());
      const rows = lines.slice(1).map((line) => {
        const cells = line.split(';').map((c) => c.replace(/"/g, '').trim());
        const row: Record<string, string> = {};
        header.forEach((h, i) => {
          if (h) row[h] = cells[i] || '';
        });
        return row;
      });
      importMutation.mutate({ rows, preview: true });
    };
    reader.readAsText(file);
  };

  const { data: dept, isLoading } = useQuery({
    queryKey: ['department', id],
    queryFn: async () => {
      const res = await api.get(`/departments/${id}/detail`);
      return res.data as any;
    },
    enabled: !!id,
  });

  const { data: kpi } = useQuery({
    queryKey: ['department', id, 'kpi'],
    queryFn: async () => {
      const res = await api.get(`/departments/${id}/kpi`);
      return res.data as any;
    },
    enabled: !!id,
  });

  const { data: membersPage } = useQuery({
    queryKey: ['department', id, 'members'],
    queryFn: async () => {
      const res = await api.get(`/departments/${id}/members?size=200`);
      return res.data as any;
    },
    enabled: !!id,
  });

  const { data: unassigned } = useQuery({
    queryKey: ['department', id, 'unassigned'],
    queryFn: async () => {
      const res = await api.get(`/departments/${id}/unassigned`);
      return res.data as any[];
    },
    enabled: !!id,
  });

  const members: any[] = membersPage?.content || [];
  const filteredMembers = memberSearch
    ? members.filter((m: any) =>
        (m.nom || '').toLowerCase().includes(memberSearch.toLowerCase()) ||
        (m.email || '').toLowerCase().includes(memberSearch.toLowerCase())
      )
    : members;

  if (isLoading) {
    return (
      <div className="page-container flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  if (!dept) {
    return (
      <div className="page-container">
        <div className="glass-card p-10 text-center">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Département non trouvé</h2>
          <Link to="/departments" className="btn-glow btn-sm mt-4 inline-flex">Retour</Link>
        </div>
      </div>
    );
  }

  const statutBadge = (statut: string) => {
    switch (statut) {
      case 'ACTIF': return <span className="badge-success text-xs"><CheckCircle2 className="w-3 h-3 mr-1" />Actif</span>;
      case 'EN_INTEGRATION': return <span className="badge-info text-xs"><Clock className="w-3 h-3 mr-1" />En intégration</span>;
      case 'EN_VEILLE': return <span className="badge-warning text-xs"><AlertCircle className="w-3 h-3 mr-1" />En veille</span>;
      case 'DECROCHE': return <span className="badge-inactive text-xs"><XCircle className="w-3 h-3 mr-1" />Décroché</span>;
      default: return <span className="text-xs text-gray-400">{statut}</span>;
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <button onClick={() => navigate('/departments')} className="btn-ghost btn-sm mb-2">
          <ArrowLeft className="w-4 h-4" /> Retour aux départements
        </button>
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
            <Building2 className="w-6 h-6" />
          </div>
          <div className="flex-1">
            <h1 className="page-title">{dept.nom}</h1>
            <p className="page-subtitle">{dept.description || 'Aucune description'}</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 mb-8">
        <div className="stat-card">
          <span className="stat-label">Responsable</span>
          <div className="flex items-center gap-2 mt-1">
            <UserCog className="w-4 h-4 text-primary-500" />
            <span className="stat-value text-lg">{dept.responsableNom || 'N/A'}</span>
          </div>
          <div className="flex items-center gap-1 mt-1">
            <Mail className="w-3 h-3 text-gray-400" />
            <span className="text-xs text-gray-500">{dept.responsableEmail || ''}</span>
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-label">Familles</span>
          <div className="flex items-center gap-2 mt-1">
            <Users className="w-4 h-4 text-violet-500" />
            <span className="stat-value text-lg">{dept.totalFamilles || 0}</span>
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-label">Membres total</span>
          <div className="flex items-center gap-2 mt-1">
            <Heart className="w-4 h-4 text-rose-500" />
            <span className="stat-value text-lg">{dept.totalMembres || 0}</span>
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-label">Gestion & rapport</span>
          <div className="flex items-center gap-2 mt-2 flex-wrap">
            <Link to={`/departments/${id}/manage`} className="btn-primary btn-sm inline-flex">
              <Users className="w-4 h-4" /> Gérer
            </Link>
            <Link to={`/departments/${id}/stats`} className="btn-ghost btn-sm inline-flex">
              <BarChart3 className="w-4 h-4" /> Stats
            </Link>
            <Link to={`/departments/${id}/report`} className="btn-ghost btn-sm inline-flex">
              <FileText className="w-4 h-4" /> Rapport
            </Link>
          </div>
        </div>
      </div>

      {/* KPI Cards */}
      {kpi && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-8">
          {[
            { label: 'Actifs', value: kpi.membresActifs, color: 'from-green-500 to-emerald-500' },
            { label: 'En intégration', value: kpi.membresEnIntegration, color: 'from-blue-500 to-indigo-500' },
            { label: 'En veille', value: kpi.membresEnVeille, color: 'from-amber-500 to-orange-500' },
            { label: 'Décrochés', value: kpi.membresDecroches, color: 'from-red-500 to-rose-500' },
            { label: 'Nvx convertis', value: kpi.nouveauxConvertis, color: 'from-teal-500 to-green-500' },
            { label: 'Faiseurs', value: kpi.totalFaiseurs, color: 'from-violet-500 to-purple-500' },
          ].map((stat) => (
            <div key={stat.label} className="stat-card p-3 text-center">
              <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${stat.color} opacity-60`} />
              <span className="stat-label text-[10px]">{stat.label}</span>
              <p className="stat-value text-xl mt-1">{String(stat.value)}</p>
            </div>
          ))}
        </div>
      )}

      {/* KPIs row 2: participation */}
      {kpi && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
          <div className="stat-card">
            <div className="flex items-center justify-between mb-2">
              <span className="stat-label">Taux de soumission</span>
              <BarChart3 className="w-4 h-4 text-primary-500" />
            </div>
            <p className="stat-value text-2xl">{kpi.tauxSoumission || 0}%</p>
            <p className="text-xs text-gray-500 mt-1">
              {kpi.rapportsSoumisSemaine}/{kpi.rapportsAttendusSemaine} rapports soumis cette semaine
            </p>
            <div className="progress-bar mt-2">
              <div className="progress-bar-fill" style={{ width: `${kpi.tauxSoumission || 0}%` }} />
            </div>
          </div>
          <div className="stat-card">
            <div className="flex items-center justify-between mb-2">
              <span className="stat-label">Présence moyenne</span>
              <BarChart3 className="w-4 h-4 text-green-500" />
            </div>
            <p className="stat-value text-2xl">{kpi.tauxPresence || 0}%</p>
            <p className="text-xs text-gray-500 mt-1">{kpi.totalPresents || 0} présents cette semaine</p>
            <div className="progress-bar mt-2">
              <div className="progress-bar-fill bg-green-500" style={{ width: `${kpi.tauxPresence || 0}%` }} />
            </div>
          </div>
          <div className="stat-card">
            <div className="flex items-center justify-between mb-2">
              <span className="stat-label">Rapports famille soumis</span>
              <FileText className="w-4 h-4 text-violet-500" />
            </div>
            <p className="stat-value text-2xl">{kpi.familyReportsSoumis}/{kpi.totalFamilles}</p>
            <p className="text-xs text-gray-500 mt-1">cette semaine</p>
          </div>
        </div>
      )}

      {/* Unassigned members alert */}
      {unassigned && unassigned.length > 0 && (
        <div className="glass-card p-4 mb-8 border-l-4 border-amber-500 animate-slide-up">
          <div className="flex items-start gap-3">
            <div className="p-2 rounded-lg bg-amber-100 dark:bg-amber-900/30">
              <AlertTriangle className="w-5 h-5 text-amber-600" />
            </div>
            <div className="flex-1">
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
                Âmes non assignées à une famille ({unassigned.length})
              </h3>
              <p className="text-xs text-gray-500 mt-1">
                Ces âmes sont suivies par des faiseurs du département mais ne sont assignées à aucune famille.
              </p>
              <div className="mt-3 space-y-1 max-h-32 overflow-y-auto">
                {unassigned.slice(0, 20).map((s: any) => (
                  <div key={s.id} className="flex items-center justify-between py-1 px-2 rounded-lg hover:bg-white/40 dark:hover:bg-gray-800/30">
                    <Link to={`/souls/${s.id}`} className="text-sm text-primary-600 hover:underline">{s.nom}</Link>
                    <span className="text-xs text-gray-400">Faiseur: {s.faiseurNom || s.faiseurId?.slice(0, 8)}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Members table */}
      <div className="glass-card p-6 mb-6">
        <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-3 mb-4">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <Heart className="w-5 h-5 text-rose-500" />
            Membres du département ({members.length})
          </h2>
          <div className="flex flex-wrap items-center gap-2">
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv"
              className="hidden"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) handleCsvFile(f);
                e.target.value = '';
              }}
            />
            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={importMutation.isPending}
              className="btn-secondary btn-sm cursor-pointer"
              title="Importer des membres depuis un CSV (nom;prenom;email;telephone;profession;ville;equipe;poste)"
            >
              {importMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Upload className="w-4 h-4" />} Importer CSV
            </button>
            <button
              onClick={() => exportMutation.mutate()}
              disabled={exportMutation.isPending}
              className="btn-secondary btn-sm cursor-pointer"
              title="Exporter les membres en CSV"
            >
              {exportMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />} Exporter
            </button>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                className="input pl-9 py-1.5 text-sm w-full sm:w-56"
                placeholder="Rechercher un membre..."
                value={memberSearch}
                onChange={(e) => setMemberSearch(e.target.value)}
              />
            </div>
          </div>
        </div>

        {/* Import preview */}
        {importPreview && (
          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
            <div className="flex items-center justify-between flex-wrap gap-2 mb-3">
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                Prévisualisation de l'import
              </h3>
              <div className="flex items-center gap-2 text-xs">
                <span className="badge badge-success text-[10px]">{importCounts?.cree ?? 0} à créer</span>
                <span className="badge badge-warning text-[10px]">{importCounts?.doublon ?? 0} doublons</span>
                <span className="badge badge-danger text-[10px]">{importCounts?.erreur ?? 0} erreurs</span>
              </div>
            </div>
            <div className="max-h-48 overflow-y-auto mb-3">
              {importPreview.map((r, i) => (
                <div key={i} className="flex items-center gap-2 py-1 px-2 text-xs rounded-lg hover:bg-white/40 dark:hover:bg-gray-800/30">
                  <span className={`w-2 h-2 rounded-full ${r.statut === 'CREER' ? 'bg-emerald-500' : r.statut === 'DOUBLON' ? 'bg-amber-500' : 'bg-red-500'}`} />
                  <span className="font-medium text-gray-800 dark:text-gray-200">{r.nom} {r.prenom || ''}</span>
                  <span className="text-gray-400">{r.email || r.telephone || ''}</span>
                  {r.equipe && <span className="badge badge-info text-[9px]">{r.equipe}</span>}
                  {r.message && <span className="text-gray-400 italic">{r.message}</span>}
                </div>
              ))}
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => { setImportPreview(null); setImportCounts(null); }} className="btn-ghost btn-xs cursor-pointer">Annuler</button>
              <button
                onClick={() => importMutation.mutate({ rows: importPreview.filter((r) => r.statut === 'CREER').map((r) => ({ nom: r.nom, prenom: r.prenom, email: r.email, telephone: r.telephone, equipe: r.equipe, poste: r.poste })), preview: false })}
                disabled={importMutation.isPending || (importCounts?.cree ?? 0) === 0}
                className="btn-primary btn-xs cursor-pointer"
              >
                <CheckCircle2 className="w-3 h-3" /> Confirmer l'import
              </button>
            </div>
          </div>
        )}
        {filteredMembers.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Nom</th>
                  <th>Famille</th>
                  <th>Faiseur</th>
                  <th>Statut</th>
                  <th>Type</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filteredMembers.map((m: any) => (
                  <tr key={m.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/30 cursor-pointer" onClick={() => navigate(`/departments/${id}/members/${m.id}`)}>
                    <td className="font-medium text-gray-900 dark:text-gray-100">{m.nom}</td>
                    <td>
                      {m.familleNom ? (
                        <span className="text-sm text-primary-600">{m.familleNom}</span>
                      ) : (
                        <span className="text-xs text-amber-600 flex items-center gap-1">
                          <AlertTriangle className="w-3 h-3" /> Non assigné
                        </span>
                      )}
                    </td>
                    <td className="text-sm text-gray-600">{m.faiseurNom || m.faiseurId?.slice(0, 8)}</td>
                    <td>{statutBadge(m.statut)}</td>
                    <td>
                      <span className={`badge text-[10px] ${m.typeDisciple === 'NOUVEAU_CONVERTI' ? 'badge-success' : 'badge-info'}`}>
                        {m.typeDisciple === 'NOUVEAU_CONVERTI' ? 'Nv converti' : 'Nv arrivant'}
                      </span>
                    </td>
                    <td className="flex items-center gap-2 justify-end">
                      <Link
                        to={`/departments/${id}/members/${m.id}`}
                        onClick={(e) => e.stopPropagation()}
                        className="btn-ghost btn-xs inline-flex"
                        title="Dossier de gestion du membre"
                      >
                        <FolderOpen className="w-3.5 h-3.5" /> Dossier
                      </Link>
                      <ChevronRight className="w-4 h-4 text-gray-400" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">
            <Heart className="w-12 h-12 mx-auto mb-3 opacity-40" />
            <p>{memberSearch ? 'Aucun résultat' : 'Aucun membre dans ce département'}</p>
          </div>
        )}
      </div>

      {/* Families list */}
      <div className="glass-card p-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          Familles du département ({dept.familles?.length || 0})
        </h2>
        {dept.familles && dept.familles.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Nom</th>
                  <th>Chef de famille</th>
                  <th>Membres</th>
                  <th>Actifs</th>
                  <th>Statut</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {dept.familles.map((fam: any) => (
                  <tr key={fam.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/30 cursor-pointer" onClick={() => navigate(`/families/${fam.id}`)}>
                    <td className="font-medium text-gray-900 dark:text-gray-100">{fam.nom}</td>
                    <td>{fam.chefNom}</td>
                    <td>{fam.totalMembres}</td>
                    <td>{fam.membresActifs}</td>
                    <td>
                      <span className={`badge ${fam.statut === 'ACTIVE' ? 'badge-success' : 'badge-inactive'}`}>
                        {fam.statut}
                      </span>
                    </td>
                    <td><ChevronRight className="w-4 h-4 text-gray-400" /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">
            <Building2 className="w-12 h-12 mx-auto mb-3 opacity-40" />
            <p>Aucune famille dans ce département</p>
          </div>
        )}
      </div>
    </div>
  );
}
