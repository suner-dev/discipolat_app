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
}

export interface TenantMembership {
  id: string;
  tenantId: string;
  userId: string;
  role: string;
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
  features?: string[];
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
  subscription?: {
    planKey: string;
    status: string;
    currentPeriodEnd: string;
  };
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
  status: 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'CANCELED';
  invitedBy?: string;
  createdAt: string;
  expiresAt: string;
  acceptedAt?: string;
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
