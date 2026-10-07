import api from '@/lib/api';

/**
 * Contrat exact du backend V240 — HealthController
 * (backend/.../health/api/HealthController.java), monture {@code /api/v1/health}.
 *
 * Les endpoints à relation renvoient des vues aplaties ({personId, personName},
 * {patientId, patientName}, {itemId, itemName}, {responsibleName}) — plus
 * d'entités LAZY partiellement sérialisées. Tous les identifiants health sont
 * des UUID côté serveur : typés {@code string}. Les endpoints paginés renvoient
 * un {@link Paged} (shape réelle de {@code PageResponse} : content, page,
 * size, totalElements, totalPages). Aucune interface de domaine inventée :
 * {@link Json}.
 */
export type Json = Record<string, unknown>;

export interface Paged<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PatientsQuery {
  search?: string;
  page?: number;
  size?: number;
}

export interface ConsultationsQuery {
  patientId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface PrescriptionsQuery {
  patientId?: string;
  consultationId?: string;
  page?: number;
  size?: number;
}

export interface PharmacyItemsQuery {
  search?: string;
  categorie?: string;
  page?: number;
  size?: number;
}

export interface PharmacyStockQuery {
  itemId?: string;
  status?: string;
  page?: number;
  size?: number;
}

export interface CampaignsQuery {
  status?: string;
  page?: number;
  size?: number;
}

// ---------- Patients ----------

export function getPatients(query: PatientsQuery = {}): Promise<Paged<Json>> {
  return api.get<Paged<Json>>('/health/patients', { params: query }).then((r) => r.data);
}

export function getPatient(id: string): Promise<Json> {
  return api.get<Json>(`/health/patients/${id}`).then((r) => r.data);
}

export function createPatient(body: Json): Promise<Json> {
  return api.post<Json>('/health/patients', body).then((r) => r.data);
}

export function updatePatient(id: string, body: Json): Promise<Json> {
  return api.put<Json>(`/health/patients/${id}`, body).then((r) => r.data);
}

export function getPatientsByCondition(condition: string): Promise<Json[]> {
  return api
    .get<Json[]>('/health/patients/by-condition', { params: { condition } })
    .then((r) => r.data);
}

// ---------- Consultations ----------

export function getConsultations(query: ConsultationsQuery = {}): Promise<Paged<Json>> {
  return api.get<Paged<Json>>('/health/consultations', { params: query }).then((r) => r.data);
}

export function getConsultation(id: string): Promise<Json> {
  return api.get<Json>(`/health/consultations/${id}`).then((r) => r.data);
}

export function createConsultation(body: Json): Promise<Json> {
  return api.post<Json>('/health/consultations', body).then((r) => r.data);
}

export function updateConsultation(id: string, body: Json): Promise<Json> {
  return api.put<Json>(`/health/consultations/${id}`, body).then((r) => r.data);
}

export function getConsultationPrescriptions(consultationId: string): Promise<Json[]> {
  return api
    .get<Json[]>(`/health/consultations/${consultationId}/prescriptions`)
    .then((r) => r.data);
}

// ---------- Prescriptions ----------

export function getPrescriptions(query: PrescriptionsQuery = {}): Promise<Paged<Json>> {
  return api.get<Paged<Json>>('/health/prescriptions', { params: query }).then((r) => r.data);
}

export function getPrescription(id: string): Promise<Json> {
  return api.get<Json>(`/health/prescriptions/${id}`).then((r) => r.data);
}

export function createPrescription(body: Json): Promise<Json> {
  return api.post<Json>('/health/prescriptions', body).then((r) => r.data);
}

// ---------- Pharmacie ----------

export function getPharmacyItems(query: PharmacyItemsQuery = {}): Promise<Paged<Json>> {
  return api.get<Paged<Json>>('/health/pharmacy/items', { params: query }).then((r) => r.data);
}

export function getPharmacyItem(id: string): Promise<Json> {
  return api.get<Json>(`/health/pharmacy/items/${id}`).then((r) => r.data);
}

export function createPharmacyItem(body: Json): Promise<Json> {
  return api.post<Json>('/health/pharmacy/items', body).then((r) => r.data);
}

export function updatePharmacyItem(id: string, body: Json): Promise<Json> {
  return api.put<Json>(`/health/pharmacy/items/${id}`, body).then((r) => r.data);
}

export function getPharmacyStock(query: PharmacyStockQuery = {}): Promise<Paged<Json>> {
  return api.get<Paged<Json>>('/health/pharmacy/stock', { params: query }).then((r) => r.data);
}

export function getPharmacyStockById(id: string): Promise<Json> {
  return api.get<Json>(`/health/pharmacy/stock/${id}`).then((r) => r.data);
}

export function updatePharmacyStock(id: string, body: Json): Promise<Json> {
  return api.put<Json>(`/health/pharmacy/stock/${id}`, body).then((r) => r.data);
}

export function getLowStockAlerts(): Promise<Json[]> {
  return api.get<Json[]>('/health/pharmacy/stock/alerts/low').then((r) => r.data);
}

export function getExpiringSoonAlerts(days = 30): Promise<Json[]> {
  return api
    .get<Json[]>('/health/pharmacy/stock/alerts/expiring', { params: { days } })
    .then((r) => r.data);
}

export function createMovement(body: Json): Promise<Json> {
  return api.post<Json>('/health/pharmacy/movements', body).then((r) => r.data);
}

// ---------- Campagnes ----------

export function getCampaigns(query: CampaignsQuery = {}): Promise<Paged<Json>> {
  return api.get<Paged<Json>>('/health/campaigns', { params: query }).then((r) => r.data);
}

export function getCampaign(id: string): Promise<Json> {
  return api.get<Json>(`/health/campaigns/${id}`).then((r) => r.data);
}

export function createCampaign(body: Json): Promise<Json> {
  return api.post<Json>('/health/campaigns', body).then((r) => r.data);
}

export function getCampaignParticipants(campaignId: string): Promise<Json[]> {
  return api.get<Json[]>(`/health/campaigns/${campaignId}/participants`).then((r) => r.data);
}

export function registerCampaignParticipant(campaignId: string, userId: string): Promise<Json> {
  return api
    .post<Json>(`/health/campaigns/${campaignId}/register`, { userId })
    .then((r) => r.data);
}

// ---------- Médicaments / Kits / Devoirs ----------

export function getMedications(): Promise<Json[]> {
  return api.get<Json[]>('/health/medications').then((r) => r.data);
}

export function getMedication(id: string): Promise<Json> {
  return api.get<Json>(`/health/medications/${id}`).then((r) => r.data);
}

export function createMedication(body: Json): Promise<Json> {
  return api.post<Json>('/health/medications', body).then((r) => r.data);
}

export function getKits(): Promise<Json[]> {
  return api.get<Json[]>('/health/kits').then((r) => r.data);
}

export function getKit(id: string): Promise<Json> {
  return api.get<Json>(`/health/kits/${id}`).then((r) => r.data);
}

export function getDuties(): Promise<Json[]> {
  return api.get<Json[]>('/health/duties').then((r) => r.data);
}

// ---------- Tableau de bord / Rapports ----------

export function getDashboardStats(): Promise<Json> {
  return api.get<Json>('/health/dashboard/stats').then((r) => r.data);
}

export function getHealthReportsStatistics(): Promise<Json> {
  return api.get<Json>('/health/reports/statistics').then((r) => r.data);
}
