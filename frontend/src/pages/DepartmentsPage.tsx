import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api, { getErrorMessage } from '@/lib/api';
import DataTable from '@/components/shared/DataTable';
import type { Department, User, PageResponse } from '@/types';
import type { ColumnDef } from '@/types/table';
import { Building2, Plus, Pencil, Trash2, Loader2, X, Calendar, UserPlus, FolderOpen, Search, Filter } from 'lucide-react';
import toast from 'react-hot-toast';

export default function DepartmentsPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Department | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);
  const [formData, setFormData] = useState({ nom: '', description: '', responsableId: '', createNewResponsable: false, newRespFirstName: '', newRespLastName: '', newRespEmail: '', newRespPhone: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['departments', page],
    queryFn: async () => {
      const res = await api.get(`/departments?size=20&page=${page}`);
      return res.data as PageResponse<Department>;
    },
  });

  const { data: users } = useQuery({
    queryKey: ['users', 'responsables'],
    queryFn: async () => {
      const res = await api.get('/users?role=RESPONSABLE&size=100');
      return res.data.content as User[];
    },
  });

  const getResponsableName = (responsableId: string) => {
    const user = users?.find((u) => u.id === responsableId);
    return user ? `${user.firstName} ${user.lastName}` : responsableId.slice(0, 8) + '...';
  };

  const createMutation = useMutation({
    mutationFn: async (data: typeof formData) => {
      await api.post('/departments', data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments'] });
      toast.success('Département créé avec succès');
      setShowModal(false);
      resetForm();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof formData }) => {
      await api.put(`/departments/${id}`, data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments'] });
      toast.success('Département mis à jour');
      setShowModal(false);
      setEditing(null);
      resetForm();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/departments/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments'] });
      toast.success('Département archivé');
      setShowDeleteConfirm(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const resetForm = () => setFormData({ nom: '', description: '', responsableId: '', createNewResponsable: false, newRespFirstName: '', newRespLastName: '', newRespEmail: '', newRespPhone: '' });

  const openCreate = () => {
    setEditing(null);
    resetForm();
    setShowModal(true);
  };

  const openEdit = (dept: Department) => {
    setEditing(dept);
    setFormData({ nom: dept.nom, description: dept.description || '', responsableId: dept.responsableId, createNewResponsable: false, newRespFirstName: '', newRespLastName: '', newRespEmail: '', newRespPhone: '' });
    setShowModal(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editing) {
      updateMutation.mutate({ id: editing.id, data: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  const columns: ColumnDef<Department>[] = [
    {
      header: 'Nom',
      cell: (dept) => (
        <div>
          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{dept.nom}</p>
          {dept.description && (
            <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-[200px]">{dept.description}</p>
          )}
        </div>
      ),
    },
    {
      header: 'Responsable',
      cell: (dept) => (
        <span className="text-sm text-gray-700 dark:text-gray-300">
          {getResponsableName(dept.responsableId)}
        </span>
      ),
    },
    {
      header: 'Statut',
      cell: (dept) => (
        <span className={dept.statut === 'ACTIVE' ? 'badge-success' : 'badge-gray'}>
          {dept.statut === 'ACTIVE' ? 'Actif' : 'Inactif'}
        </span>
      ),
    },
    {
      header: 'Créé le',
      cell: (dept) => (
        <span className="text-xs text-gray-500 dark:text-gray-400 flex items-center gap-1">
          <Calendar className="w-3 h-3" />
          {new Date(dept.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })}
        </span>
      ),
    },
    {
      header: 'Actions',
      cell: (dept) => (
        <div className="flex gap-1">
          <button onClick={() => navigate(`/departments/${dept.id}`)} className="btn-ghost btn-sm text-amber-600" title="Voir le détail">
            <FolderOpen className="w-4 h-4" />
          </button>
          <button onClick={() => openEdit(dept)} className="btn-ghost btn-sm text-blue-600" title="Modifier">
            <Pencil className="w-4 h-4" />
          </button>
          <button
            onClick={() => setShowDeleteConfirm(dept.id)}
            className="btn-ghost btn-sm text-red-500"
            title="Archiver"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      ),
    },
  ];

  const allDepartements = data?.content || [];
  const filteredDepartements = search.trim()
    ? allDepartements.filter(d => d.nom.toLowerCase().includes(search.toLowerCase()) || d.description?.toLowerCase().includes(search.toLowerCase()))
    : allDepartements;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Building2 className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Départements</h1>
          </div>
          <p className="page-subtitle">Gestion des départements et équipes</p>
        </div>
        <button onClick={openCreate} className="btn-primary btn-sm animate-scale-in">
          <Plus className="w-4 h-4" /> Nouveau département
        </button>
      </div>

      {/* Search */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="flex items-center gap-3">
          <Search className="w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Rechercher un département..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="flex-1 bg-transparent border-none outline-none text-sm text-gray-900 dark:text-gray-100 placeholder-gray-400"
          />
        </div>
      </div>

      <DataTable<Department>
        columns={columns}
        data={filteredDepartements}
        isLoading={isLoading}
        emptyMessage="Aucun département trouvé"
        emptyIcon={<Building2 className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />}
        onRowClick={(dept) => navigate(`/departments/${dept.id}`)}
      />

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Page {data.number + 1} / {data.totalPages}
          </p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-secondary btn-sm">Suivant</button>
          </div>
        </div>
      )}

      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowModal(false)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-md animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                  <Building2 className="w-5 h-5 text-primary-600" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                    {editing ? 'Modifier le département' : 'Nouveau département'}
                  </h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {editing ? 'Modifiez les informations ci-dessous' : 'Configurez le nouveau département'}
                  </p>
                </div>
              </div>
              <button onClick={() => setShowModal(false)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="label">Nom du département *</label>
                <input
                  className="input"
                  value={formData.nom}
                  onChange={(e) => setFormData({ ...formData, nom: e.target.value })}
                  placeholder="Ex: Département des jeunes"
                  required
                />
              </div>
              <div>
                <label className="label">Description</label>
                <textarea
                  className="input"
                  rows={3}
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Description du département..."
                />
              </div>
              <div>
                <label className="label">Responsable</label>
                {editing ? (
                  <select
                    className="input"
                    value={formData.responsableId}
                    onChange={(e) => setFormData({ ...formData, responsableId: e.target.value })}
                    required
                  >
                    <option value="">Sélectionner un responsable...</option>
                    {users?.map((u) => (
                      <option key={u.id} value={u.id}>
                        {u.firstName} {u.lastName} ({u.email})
                      </option>
                    ))}
                  </select>
                ) : (
                  <>
                    <div className="grid grid-cols-2 gap-2 mb-3">
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, createNewResponsable: false })}
                        className={`p-2.5 rounded-xl text-xs font-medium transition-all border-2
                          ${!formData.createNewResponsable
                            ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300'
                            : 'border-gray-200 dark:border-gray-700 text-gray-500'}`}
                      >
                        Sélectionner un responsable
                      </button>
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, createNewResponsable: true })}
                        className={`p-2.5 rounded-xl text-xs font-medium transition-all border-2
                          ${formData.createNewResponsable
                            ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300'
                            : 'border-gray-200 dark:border-gray-700 text-gray-500'}`}
                      >
                        <UserPlus className="w-3.5 h-3.5 inline mr-1 -mt-0.5" />
                        Créer un responsable
                      </button>
                    </div>
                    {!formData.createNewResponsable ? (
                      <select
                        className="input"
                        value={formData.responsableId}
                        onChange={(e) => setFormData({ ...formData, responsableId: e.target.value })}
                      >
                        <option value="">Sélectionner un responsable...</option>
                        {users?.map((u) => (
                          <option key={u.id} value={u.id}>
                            {u.firstName} {u.lastName} ({u.email})
                          </option>
                        ))}
                      </select>
                    ) : (
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <input
                          className="input"
                          value={formData.newRespFirstName}
                          onChange={(e) => setFormData({ ...formData, newRespFirstName: e.target.value })}
                          placeholder="Prénom *"
                        />
                        <input
                          className="input"
                          value={formData.newRespLastName}
                          onChange={(e) => setFormData({ ...formData, newRespLastName: e.target.value })}
                          placeholder="Nom *"
                        />
                        <input
                          type="email"
                          className="input"
                          value={formData.newRespEmail}
                          onChange={(e) => setFormData({ ...formData, newRespEmail: e.target.value })}
                          placeholder="Email *"
                        />
                        <input
                          className="input"
                          value={formData.newRespPhone}
                          onChange={(e) => setFormData({ ...formData, newRespPhone: e.target.value })}
                          placeholder="Téléphone"
                        />
                        <p className="text-[10px] text-gray-400 col-span-full">
                          Le compte du responsable sera créé automatiquement avec le rôle RESPONSABLE et affecté à ce département.
                        </p>
                      </div>
                    )}
                  </>
                )}
              </div>
              <div className="flex justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
                <button type="button" onClick={() => setShowModal(false)} className="btn-secondary">
                  Annuler
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={createMutation.isPending || updateMutation.isPending}
                >
                  {(createMutation.isPending || updateMutation.isPending) ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : editing ? (
                    <Pencil className="w-4 h-4" />
                  ) : (
                    <Plus className="w-4 h-4" />
                  )}
                  {editing ? 'Enregistrer' : 'Créer le département'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setShowDeleteConfirm(null)}>
          <div className="card p-6 w-full max-w-sm mx-4 animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                <Trash2 className="w-5 h-5 text-red-600" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Archiver le département</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Le département sera archivé. Cette action peut être annulée.
                </p>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowDeleteConfirm(null)} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => deleteMutation.mutate(showDeleteConfirm)}
                disabled={deleteMutation.isPending}
                className="btn-primary btn-sm bg-red-600 hover:bg-red-700"
              >
                {deleteMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                Archiver
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
