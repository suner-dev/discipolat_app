import api from '@/lib/api';

/**
 * Contrat exact du backend V233 — DiscipleshipController
 * (backend/.../discipleship/api/DiscipleshipController.java), monture
 * {@code /api/v1/discipleship}. Une fonction = un endpoint réellement
 * exposé ; les identifiants BIGSERIAL (journey/stage/progress/assignment/
 * meeting) sont des {@code number}, les références personnes (mentor/disciple)
 * des UUID {@code string}, conformément aux signatures serveur.
 *
 * Les corps de réponse serveur sont des {@code Map<String,Object>} ; on les
 * typise volontairement en {@link Json} plutôt qu'en interfaces inventées, pour
 * ne jamais affirmer un champ que le serveur ne garantit pas.
 */
export type Json = Record<string, unknown>;

/** Filtre de pagination + critères de la liste de progression. */
export interface ProgressQuery {
  page?: number;
  size?: number;
  journeyId?: number;
  discipleId?: string;
  status?: string;
}

export interface AssignmentsQuery {
  page?: number;
  size?: number;
  mentorId?: string;
  discipleId?: string;
  journeyId?: number;
  status?: string;
}

export interface MeetingsQuery {
  page?: number;
  size?: number;
  assignmentId?: number;
  mentorId?: string;
  status?: string;
}

// ---------- Journeys ----------

export function listJourneys(isActive?: boolean): Promise<Json[]> {
  return api
    .get<Json[]>('/discipleship/journeys', { params: isActive == null ? {} : { isActive } })
    .then((r) => r.data);
}

export function getJourney(id: number): Promise<Json> {
  return api.get<Json>(`/discipleship/journeys/${id}`).then((r) => r.data);
}

export function createJourney(body: Json): Promise<Json> {
  return api.post<Json>('/discipleship/journeys', body).then((r) => r.data);
}

// ---------- Stages ----------

export function listStages(journeyId: number): Promise<Json[]> {
  return api.get<Json[]>(`/discipleship/journeys/${journeyId}/stages`).then((r) => r.data);
}

export function getStage(id: number): Promise<Json> {
  return api.get<Json>(`/discipleship/stages/${id}`).then((r) => r.data);
}

export function createStage(body: Json): Promise<Json> {
  return api.post<Json>('/discipleship/stages', body).then((r) => r.data);
}

// ---------- Progress ----------

export function listProgress(query: ProgressQuery = {}): Promise<Json[]> {
  return api.get<Json[]>('/discipleship/progress', { params: query }).then((r) => r.data);
}

export function getProgress(id: number): Promise<Json> {
  return api.get<Json>(`/discipleship/progress/${id}`).then((r) => r.data);
}

export function createProgress(body: Json): Promise<Json> {
  return api.post<Json>('/discipleship/progress', body).then((r) => r.data);
}

export function updateRequirementProgress(
  progressId: number,
  requirementId: number,
  body: Json,
): Promise<Json> {
  return api
    .patch<Json>(`/discipleship/progress/${progressId}/requirements/${requirementId}`, body)
    .then((r) => r.data);
}

export function completeStage(progressId: number, stageId: number): Promise<Json> {
  return api
    .post<Json>(`/discipleship/progress/${progressId}/stages/${stageId}/complete`)
    .then((r) => r.data);
}

// ---------- Assignments ----------

export function listAssignments(query: AssignmentsQuery = {}): Promise<Json[]> {
  return api.get<Json[]>('/discipleship/assignments', { params: query }).then((r) => r.data);
}

export function createAssignment(body: Json): Promise<Json> {
  return api.post<Json>('/discipleship/assignments', body).then((r) => r.data);
}

export function endAssignment(id: number): Promise<Json> {
  return api.post<Json>(`/discipleship/assignments/${id}/end`).then((r) => r.data);
}

// ---------- Meetings ----------

export function listMeetings(query: MeetingsQuery = {}): Promise<Json[]> {
  return api.get<Json[]>('/discipleship/meetings', { params: query }).then((r) => r.data);
}

export function scheduleMeeting(body: Json): Promise<Json> {
  return api.post<Json>('/discipleship/meetings', body).then((r) => r.data);
}

export function completeMeeting(id: number, body?: Json): Promise<Json> {
  return api.post<Json>(`/discipleship/meetings/${id}/complete`, body ?? {}).then((r) => r.data);
}

// ---------- Reports ----------

export function getReport(journeyId: number): Promise<Json> {
  return api.get<Json>(`/discipleship/reports/${journeyId}`).then((r) => r.data);
}

export function getTopMentors(journeyId: number, limit = 10): Promise<Json[]> {
  return api
    .get<Json[]>(`/discipleship/reports/${journeyId}/top-mentors`, { params: { limit } })
    .then((r) => r.data);
}
