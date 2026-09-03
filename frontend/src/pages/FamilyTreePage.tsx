import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import api from '@/lib/api';
import {
  GitBranch, Users, Clock, AlertTriangle, TrendingUp, Loader2, ArrowLeft,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface FamilyTreeNode {
  id: string;
  name: string;
  role: string;
  parentId?: string;
  children: FamilyTreeNode[];
}

interface HistoryEntry {
  id: string;
  action: string;
  description: string;
  timestamp: string;
  actorName: string;
}

interface RiskEntry {
  id: string;
  riskLevel: string;
  reason: string;
  recordedAt: string;
  recordedBy: string;
}

export default function FamilyTreePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: tree, isLoading: treeLoading } = useQuery({
    queryKey: ['families', id, 'tree'],
    queryFn: async () => {
      const res = await api.get(`/families/${id}/tree`);
      return res.data as FamilyTreeNode;
    },
    enabled: !!id,
  });

  const { data: history = [] } = useQuery({
    queryKey: ['families', id, 'history'],
    queryFn: async () => {
      const res = await api.get(`/families/${id}/history`);
      return (res.data.content || res.data || []) as HistoryEntry[];
    },
    enabled: !!id,
  });

  const { data: riskHistory = [] } = useQuery({
    queryKey: ['families', id, 'risk-history'],
    queryFn: async () => {
      const res = await api.get(`/families/${id}/risk-history`);
      return (res.data.content || res.data || []) as RiskEntry[];
    },
    enabled: !!id,
  });

  if (treeLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;

  const renderNode = (node: FamilyTreeNode, depth = 0) => (
    <div key={node.id} style={{ marginLeft: depth * 24 }} className="mb-2">
      <div className="glass-card px-4 py-3 flex items-center gap-3 inline-flex">
        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-xs font-bold shrink-0">
          {node.name.split(' ').map((w) => w[0]).join('').slice(0, 2)}
        </div>
        <div>
          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{node.name}</p>
          <p className="text-[10px] text-gray-400">{node.role}</p>
        </div>
      </div>
      {node.children && node.children.length > 0 && (
        <div className="ml-6 mt-1 border-l-2 border-gray-200 dark:border-gray-700 pl-3">
          {node.children.map((child) => renderNode(child, depth + 1))}
        </div>
      )}
    </div>
  );

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <button onClick={() => navigate(-1)} className="btn-ghost btn-sm mb-2 -ml-2">
            <ArrowLeft className="w-4 h-4" /> Retour
          </button>
          <div className="flex items-center gap-2 mb-1">
            <GitBranch className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Arbre & historique familial</h1>
          </div>
          <p className="page-subtitle">Structure familiale, historique et suivi des risques</p>
        </div>
      </div>

      {/* Tree */}
      <div className="mb-8">
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
          <Users className="w-4 h-4 text-primary-500" />
          Arbre familial
        </h3>
        {tree ? (
          <div className="glass-card p-5">{renderNode(tree)}</div>
        ) : (
          <div className="glass-card p-8 text-center">
            <GitBranch className="w-8 h-8 text-gray-300 mb-2 mx-auto" />
            <p className="text-gray-500 text-sm">Aucune structure familiale disponible.</p>
          </div>
        )}
      </div>

      {/* History */}
      <div className="mb-8">
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
          <Clock className="w-4 h-4 text-blue-500" />
          Historique
        </h3>
        {history.length === 0 ? (
          <div className="glass-card p-8 text-center">
            <Clock className="w-8 h-8 text-gray-300 mb-2 mx-auto" />
            <p className="text-gray-500 text-sm">Aucun historique.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {history.map((entry) => (
              <div key={entry.id} className="glass-card px-5 py-3 flex items-center gap-3">
                <div className="w-2 h-2 rounded-full bg-blue-500 shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-xs text-gray-700 dark:text-gray-300">
                    <span className="font-semibold">{entry.actorName}</span> — {entry.description}
                  </p>
                  <p className="text-[10px] text-gray-400 mt-0.5">{entry.action}</p>
                </div>
                <span className="text-[10px] text-gray-400 shrink-0">
                  {new Date(entry.timestamp).toLocaleDateString('fr-FR')}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Risk history */}
      <div>
        <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 text-red-500" />
          Historique des risques
        </h3>
        {riskHistory.length === 0 ? (
          <div className="glass-card p-8 text-center">
            <AlertTriangle className="w-8 h-8 text-gray-300 mb-2 mx-auto" />
            <p className="text-gray-500 text-sm">Aucun historique de risque.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {riskHistory.map((risk) => {
              const riskColor = risk.riskLevel === 'A_RISQUE' ? 'bg-red-500' : risk.riskLevel === 'SOUS_SURVEILLANCE' ? 'bg-yellow-500' : 'bg-green-500';
              return (
                <div key={risk.id} className="glass-card px-5 py-3 flex items-center gap-3">
                  <div className={`w-2 h-2 rounded-full ${riskColor} shrink-0`} />
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-semibold text-gray-700 dark:text-gray-300">{risk.riskLevel}</p>
                    <p className="text-[10px] text-gray-400">{risk.reason}</p>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-[10px] text-gray-400">{new Date(risk.recordedAt).toLocaleDateString('fr-FR')}</p>
                    <p className="text-[10px] text-gray-400">par {risk.recordedBy}</p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
