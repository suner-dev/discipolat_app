// Types pour le multi-tenant et l'administration

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'CANCELLED' | 'PENDING_SETUP';
  plan: string;
  country: string;
  currency: string;
  timezone: string;
  locale: string;
  createdAt: string;
  updatedAt: string;
  branding?: BrandingConfig;
  features?: Record<string, boolean>;
  settings?: TenantSettings;
}

export interface TenantMembership {
  id: string;
  tenantId: string;
  userId: string;
  role: string;
  roleId?: string;
  scopeType: 'TENANT' | 'REGION' | 'CHURCH' | 'SUB_CHURCH' | 'CAMPUS' | 'DEPARTMENT' | 'FAMILY' | 'ASSIGNED' | 'OWN';
  scopeId?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED' | 'REVOKED';
  joinedAt: string;
}

export interface OrganizationNode {
  id: string;
  tenantId: string;
  parentId?: string;
  type: 'ROOT_CHURCH' | 'REGION' | 'CHURCH' | 'SUB_CHURCH' | 'CAMPUS' | 'ASSEMBLY' | 'DEPARTMENT' | 'GROUP';
  name: string;
  code: string;
  slug?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  path: string;
  level: number;
  timezone?: string;
  country?: string;
  city?: string;
  metadata?: Record<string, any>;
  responsibleId?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface PlatformMetrics {
  totalTenants: number;
  activeTenants: number;
  suspendedTenants: number;
  totalUsers: number;
  activeUsers: number;
  tenantsByPlan: Record<string, number>;
}

export interface Plan {
  id: string;
  key: string;
  name: string;
  description?: string;
  priceMonthly: number;
  priceYearly: number;
  usersLimit: number;
  churchesLimit: number;
  departmentsLimit?: number;
  storageLimitMb?: number;
  aiRequestsLimit?: number;
  features?: Record<string, boolean>;
}

export interface Quotas {
  maxUsers: number;
  maxChurches: number;
  maxDepartments: number;
  maxStorageMb: number;
  maxAiRequestsMonth: number;
  maxCourses: number;
  maxMessagesMonth: number;
  currentUsers: number;
  currentChurches: number;
  currentDepartments: number;
  currentStorageMb: number;
  currentAiRequestsMonth: number;
  currentCourses: number;
  currentMessagesMonth: number;
}

export interface Subscription {
  id: string;
  tenantId: string;
  planKey: string;
  plan?: Plan;
  status: 'TRIAL' | 'ACTIVE' | 'PAST_DUE' | 'CANCELED' | 'PAUSED' | 'EXPIRED';
  billingCycle: 'monthly' | 'yearly';
  currentPeriodStart: string;
  currentPeriodEnd: string;
  cancelAtPeriodEnd: boolean;
  canceledAt?: string;
  trialEndsAt?: string;
  quotas: Quotas;
}

export interface TenantDashboard {
  tenantId: string;
  tenantName: string;
  totalUsers: number;
  activeUsers: number;
  totalMemberships: number;
  membersByRole: Record<string, number>;
  churchCount: number;
  departmentCount: number;
  subChurchCount: number;
  campusCount: number;
  groupCount: number;
  subscription?: Subscription;
}

export interface BrandingConfig {
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  logoUrl?: string;
  faviconUrl?: string;
  churchName?: string;
  tagline?: string;
  address?: string;
  phone?: string;
  email?: string;
  website?: string;
}

export interface TenantSettings {
  language: string;
  dateFormat: string;
  phoneCountryCode: string;
  email?: string;
  phone?: string;
  website?: string;
  openingHours?: Record<string, any>;
  workingDays?: string[];
  country?: string;
  currency?: string;
  timezone?: string;
  locale?: string;
}

export interface Invitation {
  id: string;
  email: string;
  role: string;
  scopeType?: 'TENANT' | 'REGION' | 'CHURCH' | 'SUB_CHURCH' | 'CAMPUS' | 'DEPARTMENT' | 'FAMILY' | 'ASSIGNED' | 'OWN';
  scopeId?: string;
  organizationNodeId?: string;
  organizationNodeName?: string;
  status: 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'CANCELED';
  invitedBy?: string;
  createdAt: string;
  expiresAt: string;
  acceptedAt?: string;
  tenantName?: string;
}

export interface Role {
  id: string;
  key: string;
  label: string;
  description?: string;
  priority: number;
  isSystem: boolean;
  permissions?: string[];
}

export interface Permission {
  id: string;
  key: string;
  label: string;
  description?: string;
  scope: 'GLOBAL' | 'TENANT' | 'CHURCH' | 'SUB_CHURCH' | 'DEPARTMENT' | 'FAMILY' | 'OWN';
  category?: string;
  isSystem: boolean;
}

export interface TenantContextValue {
  // État du tenant
  currentTenant: Tenant | null;
  currentMembership: TenantMembership | null;
  currentOrganizationNode: OrganizationNode | null;
  availableTenants: Tenant[];
  roles: Role[];
  permissions: Permission[];
  subscription: Subscription | null;
  quotas: Quotas | null;
  branding: BrandingConfig | null;
  features: Record<string, boolean>;
  settings: TenantSettings | null;

  // Actions
  switchTenant: (tenantId: string) => Promise<void>;
  switchOrganization: (orgNodeId: string) => Promise<void>;
  refreshContext: () => Promise<void>;

  // Vérifications
  hasRole: (role: string) => boolean;
  hasPermission: (permission: string) => boolean;
  hasFeature: (feature: string) => boolean;
  canAccess: (resource: string, action: string, scopeType?: string, scopeId?: string) => boolean;
  isQuotaExceeded: (quota: keyof Quotas) => boolean;

  // État de chargement
  isLoading: boolean;
  isInitialized: boolean;
}

export interface SwitchTenantResponse {
  success: boolean;
  tenantId: string;
  tenantName: string;
  role: string;
  message: string;
}

export interface CreateInvitationRequest {
  email: string;
  role: string;
  scopeType?: 'TENANT' | 'REGION' | 'CHURCH' | 'SUB_CHURCH' | 'CAMPUS' | 'DEPARTMENT' | 'FAMILY' | 'ASSIGNED' | 'OWN';
  scopeId?: string;
  organizationNodeId?: string;
}

export interface CreateInvitationResponse {
  success: boolean;
  invitationId: string;
  email: string;
  role: string;
  invitationToken: string;
  invitationLink: string;
  expiresAt: string;
  message: string;
}