import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock de la couche réseau : on vérifie le verbe + l'URL émise par chaque
// fonction de service, sans dépendre d'axios ni du DOM.
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
import * as svc from './discipleshipService';

const mock = api as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  put: ReturnType<typeof vi.fn>;
  patch: ReturnType<typeof vi.fn>;
  delete: ReturnType<typeof vi.fn>;
};

describe('discipleshipService — contrat exact DiscipleshipController', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mock.get.mockResolvedValue({ data: [] });
    mock.post.mockResolvedValue({ data: {} });
    mock.put.mockResolvedValue({ data: {} });
    mock.patch.mockResolvedValue({ data: {} });
    mock.delete.mockResolvedValue({ data: {} });
  });

  it('listJourneys: GET sans isActive, puis avec isActive', async () => {
    await svc.listJourneys();
    expect(mock.get).toHaveBeenCalledWith('/discipleship/journeys', { params: {} });
    await svc.listJourneys(true);
    expect(mock.get).toHaveBeenCalledWith('/discipleship/journeys', { params: { isActive: true } });
  });

  it('getJourney / createJourney', async () => {
    await svc.getJourney(42);
    expect(mock.get).toHaveBeenCalledWith('/discipleship/journeys/42');
    await svc.createJourney({ name: 'x' });
    expect(mock.post).toHaveBeenCalledWith('/discipleship/journeys', { name: 'x' });
  });

  it('stages: list / get / create', async () => {
    await svc.listStages(7);
    expect(mock.get).toHaveBeenCalledWith('/discipleship/journeys/7/stages');
    await svc.getStage(3);
    expect(mock.get).toHaveBeenCalledWith('/discipleship/stages/3');
    await svc.createStage({ title: 's' });
    expect(mock.post).toHaveBeenCalledWith('/discipleship/stages', { title: 's' });
  });

  it('progress: list (query), get, create, requirement PATCH, complete', async () => {
    await svc.listProgress({ page: 0, size: 20, discipleId: 'u-1', status: 'ACTIVE' });
    expect(mock.get).toHaveBeenCalledWith('/discipleship/progress', {
      params: { page: 0, size: 20, discipleId: 'u-1', status: 'ACTIVE' },
    });
    await svc.getProgress(9);
    expect(mock.get).toHaveBeenCalledWith('/discipleship/progress/9');
    await svc.createProgress({ journeyId: 1 });
    expect(mock.post).toHaveBeenCalledWith('/discipleship/progress', { journeyId: 1 });
    await svc.updateRequirementProgress(5, 6, { done: true });
    expect(mock.patch).toHaveBeenCalledWith('/discipleship/progress/5/requirements/6', { done: true });
    await svc.completeStage(5, 6);
    expect(mock.post).toHaveBeenCalledWith('/discipleship/progress/5/stages/6/complete');
  });

  it('assignments: list, create, end', async () => {
    await svc.listAssignments({ mentorId: 'm-1' });
    expect(mock.get).toHaveBeenCalledWith('/discipleship/assignments', { params: { mentorId: 'm-1' } });
    await svc.createAssignment({ mentorId: 'm-1', discipleId: 'd-1' });
    expect(mock.post).toHaveBeenCalledWith('/discipleship/assignments', {
      mentorId: 'm-1',
      discipleId: 'd-1',
    });
    await svc.endAssignment(11);
    expect(mock.post).toHaveBeenCalledWith('/discipleship/assignments/11/end');
  });

  it('meetings: list, schedule, complete', async () => {
    await svc.listMeetings({ assignmentId: 2 });
    expect(mock.get).toHaveBeenCalledWith('/discipleship/meetings', { params: { assignmentId: 2 } });
    await svc.scheduleMeeting({ assignmentId: 2 });
    expect(mock.post).toHaveBeenCalledWith('/discipleship/meetings', { assignmentId: 2 });
    await svc.completeMeeting(8, { notes: 'ok' });
    expect(mock.post).toHaveBeenCalledWith('/discipleship/meetings/8/complete', { notes: 'ok' });
  });

  it('reports: report + topMentors', async () => {
    await svc.getReport(4);
    expect(mock.get).toHaveBeenCalledWith('/discipleship/reports/4');
    await svc.getTopMentors(4, 5);
    expect(mock.get).toHaveBeenCalledWith('/discipleship/reports/4/top-mentors', { params: { limit: 5 } });
  });
});
