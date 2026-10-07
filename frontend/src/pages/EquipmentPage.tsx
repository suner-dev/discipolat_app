import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { Wrench, Loader2 } from 'lucide-react';
import { InventoryTab } from '@/components/departments/InventoryTab';

import { tText } from '@/i18n';

// audit-routes: /departments
interface Department {
  id: string;
  nom: string;
  description?: string;
}

/**
 * Équipements — la capacité réelle est scopée par département
 * (GET/POST/PUT/DELETE /departments/{departmentId}/equipment). Il n'existe
 * aucune route globale /equipment. La page liste donc les départements,
 * laisse l'utilisateur en choisir un, puis délègue le CRUD à InventoryTab
 * (implémentation de référence, réutilisée pour éviter toute duplication).
 */
export default function EquipmentPage() {
  const [deptId, setDeptId] = useState('');

  const { data: departments = [], isLoading } = useQuery<Department[]>({
    queryKey: ['departments', 'equipment-page'],
    queryFn: async () => {
      const res = await api.get('/departments', { params: { size: 100 } });
      return (res.data?.content ?? res.data ?? []) as Department[];
    },
  });

  const selected = deptId || departments[0]?.id || '';

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-orange-500 to-amber-600 text-white shadow-lg">
          <Wrench className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">{tText('Équipement')}</h1>
          <p className="page-subtitle">{tText('Gestion du matériel par département')}</p>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : departments.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">{tText('Aucun département')}</div>
      ) : (
        <>
          <div className="glass-card p-4 mb-6">
            <label className="label" htmlFor="dept-select">{tText('Département')}</label>
            <select
              id="dept-select"
              className="input w-full md:max-w-sm"
              value={selected}
              onChange={(e) => setDeptId(e.target.value)}
            >
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.nom}</option>
              ))}
            </select>
          </div>

          {selected && <InventoryTab key={selected} deptId={selected} onChanged={() => {}} />}
        </>
      )}
    </div>
  );
}
