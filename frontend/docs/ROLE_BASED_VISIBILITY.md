# Role-Based Visibility Pattern

## Overview

Every dashboard in the application uses a consistent **`canManage` guard** pattern to control which UI sections are visible based on the user's active role. This ensures that:

- **Workspace owners** get full CRUD access (create, edit, delete)
- **Super-users** (ADMIN/PASTEUR) see a read-only overview without management actions
- **Other roles** don't see dashboards they don't own (enforced by route protection)

## Pattern

```tsx
import { useAuth } from '@/contexts/AuthContext';

export default function SomeDashboardPage() {
  const { user, activeRole } = useAuth();
  const canManage = activeRole === 'OWNER_ROLE';

  return (
    <div>
      {/* Always visible — data, stats, charts */}
      <StatsSection />

      {/* Only visible to the workspace owner */}
      {canManage && (
        <ManagementSection />
      )}
    </div>
  );
}
```

## Dashboard Matrix

| Dashboard | Route | Owner Role | `canManage` | Guarded Sections |
|---|---|---|---|---|
| **AdminDashboardPage** | `/admin` | `ADMIN` | `activeRole === 'ADMIN'` | None (route-locked, pattern for consistency) |
| **PasteurDashboardPage** | `/dashboard/pasteur` | `PASTEUR` | `activeRole === 'PASTEUR'` | Export button, manage links, transfers |
| **ResponsableDashboardPage** | `/dashboard/responsable` | `RESPONSABLE` | `activeRole === 'RESPONSABLE'` | Presence input, event attendance, quick actions, 3 stat cards |
| **ChefFamilleDashboardPage** | `/dashboard/chef-famille` | `CHEF_DE_FAMILLE` | `activeRole === 'CHEF_DE_FAMILLE'` | Faiseur workload, network view |
| **MemberDashboardPage** | `/dashboard/membre` | `MEMBRE` | `activeRole === 'MEMBRE'` | Edit profile, submit presence, send requests |
| **DepartmentManagementPage** | `/departments/:id/manage` | `RESPONSABLE` | Tab visibility by role | Membres, Équipes, Tâches tabs (RESPONSABLE only) |

## What Gets Guarded

### Management Actions (guarded by `canManage`)
- **Buttons** that create, edit, or delete data
- **Forms** for input (presence, requests, team creation)
- **Links** to management pages ("Gérer", "Voir tout")
- **Export** functionality
- **Workflow** actions (transfers, approvals)

### Read-Only Sections (always visible)
- **Stats/KPIs** — numbers, charts, progress bars
- **Data tables** — member lists, family lists, department lists
- **Charts** — pie charts, bar charts, trend lines
- **Alerts** — notification badges, warning indicators
- **History** — activity feeds, presence history

## Implementation Steps

### 1. Add `useAuth` import
```tsx
import { useAuth } from '@/contexts/AuthContext';
```

### 2. Extract `activeRole` and define `canManage`
```tsx
const { user, activeRole } = useAuth();
const canManage = activeRole === 'OWNER_ROLE';
```

### 3. Guard management sections
```tsx
{canManage && (
  <button onClick={handleCreate}>Créer</button>
)}
```

### 4. Keep data sections unconditional
```tsx
{/* Always visible */}
<div className="stat-card">
  <span className="stat-value">{data.length}</span>
</div>
```

## Route Protection vs UI Guards

**Route protection** (`ProtectedRoute`) controls which roles can *access* a page:
```tsx
<Route path="/dashboard/responsable" element={
  <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
    <ResponsableDashboardPage />
  </ProtectedRoute>
} />
```

**UI guards** (`canManage`) control what the user can *do* on the page:
```tsx
{canManage && <ManagementSection />}
```

Both work together:
1. Route protection ensures only authorized roles reach the page
2. UI guards ensure the right actions are visible within the page

## Examples

### ResponsableDashboardPage
```tsx
const canManage = activeRole === 'RESPONSABLE';

// ✅ Always visible — KPIs
<div className="stat-card">
  <span className="stat-value">{stats.totalMembres}</span>
</div>

// ✅ Guarded — presence input
{canManage && (
  <div id="saisie-presences">
    <button onClick={submitPresences}>Enregistrer</button>
  </div>
)}

// ✅ Guarded — management links
{canManage && (
  <Link to={`/departments/${id}/manage`}>Gérer</Link>
)}
```

### ChefFamilleDashboardPage
```tsx
const canManage = activeRole === 'CHEF_DE_FAMILLE';

// ✅ Always visible — disciples list
<table>
  {disciples.map(soul => <tr key={soul.id}>...</tr>)}
</table>

// ✅ Guarded — faiseur workload
{canManage && workload && (
  <div className="glass-card">
    <h3>Charge de travail des Faiseurs</h3>
  </div>
)}
```

## Testing

When testing role-based visibility:

1. **Mock `useAuth`** with the target role:
```tsx
vi.mock('@/contexts/AuthContext', async () => {
  const actual = await vi.importActual('@/contexts/AuthContext');
  return {
    ...actual,
    useAuth: vi.fn().mockReturnValue({
      activeRole: 'RESPONSABLE',
      user: { role: 'RESPONSABLE', roles: ['RESPONSABLE'] },
    }),
  };
});
```

2. **Assert guarded sections appear/disappear**:
```tsx
// As RESPONSABLE — management visible
expect(screen.getByText('Enregistrer')).toBeInTheDocument();

// As ADMIN — management hidden
expect(screen.queryByText('Enregistrer')).not.toBeInTheDocument();
```

## Key Principles

1. **Never hide data** — stats, charts, tables are always visible to authorized roles
2. **Guard actions only** — buttons, forms, links that modify data
3. **One pattern everywhere** — `canManage = activeRole === 'OWNER_ROLE'`
4. **Route protection + UI guards** — defense in depth
5. **Super-users see read-only** — ADMIN/PASTEUR get overview, not management
