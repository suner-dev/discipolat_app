import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Plus, Pencil, Trash2, Loader2, Save, GripVertical, Menu as MenuIcon,
} from 'lucide-react';
import type { MenuEntry } from '@/types';

interface MenuForm {
  key: string;
  label: string;
  href: string;
  icon: string;
  section: string;
  ordre: number;
  enabled: boolean;
  roles: string[];
}

const EMPTY_FORM: MenuForm = {
  key: '', label: '', href: '', icon: '', section: 'Général', ordre: 0, enabled: true, roles: [],
};

function shuffleArray(array: string[]) {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
}

export default function AdminMenuConstructorPage() {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [form, setForm] = useState<MenuForm>(EMPTY_FORM);

  const { data: menus = [], isLoading } = useQuery({
    queryKey: ['admin', 'menus'],
    queryFn: async () => {
      const res = await api.get('/admin/menus');
      return res.data as MenuEntry[];
    },
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['admin', 'menus'] });

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = {
        key: form.key,
        label: form.label,
        href: form.href,
        icon: form.icon,
        section: form.section,
        ordre: form.ordre,
        enabled: form.enabled,
        roles: form.roles,
      };
      if (editId) {
        await api.put(`/admin/menus/${editId}`, body);
      } else {
        await api.post('/admin/menus', body);
      }
    },
    onSuccess: () => {
      invalidate();
      setModalOpen(false);
      setEditId(null);
      toast.success(editId ? 'Menu mis à jour' : 'Menu créé');
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/admin/menus/${id}`),
    onSuccess: () => { invalidate(); toast.success('Menu supprimé'); },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const openCreate = () => {
    setEditId(null);
    setForm(EMPTY_FORM);
    setModalOpen(true);
  };

  const openEdit = (m: MenuEntry) => {
    setEditId(m.id);
    setForm({
      key: m.key,
      label: m.label || '',
      href: m.href || '',
      icon: m.icon || 'Menu',
      section: m.section || 'Général',
      ordre: m.ordre ?? 0,
      enabled: m.enabled ?? true,
      roles: m.roles ?? [],
    });
    setModalOpen(true);
  };

  if (isLoading) {
    return (
      <div className="min-h-[40vh] flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }

