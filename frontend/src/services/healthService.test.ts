import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

import api from '@/lib/api';
import * as svc from './healthService';

const mock = api as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  put: ReturnType<typeof vi.fn>;
  patch: ReturnType<typeof vi.fn>;
  delete: ReturnType<typeof vi.fn>;
};

describe('healthService — contrat exact HealthController', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mock.get.mockResolvedValue({ data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } });
    mock.post.mockResolvedValue({ data: {} });
    mock.put.mockResolvedValue({ data: {} });
  });

  it('patients: list (query), get, create, update, by-condition', async () => {
    await svc.getPatients({ search: 'abc', page: 0, size: 20 });
    expect(mock.get).toHaveBeenCalledWith('/health/patients', { params: { search: 'abc', page: 0, size: 20 } });
    await svc.getPatient('p-1');
    expect(mock.get).toHaveBeenCalledWith('/health/patients/p-1');
    await svc.createPatient({ fullName: 'x' });
    expect(mock.post).toHaveBeenCalledWith('/health/patients', { fullName: 'x' });
    await svc.updatePatient('p-1', { fullName: 'y' });
    expect(mock.put).toHaveBeenCalledWith('/health/patients/p-1', { fullName: 'y' });
    await svc.getPatientsByCondition('DIABETE');
    expect(mock.get).toHaveBeenCalledWith('/health/patients/by-condition', { params: { condition: 'DIABETE' } });
  });

  it('consultations: list, get, create, update (PUT ajouté), prescriptions liées', async () => {
    await svc.getConsultations({ patientId: 'p-1' });
    expect(mock.get).toHaveBeenCalledWith('/health/consultations', { params: { patientId: 'p-1' } });
    await svc.getConsultation('c-1');
    expect(mock.get).toHaveBeenCalledWith('/health/consultations/c-1');
    await svc.createConsultation({ motif: 'fievre' });
    expect(mock.post).toHaveBeenCalledWith('/health/consultations', { motif: 'fievre' });
    await svc.updateConsultation('c-1', { status: 'DONE' });
    expect(mock.put).toHaveBeenCalledWith('/health/consultations/c-1', { status: 'DONE' });
    await svc.getConsultationPrescriptions('c-1');
    expect(mock.get).toHaveBeenCalledWith('/health/consultations/c-1/prescriptions');
  });

  it('prescriptions: list, get, create', async () => {
    await svc.getPrescriptions({ consultationId: 'c-1' });
    expect(mock.get).toHaveBeenCalledWith('/health/prescriptions', { params: { consultationId: 'c-1' } });
    await svc.getPrescription('pr-1');
    expect(mock.get).toHaveBeenCalledWith('/health/prescriptions/pr-1');
    await svc.createPrescription({ drug: 'paracetamol' });
    expect(mock.post).toHaveBeenCalledWith('/health/prescriptions', { drug: 'paracetamol' });
  });

  it('pharmacie: items CRUD, stock get/update (PUT), alertes, mouvements', async () => {
    await svc.getPharmacyItems({ categorie: 'ANTIBIOTIQUE' });
    expect(mock.get).toHaveBeenCalledWith('/health/pharmacy/items', { params: { categorie: 'ANTIBIOTIQUE' } });
    await svc.getPharmacyItem('i-1');
    expect(mock.get).toHaveBeenCalledWith('/health/pharmacy/items/i-1');
    await svc.createPharmacyItem({ nom: 'x' });
    expect(mock.post).toHaveBeenCalledWith('/health/pharmacy/items', { nom: 'x' });
    await svc.updatePharmacyItem('i-1', { nom: 'y' });
    expect(mock.put).toHaveBeenCalledWith('/health/pharmacy/items/i-1', { nom: 'y' });
    await svc.getPharmacyStock({ status: 'LOW' });
    expect(mock.get).toHaveBeenCalledWith('/health/pharmacy/stock', { params: { status: 'LOW' } });
    await svc.getPharmacyStockById('s-1');
    expect(mock.get).toHaveBeenCalledWith('/health/pharmacy/stock/s-1');
    await svc.updatePharmacyStock('s-1', { quantite: 10 });
    expect(mock.put).toHaveBeenCalledWith('/health/pharmacy/stock/s-1', { quantite: 10 });
    await svc.getLowStockAlerts();
    expect(mock.get).toHaveBeenCalledWith('/health/pharmacy/stock/alerts/low');
    await svc.getExpiringSoonAlerts(45);
    expect(mock.get).toHaveBeenCalledWith('/health/pharmacy/stock/alerts/expiring', { params: { days: 45 } });
    await svc.createMovement({ type: 'OUT' });
    expect(mock.post).toHaveBeenCalledWith('/health/pharmacy/movements', { type: 'OUT' });
  });

  it('campagnes: list, get, create, participants, register', async () => {
    await svc.getCampaigns({ status: 'PLANNED' });
    expect(mock.get).toHaveBeenCalledWith('/health/campaigns', { params: { status: 'PLANNED' } });
    await svc.getCampaign('ca-1');
    expect(mock.get).toHaveBeenCalledWith('/health/campaigns/ca-1');
    await svc.createCampaign({ name: 'x' });
    expect(mock.post).toHaveBeenCalledWith('/health/campaigns', { name: 'x' });
    await svc.getCampaignParticipants('ca-1');
    expect(mock.get).toHaveBeenCalledWith('/health/campaigns/ca-1/participants');
    await svc.registerCampaignParticipant('ca-1', 'u-1');
    expect(mock.post).toHaveBeenCalledWith('/health/campaigns/ca-1/register', { userId: 'u-1' });
  });

  it('medicaments / kits / devoirs / dashboard / rapports', async () => {
    await svc.getMedications();
    expect(mock.get).toHaveBeenCalledWith('/health/medications');
    await svc.getMedication('m-1');
    expect(mock.get).toHaveBeenCalledWith('/health/medications/m-1');
    await svc.createMedication({ name: 'x' });
    expect(mock.post).toHaveBeenCalledWith('/health/medications', { name: 'x' });
    await svc.getKits();
    expect(mock.get).toHaveBeenCalledWith('/health/kits');
    await svc.getKit('k-1');
    expect(mock.get).toHaveBeenCalledWith('/health/kits/k-1');
    await svc.getDuties();
    expect(mock.get).toHaveBeenCalledWith('/health/duties');
    await svc.getDashboardStats();
    expect(mock.get).toHaveBeenCalledWith('/health/dashboard/stats');
    await svc.getHealthReportsStatistics();
    expect(mock.get).toHaveBeenCalledWith('/health/reports/statistics');
  });
});
