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
  userId?: string;
  chefFamilleId: string;
  /** Nom complet du chef de famille (résolu côté serveur — jamais d'UUID brut). */
  chefFamilleNom?: string;
  chefAdjointId?: string;
  chefAdjointNom?: string;
  dateCreation: string;
  statut: EntityStatus;
  latitude?: number;
  longitude?: number;
  zone?: string;
  niveauRisque?: 'NORMAL' | 'SOUS_SURVEILLANCE' | 'A_RISQUE';
  createdAt: string;
  updatedAt: string;
}

export interface FamilyChiefHistoryEntry {
  id: string;
  ancienChefId?: string;
  ancienChefNom?: string;
  nouveauChefId: string;
  nouveauChefNom?: string;
  dateChangement: string;
  raison?: string;
}

export interface FamilyRiskAssessment {
  familyId: string;
  nom: string;
  niveauActuel: 'NORMAL' | 'SOUS_SURVEILLANCE' | 'A_RISQUE';
  niveauSuggere: 'NORMAL' | 'SOUS_SURVEILLANCE' | 'A_RISQUE';
  scoreRisque: number;
  tauxPresence: number;
  amesPerdues: number;
  nouveaux30j: number;
  enVeille: number;
  absences4sem: number;
  litiges: number;
  retards: number;
  totalSouls: number;
  evaluationDate: string;
}

export interface CreateFamilyRequest {
  nom: string;
  chefFamilleId: string;
  chefAdjointId?: string;
  userId?: string;
  createNewChef?: boolean;
  newChefFirstName?: string;
  newChefLastName?: string;
  newChefEmail?: string;
  newChefPhone?: string;
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
  latitude?: number;
  longitude?: number;
  zone?: string;
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
  typeProgramme?: string;
  sousProgramme?: string;
  present?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SubmitPresenceRequest {
  semaine: string;
  presences: Record<string, boolean>;
  notes?: string;
  typeProgramme?: string;
  sousProgramme?: string;
}

export interface ProgramSubType {
  id: string;
  label: string;
  heureDebut?: string;
  heureFin?: string;
  actif: boolean;
  ordre?: number;
}

export interface ProgramType {
  id: string;
  code: string;
  label: string;
  description?: string;
  aSousProgrammes: boolean;
  couleur?: string;
  actif: boolean;
  ordre?: number;
  sousProgrammes: ProgramSubType[];
}

export interface DepartmentPresenceRecord {
  soulId: string;
  userId?: string;
  nom: string;
  telephone?: string;
  statut?: string;
  familleNom?: string;
  familleId?: string;
  dateIntegration?: string;
  presenceSaisie: boolean;
  present?: boolean;
  presences?: Record<string, boolean>;
  notes?: string;
  typeProgramme?: string;
  sousProgramme?: string;
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
  piecesJointes?: TransferAttachment[];
}

export interface CreateMemberRequest {
  type: MemberRequestType;
  cible: MemberRequestTarget;
  message: string;
  fichierIds?: string[];
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
  piecesJointes?: TransferAttachment[];
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
  fichierIds?: string[];
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
  piecesJointes?: TransferAttachment[];
}

export interface SubmitFamilyReportRequest {
  familleId: string;
  chefFamilleId: string;
  semaine: string;
  commentaireSynthese?: string;
  fichierIds?: string[];
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
  | 'ABSENCE_3_SEMAINES'
  | 'RAPPORT_NON_SOUMIS'
  | 'RAPPORT_FAMILLE_NON_SOUMIS'
  | 'ALERTE_ABSENCE'
  | 'MANUEL';
export type StatutAlerte = 'ACTIVE' | 'TRAITEE' | 'RESOLUE';
export type CibleAlerte = 'PERSONNE' | 'DEPARTEMENT' | 'FAMILLE' | 'GROUPE' | 'EGLISE';
export type PrioriteAlerte = 'BASSE' | 'MOYENNE' | 'HAUTE' | 'URGENTE';

export interface Alert {
  id: string;
  ameId?: string;
  faiseurId?: string;
  familleId?: string;
  departmentId?: string;
  cible: CibleAlerte;
  priorite: PrioriteAlerte;
  titre?: string;
  typeAlerte: TypeAlerte;
  typeAlerteManuel?: string;
  message: string;
  dateDeclenchement: string;
  statut: StatutAlerte;
  dateResolution?: string;
  resoluPar?: string;
  ameNom?: string;
  familleNom?: string;
  departmentNom?: string;
}

export interface CreateAlertRequest {
  typeAlerteManuel: string;
  titre: string;
  message: string;
  cible: CibleAlerte;
  priorite?: PrioriteAlerte;
  ameId?: string;
  faiseurId?: string;
  familleId?: string;
  departmentId?: string;
}

// Notification types
export type CanalNotification = 'PUSH' | 'EMAIL' | 'IN_APP';
export type TypeNotification =
  | 'RAPPORT_NON_SOUMIS'
  | 'ABSENCE_48H'
  | 'RAPPORT_FAMILLE_NON_SOUMIS'
  | 'ALERTE_ABSENCE'
  | 'INFORMATION'
  | 'PRIERE_EXAUCEE'
  // Transferts (workflow configurable)
  | 'TRANSFERT_DEMANDE'
  | 'TRANSFERT_VALIDATION'
  | 'TRANSFERT_VALIDEE'
  | 'TRANSFERT_REFUSEE'
  | 'TRANSFERT_INFOS_DEMANDEES'
  | 'TRANSFERT_CORRECTION'
  | 'TRANSFERT_EXECUTEE'
  | 'TRANSFERT_ANNULEE'
  | 'TRANSFERT_DELAI_DEPASSE';

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
  piecesJointes?: TransferAttachment[];
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
  fichierIds?: string[];
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
  fichierIds?: string[];
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
  chemin: string;
  typeFichier: string;
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
  chemin: string;
  typeFichier: string;
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

// ======================== CRM Interactions ========================

export type InteractionType =
  | 'APPEL' | 'SMS' | 'WHATSAPP' | 'EMAIL' | 'VISITE' | 'REUNION'
  | 'PRIERE' | 'CONSEIL' | 'SUIVI' | 'PROGRAMME';

export interface Interaction {
  id: string;
  soulId: string;
  auteurId: string;
  auteurNom?: string;
  type: InteractionType;
  canal?: string;
  objet?: string;
  contenu?: string;
  dateInteraction: string;
  aFairePar?: string;
  aFaireParNom?: string;
  rappelLe?: string;
  createdAt: string;
}

export interface CreateInteractionRequest {
  type: InteractionType;
  canal?: string;
  objet?: string;
  contenu?: string;
  dateInteraction?: string;
  aFairePar?: string;
  rappelLe?: string;
}

// ======================== Score spirituel & Assistant IA ========================

export interface SpiritualScore {
  soulId: string;
  global: number;
  sante: number;
  fidelite: number;
  engagement: number;
  participation: number;
  label: string;
  semaine: string;
}

export interface ScoreHistoryPoint {
  semaine: string;
  global: number;
  sante: number;
  fidelite: number;
  engagement: number;
  participation: number;
}

export interface AiSignal {
  severite: 'CRITIQUE' | 'ELEVE' | 'MOYEN';
  type: string;
  message: string;
  actionConseillee: string;
}

export interface AiSuggestion {
  type: string;
  titre: string;
  description: string;
}

export interface AiAnalysis {
  soulId: string;
  nom: string;
  score: SpiritualScore;
  signaux: AiSignal[];
  suggestions: AiSuggestion[];
  encouragement: string;
  resume: string;
}

// ======================== Suivi d'évangélisation (pipeline) ========================

export type EvangelismEtape =
  | 'NOUVELLE_AME'
  | 'PREMIER_CONTACT'
  | 'VISITE'
  | 'INVITATION'
  | 'PREMIER_CULTE'
  | 'SUIVI'
  | 'BAPTEME'
  | 'DEPARTEMENT'
  | 'FAMILLE'
  | 'DISCIPOLAT'
  | 'LEADER';

export interface EvangelismTrack {
  id: string;
  soulId: string;
  soulNom?: string;
  etape: EvangelismEtape;
  dateEtape: string;
  note?: string;
  creePar?: string;
  creeParNom?: string;
  creeLe: string;
  majLe: string;
}

export interface UpdateEvangelismRequest {
  etape: EvangelismEtape;
  note?: string;
}

export interface EvangelismStats {
  totalAmes: number;
  parEtape: Record<EvangelismEtape, number>;
}

export interface EvangelismHistoryEntry {
  etape: EvangelismEtape;
  creePar?: string;
  creeLe: string;
}

// ======================== Système d'objectifs ========================

export type ObjectiveType =
  | 'VISITES' | 'NOUVELLES_AMES' | 'DISCIPLES_ACTIFS'
  | 'EVANGELISATION' | 'SUIVIS' | 'PRESENCE';
export type ObjectivePeriode = 'MENSUEL' | 'TRIMESTRIEL' | 'ANNUEL';

export interface Objective {
  id: string;
  role: UserRole;
  type: ObjectiveType;
  cible: number;
  periode: ObjectivePeriode;
  actif: boolean;
  creeLe: string;
}

export interface CreateObjectiveRequest {
  role: UserRole;
  type: ObjectiveType;
  cible: number;
  periode: ObjectivePeriode;
}

export interface ObjectiveProgress {
  id: string;
  role: UserRole;
  type: ObjectiveType;
  cible: number;
  periode: ObjectivePeriode;
  realise: number;
  taux: number;
  atteint: boolean;
}

// ======================== Visites pastorales ========================

export type VisitStatut = 'PLANIFIEE' | 'REALISEE' | 'ANNULEE' | 'REPORTEE';

export interface Visit {
  id: string;
  soulId: string;
  soulNom?: string;
  visiteurId: string;
  visiteurNom?: string;
  datePrevue: string;
  dateRealisee?: string;
  statut: VisitStatut;
  motif?: string;
  objectif?: string;
  compteRendu?: string;
  photoUrl?: string;
  present?: boolean;
  createdAt: string;
}

export interface CreateVisitRequest {
  soulId: string;
  datePrevue: string;
  motif?: string;
  objectif?: string;
}

export interface UpdateVisitRequest {
  statut: VisitStatut;
  dateRealisee?: string;
  compteRendu?: string;
  photoUrl?: string;
  present?: boolean;
  datePrevue?: string;
}

// ======================== Badges & Gamification ========================

export type BadgeNiveau = 'BRONZE' | 'ARGENT' | 'OR' | 'DIAMANT';
export type BadgeCritere = 'VISITES' | 'PRESENCE' | 'EVANGELISATION' | 'INTERACTIONS' | 'FIDELITE';

export interface Badge {
  id: string;
  code: string;
  nom: string;
  description?: string;
  icone?: string;
  niveau: BadgeNiveau;
  critere: BadgeCritere;
  seuil: number;
  score: number;
  gagne: boolean;
  progression: number;
}

export interface BadgeProfile {
  userId: string;
  totalBadges: number;
  scores: Record<BadgeCritere, number>;
  badges: Badge[];
}

export interface LeaderboardEntry {
  userId: string;
  nom: string;
  badges: number;
}

// ======================== Formations ========================

export type CourseNiveau = 'DEBUTANT' | 'INTERMEDIAIRE' | 'AVANCE';
export type EnrollmentStatut = 'INSCRIT' | 'EN_COURS' | 'TERMINE';

export interface Course {
  id: string;
  titre: string;
  description?: string;
  categorie: string;
  niveau: CourseNiveau;
  dureeMinutes?: number;
  formateurId?: string;
  formateurNom?: string;
  imageUrl?: string;
  actif: boolean;
  nbModules: number;
  nbInscrits: number;
  createdAt: string;
}

export interface CourseModule {
  id: string;
  courseId: string;
  titre: string;
  contenu?: string;
  videoUrl?: string;
  ordre: number;
}

export interface QuizQuestion {
  id: string;
  moduleId: string;
  question: string;
  propositions: string;
  ordre: number;
}

export interface CreateQuestionRequest {
  question: string;
  propositions: string;
  reponseIndex: number;
  ordre: number;
}

export interface CourseEnrollment {
  id: string;
  courseId: string;
  userId: string;
  statut: EnrollmentStatut;
  progression: number;
  scoreQuiz?: number;
  dateInscription: string;
  dateTerminaison?: string;
}

export interface Certificate {
  id: string;
  enrollmentId: string;
  numero: string;
  userId: string;
  courseId: string;
  scoreFinal: number;
  delivreLe: string;
}

export interface CreateCourseRequest {
  titre: string;
  description?: string;
  categorie?: string;
  niveau: CourseNiveau;
  dureeMinutes?: number;
  formateurId?: string;
  imageUrl?: string;
}

export interface CreateModuleRequest {
  titre: string;
  contenu?: string;
  videoUrl?: string;
  ordre: number;
}

export interface SubmitQuizRequest {
  moduleId: string;
  reponses: Record<string, number>;
}

export interface QuizResult {
  score: number;
  bonnesReponses: number;
  totalQuestions: number;
  reussi: boolean;
  certificat: boolean;
}

/** Statistiques globales de la formation (GET /trainings/stats) — données réelles. */
export interface TrainingStats {
  nbCours: number;
  nbInscrits: number;
  nbCertificats: number;
  progressionMoyenne: number;
  parCategorie?: Record<string, number>;
  parStatut?: Record<string, number>;
}

// ======================== Rendez-vous ========================

export type AppointmentMotif = 'CONSEIL' | 'CONFESSION' | 'SUIVI' | 'FORMATION' | 'AUTRE';
export type AppointmentStatut = 'EN_ATTENTE' | 'CONFIRME' | 'REFUSE' | 'ANNULE' | 'TERMINE';

export interface Appointment {
  id: string;
  demandeurId: string;
  demandeurNom?: string;
  recepteurId: string;
  recepteurNom?: string;
  motif: AppointmentMotif;
  objet?: string;
  datePrevue: string;
  dureeMinutes: number;
  statut: AppointmentStatut;
  reponse?: string;
  dateTraitement?: string;
  rappelEnvoye: boolean;
  createdAt: string;
}

export interface CreateAppointmentRequest {
  recepteurId: string;
  motif: AppointmentMotif;
  objet?: string;
  datePrevue: string;
  dureeMinutes?: number;
}

export interface UpdateAppointmentStatusRequest {
  statut: AppointmentStatut;
  reponse?: string;
}

// ======================== Workflow de transfert (V37) ========================

export type TransferType =
  | 'MEMBRE_DEPARTEMENT_TRANSFERT'
  | 'MEMBRE_DEPARTEMENT_AJOUT'
  | 'MEMBRE_DEPARTEMENT_RETRAIT'
  | 'DISCIPLE_FAMILLE_TRANSFERT'
  | 'FAISEUR_FAMILLE_TRANSFERT'
  | 'CHEF_FAMILLE_TRANSFERT'
  | 'FAISEUR_DISCIPLE_CHANGEMENT'
  | 'RESPONSABLE_DEPARTEMENT_CHANGEMENT'
  | 'CHEF_ADJOINT_CHANGEMENT';

export type TransferStatus =
  | 'BROUILLON'
  | 'SOUMIS'
  | 'EN_ATTENTE_VALIDATION'
  | 'VALIDATION_PARTIELLE'
  | 'VALIDE'
  | 'REFUSE'
  | 'ANNULE'
  | 'EXECUTE'
  | 'ARCHIVE';

export type DecisionType = 'APPROBATION' | 'REFUS' | 'DEMANDE_INFORMATIONS' | 'RENVOI_CORRECTION';
export type PrioriteTransfert = 'BASSE' | 'MOYENNE' | 'HAUTE' | 'URGENTE';
export type ValidationMode = 'SEQUENTIEL' | 'PARALLELE' | 'N_VALIDATIONS_REQUISES';

export type AffectationType = 'FAMILLE' | 'DEPARTEMENT' | 'FAISEUR' | 'UTILISATEUR';

export interface Affectation {
  type: AffectationType;
  id: string;
  nom: string;
}

export interface TransferRequest {
  id: string;
  type: TransferType;
  statut: TransferStatus;
  personneId: string;
  personneType: 'SOUL' | 'USER';
  personneNom: string;
  ancienneAffectation?: Affectation | null;
  nouvelleAffectation: Affectation;
  demandeurId: string;
  demandeurNom?: string;
  justification: string;
  priorite: PrioriteTransfert;
  commentaires?: string;
  dateSoumission?: string;
  dateExecution?: string;
  delaiLimite?: string;
  etapeCourante: number;
  approbationsObtenues: number;
  totalEtapes: number;
  createdAt: string;
}

export interface CreateTransferRequest {
  type: TransferType;
  personneId: string;
  personneType: 'SOUL' | 'USER';
  ancienneAffectation?: Affectation;
  nouvelleAffectation: Affectation;
  justification: string;
  priorite: PrioriteTransfert;
  commentaires?: string;
  fichierIds?: string[];
}

export interface TransferConfiguration {
  id: string;
  type: TransferType;
  label: string;
  description?: string;
  actif: boolean;
  rolesInitiateurs: string[];
  canInitier: boolean;
  etapes: string[];
}

export interface EtapeValidation {
  id: string;
  etapeOrdre: number;
  rolesValidateurs: string[];
  label: string;
  description?: string;
  requis: boolean;
  validee: boolean;
}

export interface TransferDecision {
  id: string;
  validateurId: string;
  validateurNom?: string;
  roleValidateur?: string;
  decision: DecisionType;
  motivation?: string;
  etapeOrdre: number;
  createdAt: string;
}

export interface TransferAttachment {
  id: string;
  fileId: string;
  nom?: string;
  url?: string;
  createdAt: string;
}

export interface TransferDetail {
  transfert: TransferRequest;
  etapes: EtapeValidation[];
  decisions: TransferDecision[];
  piecesJointes: TransferAttachment[];
  peutValider: boolean;
  roleActif: string;
  modeValidation?: ValidationMode;
}

export interface TransferHistoryEntry {
  id: string;
  action: string;
  ancienStatut?: string;
  nouveauStatut?: string;
  utilisateurId?: string;
  utilisateurNom?: string;
  roleActif?: string;
  commentaire?: string;
  ancienneValeur?: Record<string, unknown>;
  nouvelleValeur?: Record<string, unknown>;
  createdAt: string;
}

export interface WorkflowStep {
  id: string;
  etapeOrdre: number;
  rolesValidateurs: string[];
  label: string;
  description?: string;
  requis: boolean;
}

export interface WorkflowConfig {
  id: string;
  transferType: TransferType;
  label: string;
  description?: string;
  actif: boolean;
  rolesInitiateurs: string[];
  modeValidation: ValidationMode;
  nombreValidationsRequises: number;
  delaiTraitementHeures: number;
  notificationsAuto: boolean;
  modeleMessageDemande?: string;
  modeleMessageValidation?: string;
  modeleMessageRefus?: string;
  modeleMessageExecution?: string;
  reglesExecution?: Record<string, unknown>;
  steps: WorkflowStep[];
}

export const TRANSFER_TYPE_LABELS: Record<TransferType, string> = {
  MEMBRE_DEPARTEMENT_TRANSFERT: 'Transfert de membre entre départements',
  MEMBRE_DEPARTEMENT_AJOUT: 'Ajout de membre dans un département',
  MEMBRE_DEPARTEMENT_RETRAIT: 'Retrait de membre d\'un département',
  DISCIPLE_FAMILLE_TRANSFERT: 'Transfert de disciple entre familles',
  FAISEUR_FAMILLE_TRANSFERT: 'Transfert de faiseur entre familles',
  CHEF_FAMILLE_TRANSFERT: 'Transfert de chef de famille',
  FAISEUR_DISCIPLE_CHANGEMENT: 'Changement du faiseur d\'un disciple',
  RESPONSABLE_DEPARTEMENT_CHANGEMENT: 'Changement du responsable d\'un département',
  CHEF_ADJOINT_CHANGEMENT: 'Changement du chef adjoint d\'une famille',
};

export const TRANSFER_STATUS_LABELS: Record<TransferStatus, string> = {
  BROUILLON: 'Brouillon',
  SOUMIS: 'Soumis',
  EN_ATTENTE_VALIDATION: 'En attente de validation',
  VALIDATION_PARTIELLE: 'Validation partielle',
  VALIDE: 'Validé',
  REFUSE: 'Refusé',
  ANNULE: 'Annulé',
  EXECUTE: 'Exécuté',
  ARCHIVE: 'Archivé',
};

export const DECISION_LABELS: Record<DecisionType, string> = {
  APPROBATION: 'Approbation',
  REFUS: 'Refus',
  DEMANDE_INFORMATIONS: 'Demande d\'informations',
  RENVOI_CORRECTION: 'Renvoi pour correction',
};

export const PRIORITE_LABELS: Record<PrioriteTransfert, string> = {
  BASSE: 'Basse',
  MOYENNE: 'Moyenne',
  HAUTE: 'Haute',
  URGENTE: 'Urgente',
};

export const ROLES: UserRole[] = ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'];

// ======================== Cartographie ========================

export type MapPointType = 'SOUL' | 'FAMILY';

export interface MapPoint {
  id: string;
  type: MapPointType;
  nom: string;
  latitude: number;
  longitude: number;
  zone?: string;
  statut?: string;
  familleNom?: string;
  departementNom?: string;
  niveauCroissance?: number;
}

export interface UpdateCoordinatesRequest {
  latitude: number;
  longitude: number;
  zone?: string;
}

// ======================== Plateforme configurable (V38) ========================

/** Identité publique de l'église (sans authentification) — pilotée par /api/v1/public/settings. */
export interface PublicBranding {
  churchName: string;
  platformName: string;
  slogan?: string;
  description?: string;
  logoUrl?: string;
  faviconUrl?: string;
  bannerUrl?: string;
  primaryColor: string;
  accentColor: string;
  buttonColor: string;
  fontFamily: string;
  allowDarkMode: boolean;
  address?: string;
  phone?: string;
  email?: string;
  website?: string;
  socialLinks?: Record<string, string>;
}

/** Vue complète des paramètres d'identité & de marque (authentifié). */
export interface ChurchSettings extends PublicBranding {
  id: string;
  contactNotes?: string;
}

export type CustomFieldType =
  | 'TEXTE' | 'NOMBRE' | 'DATE' | 'DATE_HEURE' | 'BOOLEEN' | 'SELECTION'
  | 'SELECTION_MULTIPLE' | 'FICHIER' | 'IMAGE' | 'TELEPHONE' | 'EMAIL' | 'URL' | 'TEXTAREA';

export interface CustomFieldDefinition {
  id: string;
  entiteType: 'SOUL' | 'USER' | 'DEPARTMENT' | 'FAMILY';
  code: string;
  label: string;
  type: CustomFieldType;
  obligatoire: boolean;
  ordre: number;
  options?: string[];
  placeholder?: string;
  defaultValue?: string;
  rolesLecture: string[];
  rolesEcriture: string[];
  actif: boolean;
}

export interface CustomFieldValue {
  fieldId: string;
  value?: string | number | boolean | null;
}

export interface CustomFieldBundle {
  definitions: CustomFieldDefinition[];
  values: CustomFieldValue[];
}

// ======================== Dictionnaires configurables (V52) ========================

/**
 * Entrée d'un dictionnaire de la plateforme. Les référentiels (types
 * d'événements, statuts, raisons d'absence, catégories…) sont chargés
 * depuis l'API et adaptables par l'admin — plus aucune liste codée en dur.
 */
export interface DictionaryEntry {
  id: string;
  dictKey: string;
  code: string;
  label: string;
  description?: string;
  color?: string;
  ordre: number;
  actif: boolean;
  isDefault: boolean;
}

/** Dictionnaires groupés par clé (EVENT_TYPE, SOUL_STATUS…). */
export type DictionariesMap = Record<string, DictionaryEntry[]>;

// ======================== Modules & Menus configurables ========================

export interface PlatformModule {
  key: string;
  label: string;
  description?: string;
  icon?: string;
  enabled: boolean;
  ordre: number;
  /** Section (catégorie) de navigation, ex: 'Pilotage', 'Administration'. */
  section: string;
}

export interface MenuEntry {
  id: string;
  key: string;
  label: string;
  href: string;
  icon?: string;
  section: string;
  ordre: number;
  roles: string[];
  moduleKey?: string;
  enabled: boolean;
}

// ======================== Page Builder (V65) ========================

/** Bloc d'une page personnalisée : type + configuration saisie par l'admin. */
export interface CustomPageBlock {
  type: string;
  config: Record<string, unknown>;
}

/** Page personnalisée (Page Builder). */
export interface CustomPage {
  id: string;
  key: string;
  title: string;
  description?: string;
  slug: string;
  layout: string;
  blocks: CustomPageBlock[];
  roles: string[];
  enabled: boolean;
  published: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** Bloc résolu : la configuration + les données réelles résolues côté serveur. */
export interface ResolvedBlock extends CustomPageBlock {
  data?: Record<string, unknown> | null;
}

/** Page résolue prête à l'affichage (données réelles). */
export interface ResolvedPage {
  page: CustomPage;
  blocks: ResolvedBlock[];
}

/** Source de données exploitable par les blocs du Page Builder. */
export interface PageDataSource {
  key: string;
  label: string;
  type: 'KPI' | 'TABLEAU' | 'LISTE' | 'GRAPHIQUE' | 'CALENDRIER' | 'TIMELINE' | 'FICHIERS' | 'TACHES';
  description: string;
  sensitive: boolean;
}

// ======================== Finances (V68) ========================

export type FinanceTransactionType = 'RECETTE' | 'DEPENSE';
export type FinanceBudgetStatus = 'OK' | 'ALERTE' | 'DEPASSE';

export interface FinanceTransaction {
  id: string;
  type: FinanceTransactionType;
  categorie: string;
  montant: number;
  description?: string;
  dateTransaction: string;
  createdAt?: string;
}

export interface CreateFinanceTransactionRequest {
  type: FinanceTransactionType;
  categorie: string;
  montant: number;
  description?: string;
  dateTransaction: string;
}

export interface FinanceBudget {
  id: string;
  categorie: string;
  annee: number;
  montant: number;
  depenseReelle: number;
  consommationPct: number;
  statut: FinanceBudgetStatus;
}

export interface CreateFinanceBudgetRequest {
  categorie: string;
  annee: number;
  montant: number;
}

export interface FinanceMonthPoint {
  mois: string;
  recettes: number;
  depenses: number;
}

export interface FinanceCategoryTotal {
  categorie: string;
  total: number;
}

export interface FinanceStats {
  annee: number;
  totalRecettes: number;
  totalDepenses: number;
  solde: number;
  nbTransactions: number;
  parMois: FinanceMonthPoint[];
  recettesParCategorie: FinanceCategoryTotal[];
  depensesParCategorie: FinanceCategoryTotal[];
}

// ======================== Communication (V69) ========================

export type CommunicationCible = 'TOUS' | 'ROLE' | 'FAMILLE' | 'DEPARTEMENT';
export type CommunicationStatut = 'BROUILLON' | 'PUBLIEE' | 'ARCHIVEE';

export interface Communication {
  id: string;
  titre: string;
  contenu: string;
  cible: CommunicationCible;
  roles: string[];
  familleId?: string;
  departmentId?: string;
  statut: CommunicationStatut;
  datePublication?: string;
  createdAt?: string;
}

export interface CreateCommunicationRequest {
  titre: string;
  contenu: string;
  cible: CommunicationCible;
  roles?: string[];
  familleId?: string;
  departmentId?: string;
}

// ======================== Rôles & permissions (T3) ========================

export interface PlatformRole {
  key: string;
  label: string;
  description?: string;
  system: boolean;
  permissions: string[];
}

export interface PermissionEntry {
  role: string;
  permission: string;
  enabled: boolean;
}

// ======================== Bêta-testing & feedback (V50) ========================

export interface PlatformMeta {
  appName: string;
  version: string;
  environment: string;
  betaMode: boolean;
  demoAccountsEnabled: boolean;
}

export type FeedbackCategory =
  | 'BUG'
  | 'UX'
  | 'SUGGESTION'
  | 'FONCTIONNALITE_MANQUANTE'
  | 'PERFORMANCE'
  | 'TRADUCTION'
  | 'AFFICHAGE'
  | 'AUTRE';

export type FeedbackPriority = 'BASSE' | 'MOYENNE' | 'HAUTE' | 'CRITIQUE';
export type FeedbackStatus = 'NOUVEAU' | 'EN_COURS' | 'RESOLU' | 'REJETE';

export interface Feedback {
  id: string;
  category: FeedbackCategory;
  priority: FeedbackPriority;
  subject: string;
  description?: string;
  pageUrl?: string;
  browser?: string;
  device?: string;
  os?: string;
  appVersion?: string;
  status: FeedbackStatus;
  createdBy?: string;
  reporterEmail?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFeedbackRequest {
  category: FeedbackCategory;
  priority?: FeedbackPriority;
  subject: string;
  description?: string;
  pageUrl?: string;
  userAgent?: string;
  browser?: string;
  device?: string;
  os?: string;
}

export interface FeedbackStats {
  total: number;
  nouveaux: number;
  enCours: number;
  resolus: number;
  rejetes: number;
  parCategorie: Record<string, number>;
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

// ==================== PASTEUR DASHBOARD ====================

export interface PasteurDashboardCroissance {
  totalAmes: number;
  nouveauxArrivants: number;
  nouveauxConvertis: number;
  actifs: number;
  enIntegration: number;
  enVeille: number;
  decroches: number;
  tauxConversion: number;
}

export interface PasteurDashboardDepartement {
  id: string;
  nom: string;
  totalFamilles: number;
  totalAmes: number;
  responsableId: string;
  responsableNom: string;
}

export interface PasteurDashboardFamille {
  id: string;
  nom: string;
  totalAmes: number;
  actifs: number;
  enIntegration: number;
  tauxPresence: number;
  chefFamilleId: string;
  chefNom: string;
  aRisque: boolean;
}

export interface PasteurDashboardFaiseur {
  id: string;
  nom: string;
  email: string;
  totalAmes: number;
  actifs: number;
  enIntegration: number;
  estChef: boolean;
  statut: string;
}

export interface PasteurDashboardPresences {
  tauxGlobal: number;
  presents: number;
  totalPossibles: number;
  tauxNouveauxArrivants: number;
  tauxNouveauxConvertis: number;
}

export interface PasteurDashboardRapports {
  tauxCompletion: number;
  soumis: number;
  enAttente: number;
  faiseursAyantRapporte: number;
  totalFaiseurs: number;
}

export interface PasteurDashboardTransfert {
  id: string;
  type: string;
  statut: string;
  priorite: string;
  dateSoumission: string;
  personneNom: string | null;
  cible: string | null;
}

export interface PasteurDashboardFamilleRisque {
  id: string;
  nom: string;
  tauxPresence: number;
  niveauRisque: string;
}

export interface PasteurDashboardData {
  croissance: PasteurDashboardCroissance;
  departements: PasteurDashboardDepartement[];
  familles: PasteurDashboardFamille[];
  faiseurs: PasteurDashboardFaiseur[];
  presences: PasteurDashboardPresences;
  alertesActives: number;
  rapports: PasteurDashboardRapports;
  suivisParallelesActifs: number;
  famillesARisque: PasteurDashboardFamilleRisque[];
  transfertsEnAttente: PasteurDashboardTransfert[];
  semaine: string;
}

// ==================== PRESENCE TREND ====================

export interface PresenceTrendPoint {
  mois: string;
  taux: number;
  presents: number;
  total: number;
}

export interface PresenceTrendData {
  tendance: PresenceTrendPoint[];
  tendanceGlobale: number;
}

// ==================== PASTORAL 360 ====================

export interface Pastoral360Informations {
  id: string;
  nom: string;
  prenom: string;
  email?: string;
  telephone?: string;
  adresse?: string;
  dateNaissance?: string;
  profession?: string;
  situationFamiliale?: string;
  photoUrl?: string;
}

export interface Pastoral360Indices {
  [key: string]: number;
  santeSpirituelle: number;
  fidelite: number;
  engagement: number;
  participation: number;
  global: number;
}

export interface Pastoral360Spirituel {
  typeDisciple: string;
  statut: string;
  etatSpirituel: string;
  niveauCroissance: number;
  dateIntegration: string;
  dateConversion?: string;
  dateDernierContact?: string;
}

export interface Pastoral360Alerte {
  type: string;
  message: string;
  priorite: string;
}

export interface Pastoral360Encadrement {
  faiseurId: string;
  faiseurNom?: string;
  familleId: string;
}

export interface Pastoral360Evaluation {
  moyenne: number;
  total: number;
}

export interface Pastoral360Note {
  id: string;
  contenu: string;
  auteurId: string;
  date: string;
}

export interface Pastoral360TimelineEntry {
  id: string;
  type: string;
  description?: string;
  ancienStatut?: string;
  nouveauStatut?: string;
  utilisateurId?: string;
  date: string;
}

export interface Pastoral360Data {
  informations: Pastoral360Informations;
  spirituel: Pastoral360Spirituel;
  indices: Pastoral360Indices;
  alertesAutomatiques: Pastoral360Alerte[];
  encadrement: Pastoral360Encadrement;
  timeline: Pastoral360TimelineEntry[];
  evaluations: Record<string, Pastoral360Evaluation>;
  notes: Pastoral360Note[];
  piecesJointes: TransferAttachment[];
}

// ==================== SPIRITUAL SCORE HISTORY ====================

export interface SpiritualScorePoint {
  semaine: string;
  scoreGlobal: number;
  sante: number;
  fidelite: number;
  engagement: number;
  participation: number;
}

// ==================== RECENT ACTIVITY ====================

export interface AuditRecentActivity {
  id: string;
  utilisateurNom: string;
  action: string;
  entiteType: string;
  entiteId: string;
  details: string;
  createdAt: string;
}

// ==================== TENANTS (ADMIN) ====================

export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'CANCELLED' | 'PENDING_SETUP';

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: TenantStatus;
  plan: string;
  createdAt: string;
  updatedAt: string;
}

// API Error
export interface ApiError {
  status: number;
  title: string;
  detail: string;
  instance?: string;
  errors?: Record<string, string[]>;
}
