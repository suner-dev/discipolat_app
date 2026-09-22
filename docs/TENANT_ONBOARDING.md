# Tenant Onboarding Guide — Discipolat Church OS

**Version**: 1.0  
**Date**: 2026-09-22  
**Status**: ✅ PRODUCTION READY

---

## 1. Onboarding Overview

Discipolat provides a **6-step guided wizard** for new church/tenants, taking them from account creation to operational readiness in ~30 minutes.

### 1.1 Prerequisites
- Super Admin creates tenant record
- Tenant Owner receives invitation email
- Tenant Owner has admin access to email

### 1.2 Flow Summary
```
Super Admin Creates Tenant
         │
         ▼
Invitation Sent to Tenant Owner
         │
         ▼
Tenant Owner Accepts → Account Created
         │
         ▼
Wizard Step 1: Profile & Legal
         │
         ▼
Wizard Step 2: Church Identity
         │
         ▼
Wizard Step 3: Campus & Locations
         │
         ▼
Wizard Step 4: Departments & Ministries
         │
         ▼
Wizard Step 5: Families & Teams
         │
         ▼
Wizard Step 6: Launch Configuration
         │
         ▼
🎉 Go Live!
```

---

## 2. Super Admin: Tenant Creation

### 2.1 API
```http
POST /api/platform/admin/tenants
Authorization: Bearer <super_admin_token>
Content-Type: application/json

{
  "name": "Église Vie Nouvelle",
  "slug": "eglise-vie-nouvelle",
  "legalName": "Association Vie Nouvelle",
  "country": "FR",
  "city": "Paris",
  "timezone": "Europe/Paris",
  "currency": "EUR",
  "language": "fr",
  "planKey": "STARTUP",
  "contactEmail": "pasteur@vienouvelle.fr",
  "contactPhone": "+33123456789"
}
```

### 2.2 Auto-Seeding (Triggered on Creation)
| Component | Action |
|-----------|--------|
| `organization_nodes` | Creates ROOT_CHURCH (Level 0) |
| `roles` | Seeds 9 tenant roles + permissions |
| `modules` | Enables CORE + EXISTING modules per plan |
| `saas_plan_quotas` | Applies plan limits (members, spaces, storage, AI credits) |
| `custom_status_sets` | Seeds default statuses for events, tasks, assets |
| `workflow_definitions` | Seeds standard approval workflows |
| `space_templates` | Makes 20 templates available |

### 2.3 Invitation
```http
POST /api/invitations
Authorization: Bearer <super_admin_token>
Content-Type: application/json

{
  "email": "pasteur@vienouvelle.fr",
  "roleCode": "TENANT_OWNER",
  "tenantId": "<new_tenant_id>",
  "organizationUnitId": "<root_church_id>",
  "spaceId": null,
  "message": "Bienvenue ! Configurez votre église en 6 étapes."
}
```

---

## 3. Wizard Steps (Frontend)

### 3.1 Step 1: Profile & Legal
**Route**: `/onboarding/1-profile`  
**API**: `PUT /api/tenants/{id}`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `legalName` | string | ✅ | 2-200 chars |
| `registrationNumber` | string | ❌ | Country-specific format |
| `address` | string | ✅ | 5-500 chars |
| `phone` | string | ✅ | E.164 format |
| `email` | string | ✅ | Valid email |
| `website` | string | ❌ | Valid URL |
| `description` | string | ❌ | Max 2000 chars |

**Auto-save**: Every 30s + on field blur

---

### 3.2 Step 2: Church Identity
**Route**: `/onboarding/2-identity`  
**API**: `PUT /api/tenants/{id}/branding`

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `logo` | file | ✅ | PNG/SVG, max 2MB, auto-resized |
| `coverImage` | file | ❌ | 16:9 ratio, max 5MB |
| `primaryColor` | string | ✅ | Hex, accessible contrast |
| `secondaryColor` | string | ✅ | Hex |
| `accentColor` | string | ❌ | Hex |
| `fontFamily` | string | ❌ | Google Fonts selector |
| `favicon` | file | ❌ | 32x32 PNG/ICO |

**Preview**: Real-time preview of public pages & mobile app

---

### 3.3 Step 3: Campus & Locations
**Route**: `/onboarding/3-campuses`  
**API**: `POST /api/org/campus` (repeatable)

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `name` | string | ✅ | e.g., "Campus Centre-Ville" |
| `code` | string | ✅ | Unique, auto-slugified |
| `address` | string | ✅ | Full address |
| `timezone` | string | ❌ | Inherits from tenant |
| `pastorId` | UUID | ❌ | Existing person or create new |
| `isMain` | boolean | ❌ | First campus = main |

**Minimum**: 1 campus required  
**Maximum**: Per plan quota (STARTUP: 2, GROWTH: 5, NETWORK: unlimited)

---

### 3.4 Step 4: Departments & Ministries
**Route**: `/onboarding/4-departments`  
**API**: `POST /api/org/units` (type: MINISTRY/DEPARTMENT)

**Pre-seeded Ministry Templates**:
| Ministry | Departments | Space Template |
|----------|-------------|----------------|
| Louange | Chant, Musiciens, Technique | `DEPT_WORSHIP` |
| Enfants | Crèche, École du Dimanche, Pré-ados | `DEPT_CHILDREN` |
| Jeunesse | Collégiens, Lycéens, Jeunes Adultes | `DEPT_YOUTH` |
| Accueil | Accueil, Parking, Sécurité | `DEPT_WELCOME` |
| Action Sociale | Banque Alimentaire, Visites, Aide | `DEPT_OUTREACH` |
| Administration | Secrétariat, Finances, Communication | `DEPT_ADMIN` |

**Custom**: Add custom ministries/departments

---

### 3.5 Step 5: Families & Teams
**Route**: `/onboarding/5-families`  
**API**: `POST /api/org/units` (type: FAMILY/TEAM/CELL)

**Options**:
- **Quick Mode**: Import from CSV (Name, Leader Email, Members)
- **Guided Mode**: Create family by family
- **Skip**: Configure later (recommended for large churches)

**Family Template**: `FAMILY_STANDARD` (visits, meetings, prayer, activities)

---

### 3.6 Step 6: Launch Configuration
**Route**: `/onboarding/6-launch`  
**API**: Multiple

| Configuration | API | Required |
|---------------|-----|----------|
| Enable modules | `PUT /api/tenant/features` | ✅ |
| Set default space template | `PUT /api/spaces/{id}/template` | ❌ |
| Configure notifications | `PUT /api/tenants/{id}/notifications` | ❌ |
| Set up payment methods | `POST /api/payments/providers` | ❌ |
| Invite key leaders | `POST /api/invitations` (bulk) | ❌ |
| Schedule launch date | `PUT /api/tenants/{id}/launch-date` | ❌ |
| **Confirm & Go Live** | `POST /api/tenants/{id}/launch` | ✅ |

---

## 4. Post-Launch Checklist

### 4.1 Immediate (Day 0)
- [ ] Tenant status = `ACTIVE`
- [ ] Owner can login & access dashboard
- [ ] Branding applied on all pages
- [ ] Modules enabled per plan
- [ ] At least 1 campus + 1 department created

### 4.2 Week 1
- [ ] Key leaders invited & onboarded
- [ ] First event created & published
- [ ] Members imported (CSV or manual)
- [ ] Payment provider configured (if receiving donations)
- [ ] Mobile app tested by 2+ users

### 4.3 Month 1
- [ ] 80%+ members active
- [ ] First financial reconciliation complete
- [ ] AI credits usage < 50% of quota
- [ ] Support tickets < 5
- [ ] NPS survey sent

---

## 5. API Reference

### 5.1 Tenant Onboarding Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/onboarding/status` | Current wizard step |
| `PUT` | `/api/onboarding/step/{step}` | Update step data |
| `POST` | `/api/onboarding/complete` | Mark onboarding complete |
| `GET` | `/api/onboarding/templates` | List space/dept templates |
| `POST` | `/api/onboarding/import-members` | Bulk import from CSV |

### 5.2 Invitation Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/invitations` | Create invitation |
| `GET` | `/api/invitations` | List invitations (tenant) |
| `POST` | `/api/invitations/{id}/resend` | Resend email |
| `POST` | `/api/invitations/{id}/revoke` | Revoke invitation |
| `GET` | `/api/invitations/validate/{token}` | Validate token (public) |
| `POST` | `/api/invitations/accept/{token}` | Accept invitation (public) |

---

## 6. Frontend Implementation

### 6.1 Onboarding Wizard Component
```tsx
// pages/OnboardingWizard.tsx
const steps = [
  { id: 1, title: 'Profil & Légal', component: ProfileStep, api: '/api/tenants/{id}' },
  { id: 2, title: 'Identité Visuelle', component: BrandingStep, api: '/api/tenants/{id}/branding' },
  { id: 3, title: 'Campus & Lieux', component: CampusesStep, api: '/api/org/campus' },
  { id: 4, title: 'Départements', component: DepartmentsStep, api: '/api/org/units' },
  { id: 5, title: 'Familles & Équipes', component: FamiliesStep, api: '/api/org/units' },
  { id: 6, title: 'Lancement', component: LaunchStep, api: '/api/tenants/{id}/launch' },
];

// Features:
// - Progress persistence (localStorage + server)
// - Step validation (client + server)
// - Skip optional steps
// - Resume from last step
// - Mobile responsive
```

### 6.2 State Management
```typescript
// hooks/useOnboarding.ts
interface OnboardingState {
  currentStep: number;
  completedSteps: number[];
  tenantData: Partial<Tenant>;
  brandingData: BrandingData;
  campuses: CampusData[];
  departments: OrgUnitData[];
  families: OrgUnitData[];
  launchConfig: LaunchConfig;
}
```

---

## 7. Mobile Onboarding

### 7.1 Deep Link Flow
```
Email Invitation
       │
       ▼
https://app.discipolat.com/accept/{token}
       │
       ▼
Mobile App Opens (Universal Link)
       │
       ▼
AcceptInvitationScreen
       │
       ▼
Auto-login → Dashboard
       │
       ▼
Onboarding Banner: "Configurez votre église"
       │
       ▼
Redirects to Web Wizard (deep link to step)
```

### 7.2 Mobile-First Screens
- `InvitationAcceptScreen.dart` — Token validation + profile creation
- `OnboardingBanner.dart` — Persistent banner until complete
- `QuickSetupScreen.dart` — Minimal 3-step mobile setup

---

## 8. Email Templates

### 8.1 Invitation Email
```
Subject: Bienvenue sur Discipolat — Configurez votre église

Body:
Bonjour {firstName},

{pastorName} vous invite à rejoindre {tenantName} sur Discipolat.

👉 Accepter l'invitation : {acceptLink}
⏰ Expire dans 7 jours

Une fois connecté, un assistant vous guidera en 6 étapes pour configurer votre église.

L'équipe Discipolat
```

### 8.2 Reminder Emails
- Day 3: "Votre invitation expire dans 4 jours"
- Day 6: "Dernier rappel - expire demain"

---

## 9. Error Handling & Recovery

| Scenario | Recovery |
|----------|----------|
| Email not received | Resend from Super Admin dashboard |
| Token expired | Super Admin creates new invitation |
| Wizard crash | Auto-save restores to last step |
| Validation error | Inline field errors + toast |
| Plan quota exceeded | Upgrade prompt or contact sales |
| Duplicate slug | Auto-suggest alternatives |

---

## 10. Testing Scenarios

### 10.1 Happy Path
1. Super Admin creates tenant → invitation sent
2. Owner clicks link → accepts → wizard starts
3. All 6 steps completed → tenant launched
4. Owner logs in → dashboard operational
5. Members invited → join successfully

### 10.2 Edge Cases
- Owner accepts on mobile → continues on web
- Multiple campuses added → wizard adapts
- CSV import with errors → row-level feedback
- Payment setup skipped → enabled later
- Launch delayed → tenant stays in `ONBOARDING` status

---

## 11. Monitoring & Metrics

| Metric | Target | Alert |
|--------|--------|-------|
| Invitation → Acceptance rate | > 80% | < 60% |
| Wizard completion rate | > 70% | < 50% |
| Time to launch | < 30 min | > 60 min |
| Drop-off by step | < 10% per step | > 20% |
| Support tickets (onboarding) | < 2/tenant | > 5 |

---

## 12. References

- `docs/MULTI_TENANT_ARCHITECTURE.md` — Architecture
- `docs/ADMINISTRATION_MODEL.md` — Admin workflows
- `docs/ORGANIZATION_HIERARCHY.md` — Hierarchy setup
- `frontend/src/pages/OnboardingWizard.tsx`
- `mobile/lib/features/onboarding/`
- `backend/src/main/java/com/discipolat/modules/onboarding/`