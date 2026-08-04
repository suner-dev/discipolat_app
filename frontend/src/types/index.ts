// User types
export type UserRole = 'ADMIN' | 'PASTEUR' | 'RESPONSABLE' | 'CHEF_DE_FAMILLE' | 'FAISEUR' | 'MEMBRE';
export type UserStatus = 'ACTIVE' | 'INACTIVE';
export type EntityStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: UserRole;
  roles: UserRole[];
  activeRole: UserRole;
  estChefDeFamille: boolean;
  familleGereeId?: string;
  statut: UserStatus;
  dateNaissance?: string;
  photoUrl?: string;
  situationFamiliale?: string;
  twoFactorEnabled?: boolean;
  createdAt: string;
  updatedAt: string;
}

// Auth types
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: string;
  email: string;
  role: string;
  roles: string[];
  activeRole: string;
  estChefDeFamille: boolean;
  firstName?: string;
  lastName?: string;
  twoFactorEnabled?: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// Department types
export interface Department {
  id: string;
  nom: string;
  description?: string;
  responsableId: string;
  statut: EntityStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDepartmentRequest {
  nom: string;
  description?: string;
  responsableId: string;
}

// Family types
export interface Family {
  id: string;
  nom: string;
  departementId: string;
  chefFamilleId: string;
  dateCreation: string;
  statut: EntityStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFamilyRequest {
  nom: string;
  departementId: string;
  chefFamilleId: string;
}

export interface ReassignChiefRequest {
  newChefId: string;
}

// Soul types
export type TypeDisciple = 'NOUVEL_ARRIVANT' | 'NOUVEAU_CONVERTI';
export type StatutAme =
  | 'NOUVEAU_CONVERTI'
  | 'NOUVEL_ARRIVANT'
  | 'EN_INTEGRATION'
  | 'ACTIF'
  | 'EN_VEILLE'
  | 'DECROCHE';

export interface Soul {
  id: string;
  nom: string;
  prenom?: string;
  email?: string;
  telephone?: string;
  adresse?: string;
  dateNaissance?: string;
  profession?: string;
  typeDisciple: TypeDisciple;
  dateIntegration: string;
  dateConversion?: string;
  statut: StatutAme;
  faiseurId: string;
  familleId?: string;
  notesPasteur?: string;
  dateDernierContact?: string;
  createdAt: string;
}

export interface CreateSoulRequest {
  nom: string;
  prenom?: string;
  email?: string;
  telephone?: string;
  adresse?: string;
  dateNaissance?: string;
  profession?: string;
  typeDisciple: TypeDisciple;
  dateIntegration?: string;
  dateConversion?: string;
  faiseurId: string;
  familleId?: string;
}

export interface UpdateSoulRequest {
  nom?: string;
  prenom?: string;
  email?: string;
  telephone?: string;
  adresse?: string;
  dateNaissance?: string;
  profession?: string;
  typeDisciple?: TypeDisciple;
  statut?: StatutAme;
  faiseurId?: string;
  familleId?: string;
  notesPasteur?: string;
}

export interface SoulHistoryEntry {
  id: string;
  ameId: string;
  typeEvenement: string;
  description?: string;
  ancienStatut?: string;
  nouveauStatut?: string;
  utilisateurId?: string;
  createdAt: string;
}

// ======================== Member Space (Espace Membre) ========================

export interface MemberUserInfo {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  photoUrl?: string;
  dateNaissance?: string;
  situationFamiliale?: string;
}

export interface MemberSoulInfo {
  id: string;
  profession?: string;
  niveauEtude?: string;
  nbEnfants?: number;
  dateIntegration?: string;
  statut?: string;
}

export interface MemberFamilyInfo {
  id: string;
  nom: string;
  chefFamilleId: string;
  chefNom?: string;
}

export interface MemberDepartmentInfo {
  id: string;
  nom: string;
  description?: string;
  responsableId: string;
  responsableNom?: string;
}

export interface PersonneInfo {
  id: string;
  nom: string;
}

export type StatutMembre = 'MEMBRE' | 'FAISEUR' | 'CHEF_DE_FAMILLE';

export interface MemberDashboard {
  user: MemberUserInfo;
  soul?: MemberSoulInfo;
  age?: number;
  statutMembre: StatutMembre;
  estFaiseur: boolean;
  dateArriveeEglise?: string;
  famille?: MemberFamilyInfo;
  faiseur?: PersonneInfo;
  departements: MemberDepartmentInfo[];
}

export interface UpdateMemberProfileRequest {
  phone?: string;
  photoUrl?: string;
  situationFamiliale?: string;
  dateNaissance?: string;
  profession?: string;
  niveauEtude?: string;
  nbEnfants?: number;
}

// ======================== Member Presences & Requests (Phase 2) ========================

export interface MemberPresence {
  id: string;
  userId: string;
  nomMembre?: string;
  semaine: string;
  presences: Record<string, boolean>;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SubmitPresenceRequest {
  semaine: string;
  presences: Record<string, boolean>;
  notes?: string;
}

export type MemberRequestType = 'SUGGESTION' | 'RENDEZ_VOUS' | 'SIGNALEMENT';
export type MemberRequestTarget = 'PASTEUR' | 'RESPONSABLE' | 'CHEF_DE_FAMILLE';
export type MemberRequestStatus = 'OUVERT' | 'EN_COURS' | 'RESOLU' | 'REJETE';

export interface MemberRequest {
  id: string;
  type: MemberRequestType;
  cible: MemberRequestTarget;
  message: string;
  statut: MemberRequestStatus;
  reponse?: string;
  traitePar?: string;
  traiteParNom?: string;
  dateTraitement?: string;
  createdAt: string;
  auteurId: string;
  auteurNom?: string;
  departmentId?: string;
  departmentNom?: string;
  familyId?: string;
  familyNom?: string;
}

export interface CreateMemberRequest {
  type: MemberRequestType;
  cible: MemberRequestTarget;
  message: string;
}

export interface UpdateMemberRequestStatus {
  statut: MemberRequestStatus;
  reponse?: string;
}

// Report types
export type RaisonAbsence =
  | 'MALADIE'
  | 'VOYAGE'
  | 'INDISPONIBILITE'
  | 'INJOIGNABLE'
  | 'NON_RENSEIGNE'
  | 'AUTRE';

export type MotifSortie =
  | 'INTEGRE_AUTONOME'
  | 'TRANSFERT'
  | 'ABANDON'
  | 'INJOIGNABLE_DURABLE'
  | 'DECES'
  | 'AUTRE';

export type StatutValidation =
  | 'BROUILLON'
  | 'SOUMIS'
  | 'VU_PAR_RESPONSABLE'
  | 'VU_PAR_PASTEUR';

export interface MakerReport {
  id: string;
  faiseurId: string;
  ameId: string;
  semaine: string;
  presencesParCulte: Record<string, boolean>;
  absenceRaison?: RaisonAbsence;
  absenceCommentaire?: string;
  difficultesCategorie?: string;
  difficultes?: string;
  nbSorties: number;
  motifSortie?: MotifSortie;
  nbMaintenus: number;
  notesComplementaires?: string;
  soumis: boolean;
  dateSoumission?: string;
  createdAt: string;
}

export interface SubmitMakerReportRequest {
  faiseurId: string;
  ameId: string;
  semaine: string;
  presencesParCulte: Record<string, boolean>;
  absenceRaison?: RaisonAbsence;
  absenceCommentaire?: string;
  difficultesCategorie?: string;
  difficultes?: string;
  nbSorties?: number;
  motifSortie?: MotifSortie;
  nbMaintenus?: number;
  notesComplementaires?: string;
}

export interface FamilyReport {
  id: string;
  familleId: string;
  chefFamilleId: string;
  semaine: string;
  statsAgregees?: Record<string, unknown>;
  presenceMoyenne?: number;
  totalPresents: number;
  totalAbsents: number;
  totalSorties: number;
  repartitionSorties?: Record<string, number>;
  totalMaintenus: number;
  nbSuivisParalleles: number;
  suivisParallelesDetails?: unknown[];
  faiseursSansRapport?: string[];
  commentaireSynthese?: string;
  statutValidation: StatutValidation;
  dateSoumission?: string;
  createdAt: string;
}

export interface SubmitFamilyReportRequest {
  familleId: string;
  chefFamilleId: string;
  semaine: string;
  commentaireSynthese?: string;
}

// Parallel followup types
export type RaisonSuiviParallele =
  | 'TRANSFERT_EN_COURS'
  | 'RENFORT'
  | 'VISITE'
  | 'REPRISE_CONTACT'
  | 'AUTRE';

export type StatutSuiviParallele = 'EN_COURS' | 'CLOTURE';

export interface ParallelFollowup {
  id: string;
  ameId: string;
  initiateurId: string;
  familleId?: string;
  raison: RaisonSuiviParallele;
  raisonDetail?: string;
  dateDebut: string;
  dateFin?: string;
  statut: StatutSuiviParallele;
  createdAt: string;
}

export interface CreateParallelFollowupRequest {
  ameId: string;
  initiateurId: string;
  familleId?: string;
  raison: RaisonSuiviParallele;
  raisonDetail?: string;
  dateDebut?: string;
}

// Alert types
export type TypeAlerte =
  | 'ABSENCE_48H'
  | 'RAPPORT_NON_SOUMIS'
  | 'RAPPORT_FAMILLE_NON_SOUMIS';
export type StatutAlerte = 'ACTIVE' | 'TRAITEE' | 'RESOLUE';

export interface Alert {
  id: string;
  ameId: string;
  faiseurId: string;
  familleId?: string;
  typeAlerte: TypeAlerte;
  message: string;
  dateDeclenchement: string;
  statut: StatutAlerte;
  dateResolution?: string;
  resoluPar?: string;
}

// Notification types
export type CanalNotification = 'PUSH' | 'EMAIL' | 'IN_APP';
export type TypeNotification =
  | 'RAPPORT_NON_SOUMIS'
  | 'ABSENCE_48H'
  | 'RAPPORT_FAMILLE_NON_SOUMIS'
  | 'ALERTE_ABSENCE'
  | 'INFORMATION'
  | 'PRIERE_EXAUCEE';

export interface Notification {
  id: string;
  destinataireId: string;
  type: TypeNotification;
  canal: CanalNotification;
  titre: string;
  message: string;
  lu: boolean;
  dateLecture?: string;
  createdAt: string;
}

// Prayer types
export type CategoriePriere = 'SANTE' | 'FAMILLE' | 'TRAVAIL' | 'SPIRITUEL' | 'AUTRE';
export type PrioritePriere = 'BASSE' | 'MOYENNE' | 'HAUTE';
export type StatutPriere = 'EN_COURS' | 'EXAUCEE';
export type VisibilitePriere = 'PRIVEE' | 'PARTAGEE' | 'GENERALE' | 'PASTEUR_RESPONSABLE' | 'FAISEUR';

export interface Prayer {
  id: string;
  titre: string;
  contenu?: string;
  categorie: CategoriePriere;
  priorite: PrioritePriere;
  auteurId: string;
  familleId?: string;
  ameConcerneeId?: string;
  statut: StatutPriere;
  visibilite: VisibilitePriere;
  temoignage?: string;
  dateReponse?: string;
  dateCreation: string;
  updatedAt: string;
}

export interface CreatePrayerRequest {
  titre: string;
  contenu?: string;
  categorie: CategoriePriere;
  priorite?: PrioritePriere;
  familleId?: string;
  ameConcerneeId?: string;
  visibilite?: VisibilitePriere;
}

export interface UpdatePrayerRequest {
  titre?: string;
  contenu?: string;
  categorie?: CategoriePriere;
  priorite?: PrioritePriere;
  visibilite?: VisibilitePriere;
}

// Event types
export type TypeEvenement = 'SORTIE' | 'RETRAITE' | 'EVANGELISATION' | 'REUNION' | 'VISITE' | 'CONFERENCE' | 'FORMATION' | 'ANNIVERSAIRE' | 'CULTE' | 'ETUDE_BIBLIQUE' | 'VEILLEE' | 'PRIERE' | 'AUTRE';
export type JourSemaine = 'LUNDI' | 'MARDI' | 'MERCREDI' | 'JEUDI' | 'VENDREDI' | 'SAMEDI' | 'DIMANCHE';

export interface WeeklyProgramTemplate {
  id: string;
  titre: string;
  description?: string;
  typeEvenement: TypeEvenement;
  jourSemaine: JourSemaine;
  heureDebut: string;
  heureFin?: string;
  lieu?: string;
  dureeMinutes?: number;
  actif: boolean;
  couleur?: string;
  createdBy: string;
  createdAt: string;
}
export type StatutEvenement = 'PLANIFIE' | 'EN_COURS' | 'TERMINE' | 'ANNULE';
export type StatutInscription = 'INSCRIT' | 'EN_ATTENTE' | 'ANNULEE';

export interface Evenement {
  id: string;
  titre: string;
  description?: string;
  typeEvenement: TypeEvenement;
  dateDebut: string;
  dateFin?: string;
  lieu?: string;
  familleId?: string;
  organisateurId: string;
  limitePlaces?: number;
  nbInscrits: number;
  statut: StatutEvenement;
  dateCreation: string;
  updatedAt: string;
}

export interface CreateEventRequest {
  titre: string;
  description?: string;
  typeEvenement: TypeEvenement;
  dateDebut: string;
  dateFin?: string;
  lieu?: string;
  familleId?: string;
  limitePlaces?: number;
}

export interface UpdateEventRequest {
  titre?: string;
  description?: string;
  typeEvenement?: TypeEvenement;
  dateDebut?: string;
  dateFin?: string;
  lieu?: string;
  limitePlaces?: number;
  statut?: StatutEvenement;
}

export interface InscriptionEvenement {
  id: string;
  evenementId: string;
  utilisateurId: string;
  statutInscription: StatutInscription;
  present: boolean;
  dateInscription: string;
  updatedAt: string;
}

// File/Document types
export type CategorieDocument = 'COMPTE_RENDU' | 'FORMATION' | 'PHOTO' | 'RESOURCES' | 'AUTRE';

export interface FileEntity {
  id: string;
  nom: string;
  description?: string;
  url: string;
  typeMime: string;
  taille: number;
  familleId?: string;
  evenementId?: string;
  auteurId: string;
  categorie: CategorieDocument;
  dateCreation: string;
}

export interface CreateFileRequest {
  nom: string;
  description?: string;
  url: string;
  typeMime: string;
  taille: number;
  familleId?: string;
  evenementId?: string;
  categorie?: CategorieDocument;
}

// Soul Note types
export interface SoulNote {
  id: string;
  ameId: string;
  auteurId: string;
  contenu: string;
  dateCreation: string;
  updatedAt: string;
}

export interface CreateSoulNoteRequest {
  contenu: string;
}

// Soul Retraction Request types
export type StatutDemandeRetrait = 'EN_ATTENTE' | 'APPROUVEE' | 'REJETEE';

export interface SoulRetractionRequest {
  id: string;
  ameId: string;
  demandeurId: string;
  familleId?: string;
  justification: string;
  statut: StatutDemandeRetrait;
  motifRejet?: string;
  traitePar?: string;
  dateTraitement?: string;
  dateCreation: string;
}

export interface CreateSoulRetractionRequest {
  ameId: string;
  justification: string;
}

// Dashboard types
export interface DashboardKPI {
  tauxPresenceGlobal: number;
  tauxPresenceNouveauxArrivants: number;
  tauxPresenceNouveauxConvertis: number;
  totalAmes: number;
  totalFaiseurs: number;
  totalFamilles: number;
  totalDepartements: number;
  totalSorties: number;
  totalMaintenus: number;
  suivisParallelesActifs: number;
  alertesActives: number;
  rapportsSoumis: number;
  rapportsEnAttente: number;
  famillesARisque: number;
  tendancePresence: number;
}

// Pagination
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// API Error
export interface ApiError {
  status: number;
  title: string;
  detail: string;
  instance?: string;
  errors?: Record<string, string[]>;
}
