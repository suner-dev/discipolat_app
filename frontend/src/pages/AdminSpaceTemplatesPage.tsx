import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, Edit, Trash2, Copy, Loader2, AlertCircle } from 'lucide-react';
import { api } from '../lib/api';


interface SpaceTemplate {
  id: string;
  code: string;
  name: string;
  description: string | null;
  icon: string | null;
  color: string | null;
  version: number;
  modulesJson: string[];
  defaultWorkflowsJson: any[];
  defaultStatusesJson: any[];
  defaultDashboardsJson: any[];
  createdAt: string;
  updatedAt: string;
}

const AdminSpaceTemplatesPage: React.FC = () => {
  const navigate = useNavigate();
  const [templates, setTemplates] = useState<SpaceTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<SpaceTemplate | null>(null);
  const [formData, setFormData] = useState<Partial<SpaceTemplate>>({
    code: '',
    name: '',
    description: '',
    icon: '',
    color: '#3b82f6',
    modulesJson: [],
    defaultWorkflowsJson: [],
    defaultStatusesJson: [],
    defaultDashboardsJson: [],
  });

  const fetchTemplates = async () => {
    try {
      setLoading(true);
      const res = await api.get<SpaceTemplate[]>('/api/v1/space-templates/list');
      setTemplates(res.data);
      setError(null);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Erreur lors du chargement des templates');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTemplates();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editing) {
        await api.put(`/api/v1/space-templates/${editing.id}`, formData);
      } else {
        await api.post('/api/v1/space-templates', formData);
      }
      setShowForm(false);
      setEditing(null);
      resetForm();
      fetchTemplates();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Erreur lors de la sauvegarde');
    }
  };

  const handleEdit = (template: SpaceTemplate) => {
    setEditing(template);
    setFormData({
      code: template.code,
      name: template.name,
      description: template.description || '',
      icon: template.icon || '',
      color: template.color || '#3b82f6',
      modulesJson: template.modulesJson || [],
      defaultWorkflowsJson: template.defaultWorkflowsJson || [],
      defaultStatusesJson: template.defaultStatusesJson || [],
      defaultDashboardsJson: template.defaultDashboardsJson || [],
    });
    setShowForm(true);
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Supprimer ce template ? Cette action est irréversible.')) return;
    try {
      await api.delete(`/api/v1/space-templates/${id}`);
      fetchTemplates();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Erreur lors de la suppression');
    }
  };

  const handleDuplicate = async (template: SpaceTemplate) => {
    try {
      const newTemplate = { ...template, code: `${template.code}_COPY`, name: `${template.name} (copie)` };
      delete (newTemplate as any).id;
      delete (newTemplate as any).createdAt;
      delete (newTemplate as any).updatedAt;
      await api.post('/api/v1/space-templates', newTemplate);
      fetchTemplates();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Erreur lors de la duplication');
    }
  };

  const handleCreateSpace = (template: SpaceTemplate) => {
    navigate(`/admin/spaces/create?template=${template.code}`);
  };

  const resetForm = () => {
    setFormData({
      code: '',
      name: '',
      description: '',
      icon: '',
      color: '#3b82f6',
      modulesJson: [],
      defaultWorkflowsJson: [],
      defaultStatusesJson: [],
      defaultDashboardsJson: [],
    });
  };

  const filteredTemplates = templates.filter(t =>
    t.name.toLowerCase().includes(search.toLowerCase()) ||
    t.code.toLowerCase().includes(search.toLowerCase()) ||
    t.description?.toLowerCase().includes(search.toLowerCase())
  );

  if (showForm) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900">
            {editing ? 'Modifier le template' : 'Nouveau template d\'espace'}
          </h1>
          <button
            onClick={() => { setShowForm(false); setEditing(null); resetForm(); }}
            className="text-gray-500 hover:text-gray-700"
          >
            ← Retour
          </button>
        </div>

        {error && <div className="mb-4 p-4 bg-red-50 text-red-700 rounded-lg">{error}</div>}

        <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Code *</label>
              <input
                type="text"
                value={formData.code || ''}
                onChange={e => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="AUDIOVISUAL"
                required
                disabled={!!editing}
              />
              <p className="text-xs text-gray-500 mt-1">Unique, majuscules, underscores autorisés</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Nom *</label>
              <input
                type="text"
                value={formData.name || ''}
                onChange={e => setFormData({ ...formData, name: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="Audiovisuel"
                required
              />
            </div>
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea
                value={formData.description || ''}
                onChange={e => setFormData({ ...formData, description: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                rows={3}
                placeholder="Description du template..."
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Icône (Lucide)</label>
              <input
                type="text"
                value={formData.icon || ''}
                onChange={e => setFormData({ ...formData, icon: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="Video, Music, Truck..."
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Couleur</label>
              <input
                type="color"
                value={formData.color || '#3b82f6'}
                onChange={e => setFormData({ ...formData, color: e.target.value })}
                className="w-full h-10 border border-gray-300 rounded-lg cursor-pointer"
              />
            </div>
          </div>

          <div className="border-t pt-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Modules activés par défaut</h3>
            <div className="flex flex-wrap gap-2">
              {['people', 'teams', 'assets', 'inventory', 'maintenance', 'events', 'tasks', 'finance', 'reports', 'archive',
                'rehearsal', 'repertoire', 'budget', 'prayer', 'discipleship', 'visits', 'receptions',
                'checklists', 'attendance', 'safety', 'streaming', 'media', 'gifts', 'fasting',
                'campaigns', 'kits', 'staff_duty', 'patients', 'consultations', 'pharmacy',
                'announcements', 'social', 'newsletter', 'website', 'incidents', 'access'].map(mod => (
                <label key={mod} className="inline-flex items-center gap-2 px-3 py-1 border rounded-lg cursor-pointer hover:bg-gray-50">
                  <input
                    type="checkbox"
                    checked={formData.modulesJson?.includes(mod) || false}
                    onChange={e => setFormData({
                      ...formData,
                      modulesJson: e.target.checked
                        ? [...(formData.modulesJson || []), mod]
                        : (formData.modulesJson || []).filter(m => m !== mod)
                    })}
                    className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                  <span className="text-sm capitalize">{mod.replace('_', ' ')}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t">
            <button
              type="button"
              onClick={() => { setShowForm(false); setEditing(null); resetForm(); }}
              className="px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              Annuler
            </button>
            <button
              type="submit"
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
              disabled={loading}
            >
              {loading ? <Loader2 className="w-4 h-4 animate-spin inline mr-2" /> : null}
              {editing ? 'Mettre à jour' : 'Créer'}
            </button>
          </div>
        </form>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Templates d'espaces</h1>
          <p className="text-gray-500 mt-1">Créez un espace en 1 clic depuis un template métier prédéfini</p>
        </div>
        <button
          onClick={() => { setEditing(null); resetForm(); setShowForm(true); }}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 flex items-center gap-2"
        >
          <Plus className="w-4 h-4" /> Nouveau template
        </button>
      </div>

      {error && <div className="mb-4 p-4 bg-red-50 text-red-700 rounded-lg flex items-center justify-between">
        <span>{error}</span>
        <button onClick={() => setError(null)} className="text-red-500 hover:text-red-700">×</button>
      </div>}

      <div className="mb-4">
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Rechercher un template..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
        </div>
      ) : filteredTemplates.length === 0 ? (
        <div className="text-center py-12">
          <AlertCircle className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900">Aucun template trouvé</h3>
          <p className="text-gray-500 mt-1">{search ? 'Essayez une autre recherche' : 'Créez votre premier template'}</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredTemplates.map(template => (
            <div key={template.id} className="bg-white rounded-xl border border-gray-200 hover:shadow-md transition-shadow p-5">
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-3">
                  <div className={`w-12 h-12 rounded-lg flex items-center justify-center ${template.color ? `bg-[${template.color}]/10` : 'bg-blue-100'}`}>
                    {template.icon && (
                      <span className="text-xl">{template.icon}</span>
                    )}
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">{template.name}</h3>
                    <span className="text-xs text-gray-500 uppercase tracking-wide">{template.code}</span>
                  </div>
                </div>
                <span className="text-xs px-2 py-1 bg-green-100 text-green-700 rounded-full">v{template.version}</span>
              </div>

              {template.description && (
                <p className="text-sm text-gray-600 mb-4 line-clamp-2">{template.description}</p>
              )}

              <div className="flex flex-wrap gap-1 mb-4">
                {template.modulesJson?.slice(0, 6).map((mod, i) => (
                  <span key={i} className="px-2 py-0.5 text-xs bg-gray-100 text-gray-700 rounded capitalize">
                    {mod.replace('_', ' ')}
                  </span>
                ))}
                {template.modulesJson && template.modulesJson.length > 6 && (
                  <span className="px-2 py-0.5 text-xs bg-gray-100 text-gray-500 rounded">
                    +{template.modulesJson.length - 6}
                  </span>
                )}
              </div>

              <div className="flex items-center gap-2 pt-3 border-t">
                <button
                  onClick={() => handleCreateSpace(template)}
                  className="flex-1 px-3 py-1.5 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 flex items-center justify-center gap-1"
                >
                  <Plus className="w-3 h-3" /> Créer espace
                </button>
                <button
                  onClick={() => handleEdit(template)}
                  className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg"
                  title="Modifier"
                >
                  <Edit className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleDuplicate(template)}
                  className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg"
                  title="Dupliquer"
                >
                  <Copy className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleDelete(template.id)}
                  className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg"
                  title="Supprimer"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default AdminSpaceTemplatesPage;