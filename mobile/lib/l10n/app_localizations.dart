import 'package:flutter/material.dart';

/// Comprehensive i18n system for Discipolat.
/// Supports FR, EN, PT with fallback to FR.
/// Add new strings in the _translations map for each locale.
class AppLocalizations {
  final Locale locale;
  AppLocalizations(this.locale);

  static AppLocalizations of(BuildContext context) {
    // Tolérant : retombe sur une instance FR si aucun delegate n'est installé
    // (ex.: tests widget qui pompent l'écran sans MaterialApp localisé).
    return Localizations.of<AppLocalizations>(context, AppLocalizations) ??
        AppLocalizations(const Locale('fr'));
  }

  static const LocalizationsDelegate<AppLocalizations> delegate = _AppLocalizationsDelegate();

  static const List<Locale> supportedLocales = [
    Locale('fr'),
    Locale('en'),
    Locale('pt'),
  ];

  /// All translations organized by locale code.
  static const Map<String, Map<String, String>> _translations = {
    'fr': _french,
    'en': _english,
    'pt': _portuguese,
  };

  String translate(String key) {
    final lang = locale.languageCode;
    return _translations[lang]?[key] ?? _translations['fr']?[key] ?? key;
  }

  // ==================== COMMON ====================
  String get appTitle => translate('appTitle');
  String get login => translate('login');
  String get logout => translate('logout');
  String get email => translate('email');
  String get password => translate('password');
  String get signIn => translate('signIn');
  String get signOut => translate('signOut');
  String get save => translate('save');
  String get cancel => translate('cancel');
  String get delete => translate('delete');
  String get edit => translate('edit');
  String get add => translate('add');
  String get search => translate('search');
  String get loading => translate('loading');
  String get error => translate('error');
  String get success => translate('success');
  String get noData => translate('noData');
  String get confirm => translate('confirm');
  String get back => translate('back');
  String get close => translate('close');
  String get yes => translate('yes');
  String get no => translate('no');
  String get none => translate('none');
  String get all => translate('all');
  String get filter => translate('filter');
  String get refresh => translate('refresh');
  String get retry => translate('retry');
  // ── Lot H2 : admin_settings / admin_tenants / growth_projection / my_team_family ──
  String get churchSettings => translate('churchSettings');
  String get identity => translate('identity');
  String get slogan => translate('slogan');
  String get colorsSection => translate('colorsSection');
  String get primaryColorLabel => translate('primaryColorLabel');
  String get accentColorLabel => translate('accentColorLabel');
  String get settingsSaved => translate('settingsSaved');
  String get tenantsTitle => translate('tenantsTitle');
  String get noTenants => translate('noTenants');
  String get statusSuspended => translate('statusSuspended');
  String get statusActive => translate('statusActive');
  String get growthProjectionTitle => translate('growthProjectionTitle');

  String get growthProphecyTitle => translate('growthProphecyTitle');
  String projectedAnnualGrowth(String value) =>
      translate('projectedAnnualGrowth').replaceAll('{value}', value);
  String headcountIn12Months(String value) =>
      translate('headcountIn12Months').replaceAll('{value}', value);
  String leadersNeededCount(String value) =>
      translate('leadersNeededCount').replaceAll('{value}', value);
  String get simulator => translate('simulator');
  String get annualGrowthRate => translate('annualGrowthRate');
  String get horizonMonths => translate('horizonMonths');
  String get simulate => translate('simulate');
  String get simulationSaved => translate('simulationSaved');
  String get simulationFailed => translate('simulationFailed');
  String get savedProjections => translate('savedProjections');
  String get myTeamFamilyTitle => translate('myTeamFamilyTitle');
  String teamTabMembers(int count) =>
      translate('teamTabMembers').replaceAll('{count}', '$count');
  String teamTabReceived(int count) =>
      translate('teamTabReceived').replaceAll('{count}', '$count');
  String encourageName(String name) =>
      translate('encourageName').replaceAll('{name}', name);
  String get noSpiritualFamily => translate('noSpiritualFamily');
  String get meLabel => translate('meLabel');
  String encouragementsBadge(int count) =>
      translate('encouragementsBadge').replaceAll('{count}', '$count');
  String get sendEncouragementTooltip => translate('sendEncouragementTooltip');
  String get noEncouragementsYet => translate('noEncouragementsYet');

  // ==================== LOT H1 — écrans historiques ====================
  String get newField => translate('newField');
  String get fieldNameLabel => translate('fieldNameLabel');
  String get fieldEntityLabel => translate('fieldEntityLabel');
  String get fieldTypeLabel => translate('fieldTypeLabel');
  String get create => translate('create');
  String get noCustomFields => translate('noCustomFields');
  String get deleteQuestion => translate('deleteQuestion');
  String get transferWorkflowTitle => translate('transferWorkflowTitle');
  String get noConfiguration => translate('noConfiguration');
  String get statusInactive => translate('statusInactive');
  String stepsCount(int count) => translate('stepsCount').replaceAll('{count}', '$count');
  String get initiatorRoles => translate('initiatorRoles');
  String get validationMode => translate('validationMode');
  String get requiredValidations => translate('requiredValidations');
  String get delayHours => translate('delayHours');
  String get circuitSteps => translate('circuitSteps');
  String stepLabel(int index, String label) => translate('stepLabel')
      .replaceAll('{index}', '$index').replaceAll('{label}', label);
  String get configSaved => translate('configSaved');
  String get saveFailed => translate('saveFailed');
  String get deleteConfigQuestion => translate('deleteConfigQuestion');
  String get deleteBlockedByRequests => translate('deleteBlockedByRequests');
  String get memberRequestsTitle => translate('memberRequestsTitle');
  String get tabMyRequests => translate('tabMyRequests');
  String get tabInbox => translate('tabInbox');
  String get newRequest => translate('newRequest');
  String get requestType => translate('requestType');
  String get recipient => translate('recipient');
  String get subjectOptional => translate('subjectOptional');
  String get messageHint => translate('messageHint');
  String get attachments => translate('attachments');
  String get sendError => translate('sendFailed');
  String get noSentRequests => translate('noSentRequests');
  String get noReceivedRequests => translate('noReceivedRequests');
  String fromLabel(String name) => translate('fromLabel').replaceAll('{name}', name);
  String get rejectAction => translate('reject');
  String get resolve => translate('resolve');
  String get securityTitle => translate('securityTitle');
  String get authSection => translate('authSection');
  String get biometricAuth => translate('biometricAuth');
  String get biometricSubtitle => translate('biometricSubtitle');
  String get pinCode => translate('pinCode');
  String get pinSubtitle => translate('pinSubtitle');
  String get changePinTitle => translate('changePinTitle');
  String get currentPin => translate('currentPin');
  String get newPin => translate('newPin');
  String get pinUpdated => translate('pinUpdated');
  String get pinIncorrect => translate('pinIncorrect');
  String get sessionSection => translate('sessionSection');
  String get sessionExpiry => translate('sessionExpiry');
  String sessionExpirySubtitle(String value) => translate('sessionExpirySubtitle').replaceAll('{value}', value);
  String get never => translate('never');
  String minutesCount(int count) => translate('minutesCount').replaceAll('{count}', '$count');
  String get screenProtection => translate('screenProtection');
  String get screenProtectionSubtitle => translate('screenProtectionSubtitle');
  String get auditSection => translate('auditSection');
  String auditLogEntries(int count) => translate('auditLogEntries').replaceAll('{count}', '$count');
  String get viewAuditSubtitle => translate('viewAuditSubtitle');
  String get exportLog => translate('exportLog');
  String get exportSubtitle => translate('exportSubtitle');
  String get clearLog => translate('clearLog');
  String get clearSubtitle => translate('clearSubtitle');
  String get accountSection => translate('accountSection');
  String get userLabel => translate('userLabel');
  String get orgLabel => translate('orgLabel');
  String get activeRoleLabel => translate('activeRoleLabel');
  String get rolesLabel => translate('rolesLabel');
  String get noEntries => translate('noEntries');
  String get logExported => translate('logExported');
  String entriesExported(int count) => translate('entriesExported').replaceAll('{count}', '$count');
  String get clearLogQuestion => translate('clearLogQuestion');
  String get clearAction => translate('clearAction');
  String get offline => translate('offline');

  // ── Lot H3 : member_activities / department_stats / evangelism ──
  String get myActivities => translate('myActivities');
  String get filterAll => translate('filterAll');
  String get filterPresences => translate('filterPresences');
  String get filterEvents => translate('filterEvents');
  String get filterNotes => translate('filterNotes');
  String get filterProgression => translate('filterProgression');
  String get noActivities => translate('noActivities');
  String get presenceConfirmed => translate('presenceConfirmed');
  String get absenceRecorded => translate('absenceRecorded');
  String get weekOf => translate('weekOf');
  String get makerNote => translate('makerNote');
  String spiritualLevel(String value) => translate('spiritualLevel').replaceAll('{value}', value);
  String get progressOverview => translate('progressOverview');
  String get departmentStats => translate('departmentStats');
  String get kpiMembers => translate('kpiMembers');
  String get kpiActive => translate('kpiActive');
  String get kpiNew => translate('kpiNew');
  String get kpiPresence => translate('kpiPresence');
  String get kpiOverdueTasks => translate('kpiOverdueTasks');
  String get kpiTeams => translate('kpiTeams');
  String get memberBreakdown => translate('memberBreakdown');
  String get integrating => translate('integrating');
  String get standby => translate('standby');
  String get droppedOut => translate('droppedOut');
  String get headcountEvolution => translate('headcountEvolution');
  String get attendanceSection => translate('attendanceSection');
  String get present => translate('present');
  String get absent => translate('absent');
  String get tasksByStatus => translate('tasksByStatus');
  String get disciplinaryCategory => translate('disciplinaryCategory');
  String get noDisciplinary => translate('noDisciplinary');
  String get workloadPerMember => translate('workloadPerMember');
  String get noAssignedTasks => translate('noAssignedTasks');
  String get tasksUnit => translate('tasksUnit');
  String get retardUnit => translate('retardUnit');
  String get organizationSection => translate('organizationSection');
  String get activeAssignments => translate('activeAssignments');
  String get activePositions => translate('activePositions');
  String evangelizationTitle(int count) =>
      translate('evangelizationTitle').replaceAll('{count}', '$count');
  String get funnelOfConversion => translate('funnelOfConversion');
  String get clickStageToSeeSouls => translate('clickStageToSeeSouls');
  String soulsAtStage(int count) => translate('soulsAtStage').replaceAll('{count}', '$count');
  String get searchHint => translate('searchHint');
  String get noSoulsAtStage => translate('noSoulsAtStage');
  String advanceTo(String label) => translate('advanceTo').replaceAll('{label}', label);
  String retreatLabel(String label) => translate('retreatLabel').replaceAll('{label}', label);
  String get globalView => translate('globalView');
  String sinceDate(String date) => translate('sinceDate').replaceAll('{date}', date);
  // ── Lot H4 : badges / giving ──
  String gamificationTitle(int count) =>
      translate('gamificationTitle').replaceAll('{count}', '$count');
  String get tabMyBadges => translate('tabMyBadges');
  String get tabLeaderboard => translate('tabLeaderboard');
  String get checkMyBadges => translate('checkMyBadges');
  String get progressionLabel => translate('progressionLabel');
  String percentCompleted(int pct) =>
      translate('percentCompleted').replaceAll('{pct}', '$pct');
  String badgesEarned(int count) =>
      translate('badgesEarned').replaceAll('{count}', '$count');
  String get scoresPerCriteria => translate('scoresPerCriteria');
  String earnedBadges(int count) =>
      translate('earnedBadges').replaceAll('{count}', '$count');
  String get toUnlock => translate('toUnlock');
  String get noLeaderboard => translate('noLeaderboard');
  String get badgesUnit => translate('badgesUnit');
  String newBadgesEarned(int count) =>
      translate('newBadgesEarned').replaceAll('{count}', '$count');
  String get noNewBadges => translate('noNewBadges');
  String get checkBadgesError => translate('checkBadgesError');
  String get criteriaVisits => translate('criteriaVisits');
  String get criteriaInteractions => translate('criteriaInteractions');
  String get criteriaEvangelism => translate('criteriaEvangelism');
  String get criteriaAttendance => translate('criteriaAttendance');
  String get criteriaLoyalty => translate('criteriaLoyalty');
  String get tithesAndOfferings => translate('tithesAndOfferings');
  String get giveNow => translate('giveNow');
  String get amountXOF => translate('amountXOF');
  String get invalidAmount => translate('invalidAmount');
  String get operatorLabel => translate('operatorLabel');
  String get destinationLabel => translate('destinationLabel');
  String get mobilePhoneOptional => translate('mobilePhoneOptional');
  String paymentInitiated(String ref) =>
      translate('paymentInitiated').replaceAll('{ref}', ref);
  String get paymentFailed => translate('paymentFailed');
  String get paymentConfirmed => translate('paymentConfirmed');
  String waitingConfirmation(String ref) =>
      translate('waitingConfirmation').replaceAll('{ref}', ref);
  String get noDonationsYet => translate('noDonationsYet');
  String get statusConfirmed => translate('statusConfirmed');
  String get statusPending => translate('statusPending');
  String get statusFailed => translate('statusFailed');
  String get statusCancelled => translate('statusCancelled');
  // ── Lot H5 : broadcast / forms / church-directory ──
  String get broadcastTitle => translate('broadcastTitle');
  String get newBroadcast => translate('newBroadcast');
  String get sent => translate('sent');
  String get readRate => translate('readRate');
  String get recentBroadcasts => translate('recentBroadcasts');
  String get targeting => translate('targeting');
  String get allMembers => translate('allMembers');
  String get byDepartment => translate('byDepartment');
  String get byFamily => translate('byFamily');
  String get byRole => translate('byRole');
  String get broadcastEmpty => translate('broadcastEmpty');
  String get broadcastError => translate('broadcastError');
  String get readLabel => translate('readLabel');
  String get formsTitle => translate('formsTitle');
  String get createForm => translate('createForm');
  String get createFormSubtitle => translate('createFormSubtitle');
  String get publishedForms => translate('publishedForms');
  String get fieldText => translate('fieldText');
  String get fieldChoice => translate('fieldChoice');
  String get fieldDate => translate('fieldDate');
  String get fieldFile => translate('fieldFile');
  String get fieldSignature => translate('fieldSignature');
  String get fieldNote => translate('fieldNote');
  String get formTitleLabel => translate('formTitleLabel');
  String get addFieldHint => translate('addFieldHint');
  String get formSaved => translate('formSaved');
  String responsesCount(int count) =>
      translate('responsesCount').replaceAll('{count}', '$count');
  String get formsEmpty => translate('formsEmpty');
  String get formsError => translate('formsError');
  String get churchDirectoryTitle => translate('churchDirectoryTitle');
  String get searchMember => translate('searchMember');
  String get publicProfile => translate('publicProfile');
  String get privateProfile => translate('privateProfile');
  String get directoryEmpty => translate('directoryEmpty');
  String get directoryError => translate('directoryError');
  String get unknownFamily => translate('unknownFamily');
  String get unknownRole => translate('unknownRole');

  // ── Lot H6 : dashboards + transferts ──
  String get dashGrowth => translate('dashGrowth');
  String get dashPresenceAndReports => translate('dashPresenceAndReports');
  String get dashGlobalPresence => translate('dashGlobalPresence');
  String get dashReportsSubmitted => translate('dashReportsSubmitted');
  String get dashCompletion => translate('dashCompletion');
  String get dashActiveAlerts => translate('dashActiveAlerts');
  String get attentionRequired => translate('attentionRequired');
  String get allUnderControl => translate('allUnderControl');
  String get dashDepartments => translate('dashDepartments');
  String get dashFamilies => translate('dashFamilies');
  String get dashMakers => translate('dashMakers');
  String get dashRiskFamilies => translate('dashRiskFamilies');
  String get dashShepherdsPilot => translate('dashShepherdsPilot');
  String get dashMyFamily => translate('dashMyFamily');
  String get dashMyMaker => translate('dashMyMaker');
  String get dashMyDepartments => translate('dashMyDepartments');
  String get dashMyProgression => translate('dashMyProgression');
  String get dashMakerNotes => translate('dashMakerNotes');
  String get dashRecentPresences => translate('dashRecentPresences');
  String get dashUpcomingEvents => translate('dashUpcomingEvents');
  String get dashQuickActions => translate('dashQuickActions');
  String dashMakersCount(int count) => translate('dashMakersCount').replaceAll('{count}', '$count');
  String get dashMakerWorkload => translate('dashMakerWorkload');
  String get dashDisciplesSplit => translate('dashDisciplesSplit');
  String get dashActiveAlertsTitle => translate('dashActiveAlertsTitle');
  String get dashUpcomingVisits => translate('dashUpcomingVisits');
  String get dashRecentPrayers => translate('dashRecentPrayers');
  String get dashMySpace => translate('dashMySpace');
  String get dashCurrentLevel => translate('dashCurrentLevel');
  String get dashPresenceThisWeek => translate('dashPresenceThisWeek');
  String dashNextStep(String value) => translate('dashNextStep').replaceAll('{value}', value);
  String get dashCompanion => translate('dashCompanion');
  String dashChefLabel(String name) => translate('dashChefLabel').replaceAll('{name}', name);
  String dashRespLabel(String name) => translate('dashRespLabel').replaceAll('{name}', name);
  String get dashDeptToAdmin => translate('dashDeptToAdmin');
  String get dashOverview => translate('dashOverview');
  String get dashMyDepartment => translate('dashMyDepartment');
  String get dashDeptManagement => translate('dashDeptManagement');
  String get dashOverdueTasks => translate('dashOverdueTasks');
  String get dashBirthdays => translate('dashBirthdays');
  String get dashFollowUp => translate('dashFollowUp');
  String get dashDiscipline => translate('dashDiscipline');
  String get dashTransferRequests => translate('dashTransferRequests');
  String get dashEventsAndPosts => translate('dashEventsAndPosts');
  String get dashDeptManagementTitle => translate('dashDeptManagementTitle');
  String get dashQRCheckin => translate('dashQRCheckin');
  String get dashReport => translate('dashReport');
  String get dashPresenceLabel => translate('dashPresenceLabel');
  String get dashActiveLabel => translate('dashActiveLabel');
  String get dashNewLabel => translate('dashNewLabel');
  String get dashPresentLabel => translate('dashPresentLabel');
  String get dashAbsentLabel => translate('dashAbsentLabel');
  String get dashDroppedLabel => translate('dashDroppedLabel');
  String get dashTransferLabel => translate('dashTransferLabel');
  String get dashTeamsLabel => translate('dashTeamsLabel');
  String get dashPresentShort => translate('dashPresentShort');
  String get dashAbsentShort => translate('dashAbsentShort');
  String get dashSurcharge => translate('dashSurcharge');
  String get dashLightLoad => translate('dashLightLoad');
  String get dashNormalLoad => translate('dashNormalLoad');
  String get dashSouls => translate('dashSouls');
  String get dashConverti => translate('dashConverti');
  String get dashArrivant => translate('dashArrivant');
  String get transferCreateTitle => translate('transferCreateTitle');
  String get transferDetailTitle => translate('transferDetailTitle');
  String get transferType => translate('transferType');
  String get transferDeptConcerned => translate('transferDeptConcerned');
  String get transferDeptDestination => translate('transferDeptDestination');
  String get transferDeptRetrait => translate('transferDeptRetrait');
  String get transferFamilyConcerned => translate('transferFamilyConcerned');
  String get transferFamilyDestination => translate('transferFamilyDestination');
  String get transferMemberAdd => translate('transferMemberAdd');
  String get transferMemberRemove => translate('transferMemberRemove');
  String get transferMakerToTransfer => translate('transferMakerToTransfer');
  String get transferDiscipleToTransfer => translate('transferDiscipleToTransfer');
  String get transferJustification => translate('transferJustification');
  String get transferComments => translate('transferComments');
  String get transferSubmitCreate => translate('transferSubmitCreate');
  String get transferSaveDraft => translate('transferSaveDraft');
  String get transferDraftSaved => translate('transferDraftSaved');
  String get transferSubmitted => translate('transferSubmitted');
  String get transferIncomplete => translate('transferIncomplete');
  String get transferCreateError => translate('transferCreateError');
  String get transferChoose => translate('transferChoose');
  String get transferCurrentAffectation => translate('transferCurrentAffectation');
  String get transferCircuit => translate('transferCircuit');
  String get transferApprove => translate('transferApprove');
  String get transferReject => translate('transferReject');
  String get transferModify => translate('transferModify');
  String get transferAskInfo => translate('transferAskInfo');
  String get transferExecute => translate('transferExecute');
  String get transferArchive => translate('transferArchive');
  String get transferHistory => translate('transferHistory');
  String get transferDecisions => translate('transferDecisions');
  String get transferStep => translate('transferStep');
  String get transferInfo => translate('transferInfo');
  String get transferJustificationLabel => translate('transferJustificationLabel');
  String get transferMotivation => translate('transferMotivation');
  String get transferMotivationOptional => translate('transferMotivationOptional');
  String get transferCommentsLabel => translate('transferCommentsLabel');
  String get transferConfirm => translate('transferConfirm');
  String get transferCorrection => translate('transferCorrection');
  String get transferCancelQuestion => translate('transferCancelQuestion');
  String get transferCancelExplanation => translate('transferCancelExplanation');
  String get transferNoDocument => translate('transferNoDocument');
  String get transferNoAttachment => translate('transferNoAttachment');
  String get transferCreateDoc => translate('transferCreateDoc');
  String get transferNoEvent => translate('transferNoEvent');
  String get transferOperationError => translate('transferOperationError');
  String get transferAttachments => translate('transferAttachments');
  String get transferJustificationHint => translate('transferJustificationHint');
  String get transferPriority => translate('transferPriority');
  String get transferPerson => translate('transferPerson');
  String get transferSubmit => translate('transferSubmit');
  String get transferCancel => translate('transferCancel');
  String get transferConfirmBtn => translate('transferConfirmBtn');
  String get transferApprovalExplanation => translate('transferApprovalExplanation');
  String get transferRejectionExplanation => translate('transferRejectionExplanation');
  String get transferAutoExecution => translate('transferAutoExecution');
  String transferAttachmentsCount(int count) => translate('transferAttachmentsCount').replaceAll('{count}', count.toString());
  String transferValidateCount(int count) => translate('transferValidateCount').replaceAll('{count}', count.toString());
  String get transferAttachmentsUpdated => translate('transferAttachmentsUpdated');
  String get transferAttachmentsError => translate('transferAttachmentsError');
  String get transferListTitle => translate('transferListTitle');
  String get transferListNewRequest => translate('transferListNewRequest');
  String get transferListStatusFilter => translate('transferListStatusFilter');
  String get transferListTypeFilter => translate('transferListTypeFilter');
  String get transferListEmpty => translate('transferListEmpty');
  String transferListPersonLabel(String name) => translate('transferListPersonLabel').replaceAll('{name}', name);
  String transferListValidations(int approved, int total) => translate('transferListValidations').replaceAll('{approved}', '$approved').replaceAll('{total}', '$total');
  String get transferListSubmitError => translate('transferListSubmitError');
  String get transferListCancelSuccess => translate('transferListCancelSuccess');
  String get transferListConfigDefault => translate('transferListConfigDefault');
  // ── Lot H9 : department_management ──
  String get deptMgmtTitle => translate('deptMgmtTitle');
  String get deptMgmtTooltipStats => translate('deptMgmtTooltipStats');
  String get deptMgmtTooltipTools => translate('deptMgmtTooltipTools');
  String get deptMgmtSearchHint => translate('deptMgmtSearchHint');
  String get deptMgmtKpiTeams => translate('deptMgmtKpiTeams');
  String get deptMgmtKpiPositions => translate('deptMgmtKpiPositions');
  String get deptMgmtKpiAssigned => translate('deptMgmtKpiAssigned');
  String get deptMgmtTabMembers => translate('deptMgmtTabMembers');
  String get deptMgmtTabOrg => translate('deptMgmtTabOrg');
  String get deptMgmtTabTasks => translate('deptMgmtTabTasks');
  String get deptMgmtTabAssignments => translate('deptMgmtTabAssignments');
  String get deptMgmtTabEvents => translate('deptMgmtTabEvents');
  String get deptMgmtTabActivity => translate('deptMgmtTabActivity');
  String deptMgmtMembersTitle(int count) => translate('deptMgmtMembersTitle').replaceAll('{count}', '$count');
  String get deptMgmtMembersEmpty => translate('deptMgmtMembersEmpty');
  String deptMgmtFamilyLabel(String name) => translate('deptMgmtFamilyLabel').replaceAll('{name}', name);
  String deptMgmtMakerLabel(String name) => translate('deptMgmtMakerLabel').replaceAll('{name}', name);
  String get deptMgmtOrgTitle => translate('deptMgmtOrgTitle');
  String get deptMgmtOrgEmpty => translate('deptMgmtOrgEmpty');
  String get deptMgmtTeamPermanent => translate('deptMgmtTeamPermanent');
  String get deptMgmtTeamTemporary => translate('deptMgmtTeamTemporary');
  String get deptMgmtSubDepartment => translate('deptMgmtSubDepartment');
  String deptMgmtMembersCount(int count) => translate('deptMgmtMembersCount').replaceAll('{count}', '$count');
  String deptMgmtEventLabel(String title) => translate('deptMgmtEventLabel').replaceAll('{title}', title);
  String get deptMgmtNewTeam => translate('deptMgmtNewTeam');
  String get deptMgmtTeamNameLabel => translate('deptMgmtTeamNameLabel');
  String get deptMgmtParentTeam => translate('deptMgmtParentTeam');
  String get deptMgmtNoParent => translate('deptMgmtNoParent');
  String get deptMgmtLinkedEvent => translate('deptMgmtLinkedEvent');
  String get deptMgmtLoadingEvents => translate('deptMgmtLoadingEvents');
  String get deptMgmtNoEvent => translate('deptMgmtNoEvent');
  String get deptMgmtDateStart => translate('deptMgmtDateStart');
  String get deptMgmtDateEnd => translate('deptMgmtDateEnd');
  String get deptMgmtObjective => translate('deptMgmtObjective');
  String get deptMgmtTeamCreateError => translate('deptMgmtTeamCreateError');
  String get deptMgmtTasksTitle => translate('deptMgmtTasksTitle');
  String get deptMgmtStatInProgress => translate('deptMgmtStatInProgress');
  String get deptMgmtStatTodo => translate('deptMgmtStatTodo');
  String get deptMgmtStatOverdue => translate('deptMgmtStatOverdue');
  String get deptMgmtStatDone => translate('deptMgmtStatDone');
  String get deptMgmtTaskEmpty => translate('deptMgmtTaskEmpty');
  String get deptMgmtTaskLoadError => translate('deptMgmtTaskLoadError');
  String get deptMgmtNewTask => translate('deptMgmtNewTask');
  String get deptMgmtAssignedTo => translate('deptMgmtAssignedTo');
  String get deptMgmtNoAssignee => translate('deptMgmtNoAssignee');
  String get deptMgmtPriority => translate('deptMgmtPriority');
  String get deptMgmtPriorityLow => translate('deptMgmtPriorityLow');
  String get deptMgmtPriorityMedium => translate('deptMgmtPriorityMedium');
  String get deptMgmtPriorityHigh => translate('deptMgmtPriorityHigh');
  String get deptMgmtDeadline => translate('deptMgmtDeadline');
  String get deptMgmtTaskCreateError => translate('deptMgmtTaskCreateError');
  String get deptMgmtAssignmentsTitle => translate('deptMgmtAssignmentsTitle');
  String get deptMgmtAssignmentsEmpty => translate('deptMgmtAssignmentsEmpty');
  String get deptMgmtAssignMember => translate('deptMgmtAssignMember');
  String get deptMgmtRoleChef => translate('deptMgmtRoleChef');
  String get deptMgmtRoleAdjunct => translate('deptMgmtRoleAdjunct');
  String get deptMgmtAssignBtn => translate('deptMgmtAssignBtn');
  String get deptMgmtAssignError => translate('deptMgmtAssignError');
  String get deptMgmtEndAssignment => translate('deptMgmtEndAssignment');
  String get deptMgmtEventsTitle => translate('deptMgmtEventsTitle');
  String deptMgmtEventsUpcoming(int count) => translate('deptMgmtEventsUpcoming').replaceAll('{count}', '$count');
  String deptMgmtEventsPast(int count) => translate('deptMgmtEventsPast').replaceAll('{count}', '$count');
  String get deptMgmtEventsEmpty => translate('deptMgmtEventsEmpty');
  String get deptMgmtNewEvent => translate('deptMgmtNewEvent');
  String get deptMgmtTitleRequired => translate('deptMgmtTitleRequired');
  String get deptMgmtEventCreateError => translate('deptMgmtEventCreateError');
  String get deptMgmtActivityEmpty => translate('deptMgmtActivityEmpty');
  String deptMgmtSearchResults(int count, String query) => translate('deptMgmtSearchResults').replaceAll('{count}', '$count').replaceAll('{query}', query);
  String deptMgmtSearchNoResults(String query) => translate('deptMgmtSearchNoResults').replaceAll('{query}', query);
  String get deptMgmtSearchHintDetail => translate('deptMgmtSearchHintDetail');
  String get deptMgmtNoTeam => translate('deptMgmtNoTeam');
  String get deptMgmtUnassigned => translate('deptMgmtUnassigned');
  // ── Lot H10 : security + dicts + integrations + events + discipline ──
  String get secTitle => translate('secTitle');
  String get secSessionSection => translate('secSessionSection');
  String get secAutoLogout => translate('secAutoLogout');
  String secAfterInactivity(int min) => translate('secAfterInactivity').replaceAll('{min}', '$min');
  String get secSessionDuration => translate('secSessionDuration');
  String get secBiometricSection => translate('secBiometricSection');
  String get secBiometricLogin => translate('secBiometricLogin');
  String get secBiometricUnavailable => translate('secBiometricUnavailable');
  String get secBiometricError => translate('secBiometricError');
  String get secDataSaverSection => translate('secDataSaverSection');
  String get secDataSaverMode => translate('secDataSaverMode');
  String secNetworkLabel(String label) => translate('secNetworkLabel').replaceAll('{label}', label);
  String get secAutoMode => translate('secAutoMode');
  String get secAutoModeDesc => translate('secAutoModeDesc');
  String get secImgLoading => translate('secImgLoading');
  String get secCacheStrategy => translate('secCacheStrategy');
  String get secRefreshInterval => translate('secRefreshInterval');
  String get secOrientationSection => translate('secOrientationSection');
  String get secScreenshotSection => translate('secScreenshotSection');
  String get secScreenshotToggle => translate('secScreenshotToggle');
  String get secScreenshotDesc => translate('secScreenshotDesc');
  String get secAuditSection => translate('secAuditSection');
  String get secAuditEvents => translate('secAuditEvents');
  String secAuditCount(int count) => translate('secAuditCount').replaceAll('{count}', '$count');
  String get secAuditLogTitle => translate('secAuditLogTitle');
  String get secAuditClose => translate('secAuditClose');
  String get secAuditExportCsv => translate('secAuditExportCsv');
  String secAuditExported(int count) => translate('secAuditExported').replaceAll('{count}', '$count');
  String get secAuditClear => translate('secAuditClear');
  String get secAuditViewLog => translate('secAuditViewLog');
  String get secInfoPersisted => translate('secInfoPersisted');
  String get dictTitle => translate('dictTitle');
  String get dictEmpty => translate('dictEmpty');
  String dictEntriesCount(int count) => translate('dictEntriesCount').replaceAll('{count}', '$count');
  String get integTitle => translate('integTitle');
  String get integSmtp => translate('integSmtp');
  String get integStorage => translate('integStorage');
  String get integJwt => translate('integJwt');
  String get integRateLimiting => translate('integRateLimiting');
  String get integEnabled => translate('integEnabled');
  String get integDisabled => translate('integDisabled');
  String get integTestConn => translate('integTestConn');
  String get integTestResultOk => translate('integTestResultOk');
  String integTestResultFail(String msg) => translate('integTestResultFail').replaceAll('{msg}', msg);
  String get evtTitle => translate('evtTitle');
  String get evtStatTotal => translate('evtStatTotal');
  String get evtStatUpcoming => translate('evtStatUpcoming');
  String get evtStatDone => translate('evtStatDone');
  String get evtFilterAll => translate('evtFilterAll');
  String get evtFilterUpcoming => translate('evtFilterUpcoming');
  String get evtFilterOngoing => translate('evtFilterOngoing');
  String get evtEmpty => translate('evtEmpty');
  String get evtNew => translate('evtNew');
  String get evtTitleRequired => translate('evtTitleRequired');
  String get evtCreated => translate('evtCreated');
  String get evtCreateError => translate('evtCreateError');
  String evtInscrits(int count, int total) => translate('evtInscrits').replaceAll('{count}', '$count').replaceAll('{total}', '$total');
  String get discTitle => translate('discTitle');
  String get discStatTotal => translate('discStatTotal');
  String get discStatOngoing => translate('discStatOngoing');
  String get discStatResolved => translate('discStatResolved');
  String get discFilterAll => translate('discFilterAll');
  String get discFilterOngoing => translate('discFilterOngoing');
  String get discFilterResolved => translate('discFilterResolved');
  String get discCatAll => translate('discCatAll');
  String get discSearchHint => translate('discSearchHint');
  String get discResolveTitle => translate('discResolveTitle');
  String discResolveConfirm(String title) => translate('discResolveConfirm').replaceAll('{title}', title);
  String get discResolveAction => translate('discResolveAction');
  String get discResolveSuccess => translate('discResolveSuccess');
  String get discResolveError => translate('discResolveError');
  String get discDeleteTitle => translate('discDeleteTitle');
  String discDeleteConfirm(String title) => translate('discDeleteConfirm').replaceAll('{title}', title);
  String get discDeleteAction => translate('discDeleteAction');
  String get discDeleteSuccess => translate('discDeleteSuccess');
  String get discDeleteError => translate('discDeleteError');
  String get discCreateTitle => translate('discCreateTitle');
  String get discMemberLabel => translate('discMemberLabel');
  String get discSelectMember => translate('discSelectMember');
  String get discCatLabel => translate('discCatLabel');
  String get discTypeLabel => translate('discTypeLabel');
  String get discTitleLabel => translate('discTitleLabel');
  String get discDescLabel => translate('discDescLabel');
  String get discGravityLabel => translate('discGravityLabel');
  String get discDateLabel => translate('discDateLabel');
  String get discCreateError => translate('discCreateError');
  String get discCreateSuccess => translate('discCreateSuccess');
  String get discCreateValidation => translate('discCreateValidation');
  String get discResolved => translate('discResolved');
  String get discNewEvent => translate('discNewEvent');
  String discEmptySearch(String query) => translate('discEmptySearch').replaceAll('{query}', query);
  String get discEmpty => translate('discEmpty');
  String get discClearSearch => translate('discClearSearch');
  String get bibleReadingTitle => translate('bibleReadingTitle');
  String get myProgress => translate('myProgress');
  String get days => translate('days');
  String get consecutiveDays => translate('consecutiveDays');
  String get todaysReading => translate('todaysReading');
  String get markAsRead => translate('markAsRead');
  String get addNote => translate('addNote');
  String get availablePlans => translate('availablePlans');
  String get noPlans => translate('noPlans');
  String get familySharing => translate('familySharing');
  String get devPlanTitle => translate('devPlanTitle');
  String get globalProgress => translate('globalProgress');
  String get activeObjectives => translate('activeObjectives');
  String get completedObjectives => translate('completedObjectives');
  String get spiritualJournalTitle => translate('spiritualJournalTitle');
  String get reflection => translate('reflection');
  String get thanksgiving => translate('thanksgiving');
  String get praise => translate('praise');
  String get lesson => translate('lesson');
  String get newEntry => translate('newEntry');
  String get discipleshipPathTitle => translate('discipleshipPathTitle');
  String get familyCohesionTitle => translate('familyCohesionTitle');
  String get cohesionScore => translate('cohesionScore');
  String get goodEffort => translate('goodEffort');
  String get needsImprovement => translate('needsImprovement');
  String get networkFamilies => translate('networkFamilies');
  String get noFamilies => translate('noFamilies');
  String get familyResourcesTitle => translate('familyResourcesTitle');
  String get categories => translate('categories');
  String get recentResources => translate('recentResources');
  String get noResources => translate('noResources');
  String get makerTrackingTitle => translate('makerTrackingTitle');
  String get timeline => translate('timeline');
  String get noTimeline => translate('noTimeline');
  String get sermonTranslationTitle => translate('sermonTranslationTitle');
  String get translationInProgress => translate('translationInProgress');
  String get recentTranslations => translate('recentTranslations');
  String get noTranslations => translate('noTranslations');
  String get skillMatchingTitle => translate('skillMatchingTitle');
  String get launchAiMatching => translate('launchAiMatching');
  String get matchingSubtitle => translate('matchingSubtitle');
  String get proposals => translate('proposals');
  String get noMatches => translate('noMatches');
  String get skillsMatrixTitle => translate('skillsMatrixTitle');
  String get overview => translate('overview');
  String get skills => translate('skills');
  String get evaluatedMembers => translate('evaluatedMembers');
  String get gapsFound => translate('gapsFound');
  String get skillsByDepartment => translate('skillsByDepartment');
  String get needsMoreMembers => translate('needsMoreMembers');
  String get read => translate('read');
  String get progression => translate('progression');
  String get prayer => translate('prayer');
  String get statusDraft => translate('statusDraft');
  String get statusPendingValidation => translate('statusPendingValidation');
  String get statusApproval => translate('statusApproval');
  String get statusExecuted => translate('statusExecuted');
  String get statusArchive => translate('statusArchive');
  String get statusRequestedInfo => translate('statusRequestedInfo');

  // ── Lot H7 : navigation + profil ──
  String get navDashboard => translate('navDashboard');
  String get navSearch => translate('navSearch');
  String get navMap => translate('navMap');
  String get navSouls => translate('navSouls');
  String get navFamilies => translate('navFamilies');
  String get navCrmFaiseur => translate('navCrmFaiseur');
  String get navChefDashboard => translate('navChefDashboard');
  String get navRespDashboard => translate('navRespDashboard');
  String get navDepartments => translate('navDepartments');
  String get navReports => translate('navReports');
  String get navMakerReport => translate('navMakerReport');
  String get navFamilyReport => translate('navFamilyReport');
  String get navPrayers => translate('navPrayers');
  String get navGraceActions => translate('navGraceActions');
  String get navEvents => translate('navEvents');
  String get navEvangelism => translate('navEvangelism');
  String get navObjectives => translate('navObjectives');
  String get navVisits => translate('navVisits');
  String get navParallelFollowups => translate('navParallelFollowups');
  String get navTransfers => translate('navTransfers');
  String get navTransferWorkflow => translate('navTransferWorkflow');
  String get navEvaluations => translate('navEvaluations');
  String get navDiscipline => translate('navDiscipline');
  String get navAlerts => translate('navAlerts');
  String get navRequests => translate('navRequests');
  String get navDocuments => translate('navDocuments');
  String get navAppointments => translate('navAppointments');
  String get navMessaging => translate('navMessaging');
  String get navBadges => translate('navBadges');
  String get navQuest => translate('navQuest');
  String get navTithesOfferings => translate('navTithesOfferings');
  String get navTontines => translate('navTontines');
  String get navVoiceReports => translate('navVoiceReports');
  String get navVoiceAssistant => translate('navVoiceAssistant');
  String get navFaceCheckin => translate('navFaceCheckin');
  String get navTrainings => translate('navTrainings');
  String get navFinances => translate('navFinances');
  String get navCommunications => translate('navCommunications');
  String get navInventory => translate('navInventory');
  String get navNotifications => translate('navNotifications');
  String get navProfile => translate('navProfile');
  String get navShepherdsPilot => translate('navShepherdsPilot');
  String get navModules => translate('navModules');
  String get navMenus => translate('navMenus');
  String get navCustomPages => translate('navCustomPages');
  String get navChurchSettings => translate('navChurchSettings');
  String get navCustomFields => translate('navCustomFields');
  String get navDictionaries => translate('navDictionaries');
  String get navIntegrations => translate('navIntegrations');
  String get navChurches => translate('navChurches');
  String get navSecurity => translate('navSecurity');
  String get navUsers => translate('navUsers');
  String get navPermissions => translate('navPermissions');
  String get navAudit => translate('navAudit');
  String get navCompliance => translate('navCompliance');
  String get navWhatsApp => translate('navWhatsApp');
  String get navModeration => translate('navModeration');
  String get navDataMigration => translate('navDataMigration');
  String get navUsageAnalytics => translate('navUsageAnalytics');
  String get navEncouragements => translate('navEncouragements');
  String get navChurchBenchmark => translate('navChurchBenchmark');
  String get navSabbath => translate('navSabbath');
  String get navRewards => translate('navRewards');
  String get navWeeklyChallenges => translate('navWeeklyChallenges');
  String get navGrowthProjection => translate('navGrowthProjection');
  String get navLoadPrediction => translate('navLoadPrediction');
  String get navNeighborhoodHealth => translate('navNeighborhoodHealth');
  String get navFollowUpRequests => translate('navFollowUpRequests');
  String get navDiscipleshipPath => translate('navDiscipleshipPath');
  String get navAdminSection => translate('navAdminSection');
  String get navChangeRole => translate('navChangeRole');
  String get navActiveLabel => translate('navActiveLabel');
  String get roleSwitchFailed => translate('roleSwitchFailed');
  String get feedbackTitle => translate('feedbackTitle');
  String get feedbackSubtitle => translate('feedbackSubtitle');
  String get profileTitle => translate('profileTitle');
  String get profileScore => translate('profileScore');
  String get profilePresence => translate('profilePresence');
  String get profileProgression => translate('profileProgression');
  String get profilePersonalInfo => translate('profilePersonalInfo');
  String get profileSpiritualInfo => translate('profileSpiritualInfo');
  String get profileQuickActions => translate('profileQuickActions');
  String get profileLogout => translate('profileLogout');
  String get profileRole => translate('profileRole');
  String get profilePhone => translate('profilePhone');
  String get profileEmail => translate('profileEmail');
  String get profileRegisteredOn => translate('profileRegisteredOn');
  String get profileSpiritualScore => translate('profileSpiritualScore');
  String get profileFamily => translate('profileFamily');
  String get profileDepartment => translate('profileDepartment');

  // ==================== NAVIGATION ====================
  String get navMessages => translate('navMessages');
  String get navSettings => translate('navSettings');
  String get navPresence => translate('navPresence');
  String get navAdmin => translate('navAdmin');

  // ==================== DASHBOARD ====================
  String get dashboardTitle => translate('dashboardTitle');
  String get totalMembers => translate('totalMembers');
  String get activeMembers => translate('activeMembers');
  String get presentToday => translate('presentToday');
  String get absentToday => translate('absentToday');
  String get pendingReports => translate('pendingReports');
  String get activeAlerts => translate('activeAlerts');
  String get weeklyAttendance => translate('weeklyAttendance');

  // ==================== SOULS ====================
  String get soulsTitle => translate('soulsTitle');
  String get soulDetail => translate('soulDetail');
  String get addSoul => translate('addSoul');
  String get firstName => translate('firstName');
  String get lastName => translate('lastName');
  String get phone => translate('phone');
  String get typeDisciple => translate('typeDisciple');
  String get status => translate('status');
  String get dateIntegration => translate('dateIntegration');
  String get lastContact => translate('lastContact');
  String get spiritualScore => translate('spiritualScore');

  // ==================== PRESENCE ====================
  String get presenceTitle => translate('presenceTitle');
  String get markPresent => translate('markPresent');
  String get markAbsent => translate('markAbsent');
  String get allPresent => translate('allPresent');
  String get allAbsent => translate('allAbsent');
  String get resetPresence => translate('resetPresence');
  String get submitPresence => translate('submitPresence');
  String get presenceNote => translate('presenceNote');
  String get presenceProgress => translate('presenceProgress');

  // ==================== REPORTS ====================
  String get reportsTitle => translate('reportsTitle');
  String get submitReport => translate('submitReport');
  String get viewReport => translate('viewReport');
  String get exportPdf => translate('exportPdf');
  String get weekReport => translate('weekReport');
  String get difficulties => translate('difficulties');
  String get notes => translate('notes');
  String get outings => translate('outings');
  String get maintained => translate('maintained');

  // ==================== DISCIPLINE ====================
  String get disciplineTitle => translate('disciplineTitle');
  String get addEvent => translate('addEvent');
  String get resolveEvent => translate('resolveEvent');
  String get category => translate('category');
  String get severity => translate('severity');
  String get eventTitle => translate('eventTitle');
  String get description => translate('description');
  String get resolved => translate('resolved');
  String get inProgress => translate('inProgress');

  // ==================== PRAYERS ====================
  String get prayersTitle => translate('prayersTitle');
  String get actionsDeGrace => translate('actionsDeGrace');
  String get addPrayer => translate('addPrayer');
  String get prayerAnswered => translate('prayerAnswered');

  // ==================== BADGES ====================
  String get badgesTitle => translate('badgesTitle');
  String get myBadges => translate('myBadges');
  String get leaderboard => translate('leaderboard');
  String get level => translate('level');
  String get points => translate('points');
  String get earnedDate => translate('earnedDate');

  // ==================== MESSAGES ====================
  String get messagesTitle => translate('messagesTitle');
  String get newMessage => translate('newMessage');
  String get typeMessage => translate('typeMessage');
  String get sendMessage => translate('sendMessage');
  String get startConversation => translate('startConversation');
  String get noMessages => translate('noMessages');

  // ==================== EVENTS ====================
  String get eventsTitle => translate('eventsTitle');
  String get addEventTitle => translate('addEventTitle');
  String get eventDate => translate('eventDate');
  String get eventTime => translate('eventTime');
  String get participants => translate('participants');
  String get rsvp => translate('rsvp');

  // ==================== ADMIN ====================
  String get adminSettings => translate('adminSettings');
  String get churchName => translate('churchName');
  String get customFields => translate('customFields');
  String get dictionaries => translate('dictionaries');
  String get integrations => translate('integrations');
  String get tenants => translate('tenants');
  String get platformPages => translate('platformPages');

  // ==================== EVANGELISM ====================
  String get evangelismTitle => translate('evangelismTitle');
  String get pipeline => translate('pipeline');
  String get newContact => translate('newContact');
  String get followUp => translate('followUp');
  String get convert => translate('convert');

  // ==================== TRAINING ====================
  String get trainingsTitle => translate('trainingsTitle');
  String get addTraining => translate('addTraining');
  String get participants2 => translate('participants2');
  String get completion => translate('completion');

  // ==================== FINANCES ====================
  String get financesTitle => translate('financesTitle');
  String get income => translate('income');
  String get expenses => translate('expenses');
  String get balance => translate('balance');
  String get budget => translate('budget');

  // ==================== SMART ALERTS ====================
  String get smartAlertsTitle => translate('smartAlertsTitle');
  String get runAnalysis => translate('runAnalysis');
  String get analysisRunning => translate('analysisRunning');
  String get noActiveAlerts => translate('noActiveAlerts');
  String get everythingOk => translate('everythingOk');

  // ==================== PUSH / SYNC ====================
  String get syncPending => translate('syncPending');
  String get syncing => translate('syncing');
  String get syncNow => translate('syncNow');
  String get itemsPending => translate('itemsPending');

  // ==================== QR CODE ====================
  String get scanQrCode => translate('scanQrCode');
  String get showQrCode => translate('showQrCode');
  String get qrScanSuccess => translate('qrScanSuccess');
  String get qrScanError => translate('qrScanError');

  // ==================== MODULES CONNECTÉS (écrans branchés API) ====================
  String get weeklyChallengesTitle => translate('weeklyChallengesTitle');
  String get weeklyChallengesError => translate('weeklyChallengesError');
  String get weeklyChallengesEmpty => translate('weeklyChallengesEmpty');
  String get reverseMentoringTitle => translate('reverseMentoringTitle');
  String get reverseMentoringError => translate('reverseMentoringError');
  String get reverseMentoringEmpty => translate('reverseMentoringEmpty');
  String get volunteersTitle => translate('volunteersTitle');
  String get volunteersError => translate('volunteersError');
  String get volunteersEmpty => translate('volunteersEmpty');
  String get aiVisitNotesTitle => translate('aiVisitNotesTitle');
  String get aiVisitNotesError => translate('aiVisitNotesError');
  String get aiVisitNotesEmpty => translate('aiVisitNotesEmpty');
  String get engagementAnalyticsTitle => translate('engagementAnalyticsTitle');
  String get engagementAnalyticsError => translate('engagementAnalyticsError');
  String get engagementAnalyticsEmpty => translate('engagementAnalyticsEmpty');
  String get intelligenceCenterTitle => translate('intelligenceCenterTitle');
  String get intelligenceCenterError => translate('intelligenceCenterError');
  String intelligenceCenterAlerts(int count) =>
      translate('intelligenceCenterAlerts').replaceAll('{count}', '$count');
  String get predictionsTitle => translate('predictionsTitle');
  String get predictionsError => translate('predictionsError');
  String get predictionsEmpty => translate('predictionsEmpty');
  String get successionTitle => translate('successionTitle');
  String get successionError => translate('successionError');
  String get successionEmpty => translate('successionEmpty');
  String get spiritualChallengesTitle => translate('spiritualChallengesTitle');
  String get spiritualChallengesError => translate('spiritualChallengesError');
  String get spiritualChallengesEmpty => translate('spiritualChallengesEmpty');
  String get personalObjectivesTitle => translate('personalObjectivesTitle');
  String get personalObjectivesError => translate('personalObjectivesError');
  String get personalObjectivesEmpty => translate('personalObjectivesEmpty');
  String get kpiNarrativeTitle => translate('kpiNarrativeTitle');
  String get kpiNarrativeError => translate('kpiNarrativeError');
  String get kpiNarrativeEmpty => translate('kpiNarrativeEmpty');
  String get rewardsTitle => translate('rewardsTitle');
  String get rewardsError => translate('rewardsError');
  String get rewardsEmpty => translate('rewardsEmpty');
  String get aiMentoringTitle => translate('aiMentoringTitle');
  String get aiMentoringError => translate('aiMentoringError');
  String get aiMentoringEmpty => translate('aiMentoringEmpty');
  String get familyMeetingTitle => translate('familyMeetingTitle');
  String get familyMeetingError => translate('familyMeetingError');
  String get familyMeetingEmpty => translate('familyMeetingEmpty');
  String get eventChecklistTitle => translate('eventChecklistTitle');
  String get eventChecklistError => translate('eventChecklistError');
  String get eventChecklistEmpty => translate('eventChecklistEmpty');
  String eventChecklistProgress(int done, int total) => translate('eventChecklistProgress')
      .replaceAll('{done}', '$done').replaceAll('{total}', '$total');
  String get churchComparisonTitle => translate('churchComparisonTitle');
  String get churchComparisonError => translate('churchComparisonError');
  String get churchComparisonEmpty => translate('churchComparisonEmpty');
  String get adminRequestsTitle => translate('adminRequestsTitle');
  String get adminRequestsError => translate('adminRequestsError');
  String get adminRequestsEmpty => translate('adminRequestsEmpty');
  String get demoDataBanner => translate('demoDataBanner');

  // ==================== MODULES CONNECTÉS — LOT 2 ====================
  String get streamingTitle => translate('streamingTitle');
  String get streamingError => translate('streamingError');
  String get liveBadge => translate('liveBadge');
  String get statLive => translate('statLive');
  String get statViewers => translate('statViewers');
  String get statStreams => translate('statStreams');
  String get upcomingStreams => translate('upcomingStreams');
  String get streamsEmpty => translate('streamsEmpty');
  String scheduledAt(String date) => translate('scheduledAt').replaceAll('{date}', date);
  String get endedLabel => translate('endedLabel');
  String get inventoryTitle => translate('inventoryTitle');
  String get inventoryError => translate('inventoryError');
  String get inventoryEmpty => translate('inventoryEmpty');
  String lowStockBanner(int count) => translate('lowStockBanner').replaceAll('{count}', '$count');
  String get searchItemHint => translate('searchItemHint');
  String unitsCount(int count) => translate('unitsCount').replaceAll('{count}', '$count');
  String get lowStockTag => translate('lowStockTag');
  String get marketplaceTitle => translate('marketplaceTitle');
  String get marketplaceError => translate('marketplaceError');
  String get marketplaceEmpty => translate('marketplaceEmpty');
  String get filterOffers => translate('filterOffers');
  String get filterRequests => translate('filterRequests');
  String get filterServices => translate('filterServices');
  String get filterFree => translate('filterFree');
  String get contact => translate('contact');
  String get sellerLabel => translate('sellerLabel');
  String sellerWithId(String id) => translate('sellerWithId').replaceAll('{id}', id);
  String get moderationTitle => translate('moderationTitle');
  String get moderationError => translate('moderationError');
  String get moderationEmpty => translate('moderationEmpty');
  String get approve => translate('approve');
  String get reject => translate('reject');
  String reviewDone(String decision) => translate('reviewDone').replaceAll('{decision}', decision);
  String get reviewError => translate('reviewError');
  String get predictionsMlTitle => translate('predictionsMlTitle');
  String get predictionsMlEmpty => translate('predictionsMlEmpty');
  String predictedValue(num value) => translate('predictedValue').replaceAll('{value}', value.toStringAsFixed(0));
  String get executiveInsightsTitle => translate('executiveInsightsTitle');
  String get executiveInsightsError => translate('executiveInsightsError');
  String get executiveInsightsEmpty => translate('executiveInsightsEmpty');
  String get prayerJournalTitle => translate('prayerJournalTitle');
  String get prayerJournalEmpty => translate('prayerJournalEmpty');
  String get statTotal => translate('statTotal');
  String get statOngoing => translate('statOngoing');
  String get statAnswered => translate('statAnswered');
  String get newPrayer => translate('newPrayer');
  String get yourPrayer => translate('yourPrayer');
  String get categoryLabel => translate('categoryLabel');
  String get catPrayer => translate('catPrayer');
  String get catPraise => translate('catPraise');
  String get catIntercession => translate('catIntercession');
  String get catGrace => translate('catGrace');
  String get digitalTwinTitle => translate('digitalTwinTitle');
  String get quickScenarios => translate('quickScenarios');
  String get scenarioStagnation => translate('scenarioStagnation');
  String get scenarioSoftGrowth => translate('scenarioSoftGrowth');
  String get scenarioMakersAwakening => translate('scenarioMakersAwakening');
  String get scenarioAwakeningRetention => translate('scenarioAwakeningRetention');
  String get scenarioSpiritualAwakening => translate('scenarioSpiritualAwakening');
  String get parameters => translate('parameters');
  String get makerMultiplier => translate('makerMultiplier');
  String get retentionGain => translate('retentionGain');
  String get pipelineBoost => translate('pipelineBoost');
  String get horizon => translate('horizon');
  String get projectedStat => translate('projectedStat');
  String get soulsUnit => translate('soulsUnit');
  String get growthStat => translate('growthStat');
  String get leadersNeeded => translate('leadersNeeded');
  String get leadersSufficient => translate('leadersSufficient');
  String leadersMissing(int gap) => translate('leadersMissing').replaceAll('{gap}', '$gap');
  String get monthlyProjection => translate('monthlyProjection');
  String monthLabel(int n) => translate('monthLabel').replaceAll('{n}', '$n');

  // ==================== MODULES CONNECTÉS — LOT 3 (P3) ====================
  String get dataMigrationTitle => translate('dataMigrationTitle');
  String get importData => translate('importData');
  String get importDataHint => translate('importDataHint');
  String get selectFile => translate('selectFile');
  String get aiAnalysis => translate('aiAnalysis');
  String get detectedFile => translate('detectedFile');
  String get detectedRows => translate('detectedRows');
  String get mappedColumns => translate('mappedColumns');
  String get mappingConfidence => translate('mappingConfidence');
  // ── Lot H8 : communications + compliance ──
  String get commTitle => translate('commTitle');
  String get commManagement => translate('commManagement');
  String get commEmpty => translate('commEmpty');
  String get commPublishedEmpty => translate('commPublishedEmpty');
  String get commNew => translate('commNew');
  String get commEdit => translate('commEdit');
  String get commPublished => translate('commPublished');
  String commPublishSuccess(String count) => translate('commPublishSuccess').replaceAll('{count}', count);
  String get commPublishError => translate('commPublishError');
  String get commDeleteTitle => translate('commDeleteTitle');
  String get commDeleted => translate('commDeleted');
  String get commDeleteError => translate('commDeleteError');
  String get commCreated => translate('commCreated');
  String get commModified => translate('commModified');
  String get commSaveError => translate('commSaveError');
  String get commStatusDraft => translate('commStatusDraft');
  String get commStatusPublished => translate('commStatusPublished');
  String get commStatusArchived => translate('commStatusArchived');
  String get commCibleAll => translate('commCibleAll');
  String get commCibleRole => translate('commCibleRole');
  String get commCibleFamily => translate('commCibleFamily');
  String get commCibleDept => translate('commCibleDept');
  String get commBtnCreate => translate('commBtnCreate');
  String get commBtnSave => translate('commBtnSave');
  String get commInputTitle => translate('commInputTitle');
  String get commInputContent => translate('commInputContent');
  String get commInputCible => translate('commInputCible');
  String get commInputFamily => translate('commInputFamily');
  String get commInputDept => translate('commInputDept');
  String get complianceTitle => translate('complianceTitle');
  String get complianceVerifyAudit => translate('complianceVerifyAudit');
  String get complianceTabOverview => translate('complianceTabOverview');
  String get complianceTabRetention => translate('complianceTabRetention');
  String get complianceTabAudit => translate('complianceTabAudit');
  String get complianceTabPortability => translate('complianceTabPortability');
  String get complianceStatPolicies => translate('complianceStatPolicies');
  String get complianceStatConsents => translate('complianceStatConsents');
  String get complianceStatAuditEntries => translate('complianceStatAuditEntries');
  String get complianceStatGdprRequests => translate('complianceStatGdprRequests');
  String get complianceAuditIntegrity => translate('complianceAuditIntegrity');
  String get complianceAuditValid => translate('complianceAuditValid');
  String get complianceAuditInvalid => translate('complianceAuditInvalid');
  String complianceVerifySuccess(String count) => translate('complianceVerifySuccess').replaceAll('{count}', count);
  String complianceVerifyBroken(String count) => translate('complianceVerifyBroken').replaceAll('{count}', count);
  String get complianceVerifyImpossible => translate('complianceVerifyImpossible');
  String complianceExportSuccess(String count) => translate('complianceExportSuccess').replaceAll('{count}', count);
  String get complianceExportError => translate('complianceExportError');
  String get complianceChecklist => translate('complianceChecklist');
  String get complianceCheckPolicy => translate('complianceCheckPolicy');
  String get complianceCheckConsents => translate('complianceCheckConsents');
  String get complianceCheckAudit => translate('complianceCheckAudit');
  String get complianceCheckPortability => translate('complianceCheckPortability');
  String get complianceCheckRightToForget => translate('complianceCheckRightToForget');
  String get complianceCheckEncryption => translate('complianceCheckEncryption');
  String get compliancePurgeTitle => translate('compliancePurgeTitle');
  String get compliancePurgeContent => translate('compliancePurgeContent');
  String get compliancePurgeAction => translate('compliancePurgeAction');
  String get compliancePurgeSuccess => translate('compliancePurgeSuccess');
  String get complianceRetentionTitle => translate('complianceRetentionTitle');
  String get complianceRetentionEmpty => translate('complianceRetentionEmpty');
  String get complianceRetentionDurations => translate('complianceRetentionDurations');
  String get complianceActionAnonymize => translate('complianceActionAnonymize');
  String get complianceActionArchive => translate('complianceActionArchive');
  String get complianceAuditEmpty => translate('complianceAuditEmpty');
  String get complianceExportTitle => translate('complianceExportTitle');
  String get complianceExportSubtitle => translate('complianceExportSubtitle');
  String get complianceExportContent => translate('complianceExportContent');
  String get complianceExportProfile => translate('complianceExportProfile');
  String get complianceExportSouls => translate('complianceExportSouls');
  String get complianceExportConsents => translate('complianceExportConsents');
  String get complianceExportGdpr => translate('complianceExportGdpr');
  String get complianceExportMeta => translate('complianceExportMeta');
  String get complianceExportBtn => translate('complianceExportBtn');
  String get launchMigration => translate('launchMigration');
  String get previousMigrations => translate('previousMigrations');
  String get noMigrations => translate('noMigrations');
  String get rerun => translate('rerun');
  String get migrationLaunched => translate('migrationLaunched');
  String get migrationError => translate('migrationError');
  String rowsCount(int count) => translate('rowsCount').replaceAll('{count}', '$count');
  String get surveysTitle => translate('surveysTitle');
  String get surveysEmpty => translate('surveysEmpty');
  String get encouragementsTitle => translate('encouragementsTitle');
  String tabReceived(int count) => translate('tabReceived').replaceAll('{count}', '$count');
  String tabSent(int count) => translate('tabSent').replaceAll('{count}', '$count');
  String tabTeam(int count) => translate('tabTeam').replaceAll('{count}', '$count');
  String get emptyReceivedEnc => translate('emptyReceivedEnc');
  String get emptySentEnc => translate('emptySentEnc');
  String get emptyTeam => translate('emptyTeam');
  String get composeEncouragement => translate('composeEncouragement');
  String get encTypePrayer => translate('encTypePrayer');
  String get encTypePraise => translate('encTypePraise');
  String get encTypeThanks => translate('encTypeThanks');
  String get encTypeSupport => translate('encTypeSupport');
  String get encTypeWelcome => translate('encTypeWelcome');
  String get encTypeScripture => translate('encTypeScripture');
  String get writeEncouragementHint => translate('writeEncouragementHint');
  String get encouragementSent => translate('encouragementSent');
  String get send => translate('send');
  String encouragementsReceived(int count) => translate('encouragementsReceived').replaceAll('{count}', '$count');
  String get followUpTitle => translate('followUpTitle');
  String get fuTypeMaker => translate('fuTypeMaker');
  String get fuTypeSpiritual => translate('fuTypeSpiritual');
  String get fuTypePastoral => translate('fuTypePastoral');
  String get newFollowUpRequest => translate('newFollowUpRequest');
  String get describeNeed => translate('describeNeed');
  String get requestSent => translate('requestSent');
  String get requestFailed => translate('requestFailed');
  String myRequests(int count) => translate('myRequests').replaceAll('{count}', '$count');
  String assignedToMe(int count) => translate('assignedToMe').replaceAll('{count}', '$count');
  String get emptyMyRequests => translate('emptyMyRequests');
  String get emptyAssignedRequests => translate('emptyAssignedRequests');
  String get markComplete => translate('markComplete');
  String get askAction => translate('askAction');
  String get neighborhoodHealthTitle => translate('neighborhoodHealthTitle');
  String get neighborhoodEmpty => translate('neighborhoodEmpty');
  String soulsInZone(int count) => translate('soulsInZone').replaceAll('{count}', '$count');
  String scoreLabel(int score) => translate('scoreLabel').replaceAll('{score}', '$score');
  String recentContacts(int count) => translate('recentContacts').replaceAll('{count}', '$count');
  String get sabbathTitle => translate('sabbathTitle');
  String get globalMaturity => translate('globalMaturity');
  String get activeSouls => translate('activeSouls');
  String get activeMakers => translate('activeMakers');
  String get familiesAtRisk => translate('familiesAtRisk');
  String get twelveAxes => translate('twelveAxes');

  // ==================== ONBOARDING / LANDING ====================
  String get onboardingSkip => translate('onboardingSkip');
  String get onboardingNext => translate('onboardingNext');
  String get onboardingStart => translate('onboardingStart');
  String get onboardingWelcomeTitle => translate('onboardingWelcomeTitle');
  String get onboardingWelcomeDesc => translate('onboardingWelcomeDesc');
  String get onboardingTrackingTitle => translate('onboardingTrackingTitle');
  String get onboardingTrackingDesc => translate('onboardingTrackingDesc');
  String get onboardingOfflineTitle => translate('onboardingOfflineTitle');
  String get onboardingOfflineDesc => translate('onboardingOfflineDesc');
  String get onboardingNotificationsTitle => translate('onboardingNotificationsTitle');
  String get onboardingNotificationsDesc => translate('onboardingNotificationsDesc');
  String get onboardingSecurityTitle => translate('onboardingSecurityTitle');
  String get onboardingSecurityDesc => translate('onboardingSecurityDesc');
  String get onboardingAR => translate('onboardingAR');

  // ==================== FRENCH ====================
  static const Map<String, String> _french = {
    'appTitle': 'Discipolat',
    'login': 'Connexion',
    'logout': 'Déconnexion',
    'email': 'Adresse e-mail',
    'password': 'Mot de passe',
    'signIn': 'Se connecter',
    'signOut': 'Se déconnecter',
    'save': 'Enregistrer',
    'cancel': 'Annuler',
    'delete': 'Supprimer',
    'edit': 'Modifier',
    'add': 'Ajouter',
    'search': 'Rechercher',
    'loading': 'Chargement...',
    'error': 'Erreur',
    'success': 'Succès',
    'noData': 'Aucune donnée',
    'confirm': 'Confirmer',
    'back': 'Retour',
    'close': 'Fermer',
    'yes': 'Oui',
    'no': 'Non',
    'none': 'Aucun',
    'all': 'Tous',
    'filter': 'Filtrer',
    'refresh': 'Rafraîchir',
    'retry': 'Réessayer',
    'offline': 'Hors ligne',
    'navDashboard': 'Tableau de bord',
    'navSouls': 'Âmes',
    'navFamilies': 'Familles',
    'navDepartments': 'Départements',
    'navReports': 'Rapports',
    'navPrayers': 'Prières',
    'navEvents': 'Événements',
    'navAlerts': 'Alertes',
    'navMessages': 'Messages',
    'navNotifications': 'Notifications',
    'navProfile': 'Profil',
    'navSettings': 'Paramètres',
    'navUsers': 'Utilisateurs',
    'navBadges': 'Badges',
    'navEvangelism': 'Évangélisation',
    'navDiscipline': 'Discipline',
    'navPresence': 'Présences',
    'navTrainings': 'Formations',
    'navFinances': 'Finances',
    'navCommunications': 'Annonces',
    'navMap': 'Cartographie',
    'navSearch': 'Recherche',
    'navAdmin': 'Administration',
    'dashboardTitle': 'Tableau de bord',
    'totalMembers': 'Total membres',
    'activeMembers': 'Membres actifs',
    'presentToday': 'Présents aujourd\'hui',
    'absentToday': 'Absents aujourd\'hui',
    'pendingReports': 'Rapports en attente',
    'activeAlerts': 'Alertes actives',
    'weeklyAttendance': 'Fréquentation hebdomadaire',
    'soulsTitle': 'Liste des âmes',
    'soulDetail': 'Fiche âme',
    'addSoul': 'Ajouter une âme',
    'firstName': 'Prénom',
    'lastName': 'Nom',
    'phone': 'Téléphone',
    'typeDisciple': 'Type de disciple',
    'status': 'Statut',
    'dateIntegration': 'Date d\'intégration',
    'lastContact': 'Dernier contact',
    'spiritualScore': 'Score spirituel',
    'presenceTitle': 'Saisie des présences',
    'markPresent': 'Marquer présent',
    'markAbsent': 'Marquer absent',
    'allPresent': 'Tous présents',
    'allAbsent': 'Tous absents',
    'resetPresence': 'Réinitialiser',
    'submitPresence': 'Enregistrer les présences',
    'presenceNote': 'Note',
    'presenceProgress': 'Progression',
    'reportsTitle': 'Rapports',
    'submitReport': 'Soumettre le rapport',
    'viewReport': 'Voir le rapport',
    'exportPdf': 'Exporter en PDF',
    'weekReport': 'Rapport hebdomadaire',
    'difficulties': 'Difficultés',
    'notes': 'Notes',
    'outings': 'Sorties',
    'maintained': 'Maintenus',
    'disciplineTitle': 'Discipline',
    'addEvent': 'Ajouter un événement',
    'resolveEvent': 'Résoudre',
    'category': 'Catégorie',
    'severity': 'Gravité',
    'eventTitle': 'Titre',
    'description': 'Description',
    'resolved': 'Résolu',
    'inProgress': 'En cours',
    'prayersTitle': 'Prières',
    'actionsDeGrace': 'Actions de grâce',
    'addPrayer': 'Ajouter une prière',
    'prayerAnswered': 'Prière exaucée',
    'badgesTitle': 'Badges',
    'myBadges': 'Mes badges',
    'leaderboard': 'Classement',
    'level': 'Niveau',
    'points': 'Points',
    'earnedDate': 'Date d\'obtention',
    'messagesTitle': 'Messages',
    'newMessage': 'Nouveau message',
    'typeMessage': 'Tapez votre message...',
    'sendMessage': 'Envoyer',
    'startConversation': 'Démarrer une conversation',
    'noMessages': 'Aucun message',
    'eventsTitle': 'Événements',
    'addEventTitle': 'Nouvel événement',
    'eventDate': 'Date',
    'eventTime': 'Heure',
    'participants': 'Participants',
    'rsvp': 'Confirmer présence',
    'adminSettings': 'Paramètres admin',
    'churchName': 'Nom de l\'église',
    'customFields': 'Champs personnalisés',
    'dictionaries': 'Dictionnaires',
    'integrations': 'Intégrations',
    'tenants': 'Organisations',
    'platformPages': 'Pages plateforme',
    'evangelismTitle': 'Évangélisation',
    'pipeline': 'Pipeline',
    'newContact': 'Nouveau contact',
    'followUp': 'Suivi',
    'convert': 'Conversion',
    'trainingsTitle': 'Formations',
    'addTraining': 'Nouvelle formation',
    'participants2': 'Participants',
    'completion': 'Achèvement',
    'financesTitle': 'Finances',
    'income': 'Recettes',
    'expenses': 'Dépenses',
    'balance': 'Solde',
    'budget': 'Budget',
    'smartAlertsTitle': 'Alertes intelligentes',
    'runAnalysis': 'Lancer l\'analyse',
    'analysisRunning': 'Analyse en cours...',
    'noActiveAlerts': 'Aucune alerte active',
    'everythingOk': 'Tout est en ordre !',
    'syncPending': 'Synchronisation en attente',
    'syncing': 'Synchronisation...',
    'syncNow': 'Synchroniser',
    'itemsPending': 'éléments en attente',
    'scanQrCode': 'Scanner un QR Code',
    'showQrCode': 'Afficher mon QR Code',
    'qrScanSuccess': 'QR Code scanné avec succès',
    'qrScanError': 'Erreur lors du scan du QR Code',
    'sessionExpiringWarning': 'Session expire dans 2 minutes. Touchez pour continuer.',
    'syncingCount': 'Synchronisation… {count} éléments en attente',
    'offlineCount': 'Hors ligne — {count} éléments en attente',
    'pageNotFound': 'Page introuvable',
    'pageNotFoundWithPath': 'La page "{path}" n\'existe pas.',
    'pageNotFoundGeneric': 'La page que vous cherchez n\'existe pas ou a été déplacée.',
    'availablePages': 'PAGES DISPONIBLES',
    'navHome': 'Accueil',
    'navMore': 'Plus',
    'quickLinkHome': 'Accueil',
    'quickLinkSouls': 'Âmes',
    'quickLinkFamilies': 'Familles',
    'quickLinkEvents': 'Événements',
    'quickLinkReports': 'Rapports',
    'quickLinkDocuments': 'Documents',
    'appTagline': 'Gestion du Discipolat',
    'weeklyChallengesTitle': '🏆 Défis hebdomadaires',
    'weeklyChallengesError': 'Impossible de charger les défis.',
    'weeklyChallengesEmpty': 'Aucun défi actif pour le moment.',
    'reverseMentoringTitle': '🔄 Mentorat inversé',
    'reverseMentoringError': 'Impossible de charger les demandes.',
    'reverseMentoringEmpty': 'Aucune demande de mentorat inversé.',
    'volunteersTitle': '🤝 Bénévoles',
    'volunteersError': 'Impossible de charger les bénévoles.',
    'volunteersEmpty': 'Aucun bénévole enregistré.',
    'aiVisitNotesTitle': '📝 Notes IA visites',
    'aiVisitNotesError': 'Impossible de charger les notes IA.',
    'aiVisitNotesEmpty': 'Aucune note de visite analysée.',
    'engagementAnalyticsTitle': '📈 Engagement',
    'engagementAnalyticsError': 'Impossible de charger les métriques.',
    'engagementAnalyticsEmpty': 'Aucune métrique enregistrée.',
    'intelligenceCenterTitle': "🏛️ Centre d'intelligence",
    'intelligenceCenterError': 'Impossible de charger les KPIs. Le centre doit être initialisé côté admin.',
    'intelligenceCenterAlerts': '{count} alerte(s) active(s)',
    'predictionsTitle': '🔮 Prédictions IA',
    'predictionsError': 'Impossible de charger les prédictions.',
    'predictionsEmpty': 'Aucune prédiction générée.',
    'successionTitle': '👑 Succession',
    'successionError': 'Impossible de charger les plans de succession.',
    'successionEmpty': 'Aucun plan de succession.',
    'spiritualChallengesTitle': '🔥 Défis spirituels',
    'spiritualChallengesError': 'Impossible de charger les défis spirituels.',
    'spiritualChallengesEmpty': 'Aucun défi en cours.',
    'personalObjectivesTitle': '🎯 Objectifs personnels',
    'personalObjectivesError': 'Impossible de charger vos objectifs.',
    'personalObjectivesEmpty': 'Aucun objectif défini.',
    'kpiNarrativeTitle': '📖 Narration des KPIs',
    'kpiNarrativeError': 'Impossible de charger les narrations KPI.',
    'kpiNarrativeEmpty': 'Aucune narration générée. Utilisez « Générer » côté web.',
    'rewardsTitle': '🏅 Mes récompenses',
    'rewardsError': 'Impossible de charger vos récompenses.',
    'rewardsEmpty': 'Aucun certificat pour le moment. Continuez vos efforts !',
    'aiMentoringTitle': '🧠 Mentorat IA',
    'aiMentoringError': 'Impossible de charger les suggestions.',
    'aiMentoringEmpty': 'Aucune suggestion. Générez-en depuis le web.',
    'familyMeetingTitle': '👨‍👩‍👧 Réunions famille',
    'familyMeetingError': 'Impossible de charger les réunions.',
    'familyMeetingEmpty': 'Aucune réunion programmée.',
    'eventChecklistTitle': '✅ Checklists',
    'eventChecklistError': 'Impossible de charger les checklists.',
    'eventChecklistEmpty': 'Aucune tâche de checklist.',
    'eventChecklistProgress': '{done} / {total} tâches terminées',
    'churchComparisonTitle': '⚖️ Comparaison',
    'churchComparisonError': 'Impossible de charger les comparaisons.',
    'churchComparisonEmpty': 'Aucune comparaison enregistrée.',
    'adminRequestsTitle': '📋 Demandes admin',
    'adminRequestsError': 'Impossible de charger les demandes.',
    'adminRequestsEmpty': 'Aucune demande.',
    'demoDataBanner': 'Aperçu — données de démonstration, pas encore connectées à votre église.',
    'streamingTitle': 'Streaming & Live',
    'streamingError': 'Impossible de charger les streams.',
    'liveBadge': 'EN DIRECT',
    'statLive': 'En direct',
    'statViewers': 'Spectateurs',
    'statStreams': 'Streams',
    'upcomingStreams': 'Prochains streams',
    'streamsEmpty': 'Aucun stream planifié pour le moment.',
    'scheduledAt': 'Planifié — {date}',
    'endedLabel': 'Terminé',
    'inventoryTitle': 'Inventaire',
    'inventoryError': "Impossible de charger l'inventaire.",
    'inventoryEmpty': 'Aucun article enregistré.',
    'lowStockBanner': '{count} article(s) en stock bas',
    'searchItemHint': 'Rechercher un article...',
    'unitsCount': '{count} unités',
    'lowStockTag': 'Stock bas',
    'marketplaceTitle': 'Marketplace',
    'marketplaceError': 'Impossible de charger le marketplace.',
    'marketplaceEmpty': 'Aucune annonce pour le moment.',
    'searchHint': 'Rechercher...',
    'filterAll': 'Tout',
    'filterOffers': 'Offres',
    'filterRequests': 'Demandes',
    'filterServices': 'Services',
    'filterFree': 'Gratuit',
    'contact': 'Contacter',
    'sellerLabel': 'Vendeur',
    'sellerWithId': 'Vendeur #{id}',
    'moderationTitle': 'Modération IA',
    'moderationError': 'Impossible de charger la file de modération.',
    'moderationEmpty': 'Aucun contenu à modérer.',
    'approve': 'Approuver',
    'reject': 'Rejeter',
    'reviewDone': 'Contenu {decision}',
    'reviewError': 'Erreur lors de la modération',
    'predictionsMlTitle': 'Prédictions ML',
    'predictionsMlEmpty': 'Aucune prédiction disponible.',
    'predictedValue': 'Prédit : {value}',
    'executiveInsightsTitle': 'Insights Exécutifs IA',
    'executiveInsightsError': 'Impossible de charger les insights.',
    'executiveInsightsEmpty': 'Aucun insight actif.',
    'prayerJournalTitle': 'Journal de Prière',
    'prayerJournalEmpty': 'Commencez à écrire vos prières',
    'statTotal': 'Total',
    'statOngoing': 'En cours',
    'statAnswered': 'Exaucées',
    'newPrayer': 'Nouvelle prière',
    'yourPrayer': 'Votre prière',
    'categoryLabel': 'Catégorie',
    'catPrayer': 'Prière',
    'catPraise': 'Louange',
    'catIntercession': 'Intercession',
    'catGrace': 'Grâce',
    'digitalTwinTitle': '🔮 Jumeau Numérique',
    'quickScenarios': '⚡ Scénarios rapides',
    'scenarioStagnation': 'Stagnation',
    'scenarioSoftGrowth': 'Croissance douce',
    'scenarioMakersAwakening': 'Réveil faiseurs',
    'scenarioAwakeningRetention': 'Réveil + rétention',
    'scenarioSpiritualAwakening': 'Réveil spirituel',
    'parameters': '⚙️ Paramètres',
    'makerMultiplier': 'Multiplicateur faiseurs',
    'retentionGain': 'Gain rétention (%)',
    'pipelineBoost': 'Boost pipeline',
    'horizon': 'Horizon',
    'projectedStat': 'Projeté',
    'soulsUnit': 'âmes',
    'growthStat': 'Croissance',
    'leadersNeeded': 'Leaders nécessaires',
    'leadersNeededCount': 'Nouveaux leaders nécessaires : {value}',
    'leadersSufficient': 'suffisant',
    'leadersMissing': '+{gap} manquants',
    'monthlyProjection': '📈 Projection mois par mois',
    'monthLabel': 'Mois {n}',
    'dataMigrationTitle': 'Migration de données',
    'importData': 'Importer des données',
    'importDataHint': "Importez vos membres depuis Excel ou CSV. L'assistant mapping détecte automatiquement les colonnes.",
    'selectFile': 'Sélectionner un fichier',
    'aiAnalysis': 'Analyse IA',
    'detectedFile': 'Fichier détecté',
    'detectedRows': 'Lignes détectées',
    'mappedColumns': 'Colonnes mappées',
    'mappingConfidence': 'Confiance mapping',
    'launchMigration': 'Lancer la migration',
    'previousMigrations': 'Migrations précédentes',
    'noMigrations': 'Aucune migration effectuée',
    'rerun': 'Relancer',
    'migrationLaunched': 'Migration lancée ✅',
    'migrationError': 'Erreur lors de la migration',
    'rowsCount': '{count} lignes',
    'surveysTitle': 'Sondages',
    'surveysEmpty': 'Aucun sondage',
    'responsesCount': '{count} réponses',
    'encouragementsTitle': 'Encouragements',
    'tabReceived': 'Reçus ({count})',
    'tabSent': 'Envoyés ({count})',
    'tabTeam': 'Mon équipe ({count})',
    'emptyReceivedEnc': 'Aucun encouragement reçu',
    'emptySentEnc': 'Aucun encouragement envoyé',
    'emptyTeam': 'Aucun membre dans votre équipe',
    'composeEncouragement': 'Envoyer un encouragement',
    'encTypePrayer': '🙏 Prière',
    'encTypePraise': '⭐ Louange',
    'encTypeThanks': '❤️ Merci',
    'encTypeSupport': '💪 Soutien',
    'encTypeWelcome': '👋 Bienvenue',
    'encTypeScripture': '📖 Verset',
    'writeEncouragementHint': 'Écrivez votre encouragement...',
    'encouragementSent': 'Encouragement envoyé 🙏',
    'send': 'Envoyer',
    'encouragementsReceived': '{count} encouragements reçus',
    'followUpTitle': '🤝 Demandes de suivi',
    'fuTypeMaker': 'Demander un faiseur',
    'fuTypeSpiritual': 'Accompagnement spirituel',
    'fuTypePastoral': 'Conseil pastoral',
    'newFollowUpRequest': 'Nouvelle demande de suivi',
    'describeNeed': 'Décrivez votre besoin…',
    'requestSent': 'Demande envoyée ✅',
    'requestFailed': "Échec de l'envoi",
    'myRequests': 'Mes demandes ({count})',
    'assignedToMe': 'Assignées à moi ({count})',
    'emptyMyRequests': 'Aucune demande. Appuyez sur « Demander » pour commencer.',
    'emptyAssignedRequests': 'Aucune demande ne vous est assignée.',
    'markComplete': 'Marquer terminée',
    'askAction': 'Demander',
    'neighborhoodHealthTitle': '💗 Santé par quartier',
    'neighborhoodEmpty': 'Aucune zone définie. Renseignez le champ « zone » des âmes.',
    'soulsInZone': '{count} âmes',
    'scoreLabel': 'Score : {score}/100',
    'recentContacts': '{count} contacts récents',
    'sabbathTitle': '🕊️ Tableau sabbatique',
    'globalMaturity': 'Maturité spirituelle globale',
    'activeSouls': 'Âmes actives',
    'activeMakers': 'Faiseurs actifs',
    'familiesAtRisk': 'Familles à risque',
    'twelveAxes': 'Les 12 axes de maturité',
    // ---- Lot H1 : écrans historiques (admin custom-fields, workflow transferts) ----
    'newField': 'Nouveau champ',
    'fieldNameLabel': 'Nom du champ',
    'fieldEntityLabel': 'Entité (SOUL, USER, DEPARTMENT…)',
    'fieldTypeLabel': 'Type (TEXT, NUMBER, DATE, SELECT, BOOLEAN)',
    'create': 'Créer',
    'noCustomFields': 'Aucun champ personnalisé',
    'deleteQuestion': 'Supprimer ?',
    'transferWorkflowTitle': 'Workflow de transfert',
    'noConfiguration': 'Aucune configuration',
    'statusActive': 'Actif',
    'statusInactive': 'Inactif',
    'stepsCount': '{count} étape(s)',
    'initiatorRoles': 'Rôles initiateurs',
    'validationMode': 'Mode de validation',
    'requiredValidations': 'Validations requises',
    'delayHours': 'Délai (heures)',
    'circuitSteps': 'Étapes du circuit',
    'stepLabel': 'Étape {index} — {label}',
    'configSaved': 'Configuration enregistrée',
    'saveFailed': 'Erreur lors de l\u2019enregistrement',
    'deleteConfigQuestion': 'Supprimer cette configuration ?',
    'deleteBlockedByRequests': 'Suppression impossible : des demandes utilisent cette configuration',
    // ---- Lot H1b : demandes membres + sécurité ----
    'memberRequestsTitle': 'Demandes',
    'tabMyRequests': 'Mes demandes',
    'tabInbox': 'Reçues',
    'newRequest': 'Nouvelle demande',
    'requestType': 'Type',
    'recipient': 'Destinataire',
    'subjectOptional': 'Objet (optionnel)',
    'messageHint': 'Message...',
    'attachments': 'Pièces jointes',
    'sendFailed': 'Erreur lors de l\u2019envoi',
    'noSentRequests': 'Aucune demande envoyée',
    'noReceivedRequests': 'Aucune demande reçue',
    'fromLabel': 'De : {name}',
    'resolve': 'Résoudre',
    'securityTitle': 'Sécurité et confidentialité',
    'authSection': 'Authentification',
    'biometricAuth': 'Authentification biométrique',
    'biometricSubtitle': 'Empreintes ou Face ID',
    'pinCode': 'Code PIN',
    'pinSubtitle': 'Configurer un code PIN de secours',
    'changePinTitle': 'Changer le code PIN',
    'currentPin': 'Code PIN actuel',
    'newPin': 'Nouveau code PIN',
    'pinUpdated': 'Code PIN mis à jour',
    'pinIncorrect': 'Code PIN incorrect',
    'sessionSection': 'Session',
    'sessionExpiry': 'Expiration de session',
    'sessionExpirySubtitle': 'Déconnexion après {value} d\u2019inactivité',
    'never': 'Jamais',
    'minutesCount': '{count} minutes',
    'screenProtection': 'Protection d\u2019écran',
    'screenProtectionSubtitle': 'Empêcher les captures d\u2019écran',
    'auditSection': 'Audit et activité',
    'auditLogEntries': 'Journal d\u2019audit ({count} entrées)',
    'viewAuditSubtitle': 'Consulter les actions enregistrées',
    'exportLog': 'Exporter le journal',
    'exportSubtitle': 'CSV ou JSON pour archivage',
    'clearLog': 'Effacer le journal',
    'clearSubtitle': 'Supprimer toutes les entrées (RGPD)',
    'accountSection': 'Informations du compte',
    'userLabel': 'Utilisateur',
    'orgLabel': 'Organisation',
    'activeRoleLabel': 'Rôle actif',
    'rolesLabel': 'Rôles',
    'noEntries': 'Aucune entrée',
    'logExported': 'Journal exporté',
    'entriesExported': '{count} entrées exportées',
    'clearLogQuestion': 'Effacer le journal d\u2019audit ?',
    'clearAction': 'Effacer',
    // ── Lot H2 : admin_settings / admin_tenants / growth_projection / my_team_family ──
    'churchSettings': 'Paramètres de l\'église',
    'identity': 'Identité',
    'slogan': 'Slogan',
    'colorsSection': 'Couleurs',
    'primaryColorLabel': 'Couleur principale',
    'accentColorLabel': 'Couleur accent',
    'settingsSaved': '✅ Paramètres enregistrés',
    'tenantsTitle': 'Églises (tenants)',
    'noTenants': 'Aucune église configurée',
    'statusSuspended': 'Suspendu',
    'growthProjectionTitle': '📊 Projection de croissance',
    'growthProphecyTitle': 'Prophétie de croissance (analyse réelle)',
    'projectedAnnualGrowth': 'Croissance annuelle projetée : {value} %',
    'headcountIn12Months': 'Effectif dans 12 mois : {value}',
    'simulator': 'Simulateur',
    'annualGrowthRate': 'Taux de croissance annuel (%)',
    'horizonMonths': 'Horizon (mois)',
    'simulate': 'Simuler',
    'simulationSaved': 'Simulation enregistrée ✅',
    'simulationFailed': 'Échec de la simulation',
    'savedProjections': 'Projections enregistrées',
    'myTeamFamilyTitle': '👨‍👩‍👧‍👦 Mon équipe / ma famille',
    'teamTabMembers': 'Membres ({count})',
    'teamTabReceived': 'Encouragements reçus ({count})',
    'encourageName': 'Encourager {name}',
    'noSpiritualFamily': 'Vous n\'êtes rattaché à aucune famille spirituelle.',
    'meLabel': '(moi)',
    'encouragementsBadge': '{count} encouragements',
    'sendEncouragementTooltip': 'Envoyer un encouragement',
    'noEncouragementsYet': 'Aucun encouragement reçu pour le moment.',
    // ── Lot H3 : member_activities / department_stats / evangelism ──
    'myActivities': 'Mes activités',
    'filterPresences': 'Présences',
    'filterEvents': 'Événements',
    'filterNotes': 'Notes',
    'filterProgression': 'Progression',
    'noActivities': 'Aucune activité',
    'presenceConfirmed': 'Présence confirmée',
    'absenceRecorded': 'Absence enregistrée',
    'weekOf': 'Semaine du',
    'makerNote': 'Note du faiseur',
    'spiritualLevel': 'Niveau spirituel : {value}',
    'progressOverview': 'Progression globale',
    'departmentStats': 'Statistiques du département',
    'kpiMembers': 'Membres',
    'kpiActive': 'Actifs',
    'kpiNew': 'Nouveaux',
    'kpiPresence': 'Présence',
    'kpiOverdueTasks': 'Tâches retard',
    'kpiTeams': 'Équipes',
    'memberBreakdown': 'Répartition des membres',
    'integrating': 'En intégration',
    'standby': 'En veille',
    'droppedOut': 'Décrochés',
    'headcountEvolution': 'Évolution de l\'effectif (12 mois)',
    'attendanceSection': 'Présence',
    'present': 'Présents',
    'absent': 'Absents',
    'tasksByStatus': 'Tâches par statut',
    'disciplinaryCategory': 'Discipline par catégorie',
    'noDisciplinary': 'Aucun événement disciplinaire',
    'workloadPerMember': 'Charge de travail par membre',
    'noAssignedTasks': 'Aucune tâche ouverte assignée',
    'tasksUnit': 'tâches',
    'retardUnit': 'retard',
    'organizationSection': 'Organisation',
    'activeAssignments': 'Affectations actives',
    'activePositions': 'Postes actifs',
    'evangelizationTitle': 'Évangélisation · {count} âmes',
    'funnelOfConversion': 'Funnel de conversion',
    'clickStageToSeeSouls': 'Cliquez sur une étape pour voir les âmes',
    'soulsAtStage': '{count} âme(s)',
    'noSoulsAtStage': 'Aucune âme à cette étape',
    'advanceTo': 'Avancer → {label}',
    'retreatLabel': '← Reculer {label}',
    'globalView': 'Vue globale',
    'sinceDate': 'Depuis {date}',
    // ── Lot H4 : badges / giving ──
    'gamificationTitle': 'Gamification · {count} badges',
    'tabMyBadges': 'Mes badges',
    'tabLeaderboard': 'Classement',
    'checkMyBadges': 'Vérifier mes badges',
    'progressionLabel': 'Progression',
    'percentCompleted': '{pct}% complété',
    'badgesEarned': '{count} badge(s) gagné(s)',
    'scoresPerCriteria': 'Scores par critère',
    'earnedBadges': 'Badges gagnés ({count})',
    'toUnlock': 'À débloquer',
    'noLeaderboard': 'Aucun classement disponible',
    'badgesUnit': 'badge(s)',
    'newBadgesEarned': '🎉 {count} nouveau(x) badge(s) gagné(s) !',
    'noNewBadges': 'Aucun nouveau badge pour le moment',
    'checkBadgesError': 'Erreur lors de la vérification',
    'criteriaVisits': 'Visites',
    'criteriaInteractions': 'Interactions',
    'criteriaEvangelism': 'Évangélisation',
    'criteriaAttendance': 'Présence',
    'criteriaLoyalty': 'Fidélité',
    'tithesAndOfferings': 'Dîmes & Offrandes',
    'giveNow': 'Donner maintenant',
    'amountXOF': 'Montant (XOF)',
    'invalidAmount': 'Montant invalide',
    'operatorLabel': 'Opérateur',
    'destinationLabel': 'Destination',
    'mobilePhoneOptional': 'Téléphone Mobile Money (optionnel)',
    'paymentInitiated': 'Paiement initié — référence {ref}',
    'paymentFailed': 'Échec de l\'initiation du paiement',
    'paymentConfirmed': 'Paiement confirmé — merci pour votre don !',
    'waitingConfirmation': 'En attente de confirmation ({ref})…',
    'noDonationsYet': 'Aucun don enregistré pour le moment.\nQue le Seigneur bénisse votre générosité !',
    'statusConfirmed': 'Confirmé',
    'statusPending': 'En attente',
    'statusFailed': 'Échoué',
    'statusCancelled': 'Annulé',
    // ── Lot H5 : broadcast / forms / church-directory ──
    'broadcastTitle': 'Diffusion / Broadcast',
    'newBroadcast': 'Nouvelle diffusion',
    'sent': 'Envoyées',
    'readRate': 'Lues',
    'recentBroadcasts': 'Diffusions récentes',
    'targeting': 'Ciblage',
    'allMembers': 'Tous les membres',
    'byDepartment': 'Par département',
    'byFamily': 'Par famille',
    'byRole': 'Par rôle',
    'broadcastEmpty': 'Aucune diffusion',
    'broadcastError': 'Erreur lors du chargement des diffusions',
    'readLabel': 'lu',
    'formsTitle': 'Formulaires',
    'createForm': 'Créer un formulaire',
    'createFormSubtitle': 'Drag & drop avec conditions logiques',
    'publishedForms': 'Formulaires publiés',
    'fieldText': 'Texte',
    'fieldChoice': 'Choix multiple',
    'fieldDate': 'Date',
    'fieldFile': 'Fichier',
    'fieldSignature': 'Signature',
    'fieldNote': 'Note',
    'formTitleLabel': 'Titre du formulaire',
    'addFieldHint': 'Appuyez sur un type de champ pour l\'ajouter',
    'formSaved': 'Formulaire sauvegardé !',
    'formsEmpty': 'Aucun formulaire publié',
    'formsError': 'Erreur lors du chargement des formulaires',
    'churchDirectoryTitle': 'Annuaire de l\'Église',
    'searchMember': 'Rechercher un membre...',
    'publicProfile': 'Profil public',
    'privateProfile': 'Profil privé',
    'directoryEmpty': 'Aucun membre dans l\'annuaire',
    'directoryError': 'Erreur lors du chargement de l\'annuaire',
    'unknownFamily': 'Famille inconnue',
    'unknownRole': 'Rôle inconnu',

    // ── H7 nav+profile (re-add) ──
    'feedbackSubtitle': 'Bug, suggestion, problème…',
    'feedbackTitle': 'Un retour ?',
    'navActiveLabel': 'ACTIF',
    'navAdminSection': 'ADMINISTRATION',
    'navAppointments': 'Rendez-vous',
    'navAudit': 'Audit',
    'navChangeRole': 'Changer de rôle',
    'navChefDashboard': 'Dashboard Chef',
    'navChurchBenchmark': 'Benchmark églises',
    'navChurchSettings': 'Paramètres église',
    'navChurches': 'Églises (tenants)',
    'navCompliance': 'Compliance RGPD',
    'navCrmFaiseur': 'CRM Faiseur',
    'navCustomFields': 'Champs personnalisés',
    'navCustomPages': 'Pages personnalisées',
    'navDataMigration': 'Migration données',
    'navDictionaries': 'Dictionnaires',
    'navDiscipleshipPath': 'Parcours spirituel',
    'navDocuments': 'Documents',
    'navEncouragements': 'Encouragements',
    'navEvaluations': 'Évaluations',
    'navFaceCheckin': 'Pointage facial',
    'navFamilyReport': 'Rapport famille',
    'navFollowUpRequests': 'Demandes suivi',
    'navGraceActions': 'Actions de grâce',
    'navGrowthProjection': 'Croissance',
    'navIntegrations': 'Intégrations',
    'navInventory': 'Inventaire',
    'navLoadPrediction': 'Prédiction charge',
    'navMakerReport': 'Rapport faiseur',
    'navMenus': 'Menus plateforme',
    'navMessaging': 'Messagerie',
    'navModeration': 'Modération',
    'navModules': 'Modules plateforme',
    'navNeighborhoodHealth': 'Santé quartiers',
    'navObjectives': 'Objectifs',
    'navParallelFollowups': 'Suivis parallèles',
    'navPermissions': 'Permissions',
    'navQuest': 'Quest (XP)',
    'navRequests': 'Demandes',
    'navRespDashboard': 'Dashboard Responsable',
    'navRewards': 'Récompenses',
    'navSabbath': 'Sabbath Dashboard',
    'navSecurity': 'Sécurité',
    'navShepherdsPilot': 'Pilotage Pasteur',
    'navTithesOfferings': 'Dîmes & offrandes',
    'navTontines': 'Tontines',
    'navTransferWorkflow': 'Workflow transfert',
    'navTransfers': 'Transferts',
    'navUsageAnalytics': 'Analytics usage',
    'navVisits': 'Visites',
    'navVoiceAssistant': 'PasteurBot Vocal',
    'navVoiceReports': 'Rapports vocaux',
    'navWeeklyChallenges': 'Défis hebdo',
    'navWhatsApp': 'WhatsApp Rappels',
    'profileDepartment': 'Département',
    'profileEmail': 'Email',
    'profileFamily': 'Famille',
    'profileLogout': 'Déconnexion',
    'profilePersonalInfo': 'Informations personnelles',
    'profilePhone': 'Téléphone',
    'profilePresence': 'Présence',
    'profileProgression': 'Progression',
    'profileQuickActions': 'Actions rapides',
    'profileRegisteredOn': 'Inscrit le',
    'profileRole': 'Rôle',
    'profileScore': 'Score',
    'profileSpiritualInfo': 'Informations spirituelles',
    'profileSpiritualScore': 'Score spirituel',
    'profileTitle': 'Profil',
    'rejectAction': 'Cancelar',
    'roleSwitchFailed': 'Échec du changement de rôle',
    'sendError': 'Erro ao enviar',

    // ── H7 nav+profile (re-add) ──

    // ── H7 nav+profile (re-add) ──

    // ── Lot H6 : dashboards + transferts ──
    'dashGrowth': 'Croissance',
    'dashPresenceAndReports': 'Présences & Rapports',
    'dashGlobalPresence': 'Présence globale',
    'dashReportsSubmitted': 'Rapports soumis',
    'dashCompletion': 'Complétion',
    'dashActiveAlerts': 'alerte(s) active(s)',
    'attentionRequired': 'Attention requise',
    'allUnderControl': 'Tout est sous contrôle',
    'dashDepartments': 'Départements',
    'dashFamilies': 'Familles',
    'dashMakers': 'Faiseurs',
    'dashRiskFamilies': 'Familles à risque',
    'dashShepherdsPilot': 'Pilotage Pasteur',
    'dashMyFamily': 'Ma famille',
    'dashMyMaker': 'Mon faiseur',
    'dashMyDepartments': 'Mes départements',
    'dashMyProgression': 'Ma progression',
    'dashMakerNotes': 'Notes de mon faiseur',
    'dashRecentPresences': 'Mes présences récentes',
    'dashUpcomingEvents': 'Événements à venir',
    'dashQuickActions': 'Actions rapides',
    'dashMakersCount': 'Faiseurs ({count})',
    'dashMakerWorkload': 'Charge de travail des Faiseurs',
    'dashDisciplesSplit': 'Répartition des disciples',
    'dashActiveAlertsTitle': 'Alertes actives',
    'dashUpcomingVisits': 'Visites à venir',
    'dashRecentPrayers': 'Prières récentes',
    'dashMySpace': 'Mon espace',
    'dashCurrentLevel': 'Niveau actuel',
    'dashPresenceThisWeek': 'Présences cette semaine',
    'dashNextStep': 'Prochaine étape : {value}',
    'dashCompanion': 'Accompagnateur',
    'dashChefLabel': 'Chef : {name}',
    'dashRespLabel': 'Responsable : {name}',
    'dashDeptToAdmin': 'Département à administrer',
    'dashOverview': 'Vue d\'ensemble',
    'dashMyDepartment': 'Mon département',
    'dashDeptManagement': 'Gestion du département',
    'dashOverdueTasks': 'Tâches en retard',
    'dashBirthdays': 'Anniversaires du mois',
    'dashFollowUp': 'À suivre cette semaine',
    'dashDiscipline': 'Évaluations',
    'dashTransferRequests': 'Des membres souhaitent changer de famille',
    'dashEventsAndPosts': 'Équipes & Postes',
    'dashDeptManagementTitle': 'Gestion',
    'dashQRCheckin': 'QR Check-in',
    'dashReport': 'Rapport',
    'dashPresenceLabel': 'Présence',
    'dashActiveLabel': 'Actifs',
    'dashNewLabel': 'Nouveaux',
    'dashPresentLabel': 'Présents',
    'dashAbsentLabel': 'Absents',
    'dashDroppedLabel': 'Décrochés',
    'dashTransferLabel': 'Transferts',
    'dashTeamsLabel': 'Équipes',
    'dashPresentShort': 'Présent',
    'dashAbsentShort': 'Absent',
    'dashSurcharge': 'Surchargé',
    'dashLightLoad': 'Léger',
    'dashNormalLoad': 'Normal',
    'dashSouls': 'âmes',
    'dashConverti': 'Converti',
    'dashArrivant': 'Arrivant',
    'transferCreateTitle': 'Créer une demande de transfert',
    'transferDetailTitle': 'Demande de transfert',
    'transferType': 'Type de transfert',
    'transferDeptConcerned': 'Département concerné',
    'transferDeptDestination': 'Département de destination',
    'transferDeptRetrait': 'Département de retrait',
    'transferFamilyConcerned': 'Famille concernée',
    'transferFamilyDestination': 'Famille de destination',
    'transferMemberAdd': 'Membre (âme) à ajouter',
    'transferMemberRemove': 'Membre (âme) à retirer',
    'transferMakerToTransfer': 'Faiseur à transférer',
    'transferDiscipleToTransfer': 'Disciple (âme) à transférer',
    'transferJustification': 'Justification détaillée *',
    'transferComments': 'Commentaires (optionnel)',
    'transferSubmitCreate': 'Créer et soumettre',
    'transferSaveDraft': 'Enregistrer le brouillon',
    'transferDraftSaved': 'Brouillon enregistré',
    'transferSubmitted': 'Demande soumise au circuit de validation',
    'transferIncomplete': 'Formulaire incomplet',
    'transferCreateError': 'Erreur lors de la création',
    'transferChoose': 'Choisir',
    'transferCurrentAffectation': 'Affectation actuelle',
    'transferCircuit': 'Circuit de validation',
    'transferApprove': 'Approuver',
    'transferReject': 'Annuler',
    'transferModify': 'Modifier',
    'transferAskInfo': 'Demander des informations',
    'transferExecute': 'Exécuter',
    'transferArchive': 'Archiver',
    'transferHistory': 'Historique',
    'transferDecisions': 'Décisions',
    'transferStep': 'Étape',
    'transferInfo': 'Infos',
    'transferJustificationLabel': 'Justification',
    'transferMotivation': 'Motivation *',
    'transferMotivationOptional': 'Motivation (optionnelle)',
    'transferCommentsLabel': 'Commentaires :',
    'transferConfirm': 'Confirmer',
    'transferCorrection': 'Correction',
    'transferCancelQuestion': 'Annuler la demande ?',
    'transferCancelExplanation': 'La demande sera renvoyée au demandeur.',
    'transferNoDocument': 'Aucun document dans le module Documents.',
    'transferNoAttachment': 'Aucune pièce jointe.',
    'transferCreateDoc': 'Créer un document',
    'transferNoEvent': 'Aucun événement',
    'transferOperationError': 'Erreur lors de l\'opération',
    'transferAttachments': 'Pièces jointes',
    'transferJustificationHint': 'Motifs pastoraux, organisationnels...',
    'transferPriority': 'Priorité',
    'transferPerson': 'Personne',
    'transferSubmit': 'Soumettre',
    'transferCancel': 'Annuler',
    'transferConfirmBtn': 'Confirmer',
    'transferApprovalExplanation': 'La demande avancera dans le circuit de validation.',
    'transferRejectionExplanation': 'La demande sera définitivement refusée et notifiée.',
    'transferAutoExecution': 'Aucune validation requise — exécution automatique dès la soumission.',
    'transferAttachmentsCount': 'Pièces jointes ({count})',
    'transferValidateCount': 'Valider ({count})',
    'transferAttachmentsUpdated': 'Pièces jointes mises à jour',
    'transferAttachmentsError': 'Erreur lors de la mise à jour des pièces jointes',
    'transferListTitle': 'Transferts',
    'transferListNewRequest': 'Demande',
    'transferListStatusFilter': 'Statut',
    'transferListTypeFilter': 'Type',
    'transferListEmpty': 'Aucune demande de transfert',
    'transferListPersonLabel': 'Personne : {name}',
    'transferListValidations': '{approved}/{total} validations',
    'transferListSubmitError': 'Erreur lors de la soumission',
    'transferListCancelSuccess': 'Demande annulée',
    'transferListConfigDefault': 'Configuration',
    'statusDraft': 'Brouillon',
    'statusPendingValidation': 'En attente de validation',
    'statusApproval': 'Approbation',
    'statusExecuted': 'Exécuté',
    'statusArchive': 'Archivé',
    'statusRequestedInfo': 'Demande d\'informations',
    'bibleReadingTitle': 'Plan de Lecture Biblique',
    'myProgress': 'Ma progression',
    'days': 'jours',
    'consecutiveDays': 'jours consécutifs',
    'todaysReading': 'Lecture du jour',
    'markAsRead': 'Marquer lu',
    'addNote': 'Ajouter une note',
    'availablePlans': 'Plans disponibles',
    'noPlans': 'Aucun plan disponible',
    'familySharing': 'Partagé avec ma famille',
    'devPlanTitle': 'Mon Plan de Développement',
    'globalProgress': 'Progression globale',
    'activeObjectives': 'objectifs actifs',
    'completedObjectives': 'Objectifs terminés',
    'spiritualJournalTitle': 'Journal Spirituel',
    'reflection': 'Réflexion',
    'thanksgiving': 'Remerciement',
    'praise': 'Louange',
    'lesson': 'Leçon',
    'newEntry': 'Nouvelle entrée',
    'discipleshipPathTitle': 'Parcours de discipolat',
    'familyCohesionTitle': 'Cohésion Familiale',
    'cohesionScore': 'Score de cohésion',
    'goodEffort': 'Bon — Maintenir les efforts',
    'needsImprovement': 'À améliorer',
    'networkFamilies': 'Familles du réseau',
    'noFamilies': 'Aucune famille',
    'familyResourcesTitle': 'Ressources Familiales',
    'categories': 'Catégories',
    'recentResources': 'Ressources récentes',
    'noResources': 'Aucune ressource',
    'makerTrackingTitle': 'Mon Parcours de Faiseur',
    'timeline': 'Timeline',
    'noTimeline': 'Aucune activité',
    'sermonTranslationTitle': 'Traduction des sermons',
    'translationInProgress': 'Traduction en cours',
    'recentTranslations': 'Traductions récentes',
    'noTranslations': 'Aucune traduction',
    'skillMatchingTitle': 'Matching Compétences',
    'launchAiMatching': 'Lancer le matching IA',
    'matchingSubtitle': 'Analyser les compétences vs besoins',
    'proposals': 'Propositions',
    'noMatches': 'Aucune proposition',
    'skillsMatrixTitle': 'Matrice de Compétences',
    'overview': 'Vue d\'ensemble',
    'skills': 'Compétences',
    'evaluatedMembers': 'Membres évalués',
    'gapsFound': 'Gaps identifiés',
    'skillsByDepartment': 'Compétences par département',
    'needsMoreMembers': 'besoin de membres supplémentaires',
    'read': 'Lu',
    'progression': 'Progression',
    'prayer': 'Prière',
    'commTitle': 'Annonces',
    'commManagement': 'Gestion des annonces',
    'commEmpty': 'Aucune annonce. Créez la première avec le bouton +.',
    'commPublishedEmpty': 'Aucune annonce publiée pour vous pour le moment',
    'commNew': 'Nouvelle annonce',
    'commEdit': 'Modifier l’annonce',
    'commPublished': 'Annonces publiées',
    'commPublishSuccess': 'Annonce publiée et diffusée à {count} destinataire(s)',
    'commPublishError': 'Erreur lors de la publication',
    'commDeleteTitle': 'Supprimer l’annonce ?',
    'commDeleted': 'Annonce supprimée',
    'commDeleteError': 'Erreur lors de la suppression',
    'commCreated': 'Annonce créée',
    'commModified': 'Annonce modifiée',
    'commSaveError': 'Échec de l\'enregistrement',
    'commStatusDraft': 'Brouillon',
    'commStatusPublished': 'Publiée',
    'commStatusArchived': 'Archivée',
    'commCibleAll': 'Toute l\'église',
    'commCibleRole': 'Par rôle',
    'commCibleFamily': 'Par famille',
    'commCibleDept': 'Par département',
    'commBtnCreate': 'Créer',
    'commBtnSave': 'Enregistrer',
    'commInputTitle': 'Titre',
    'commInputContent': 'Contenu',
    'commInputCible': 'Cible de diffusion',
    'commInputFamily': 'Famille',
    'commInputDept': 'Département',
    'complianceTitle': 'Compliance RGPD',
    'complianceVerifyAudit': 'Vérifier audit',
    'complianceTabOverview': 'Vue d\'ensemble',
    'complianceTabRetention': 'Rétention',
    'complianceTabAudit': 'Audit',
    'complianceTabPortability': 'Portabilité',
    'complianceStatPolicies': 'Politiques actives',
    'complianceStatConsents': 'Consentements',
    'complianceStatAuditEntries': 'Entrées audit',
    'complianceStatGdprRequests': 'Demandes RGPD',
    'complianceAuditIntegrity': 'Intégrité de l’audit',
    'complianceAuditValid': 'Chaîne intacte — aucune altération',
    'complianceAuditInvalid': 'Altération détectée',
    'complianceVerifySuccess': 'Chaîne valide — {count} entrées vérifiées',
    'complianceVerifyBroken': '{count} lien(s) brisé(s) dans la chaîne !',
    'complianceVerifyImpossible': 'Vérification impossible',
    'complianceExportSuccess': 'Export téléchargé ({count} sections)',
    'complianceExportError': 'Erreur lors de l\'export',
    'complianceChecklist': 'Checklist de conformité',
    'complianceCheckPolicy': 'Politique de rétention',
    'complianceCheckConsents': 'Consentements collectés',
    'complianceCheckAudit': 'Audit trail immuable',
    'complianceCheckPortability': 'Portabilité 1-clic',
    'complianceCheckRightToForget': 'Droit à l’oubli',
    'complianceCheckEncryption': 'Chiffrement AES-256',
    'compliancePurgeTitle': 'Exécuter la purge ?',
    'compliancePurgeContent': 'Les données dépassant la durée de rétention seront traitées.',
    'compliancePurgeAction': 'Purger',
    'compliancePurgeSuccess': 'Purge exécutée',
    'complianceRetentionTitle': 'Politiques de rétention',
    'complianceRetentionEmpty': 'Aucune politique configurée',
    'complianceRetentionDurations': 'Durées suggérées',
    'complianceActionAnonymize': 'Anonymiser',
    'complianceActionArchive': 'Archiver',
    'complianceAuditEmpty': 'Aucune entrée d\'audit',
    'complianceExportTitle': 'Export portabilité',
    'complianceExportSubtitle': 'RGPD Art. 20 — Format JSON',
    'complianceExportContent': 'Contenu de l\'export :',
    'complianceExportProfile': 'Profil utilisateur',
    'complianceExportSouls': 'Âmes liées (disciples)',
    'complianceExportConsents': 'Historique consentements',
    'complianceExportGdpr': 'Demandes RGPD passées',
    'complianceExportMeta': 'Métadonnées (format, version)',
    'complianceExportBtn': 'Exporter mes données',
    'onboardingSkip': 'Passer',
    'onboardingNext': 'Suivant',
    'onboardingStart': 'Commencer',
    'onboardingAR': 'Visite AR',
    'onboardingWelcomeTitle': 'Bienvenue sur Discipolat',
    'onboardingWelcomeDesc': 'La plateforme qui facilite le suivi de disciples de votre église.',
    'onboardingTrackingTitle': 'Suivi en temps réel',
    'onboardingTrackingDesc': 'Accédez à vos âmes, rapports et événements depuis n\'importe où.',
    'onboardingOfflineTitle': 'Mode hors ligne',
    'onboardingOfflineDesc': 'Toutes les fonctionnalités critiques fonctionnent sans connexion.',
    'onboardingNotificationsTitle': 'Notifications intelligentes',
    'onboardingNotificationsDesc': 'Recevez des alertes personnalisées selon votre rôle.',
    'onboardingSecurityTitle': 'Sécurité renforcée',
    'onboardingSecurityDesc': 'Authentification biométrique, session sécurisée et protection des données.',
    'deptMgmtTitle': 'Gestion du département',
    'deptMgmtTooltipStats': 'Statistiques',
    'deptMgmtTooltipTools': 'Rapports · Checklists · Inventaire',
    'deptMgmtSearchHint': 'Recherche rapide : membre, équipe, tâche…',
    'deptMgmtKpiTeams': 'Équipes',
    'deptMgmtKpiPositions': 'Postes',
    'deptMgmtKpiAssigned': 'Affectés',
    'deptMgmtTabMembers': 'Membres',
    'deptMgmtTabOrg': 'Organisation',
    'deptMgmtTabTasks': 'Tâches',
    'deptMgmtTabAssignments': 'Affectations',
    'deptMgmtTabEvents': 'Événements',
    'deptMgmtTabActivity': 'Activité',
    'deptMgmtMembersTitle': 'Membres du département ({count})',
    'deptMgmtMembersEmpty': 'Aucun membre dans ce département',
    'deptMgmtFamilyLabel': 'Famille : {name}',
    'deptMgmtMakerLabel': 'Faiseur : {name}',
    'deptMgmtOrgTitle': 'Organigramme',
    'deptMgmtOrgEmpty': 'Aucune équipe — créez votre premier sous-département',
    'deptMgmtTeamPermanent': 'Équipe permanente',
    'deptMgmtTeamTemporary': 'Temporaire',
    'deptMgmtSubDepartment': 'Sous-département',
    'deptMgmtMembersCount': '{count} membres',
    'deptMgmtEventLabel': 'Événement : {title}',
    'deptMgmtNewTeam': 'Nouvelle équipe',
    'deptMgmtTeamNameLabel': 'Nom de l\'équipe',
    'deptMgmtParentTeam': 'Équipe parente',
    'deptMgmtNoParent': '— Aucune (racine) —',
    'deptMgmtLinkedEvent': 'Événement lié (optionnel)',
    'deptMgmtLoadingEvents': 'Chargement des événements…',
    'deptMgmtNoEvent': '— Aucun événement —',
    'deptMgmtDateStart': 'Date début (AAAA-MM-JJ)',
    'deptMgmtDateEnd': 'Date fin (AAAA-MM-JJ)',
    'deptMgmtObjective': 'Objectif (optionnel)',
    'deptMgmtTeamCreateError': 'Échec de la création de l\'équipe',
    'deptMgmtTasksTitle': 'Tâches du département',
    'deptMgmtStatInProgress': 'En cours',
    'deptMgmtStatTodo': 'À faire',
    'deptMgmtStatOverdue': 'En retard',
    'deptMgmtStatDone': 'Terminées',
    'deptMgmtTaskEmpty': 'Aucune tâche',
    'deptMgmtTaskLoadError': 'Erreur de chargement',
    'deptMgmtNewTask': 'Nouvelle tâche',
    'deptMgmtAssignedTo': 'Assignée à',
    'deptMgmtNoAssignee': '— Non assignée —',
    'deptMgmtPriority': 'Priorité',
    'deptMgmtPriorityLow': 'Basse',
    'deptMgmtPriorityMedium': 'Moyenne',
    'deptMgmtPriorityHigh': 'Haute',
    'deptMgmtDeadline': 'Échéance (AAAA-MM-JJ, optionnel)',
    'deptMgmtTaskCreateError': 'Échec de la création de la tâche',
    'deptMgmtAssignmentsTitle': 'Affectations actives',
    'deptMgmtAssignmentsEmpty': 'Aucune affectation active',
    'deptMgmtAssignMember': 'Affecter un membre',
    'deptMgmtRoleChef': 'Chef',
    'deptMgmtRoleAdjunct': 'Adjoint',
    'deptMgmtAssignBtn': 'Affecter',
    'deptMgmtAssignError': 'Échec de l\'affectation',
    'deptMgmtEndAssignment': 'Mettre fin',
    'deptMgmtEventsTitle': 'Événements du département',
    'deptMgmtEventsUpcoming': 'À venir ({count})',
    'deptMgmtEventsPast': 'Passés ({count})',
    'deptMgmtEventsEmpty': 'Aucun événement planifié. Créez le premier événement de votre département.',
    'deptMgmtNewEvent': 'Nouvel événement du département',
    'deptMgmtTitleRequired': 'Le titre est requis',
    'deptMgmtEventCreateError': 'Échec de la création de l\'événement',
    'deptMgmtActivityEmpty': 'Aucune activité pour l\'instant',
    'deptMgmtSearchResults': '{count} résultat(s) pour « {query} »',
    'deptMgmtSearchNoResults': 'Aucun résultat pour « {query} »',
    'deptMgmtSearchHintDetail': 'Essayez un nom, un poste, une équipe, une tâche ou un événement.',
    'deptMgmtNoTeam': 'Sans équipe',
    'deptMgmtUnassigned': 'Non assignée',
    'secTitle': 'Sécurité & Paramètres',
    'secSessionSection': 'Session & Inactivité',
    'secAutoLogout': 'Déconnexion automatique',
    'secAfterInactivity': 'Après {min} minutes d\'inactivité',
    'secSessionDuration': 'Durée de la session :',
    'secBiometricSection': 'Authentification Biométrique',
    'secBiometricLogin': 'Connexion par biométrie',
    'secBiometricUnavailable': 'Non disponible sur cet appareil',
    'secBiometricError': 'Échec de l\'activation biométrique',
    'secDataSaverSection': 'Économiseur de données',
    'secDataSaverMode': 'Mode économiseur',
    'secNetworkLabel': 'Réseau : {label}',
    'secAutoMode': 'Mode automatique',
    'secAutoModeDesc': 'Active automatiquement sur données mobiles',
    'secImgLoading': 'Chargement images',
    'secCacheStrategy': 'Stratégie cache',
    'secRefreshInterval': 'Intervalle refresh',
    'secOrientationSection': 'Orientation de l\'écran',
    'secScreenshotSection': 'Protection contre les captures d\'écran',
    'secScreenshotToggle': 'Protection anti-capture d\'écran',
    'secScreenshotDesc': 'Empêche les captures d\'écran sur les écrans sensibles',
    'secAuditSection': 'Journal d\'audit mobile',
    'secAuditEvents': 'Événements enregistrés',
    'secAuditCount': '{count} événements dans le journal',
    'secAuditLogTitle': 'Journal d\'audit',
    'secAuditClose': 'Fermer',
    'secAuditExportCsv': 'Exporter CSV',
    'secAuditExported': 'Journal exporté ({count} caractères)',
    'secAuditClear': 'Effacer',
    'secAuditViewLog': 'Voir le journal',
    'secInfoPersisted': 'Ces paramètres sont sauvegardés localement et persistés entre les sessions.',
    'dictTitle': 'Dictionnaires',
    'dictEmpty': 'Aucun dictionnaire',
    'dictEntriesCount': '{count} entrée(s)',
    'integTitle': 'Intégrations',
    'integSmtp': 'SMTP / Email',
    'integStorage': 'Stockage / MinIO',
    'integJwt': 'JWT / Auth',
    'integRateLimiting': 'Rate Limiting',
    'integEnabled': 'Activé',
    'integDisabled': 'Désactivé',
    'integTestConn': 'Tester la connexion',
    'integTestResultOk': 'Connexion réussie',
    'integTestResultFail': 'Erreur: {msg}',
    'evtTitle': 'Événements',
    'evtStatTotal': 'Total',
    'evtStatUpcoming': 'À venir',
    'evtStatDone': 'Terminés',
    'evtFilterAll': 'Tous',
    'evtFilterUpcoming': 'À venir',
    'evtFilterOngoing': 'En cours',
    'evtEmpty': 'Aucun événement',
    'evtNew': 'Nouvel événement',
    'evtTitleRequired': 'Le titre est requis',
    'evtCreated': 'Événement créé',
    'evtCreateError': 'Erreur lors de la création',
    'evtInscrits': '{count}/{total} inscrits',
    'discTitle': 'Discipline',
    'discStatTotal': 'Total',
    'discStatOngoing': 'En cours',
    'discStatResolved': 'Résolus',
    'discFilterAll': 'Tous',
    'discFilterOngoing': 'En cours',
    'discFilterResolved': 'Résolus',
    'discCatAll': 'Toutes',
    'discSearchHint': 'Rechercher par nom, titre…',
    'discResolveTitle': 'Résoudre cet événement ?',
    'discResolveConfirm': 'Marquer "{title}" comme résolu ?',
    'discResolveAction': 'Résoudre',
    'discResolveSuccess': '✅ Événement résolu',
    'discResolveError': 'Échec de la résolution',
    'discDeleteTitle': 'Supprimer cet événement ?',
    'discDeleteConfirm': 'Supprimer définitivement "{title}" ?',
    'discDeleteAction': 'Supprimer',
    'discDeleteSuccess': 'Événement supprimé',
    'discDeleteError': 'Échec de la suppression',
    'discCreateTitle': 'Nouvel événement disciplinaire',
    'discMemberLabel': 'Membre *',
    'discSelectMember': 'Sélectionner un membre',
    'discCatLabel': 'Catégorie *',
    'discTypeLabel': 'Type *',
    'discTitleLabel': 'Titre *',
    'discDescLabel': 'Description',
    'discGravityLabel': 'Gravité',
    'discDateLabel': 'Date',
    'discCreateError': 'Échec de la création',
    'discCreateSuccess': '✅ Événement disciplinaire enregistré',
    'discCreateValidation': 'Sélectionnez un membre et saisissez un titre',
    'discResolved': 'Résolu',
    'discNewEvent': 'Nouvel événement',
    'discEmptySearch': 'Aucun résultat pour "{query}"',
    'discEmpty': 'Aucun événement disciplinaire',
    'discClearSearch': 'Effacer la recherche',
  };

  // ==================== ENGLISH ====================
  static const Map<String, String> _english = {
    'appTitle': 'Discipolat',
    'login': 'Login',
    'logout': 'Logout',
    'email': 'Email address',
    'password': 'Password',
    'signIn': 'Sign in',
    'signOut': 'Sign out',
    'save': 'Save',
    'cancel': 'Cancel',
    'delete': 'Delete',
    'edit': 'Edit',
    'add': 'Add',
    'search': 'Search',
    'loading': 'Loading...',
    'error': 'Error',
    'success': 'Success',
    'noData': 'No data',
    'confirm': 'Confirm',
    'back': 'Back',
    'close': 'Close',
    'yes': 'Yes',
    'no': 'No',
    'none': 'None',
    'all': 'All',
    'filter': 'Filter',
    'refresh': 'Refresh',
    'retry': 'Retry',
    'offline': 'Offline',
    'navDashboard': 'Dashboard',
    'navSouls': 'Souls',
    'navFamilies': 'Families',
    'navDepartments': 'Departments',
    'navReports': 'Reports',
    'navPrayers': 'Prayers',
    'navEvents': 'Events',
    'navAlerts': 'Alerts',
    'navMessages': 'Messages',
    'navNotifications': 'Notifications',
    'navProfile': 'Profile',
    'navSettings': 'Settings',
    'navUsers': 'Users',
    'navBadges': 'Badges',
    'navEvangelism': 'Evangelism',
    'navDiscipline': 'Discipline',
    'navPresence': 'Attendance',
    'navTrainings': 'Trainings',
    'navFinances': 'Finances',
    'navCommunications': 'Communications',
    'navMap': 'Map',
    'navSearch': 'Search',
    'navAdmin': 'Administration',
    'dashboardTitle': 'Dashboard',
    'totalMembers': 'Total members',
    'activeMembers': 'Active members',
    'presentToday': 'Present today',
    'absentToday': 'Absent today',
    'pendingReports': 'Pending reports',
    'activeAlerts': 'Active alerts',
    'weeklyAttendance': 'Weekly attendance',
    'soulsTitle': 'Souls list',
    'soulDetail': 'Soul detail',
    'addSoul': 'Add a soul',
    'firstName': 'First name',
    'lastName': 'Last name',
    'phone': 'Phone',
    'typeDisciple': 'Disciple type',
    'status': 'Status',
    'dateIntegration': 'Integration date',
    'lastContact': 'Last contact',
    'spiritualScore': 'Spiritual score',
    'presenceTitle': 'Attendance entry',
    'markPresent': 'Mark present',
    'markAbsent': 'Mark absent',
    'allPresent': 'All present',
    'allAbsent': 'All absent',
    'resetPresence': 'Reset',
    'submitPresence': 'Submit attendance',
    'presenceNote': 'Note',
    'presenceProgress': 'Progress',
    'reportsTitle': 'Reports',
    'submitReport': 'Submit report',
    'viewReport': 'View report',
    'exportPdf': 'Export PDF',
    'weekReport': 'Weekly report',
    'difficulties': 'Difficulties',
    'notes': 'Notes',
    'outings': 'Outings',
    'maintained': 'Maintained',
    'disciplineTitle': 'Discipline',
    'addEvent': 'Add event',
    'resolveEvent': 'Resolve',
    'category': 'Category',
    'severity': 'Severity',
    'eventTitle': 'Title',
    'description': 'Description',
    'resolved': 'Resolved',
    'inProgress': 'In progress',
    'prayersTitle': 'Prayers',
    'actionsDeGrace': 'Answers to prayer',
    'addPrayer': 'Add a prayer',
    'prayerAnswered': 'Prayer answered',
    'badgesTitle': 'Badges',
    'myBadges': 'My badges',
    'leaderboard': 'Leaderboard',
    'level': 'Level',
    'points': 'Points',
    'earnedDate': 'Date earned',
    'messagesTitle': 'Messages',
    'newMessage': 'New message',
    'typeMessage': 'Type your message...',
    'sendMessage': 'Send',
    'startConversation': 'Start a conversation',
    'noMessages': 'No messages',
    'eventsTitle': 'Events',
    'addEventTitle': 'New event',
    'eventDate': 'Date',
    'eventTime': 'Time',
    'participants': 'Participants',
    'rsvp': 'RSVP',
    'adminSettings': 'Admin settings',
    'churchName': 'Church name',
    'customFields': 'Custom fields',
    'dictionaries': 'Dictionaries',
    'integrations': 'Integrations',
    'tenants': 'Organizations',
    'platformPages': 'Platform pages',
    'evangelismTitle': 'Evangelism',
    'pipeline': 'Pipeline',
    'newContact': 'New contact',
    'followUp': 'Follow-up',
    'convert': 'Conversion',
    'trainingsTitle': 'Trainings',
    'addTraining': 'New training',
    'participants2': 'Participants',
    'completion': 'Completion',
    'financesTitle': 'Finances',
    'income': 'Income',
    'expenses': 'Expenses',
    'balance': 'Balance',
    'budget': 'Budget',
    'smartAlertsTitle': 'Smart Alerts',
    'runAnalysis': 'Run analysis',
    'analysisRunning': 'Analysis running...',
    'noActiveAlerts': 'No active alerts',
    'everythingOk': 'Everything is fine!',
    'syncPending': 'Pending sync',
    'syncing': 'Syncing...',
    'syncNow': 'Sync now',
    'itemsPending': 'items pending',
    'scanQrCode': 'Scan QR Code',
    'showQrCode': 'Show my QR Code',
    'qrScanSuccess': 'QR Code scanned successfully',
    'qrScanError': 'Error scanning QR Code',
    'sessionExpiringWarning': 'Session expires in 2 minutes. Tap to continue.',
    'syncingCount': 'Syncing… {count} items pending',
    'offlineCount': 'Offline — {count} items pending',
    'pageNotFound': 'Page not found',
    'pageNotFoundWithPath': 'The page "{path}" does not exist.',
    'pageNotFoundGeneric': 'The page you\'re looking for doesn\'t exist or has been moved.',
    'availablePages': 'AVAILABLE PAGES',
    'navHome': 'Home',
    'navMore': 'More',
    'quickLinkHome': 'Home',
    'quickLinkSouls': 'Souls',
    'quickLinkFamilies': 'Families',
    'quickLinkEvents': 'Events',
    'quickLinkReports': 'Reports',
    'quickLinkDocuments': 'Documents',
    'appTagline': 'Discipleship Management',
    'weeklyChallengesTitle': '🏆 Weekly challenges',
    'weeklyChallengesError': 'Unable to load challenges.',
    'weeklyChallengesEmpty': 'No active challenge right now.',
    'reverseMentoringTitle': '🔄 Reverse mentoring',
    'reverseMentoringError': 'Unable to load requests.',
    'reverseMentoringEmpty': 'No reverse mentoring request.',
    'volunteersTitle': '🤝 Volunteers',
    'volunteersError': 'Unable to load volunteers.',
    'volunteersEmpty': 'No volunteer registered.',
    'aiVisitNotesTitle': '📝 AI visit notes',
    'aiVisitNotesError': 'Unable to load AI notes.',
    'aiVisitNotesEmpty': 'No analysed visit note yet.',
    'engagementAnalyticsTitle': '📈 Engagement',
    'engagementAnalyticsError': 'Unable to load metrics.',
    'engagementAnalyticsEmpty': 'No metric recorded yet.',
    'intelligenceCenterTitle': "🏛️ Intelligence center",
    'intelligenceCenterError': 'Unable to load KPIs. The center must be initialised by an admin.',
    'intelligenceCenterAlerts': '{count} active alert(s)',
    'predictionsTitle': '🔮 AI predictions',
    'predictionsError': 'Unable to load predictions.',
    'predictionsEmpty': 'No prediction generated yet.',
    'successionTitle': '👑 Succession',
    'successionError': 'Unable to load succession plans.',
    'successionEmpty': 'No succession plan.',
    'spiritualChallengesTitle': '🔥 Spiritual challenges',
    'spiritualChallengesError': 'Unable to load spiritual challenges.',
    'spiritualChallengesEmpty': 'No ongoing challenge.',
    'personalObjectivesTitle': '🎯 Personal objectives',
    'personalObjectivesError': 'Unable to load your objectives.',
    'personalObjectivesEmpty': 'No objective defined.',
    'kpiNarrativeTitle': '📖 KPI narrative',
    'kpiNarrativeError': 'Unable to load KPI narratives.',
    'kpiNarrativeEmpty': 'No narrative generated yet. Use "Generate" on the web app.',
    'rewardsTitle': '🏅 My rewards',
    'rewardsError': 'Unable to load your rewards.',
    'rewardsEmpty': 'No certificate yet. Keep up the effort!',
    'aiMentoringTitle': '🧠 AI mentoring',
    'aiMentoringError': 'Unable to load suggestions.',
    'aiMentoringEmpty': 'No suggestion. Generate some from the web app.',
    'familyMeetingTitle': '👨‍👩‍👧 Family meetings',
    'familyMeetingError': 'Unable to load meetings.',
    'familyMeetingEmpty': 'No meeting scheduled.',
    'eventChecklistTitle': '✅ Checklists',
    'eventChecklistError': 'Unable to load checklists.',
    'eventChecklistEmpty': 'No checklist task.',
    'eventChecklistProgress': '{done} / {total} tasks completed',
    'churchComparisonTitle': '⚖️ Comparison',
    'churchComparisonError': 'Unable to load comparisons.',
    'churchComparisonEmpty': 'No comparison recorded.',
    'adminRequestsTitle': '📋 Admin requests',
    'adminRequestsError': 'Unable to load requests.',
    'adminRequestsEmpty': 'No request.',
    'demoDataBanner': 'Preview — demo data, not yet connected to your church.',
    'streamingTitle': 'Streaming & Live',
    'streamingError': 'Unable to load streams.',
    'liveBadge': 'LIVE',
    'statLive': 'Live',
    'statViewers': 'Viewers',
    'statStreams': 'Streams',
    'upcomingStreams': 'Upcoming streams',
    'streamsEmpty': 'No stream scheduled yet.',
    'scheduledAt': 'Scheduled — {date}',
    'endedLabel': 'Ended',
    'inventoryTitle': 'Inventory',
    'inventoryError': 'Unable to load inventory.',
    'inventoryEmpty': 'No item registered.',
    'lowStockBanner': '{count} low-stock item(s)',
    'searchItemHint': 'Search an item...',
    'unitsCount': '{count} units',
    'lowStockTag': 'Low stock',
    'marketplaceTitle': 'Marketplace',
    'marketplaceError': 'Unable to load the marketplace.',
    'marketplaceEmpty': 'No listing yet.',
    'searchHint': 'Search...',
    'filterAll': 'All',
    'filterOffers': 'Offers',
    'filterRequests': 'Requests',
    'filterServices': 'Services',
    'filterFree': 'Free',
    'contact': 'Contact',
    'sellerLabel': 'Seller',
    'sellerWithId': 'Seller #{id}',
    'moderationTitle': 'AI moderation',
    'moderationError': 'Unable to load the moderation queue.',
    'moderationEmpty': 'No content to moderate.',
    'approve': 'Approve',
    'reject': 'Reject',
    'reviewDone': 'Content {decision}',
    'reviewError': 'Error while moderating',
    'predictionsMlTitle': 'ML predictions',
    'predictionsMlEmpty': 'No prediction available.',
    'predictedValue': 'Predicted: {value}',
    'executiveInsightsTitle': 'AI executive insights',
    'executiveInsightsError': 'Unable to load insights.',
    'executiveInsightsEmpty': 'No active insight.',
    'prayerJournalTitle': 'Prayer journal',
    'prayerJournalEmpty': 'Start writing your prayers',
    'statTotal': 'Total',
    'statOngoing': 'Ongoing',
    'statAnswered': 'Answered',
    'newPrayer': 'New prayer',
    'yourPrayer': 'Your prayer',
    'categoryLabel': 'Category',
    'catPrayer': 'Prayer',
    'catPraise': 'Praise',
    'catIntercession': 'Intercession',
    'catGrace': 'Thanksgiving',
    'digitalTwinTitle': '🔮 Digital Twin',
    'quickScenarios': '⚡ Quick scenarios',
    'scenarioStagnation': 'Stagnation',
    'scenarioSoftGrowth': 'Soft growth',
    'scenarioMakersAwakening': 'Makers awakening',
    'scenarioAwakeningRetention': 'Awakening + retention',
    'scenarioSpiritualAwakening': 'Spiritual awakening',
    'parameters': '⚙️ Parameters',
    'makerMultiplier': 'Maker multiplier',
    'retentionGain': 'Retention gain (%)',
    'pipelineBoost': 'Pipeline boost',
    'horizon': 'Horizon',
    'projectedStat': 'Projected',
    'soulsUnit': 'souls',
    'growthStat': 'Growth',
    'leadersNeeded': 'Leaders needed',
    'leadersSufficient': 'sufficient',
    'leadersMissing': '+{gap} missing',
    'monthlyProjection': '📈 Month-by-month projection',
    'monthLabel': 'Month {n}',
    'dataMigrationTitle': 'Data migration',
    'importData': 'Import data',
    'importDataHint': 'Import your members from Excel or CSV. The mapping assistant detects columns automatically.',
    'selectFile': 'Select a file',
    'aiAnalysis': 'AI analysis',
    'detectedFile': 'Detected file',
    'detectedRows': 'Detected rows',
    'mappedColumns': 'Mapped columns',
    'mappingConfidence': 'Mapping confidence',
    'launchMigration': 'Run migration',
    'previousMigrations': 'Previous migrations',
    'noMigrations': 'No migration performed yet',
    'rerun': 'Re-run',
    'migrationLaunched': 'Migration started ✅',
    'migrationError': 'Migration error',
    'rowsCount': '{count} rows',
    'surveysTitle': 'Surveys',
    'surveysEmpty': 'No survey',
    'responsesCount': '{count} responses',
    'encouragementsTitle': 'Encouragements',
    'tabReceived': 'Received ({count})',
    'tabSent': 'Sent ({count})',
    'tabTeam': 'My team ({count})',
    'emptyReceivedEnc': 'No encouragement received',
    'emptySentEnc': 'No encouragement sent',
    'emptyTeam': 'No member in your team',
    'composeEncouragement': 'Send an encouragement',
    'encTypePrayer': '🙏 Prayer',
    'encTypePraise': '⭐ Praise',
    'encTypeThanks': '❤️ Thanks',
    'encTypeSupport': '💪 Support',
    'encTypeWelcome': '👋 Welcome',
    'encTypeScripture': '📖 Scripture',
    'writeEncouragementHint': 'Write your encouragement...',
    'encouragementSent': 'Encouragement sent 🙏',
    'send': 'Send',
    'encouragementsReceived': '{count} encouragements received',
    'followUpTitle': '🤝 Follow-up requests',
    'fuTypeMaker': 'Request a maker',
    'fuTypeSpiritual': 'Spiritual accompaniment',
    'fuTypePastoral': 'Pastoral counsel',
    'newFollowUpRequest': 'New follow-up request',
    'describeNeed': 'Describe your need…',
    'requestSent': 'Request sent ✅',
    'requestFailed': 'Failed to send',
    'myRequests': 'My requests ({count})',
    'assignedToMe': 'Assigned to me ({count})',
    'emptyMyRequests': 'No request yet. Tap "Request" to start.',
    'emptyAssignedRequests': 'No request assigned to you.',
    'markComplete': 'Mark as completed',
    'askAction': 'Request',
    'neighborhoodHealthTitle': '💗 Neighborhood health',
    'neighborhoodEmpty': 'No zone defined. Fill in the "zone" field of the souls.',
    'soulsInZone': '{count} souls',
    'scoreLabel': 'Score: {score}/100',
    'recentContacts': '{count} recent contacts',
    'sabbathTitle': '🕊️ Sabbath dashboard',
    'globalMaturity': 'Global spiritual maturity',
    'activeSouls': 'Active souls',
    'activeMakers': 'Active makers',
    'familiesAtRisk': 'Families at risk',
    'twelveAxes': 'The 12 maturity axes',
    // ---- Batch H1: historic screens (admin custom fields, transfer workflows) ----
    'newField': 'New field',
    'fieldNameLabel': 'Field name',
    'fieldEntityLabel': 'Entity (SOUL, USER, DEPARTMENT…)',
    'fieldTypeLabel': 'Type (TEXT, NUMBER, DATE, SELECT, BOOLEAN)',
    'create': 'Create',
    'noCustomFields': 'No custom fields',
    'deleteQuestion': 'Delete?',
    'transferWorkflowTitle': 'Transfer workflow',
    'noConfiguration': 'No configuration',
    'statusActive': 'Active',
    'statusInactive': 'Inactive',
    'stepsCount': '{count} step(s)',
    'initiatorRoles': 'Initiator roles',
    'validationMode': 'Validation mode',
    'requiredValidations': 'Required validations',
    'delayHours': 'Delay (hours)',
    'circuitSteps': 'Circuit steps',
    'stepLabel': 'Step {index} — {label}',
    'configSaved': 'Configuration saved',
    'saveFailed': 'Error while saving',
    'deleteConfigQuestion': 'Delete this configuration?',
    'deleteBlockedByRequests': 'Cannot delete: requests use this configuration',
    // ---- Batch H1b: member requests + security ----
    'memberRequestsTitle': 'Requests',
    'tabMyRequests': 'My requests',
    'tabInbox': 'Received',
    'newRequest': 'New request',
    'requestType': 'Type',
    'recipient': 'Recipient',
    'subjectOptional': 'Subject (optional)',
    'messageHint': 'Message...',
    'attachments': 'Attachments',
    'sendFailed': 'Error while sending',
    'noSentRequests': 'No sent requests',
    'noReceivedRequests': 'No received requests',
    'fromLabel': 'From: {name}',
    'resolve': 'Resolve',
    'securityTitle': 'Security & privacy',
    'authSection': 'Authentication',
    'biometricAuth': 'Biometric authentication',
    'biometricSubtitle': 'Fingerprints or Face ID',
    'pinCode': 'PIN code',
    'pinSubtitle': 'Set up a backup PIN code',
    'changePinTitle': 'Change PIN code',
    'currentPin': 'Current PIN code',
    'newPin': 'New PIN code',
    'pinUpdated': 'PIN code updated',
    'pinIncorrect': 'Incorrect PIN code',
    'sessionSection': 'Session',
    'sessionExpiry': 'Session expiry',
    'sessionExpirySubtitle': 'Sign out after {value} of inactivity',
    'never': 'Never',
    'minutesCount': '{count} minutes',
    'screenProtection': 'Screen protection',
    'screenProtectionSubtitle': 'Prevent screenshots',
    'auditSection': 'Audit & activity',
    'auditLogEntries': 'Audit log ({count} entries)',
    'viewAuditSubtitle': 'Review recorded actions',
    'exportLog': 'Export log',
    'exportSubtitle': 'CSV or JSON for archiving',
    'clearLog': 'Clear log',
    'clearSubtitle': 'Delete all entries (GDPR)',
    'accountSection': 'Account information',
    'userLabel': 'User',
    'orgLabel': 'Organization',
    'activeRoleLabel': 'Active role',
    'rolesLabel': 'Roles',
    'noEntries': 'No entries',
    'logExported': 'Log exported',
    'entriesExported': '{count} entries exported',
    'clearLogQuestion': 'Clear the audit log?',
    'clearAction': 'Clear',
    'churchSettings': 'Church settings',
    'identity': 'Identity',
    'slogan': 'Slogan',
    'colorsSection': 'Colors',
    'primaryColorLabel': 'Primary color',
    'accentColorLabel': 'Accent color',
    'settingsSaved': '✅ Settings saved',
    'tenantsTitle': 'Churches (tenants)',
    'noTenants': 'No churches configured',
    'statusSuspended': 'Suspended',
    'growthProjectionTitle': '📊 Growth projection',
    'growthProphecyTitle': 'Growth prophecy (real analysis)',
    'projectedAnnualGrowth': 'Projected annual growth: {value} %',
    'headcountIn12Months': 'Headcount in 12 months: {value}',
    'leadersNeededCount': 'New leaders needed: {value}',
    'simulator': 'Simulator',
    'annualGrowthRate': 'Annual growth rate (%)',
    'horizonMonths': 'Horizon (months)',
    'simulate': 'Simulate',
    'simulationSaved': 'Simulation saved ✅',
    'simulationFailed': 'Simulation failed',
    'savedProjections': 'Saved projections',
    'myTeamFamilyTitle': '👨‍👩‍👧‍👦 My team / my family',
    'teamTabMembers': 'Members ({count})',
    'teamTabReceived': 'Encouragements received ({count})',
    'encourageName': 'Encourage {name}',
    'noSpiritualFamily': 'You are not attached to any spiritual family.',
    'meLabel': '(me)',
    'encouragementsBadge': '{count} encouragements',
    'sendEncouragementTooltip': 'Send an encouragement',
    'noEncouragementsYet': 'No encouragements received yet.',
    'myActivities': 'My activities',
    'filterPresences': 'Presences',
    'filterEvents': 'Events',
    'filterNotes': 'Notes',
    'filterProgression': 'Progression',
    'noActivities': 'No activities',
    'presenceConfirmed': 'Presence confirmed',
    'absenceRecorded': 'Absence recorded',
    'weekOf': 'Week of',
    'makerNote': 'Maker note',
    'spiritualLevel': 'Spiritual level: {value}',
    'progressOverview': 'Global progression',
    'departmentStats': 'Department statistics',
    'kpiMembers': 'Members',
    'kpiActive': 'Active',
    'kpiNew': 'New',
    'kpiPresence': 'Attendance',
    'kpiOverdueTasks': 'Overdue tasks',
    'kpiTeams': 'Teams',
    'memberBreakdown': 'Member breakdown',
    'integrating': 'Onboarding',
    'standby': 'On standby',
    'droppedOut': 'Dropped out',
    'headcountEvolution': 'Headcount evolution (12 months)',
    'attendanceSection': 'Attendance',
    'present': 'Present',
    'absent': 'Absent',
    'tasksByStatus': 'Tasks by status',
    'disciplinaryCategory': 'Discipline by category',
    'noDisciplinary': 'No disciplinary events',
    'workloadPerMember': 'Workload per member',
    'noAssignedTasks': 'No assigned open tasks',
    'tasksUnit': 'tasks',
    'retardUnit': 'overdue',
    'organizationSection': 'Organization',
    'activeAssignments': 'Active assignments',
    'activePositions': 'Active positions',
    'evangelizationTitle': 'Evangelism · {count} souls',
    'funnelOfConversion': 'Conversion funnel',
    'clickStageToSeeSouls': 'Click a stage to see the souls',
    'soulsAtStage': '{count} soul(s)',
    'noSoulsAtStage': 'No souls at this stage',
    'advanceTo': 'Advance → {label}',
    'retreatLabel': '← Retreat {label}',
    'globalView': 'Global view',
    'sinceDate': 'Since {date}',
    'gamificationTitle': 'Gamification · {count} badges',
    'tabMyBadges': 'My badges',
    'tabLeaderboard': 'Leaderboard',
    'checkMyBadges': 'Check my badges',
    'progressionLabel': 'Progression',
    'percentCompleted': '{pct}% completed',
    'badgesEarned': '{count} badge(s) earned',
    'scoresPerCriteria': 'Scores per criteria',
    'earnedBadges': 'Earned badges ({count})',
    'toUnlock': 'To unlock',
    'noLeaderboard': 'No leaderboard available',
    'badgesUnit': 'badge(s)',
    'newBadgesEarned': '🎉 {count} new badge(s) earned!',
    'noNewBadges': 'No new badges for now',
    'checkBadgesError': 'Error while checking badges',
    'criteriaVisits': 'Visits',
    'criteriaInteractions': 'Interactions',
    'criteriaEvangelism': 'Evangelism',
    'criteriaAttendance': 'Attendance',
    'criteriaLoyalty': 'Loyalty',
    'tithesAndOfferings': 'Tithes & offerings',
    'giveNow': 'Give now',
    'amountXOF': 'Amount (XOF)',
    'invalidAmount': 'Invalid amount',
    'operatorLabel': 'Operator',
    'destinationLabel': 'Destination',
    'mobilePhoneOptional': 'Mobile Money phone (optional)',
    'paymentInitiated': 'Payment initiated — reference {ref}',
    'paymentFailed': 'Payment initiation failed',
    'paymentConfirmed': 'Payment confirmed — thank you for your donation!',
    'waitingConfirmation': 'Waiting for confirmation ({ref})…',
    'noDonationsYet': 'No donations recorded yet.\nMay the Lord bless your generosity!',
    'statusConfirmed': 'Confirmed',
    'statusPending': 'Pending',
    'statusFailed': 'Failed',
    'statusCancelled': 'Cancelled',
    'broadcastTitle': 'Broadcast',
    'newBroadcast': 'New broadcast',
    'sent': 'Sent',
    'readRate': 'Read',
    'recentBroadcasts': 'Recent broadcasts',
    'targeting': 'Targeting',
    'allMembers': 'All members',
    'byDepartment': 'By department',
    'byFamily': 'By family',
    'byRole': 'By role',
    'broadcastEmpty': 'No broadcasts',
    'broadcastError': 'Error loading broadcasts',
    'readLabel': 'read',
    'formsTitle': 'Forms',
    'createForm': 'Create a form',
    'createFormSubtitle': 'Drag & drop with logic conditions',
    'publishedForms': 'Published forms',
    'fieldText': 'Text',
    'fieldChoice': 'Multiple choice',
    'fieldDate': 'Date',
    'fieldFile': 'File',
    'fieldSignature': 'Signature',
    'fieldNote': 'Note',
    'formTitleLabel': 'Form title',
    'addFieldHint': 'Tap a field type to add it',
    'formSaved': 'Form saved!',
    'formsEmpty': 'No published forms',
    'formsError': 'Error loading forms',
    'churchDirectoryTitle': 'Church Directory',
    'searchMember': 'Search a member...',
    'publicProfile': 'Public profile',
    'privateProfile': 'Private profile',
    'directoryEmpty': 'No members in the directory',
    'directoryError': 'Error loading directory',
    'unknownFamily': 'Unknown family',
    'unknownRole': 'Unknown role',

    // ── Lot H6 : dashboards + transfers ──
    'dashGrowth': 'Growth',
    'dashPresenceAndReports': 'Presences & Reports',
    'dashGlobalPresence': 'Global attendance',
    'dashReportsSubmitted': 'Reports submitted',
    'dashCompletion': 'Completion',
    'dashActiveAlerts': 'active alert(s)',
    'attentionRequired': 'Attention required',
    'allUnderControl': 'All under control',
    'dashDepartments': 'Departments',
    'dashFamilies': 'Families',
    'dashMakers': 'Makers',
    'dashRiskFamilies': 'Families at risk',
    'dashShepherdsPilot': 'Shepherd\'s Dashboard',
    'dashMyFamily': 'My family',
    'dashMyMaker': 'My maker',
    'dashMyDepartments': 'My departments',
    'dashMyProgression': 'My progression',
    'dashMakerNotes': 'Maker notes',
    'dashRecentPresences': 'My recent presences',
    'dashUpcomingEvents': 'Upcoming events',
    'dashQuickActions': 'Quick actions',
    'dashMakersCount': 'Makers ({count})',
    'dashMakerWorkload': 'Maker workload',
    'dashDisciplesSplit': 'Disciple breakdown',
    'dashActiveAlertsTitle': 'Active alerts',
    'dashUpcomingVisits': 'Upcoming visits',
    'dashRecentPrayers': 'Recent prayers',
    'dashMySpace': 'My space',
    'dashCurrentLevel': 'Current level',
    'dashPresenceThisWeek': 'Presences this week',
    'dashNextStep': 'Next step: {value}',
    'dashCompanion': 'Companion',
    'dashChefLabel': 'Head: {name}',
    'dashRespLabel': 'Manager: {name}',
    'dashDeptToAdmin': 'Department to manage',
    'dashOverview': 'Overview',
    'dashMyDepartment': 'My department',
    'dashDeptManagement': 'Department management',
    'dashOverdueTasks': 'Overdue tasks',
    'dashBirthdays': 'Birthdays this month',
    'dashFollowUp': 'Follow-up this week',
    'dashDiscipline': 'Evaluations',
    'dashTransferRequests': 'Members wish to change family',
    'dashEventsAndPosts': 'Teams & Positions',
    'dashDeptManagementTitle': 'Management',
    'dashQRCheckin': 'QR Check-in',
    'dashReport': 'Report',
    'dashPresenceLabel': 'Attendance',
    'dashActiveLabel': 'Active',
    'dashNewLabel': 'New',
    'dashPresentLabel': 'Present',
    'dashAbsentLabel': 'Absent',
    'dashDroppedLabel': 'Dropped',
    'dashTransferLabel': 'Transfers',
    'dashTeamsLabel': 'Teams',
    'dashPresentShort': 'Present',
    'dashAbsentShort': 'Absent',
    'dashSurcharge': 'Overloaded',
    'dashLightLoad': 'Light',
    'dashNormalLoad': 'Normal',
    'dashSouls': 'souls',
    'dashConverti': 'Converted',
    'dashArrivant': 'Newcomer',
    'transferCreateTitle': 'Create transfer request',
    'transferDetailTitle': 'Transfer request',
    'transferType': 'Transfer type',
    'transferDeptConcerned': 'Department concerned',
    'transferDeptDestination': 'Destination department',
    'transferDeptRetrait': 'Withdrawal department',
    'transferFamilyConcerned': 'Family concerned',
    'transferFamilyDestination': 'Destination family',
    'transferMemberAdd': 'Member to add',
    'transferMemberRemove': 'Member to remove',
    'transferMakerToTransfer': 'Maker to transfer',
    'transferDiscipleToTransfer': 'Disciple to transfer',
    'transferJustification': 'Detailed justification *',
    'transferComments': 'Comments (optional)',
    'transferSubmitCreate': 'Create and submit',
    'transferSaveDraft': 'Save draft',
    'transferDraftSaved': 'Draft saved',
    'transferSubmitted': 'Request submitted to validation circuit',
    'transferIncomplete': 'Incomplete form',
    'transferCreateError': 'Error creating request',
    'transferChoose': 'Choose',
    'transferCurrentAffectation': 'Current assignment',
    'transferCircuit': 'Validation circuit',
    'transferApprove': 'Approve',
    'transferReject': 'Cancel',
    'transferModify': 'Edit',
    'transferAskInfo': 'Request information',
    'transferExecute': 'Execute',
    'transferArchive': 'Archive',
    'transferHistory': 'History',
    'transferDecisions': 'Decisions',
    'transferStep': 'Step',
    'transferInfo': 'Info',
    'transferJustificationLabel': 'Justification',
    'transferMotivation': 'Motivation *',
    'transferMotivationOptional': 'Motivation (optional)',
    'transferCommentsLabel': 'Comments:',
    'transferConfirm': 'Confirm',
    'transferCorrection': 'Correction',
    'transferCancelQuestion': 'Cancel the request?',
    'transferCancelExplanation': 'The request will be sent back to the requester.',
    'transferNoDocument': 'No documents in the Documents module.',
    'transferNoAttachment': 'No attachments.',
    'transferCreateDoc': 'Create a document',
    'transferNoEvent': 'No events',
    'transferOperationError': 'Operation failed',
    'transferAttachments': 'Attachments',
    'transferJustificationHint': 'Pastoral, organizational reasons...',
    'transferPriority': 'Priority',
    'transferPerson': 'Person',
    'transferSubmit': 'Submit',
    'transferCancel': 'Cancel',
    'transferConfirmBtn': 'Confirm',
    'transferApprovalExplanation': 'The request will proceed in the validation circuit.',
    'transferRejectionExplanation': 'The request will be permanently rejected and notified.',
    'transferAutoExecution': 'No validation required — automatic execution upon submission.',
    'transferAttachmentsCount': 'Attachments ({count})',
    'transferValidateCount': 'Validate ({count})',
    'transferAttachmentsUpdated': 'Attachments updated',
    'transferAttachmentsError': 'Error updating attachments',
    'transferListTitle': 'Transfers',
    'transferListNewRequest': 'Request',
    'transferListStatusFilter': 'Status',
    'transferListTypeFilter': 'Type',
    'transferListEmpty': 'No transfer request',
    'transferListPersonLabel': 'Person: {name}',
    'transferListValidations': '{approved}/{total} validations',
    'transferListSubmitError': 'Submission error',
    'transferListCancelSuccess': 'Request cancelled',
    'transferListConfigDefault': 'Configuration',
    'statusDraft': 'Draft',
    'statusPendingValidation': 'Pending validation',
    'statusApproval': 'Approval',
    'statusExecuted': 'Executed',
    'statusArchive': 'Archived',
    'statusRequestedInfo': 'Information requested',
    'bibleReadingTitle': 'Bible Reading Plan',
    'myProgress': 'My progress',
    'days': 'days',
    'consecutiveDays': 'consecutive days',
    'todaysReading': 'Today reading',
    'markAsRead': 'Mark as read',
    'addNote': 'Add a note',
    'availablePlans': 'Available plans',
    'noPlans': 'No plans available',
    'familySharing': 'Shared with my family',
    'devPlanTitle': 'My Development Plan',
    'globalProgress': 'Global progress',
    'activeObjectives': 'active objectives',
    'completedObjectives': 'Completed objectives',
    'spiritualJournalTitle': 'Spiritual Journal',
    'reflection': 'Reflection',
    'thanksgiving': 'Thanksgiving',
    'praise': 'Praise',
    'lesson': 'Lesson',
    'newEntry': 'New entry',
    'discipleshipPathTitle': 'Discipleship Path',
    'familyCohesionTitle': 'Family Cohesion',
    'cohesionScore': 'Cohesion score',
    'goodEffort': 'Good — Keep it up',
    'needsImprovement': 'Needs improvement',
    'networkFamilies': 'Network families',
    'noFamilies': 'No families',
    'familyResourcesTitle': 'Family Resources',
    'categories': 'Categories',
    'recentResources': 'Recent resources',
    'noResources': 'No resources',
    'makerTrackingTitle': 'My Maker Journey',
    'timeline': 'Timeline',
    'noTimeline': 'No activity',
    'sermonTranslationTitle': 'Sermon Translations',
    'translationInProgress': 'Translation in progress',
    'recentTranslations': 'Recent translations',
    'noTranslations': 'No translations',
    'skillMatchingTitle': 'Skill Matching',
    'launchAiMatching': 'Launch AI matching',
    'matchingSubtitle': 'Analyze skills vs needs',
    'proposals': 'Proposals',
    'noMatches': 'No proposals',
    'skillsMatrixTitle': 'Skills Matrix',
    'overview': 'Overview',
    'skills': 'Skills',
    'evaluatedMembers': 'Evaluated members',
    'gapsFound': 'Gaps found',
    'skillsByDepartment': 'Skills by department',
    'needsMoreMembers': 'needs additional members',
    'read': 'Read',
    'progression': 'Progression',
    'prayer': 'Prayer',
    'commTitle': 'Announcements',
    'commManagement': 'Announcement management',
    'commEmpty': 'No announcements. Create the first with the + button.',
    'commPublishedEmpty': 'No published announcements for you yet',
    'commNew': 'New announcement',
    'commEdit': 'Edit announcement',
    'commPublished': 'Published announcements',
    'commPublishSuccess': 'Announcement published and sent to {count} recipient(s)',
    'commPublishError': 'Error publishing',
    'commDeleteTitle': 'Delete announcement?',
    'commDeleted': 'Announcement deleted',
    'commDeleteError': 'Error deleting',
    'commCreated': 'Announcement created',
    'commModified': 'Announcement updated',
    'commSaveError': 'Failed to save',
    'commStatusDraft': 'Draft',
    'commStatusPublished': 'Published',
    'commStatusArchived': 'Archived',
    'commCibleAll': 'Whole church',
    'commCibleRole': 'By role',
    'commCibleFamily': 'By family',
    'commCibleDept': 'By department',
    'commBtnCreate': 'Create',
    'commBtnSave': 'Save',
    'commInputTitle': 'Title',
    'commInputContent': 'Content',
    'commInputCible': 'Target audience',
    'commInputFamily': 'Family',
    'commInputDept': 'Department',
    'complianceTitle': 'GDPR Compliance',
    'complianceVerifyAudit': 'Verify audit',
    'complianceTabOverview': 'Overview',
    'complianceTabRetention': 'Retention',
    'complianceTabAudit': 'Audit',
    'complianceTabPortability': 'Portability',
    'complianceStatPolicies': 'Active policies',
    'complianceStatConsents': 'Consents',
    'complianceStatAuditEntries': 'Audit entries',
    'complianceStatGdprRequests': 'GDPR requests',
    'complianceAuditIntegrity': 'Audit integrity',
    'complianceAuditValid': 'Chain intact — no tampering',
    'complianceAuditInvalid': 'Tampering detected',
    'complianceVerifySuccess': 'Chain valid — {count} entries verified',
    'complianceVerifyBroken': '{count} broken link(s) in chain!',
    'complianceVerifyImpossible': 'Verification impossible',
    'complianceExportSuccess': 'Export downloaded ({count} sections)',
    'complianceExportError': 'Export error',
    'complianceChecklist': 'Compliance checklist',
    'complianceCheckPolicy': 'Retention policy',
    'complianceCheckConsents': 'Consents collected',
    'complianceCheckAudit': 'Immutable audit trail',
    'complianceCheckPortability': '1-click portability',
    'complianceCheckRightToForget': 'Right to be forgotten',
    'complianceCheckEncryption': 'AES-256 encryption',
    'compliancePurgeTitle': 'Execute purge?',
    'compliancePurgeContent': 'Data exceeding the retention period will be processed.',
    'compliancePurgeAction': 'Purge',
    'compliancePurgeSuccess': 'Purge executed',
    'complianceRetentionTitle': 'Retention policies',
    'complianceRetentionEmpty': 'No policies configured',
    'complianceRetentionDurations': 'Suggested durations',
    'complianceActionAnonymize': 'Anonymize',
    'complianceActionArchive': 'Archive',
    'complianceAuditEmpty': 'No audit entries',
    'complianceExportTitle': 'Export portability',
    'complianceExportSubtitle': 'GDPR Art. 20 — JSON format',
    'complianceExportContent': 'Export content:',
    'complianceExportProfile': 'User profile',
    'complianceExportSouls': 'Related souls (disciples)',
    'complianceExportConsents': 'Consent history',
    'complianceExportGdpr': 'Past GDPR requests',
    'complianceExportMeta': 'Metadata (format, version)',
    'complianceExportBtn': 'Export my data',
    'onboardingSkip': 'Skip',
    'onboardingNext': 'Next',
    'onboardingStart': 'Get Started',
    'onboardingAR': 'AR Visit',
    'onboardingWelcomeTitle': 'Welcome to Discipolat',
    'onboardingWelcomeDesc': 'The platform that makes disciple tracking easy for your church.',
    'onboardingTrackingTitle': 'Real-time tracking',
    'onboardingTrackingDesc': 'Access your souls, reports and events from anywhere.',
    'onboardingOfflineTitle': 'Offline mode',
    'onboardingOfflineDesc': 'All critical features work without connection.',
    'onboardingNotificationsTitle': 'Smart notifications',
    'onboardingNotificationsDesc': 'Receive personalized alerts based on your role.',
    'onboardingSecurityTitle': 'Enhanced security',
    'onboardingSecurityDesc': 'Biometric authentication, secure session and data protection.',
    'deptMgmtTitle': 'Department management',
    'deptMgmtTooltipStats': 'Statistics',
    'deptMgmtTooltipTools': 'Reports · Checklists · Inventory',
    'deptMgmtSearchHint': 'Quick search: member, team, task…',
    'deptMgmtKpiTeams': 'Teams',
    'deptMgmtKpiPositions': 'Positions',
    'deptMgmtKpiAssigned': 'Assigned',
    'deptMgmtTabMembers': 'Members',
    'deptMgmtTabOrg': 'Organization',
    'deptMgmtTabTasks': 'Tasks',
    'deptMgmtTabAssignments': 'Assignments',
    'deptMgmtTabEvents': 'Events',
    'deptMgmtTabActivity': 'Activity',
    'deptMgmtMembersTitle': 'Department members ({count})',
    'deptMgmtMembersEmpty': 'No members in this department',
    'deptMgmtFamilyLabel': 'Family: {name}',
    'deptMgmtMakerLabel': 'Maker: {name}',
    'deptMgmtOrgTitle': 'Org chart',
    'deptMgmtOrgEmpty': 'No teams — create your first sub-department',
    'deptMgmtTeamPermanent': 'Permanent team',
    'deptMgmtTeamTemporary': 'Temporary',
    'deptMgmtSubDepartment': 'Sub-department',
    'deptMgmtMembersCount': '{count} members',
    'deptMgmtEventLabel': 'Event: {title}',
    'deptMgmtNewTeam': 'New team',
    'deptMgmtTeamNameLabel': 'Team name',
    'deptMgmtParentTeam': 'Parent team',
    'deptMgmtNoParent': '— None (root) —',
    'deptMgmtLinkedEvent': 'Linked event (optional)',
    'deptMgmtLoadingEvents': 'Loading events…',
    'deptMgmtNoEvent': '— No event —',
    'deptMgmtDateStart': 'Start date (YYYY-MM-DD)',
    'deptMgmtDateEnd': 'End date (YYYY-MM-DD)',
    'deptMgmtObjective': 'Objective (optional)',
    'deptMgmtTeamCreateError': 'Team creation failed',
    'deptMgmtTasksTitle': 'Department tasks',
    'deptMgmtStatInProgress': 'In progress',
    'deptMgmtStatTodo': 'To do',
    'deptMgmtStatOverdue': 'Overdue',
    'deptMgmtStatDone': 'Completed',
    'deptMgmtTaskEmpty': 'No tasks',
    'deptMgmtTaskLoadError': 'Loading error',
    'deptMgmtNewTask': 'New task',
    'deptMgmtAssignedTo': 'Assigned to',
    'deptMgmtNoAssignee': '— Unassigned —',
    'deptMgmtPriority': 'Priority',
    'deptMgmtPriorityLow': 'Low',
    'deptMgmtPriorityMedium': 'Medium',
    'deptMgmtPriorityHigh': 'High',
    'deptMgmtDeadline': 'Deadline (YYYY-MM-DD, optional)',
    'deptMgmtTaskCreateError': 'Task creation failed',
    'deptMgmtAssignmentsTitle': 'Active assignments',
    'deptMgmtAssignmentsEmpty': 'No active assignments',
    'deptMgmtAssignMember': 'Assign a member',
    'deptMgmtRoleChef': 'Leader',
    'deptMgmtRoleAdjunct': 'Deputy',
    'deptMgmtAssignBtn': 'Assign',
    'deptMgmtAssignError': 'Assignment failed',
    'deptMgmtEndAssignment': 'End',
    'deptMgmtEventsTitle': 'Department events',
    'deptMgmtEventsUpcoming': 'Upcoming ({count})',
    'deptMgmtEventsPast': 'Past ({count})',
    'deptMgmtEventsEmpty': 'No events planned. Create the first event for your department.',
    'deptMgmtNewEvent': 'New department event',
    'deptMgmtTitleRequired': 'Title is required',
    'deptMgmtEventCreateError': 'Event creation failed',
    'deptMgmtActivityEmpty': 'No activity yet',
    'deptMgmtSearchResults': '{count} result(s) for "{query}"',
    'deptMgmtSearchNoResults': 'No results for "{query}"',
    'deptMgmtSearchHintDetail': 'Try a name, position, team, task, or event.',
    'deptMgmtNoTeam': 'No team',
    'deptMgmtUnassigned': 'Unassigned',
    'secTitle': 'Security & Settings',
    'secSessionSection': 'Session & Inactivity',
    'secAutoLogout': 'Auto logout',
    'secAfterInactivity': 'After {min} min of inactivity',
    'secSessionDuration': 'Session duration:',
    'secBiometricSection': 'Biometric Authentication',
    'secBiometricLogin': 'Biometric login',
    'secBiometricUnavailable': 'Not available on this device',
    'secBiometricError': 'Biometric activation failed',
    'secDataSaverSection': 'Data Saver',
    'secDataSaverMode': 'Saver mode',
    'secNetworkLabel': 'Network: {label}',
    'secAutoMode': 'Auto mode',
    'secAutoModeDesc': 'Auto-activates on mobile data',
    'secImgLoading': 'Image loading',
    'secCacheStrategy': 'Cache strategy',
    'secRefreshInterval': 'Refresh interval',
    'secOrientationSection': 'Screen orientation',
    'secScreenshotSection': 'Screenshot protection',
    'secScreenshotToggle': 'Screenshot protection',
    'secScreenshotDesc': 'Prevents screenshots on sensitive screens',
    'secAuditSection': 'Mobile audit log',
    'secAuditEvents': 'Recorded events',
    'secAuditCount': '{count} events in log',
    'secAuditLogTitle': 'Audit log',
    'secAuditClose': 'Close',
    'secAuditExportCsv': 'Export CSV',
    'secAuditExported': 'Log exported ({count} chars)',
    'secAuditClear': 'Clear',
    'secAuditViewLog': 'View log',
    'secInfoPersisted': 'Settings are saved locally and persist between sessions.',
    'dictTitle': 'Dictionaries',
    'dictEmpty': 'No dictionaries',
    'dictEntriesCount': '{count} entry/entries',
    'integTitle': 'Integrations',
    'integSmtp': 'SMTP / Email',
    'integStorage': 'Storage / MinIO',
    'integJwt': 'JWT / Auth',
    'integRateLimiting': 'Rate Limiting',
    'integEnabled': 'Enabled',
    'integDisabled': 'Disabled',
    'integTestConn': 'Test connection',
    'integTestResultOk': 'Connection successful',
    'integTestResultFail': 'Error: {msg}',
    'evtTitle': 'Events',
    'evtStatTotal': 'Total',
    'evtStatUpcoming': 'Upcoming',
    'evtStatDone': 'Completed',
    'evtFilterAll': 'All',
    'evtFilterUpcoming': 'Upcoming',
    'evtFilterOngoing': 'In progress',
    'evtEmpty': 'No events',
    'evtNew': 'New event',
    'evtTitleRequired': 'Title is required',
    'evtCreated': 'Event created',
    'evtCreateError': 'Creation error',
    'evtInscrits': '{count}/{total} registered',
    'discTitle': 'Discipline',
    'discStatTotal': 'Total',
    'discStatOngoing': 'In progress',
    'discStatResolved': 'Resolved',
    'discFilterAll': 'All',
    'discFilterOngoing': 'In progress',
    'discFilterResolved': 'Resolved',
    'discCatAll': 'All',
    'discSearchHint': 'Search by name, title…',
    'discResolveTitle': 'Resolve this event?',
    'discResolveConfirm': 'Mark "{title}" as resolved?',
    'discResolveAction': 'Resolve',
    'discResolveSuccess': 'Event resolved',
    'discResolveError': 'Resolution failed',
    'discDeleteTitle': 'Delete this event?',
    'discDeleteConfirm': 'Permanently delete "{title}"?',
    'discDeleteAction': 'Delete',
    'discDeleteSuccess': 'Event deleted',
    'discDeleteError': 'Deletion failed',
    'discCreateTitle': 'New disciplinary event',
    'discMemberLabel': 'Member *',
    'discSelectMember': 'Select a member',
    'discCatLabel': 'Category *',
    'discTypeLabel': 'Type *',
    'discTitleLabel': 'Title *',
    'discDescLabel': 'Description',
    'discGravityLabel': 'Severity',
    'discDateLabel': 'Date',
    'discCreateError': 'Creation failed',
    'discCreateSuccess': 'Disciplinary event recorded',
    'discCreateValidation': 'Select a member and enter a title',
    'discResolved': 'Resolved',
    'discNewEvent': 'New event',
    'discEmptySearch': 'No results for "{query}"',
    'discEmpty': 'No disciplinary events',
    'discClearSearch': 'Clear search',
  };

  // ==================== PORTUGUESE ====================
  static const Map<String, String> _portuguese = {
    'appTitle': 'Discipolat',
    'login': 'Entrar',
    'logout': 'Sair',
    'email': 'Endereço de e-mail',
    'password': 'Senha',
    'signIn': 'Entrar',
    'signOut': 'Sair',
    'save': 'Salvar',
    'cancel': 'Cancelar',
    'delete': 'Excluir',
    'edit': 'Editar',
    'add': 'Adicionar',
    'search': 'Pesquisar',
    'loading': 'Carregando...',
    'error': 'Erro',
    'success': 'Sucesso',
    'noData': 'Sem dados',
    'confirm': 'Confirmar',
    'back': 'Voltar',
    'close': 'Fechar',
    'yes': 'Sim',
    'no': 'Não',
    'none': 'Nenhum',
    'all': 'Todos',
    'filter': 'Filtrar',
    'refresh': 'Atualizar',
    'retry': 'Tentar novamente',
    'offline': 'Offline',
    'navDashboard': 'Painel',
    'navSouls': 'Almas',
    'navFamilies': 'Famílias',
    'navDepartments': 'Departamentos',
    'navReports': 'Relatórios',
    'navPrayers': 'Orações',
    'navEvents': 'Eventos',
    'navAlerts': 'Alertas',
    'navMessages': 'Mensagens',
    'navNotifications': 'Notificações',
    'navProfile': 'Perfil',
    'navSettings': 'Configurações',
    'navUsers': 'Utilizadores',
    'navBadges': 'Emblemas',
    'navEvangelism': 'Evangelismo',
    'navDiscipline': 'Disciplina',
    'navPresence': 'Presenças',
    'navTrainings': 'Formações',
    'navFinances': 'Finanças',
    'navCommunications': 'Comunicações',
    'navMap': 'Mapa',
    'navSearch': 'Pesquisa',
    'navAdmin': 'Administração',
    'dashboardTitle': 'Painel de controlo',
    'totalMembers': 'Total de membros',
    'activeMembers': 'Membros ativos',
    'presentToday': 'Presentes hoje',
    'absentToday': 'Ausentes hoje',
    'pendingReports': 'Relatórios pendentes',
    'activeAlerts': 'Alertas ativos',
    'weeklyAttendance': 'Frequência semanal',
    'soulsTitle': 'Lista de almas',
    'soulDetail': 'Ficha da alma',
    'addSoul': 'Adicionar alma',
    'firstName': 'Nome',
    'lastName': 'Apelido',
    'phone': 'Telefone',
    'typeDisciple': 'Tipo de discípulo',
    'status': 'Estado',
    'dateIntegration': 'Data de integração',
    'lastContact': 'Último contato',
    'spiritualScore': 'Pontuação espiritual',
    'presenceTitle': 'Registo de presenças',
    'markPresent': 'Marcar presente',
    'markAbsent': 'Marcar ausente',
    'allPresent': 'Todos presentes',
    'allAbsent': 'Todos ausentes',
    'resetPresence': 'Reiniciar',
    'submitPresence': 'Submeter presenças',
    'presenceNote': 'Nota',
    'presenceProgress': 'Progresso',
    'reportsTitle': 'Relatórios',
    'submitReport': 'Submeter relatório',
    'viewReport': 'Ver relatório',
    'exportPdf': 'Exportar PDF',
    'weekReport': 'Relatório semanal',
    'difficulties': 'Dificuldades',
    'notes': 'Notas',
    'outings': 'Saídas',
    'maintained': 'Mantidos',
    'disciplineTitle': 'Disciplina',
    'addEvent': 'Adicionar evento',
    'resolveEvent': 'Resolver',
    'category': 'Categoria',
    'severity': 'Gravidade',
    'eventTitle': 'Título',
    'description': 'Descrição',
    'resolved': 'Resolvido',
    'inProgress': 'Em andamento',
    'prayersTitle': 'Orações',
    'actionsDeGrace': 'Ações de graça',
    'addPrayer': 'Adicionar oração',
    'prayerAnswered': 'Oração atendida',
    'badgesTitle': 'Emblemas',
    'myBadges': 'Meus emblemas',
    'leaderboard': 'Classificação',
    'level': 'Nível',
    'points': 'Pontos',
    'earnedDate': 'Data de obtenção',
    'messagesTitle': 'Mensagens',
    'newMessage': 'Nova mensagem',
    'typeMessage': 'Digite sua mensagem...',
    'sendMessage': 'Enviar',
    'startConversation': 'Iniciar conversa',
    'noMessages': 'Sem mensagens',
    'eventsTitle': 'Eventos',
    'addEventTitle': 'Novo evento',
    'eventDate': 'Data',
    'eventTime': 'Hora',
    'participants': 'Participantes',
    'rsvp': 'Confirmar presença',
    'adminSettings': 'Configurações admin',
    'churchName': 'Nome da igreja',
    'customFields': 'Campos personalizados',
    'dictionaries': 'Dicionários',
    'integrations': 'Integrações',
    'tenants': 'Organizações',
    'platformPages': 'Páginas da plataforma',
    'evangelismTitle': 'Evangelismo',
    'pipeline': 'Pipeline',
    'newContact': 'Novo contato',
    'followUp': 'Acompanhamento',
    'convert': 'Conversão',
    'trainingsTitle': 'Formações',
    'addTraining': 'Nova formação',
    'participants2': 'Participantes',
    'completion': 'Conclusão',
    'financesTitle': 'Finanças',
    'income': 'Receitas',
    'expenses': 'Despesas',
    'balance': 'Saldo',
    'budget': 'Orçamento',
    'smartAlertsTitle': 'Alertas inteligentes',
    'runAnalysis': 'Executar análise',
    'analysisRunning': 'Análise em execução...',
    'noActiveAlerts': 'Sem alertas ativos',
    'everythingOk': 'Tudo está bem!',
    'syncPending': 'Sincronização pendente',
    'syncing': 'Sincronizando...',
    'syncNow': 'Sincronizar',
    'itemsPending': 'itens pendentes',
    'scanQrCode': 'Escanear QR Code',
    'showQrCode': 'Mostrar meu QR Code',
    'qrScanSuccess': 'QR Code escaneado com sucesso',
    'qrScanError': 'Erro ao escanear QR Code',
    'sessionExpiringWarning': 'A sessão expira em 2 minutos. Toque para continuar.',
    'syncingCount': 'Sincronizando… {count} itens pendentes',
    'offlineCount': 'Offline — {count} itens pendentes',
    'pageNotFound': 'Página não encontrada',
    'pageNotFoundWithPath': 'A página "{path}" não existe.',
    'pageNotFoundGeneric': 'A página que procura não existe ou foi movida.',
    'availablePages': 'PÁGINAS DISPONÍVEIS',
    'navHome': 'Início',
    'navMore': 'Mais',
    'quickLinkHome': 'Início',
    'quickLinkSouls': 'Almas',
    'quickLinkFamilies': 'Famílias',
    'quickLinkEvents': 'Eventos',
    'quickLinkReports': 'Relatórios',
    'quickLinkDocuments': 'Documentos',
    'appTagline': 'Gestão do Discipolado',
    'weeklyChallengesTitle': '🏆 Desafios semanais',
    'weeklyChallengesError': 'Não foi possível carregar os desafios.',
    'weeklyChallengesEmpty': 'Nenhum desafio ativo no momento.',
    'reverseMentoringTitle': '🔄 Mentoria invertida',
    'reverseMentoringError': 'Não foi possível carregar os pedidos.',
    'reverseMentoringEmpty': 'Nenhum pedido de mentoria invertida.',
    'volunteersTitle': '🤝 Voluntários',
    'volunteersError': 'Não foi possível carregar os voluntários.',
    'volunteersEmpty': 'Nenhum voluntário registado.',
    'aiVisitNotesTitle': '📝 Notas IA de visitas',
    'aiVisitNotesError': 'Não foi possível carregar as notas IA.',
    'aiVisitNotesEmpty': 'Nenhuma nota de visita analisada.',
    'engagementAnalyticsTitle': '📈 Envolvimento',
    'engagementAnalyticsError': 'Não foi possível carregar as métricas.',
    'engagementAnalyticsEmpty': 'Nenhuma métrica registada.',
    'intelligenceCenterTitle': '🏛️ Centro de inteligência',
    'intelligenceCenterError': 'Não foi possível carregar os KPIs. O centro deve ser inicializado pelo admin.',
    'intelligenceCenterAlerts': '{count} alerta(s) ativo(s)',
    'predictionsTitle': '🔮 Previsões IA',
    'predictionsError': 'Não foi possível carregar as previsões.',
    'predictionsEmpty': 'Nenhuma previsão gerada.',
    'successionTitle': '👑 Sucessão',
    'successionError': 'Não foi possível carregar os planos de sucessão.',
    'successionEmpty': 'Nenhum plano de sucessão.',
    'spiritualChallengesTitle': '🔥 Desafios espirituais',
    'spiritualChallengesError': 'Não foi possível carregar os desafios espirituais.',
    'spiritualChallengesEmpty': 'Nenhum desafio em curso.',
    'personalObjectivesTitle': '🎯 Objetivos pessoais',
    'personalObjectivesError': 'Não foi possível carregar os seus objetivos.',
    'personalObjectivesEmpty': 'Nenhum objetivo definido.',
    'kpiNarrativeTitle': '📖 Narração dos KPIs',
    'kpiNarrativeError': 'Não foi possível carregar as narrações KPI.',
    'kpiNarrativeEmpty': 'Nenhuma narração gerada. Use « Gerar » na aplicação web.',
    'rewardsTitle': '🏅 As minhas recompensas',
    'rewardsError': 'Não foi possível carregar as suas recompensas.',
    'rewardsEmpty': 'Ainda sem certificado. Continue os seus esforços!',
    'aiMentoringTitle': '🧠 Mentoria IA',
    'aiMentoringError': 'Não foi possível carregar as sugestões.',
    'aiMentoringEmpty': 'Nenhuma sugestão. Gere-as a partir da aplicação web.',
    'familyMeetingTitle': '👨‍👩‍👧 Reuniões de família',
    'familyMeetingError': 'Não foi possível carregar as reuniões.',
    'familyMeetingEmpty': 'Nenhuma reunião programada.',
    'eventChecklistTitle': '✅ Checklists',
    'eventChecklistError': 'Não foi possível carregar as checklists.',
    'eventChecklistEmpty': 'Nenhuma tarefa de checklist.',
    'eventChecklistProgress': '{done} / {total} tarefas concluídas',
    'churchComparisonTitle': '⚖️ Comparação',
    'churchComparisonError': 'Não foi possível carregar as comparações.',
    'churchComparisonEmpty': 'Nenhuma comparação registada.',
    'adminRequestsTitle': '📋 Pedidos administrativos',
    'adminRequestsError': 'Não foi possível carregar os pedidos.',
    'adminRequestsEmpty': 'Nenhum pedido.',
    'demoDataBanner': 'Pré-visualização — dados de demonstração, ainda não ligados à sua igreja.',
    'streamingTitle': 'Streaming & Live',
    'streamingError': 'Não foi possível carregar as transmissões.',
    'liveBadge': 'EM DIRETO',
    'statLive': 'Em direto',
    'statViewers': 'Espectadores',
    'statStreams': 'Streams',
    'upcomingStreams': 'Próximas transmissões',
    'streamsEmpty': 'Nenhuma transmissão programada.',
    'scheduledAt': 'Programado — {date}',
    'endedLabel': 'Terminado',
    'inventoryTitle': 'Inventário',
    'inventoryError': 'Não foi possível carregar o inventário.',
    'inventoryEmpty': 'Nenhum artigo registado.',
    'lowStockBanner': '{count} artigo(s) com stock baixo',
    'searchItemHint': 'Pesquisar um artigo...',
    'unitsCount': '{count} unidades',
    'lowStockTag': 'Stock baixo',
    'marketplaceTitle': 'Marketplace',
    'marketplaceError': 'Não foi possível carregar o marketplace.',
    'marketplaceEmpty': 'Nenhum anúncio por agora.',
    'searchHint': 'Pesquisar...',
    'filterAll': 'Tudo',
    'filterOffers': 'Ofertas',
    'filterRequests': 'Pedidos',
    'filterServices': 'Serviços',
    'filterFree': 'Grátis',
    'contact': 'Contactar',
    'sellerLabel': 'Vendedor',
    'sellerWithId': 'Vendedor #{id}',
    'moderationTitle': 'Moderação IA',
    'moderationError': 'Não foi possível carregar a fila de moderação.',
    'moderationEmpty': 'Nenhum conteúdo para moderar.',
    'approve': 'Aprovar',
    'reject': 'Rejeitar',
    'reviewDone': 'Conteúdo {decision}',
    'reviewError': 'Erro durante a moderação',
    'predictionsMlTitle': 'Previsões ML',
    'predictionsMlEmpty': 'Nenhuma previsão disponível.',
    'predictedValue': 'Previsto: {value}',
    'executiveInsightsTitle': 'Insights executivos IA',
    'executiveInsightsError': 'Não foi possível carregar os insights.',
    'executiveInsightsEmpty': 'Nenhum insight ativo.',
    'prayerJournalTitle': 'Diário de oração',
    'prayerJournalEmpty': 'Comece a escrever as suas orações',
    'statTotal': 'Total',
    'statOngoing': 'Em curso',
    'statAnswered': 'Atendidas',
    'newPrayer': 'Nova oração',
    'yourPrayer': 'A sua oração',
    'categoryLabel': 'Categoria',
    'catPrayer': 'Oração',
    'catPraise': 'Louvor',
    'catIntercession': 'Intercessão',
    'catGrace': 'Graças',
    'digitalTwinTitle': '🔮 Gémeo Digital',
    'quickScenarios': '⚡ Cenários rápidos',
    'scenarioStagnation': 'Estagnação',
    'scenarioSoftGrowth': 'Crescimento suave',
    'scenarioMakersAwakening': 'Despertar dos faiseurs',
    'scenarioAwakeningRetention': 'Despertar + retenção',
    'scenarioSpiritualAwakening': 'Despertar espiritual',
    'parameters': '⚙️ Parâmetros',
    'makerMultiplier': 'Multiplicador faiseurs',
    'retentionGain': 'Ganho de retenção (%)',
    'pipelineBoost': 'Boost pipeline',
    'horizon': 'Horizonte',
    'projectedStat': 'Projetado',
    'soulsUnit': 'almas',
    'growthStat': 'Crescimento',
    'leadersNeeded': 'Líderes necessários',
    'leadersSufficient': 'suficiente',
    'leadersMissing': '+{gap} em falta',
    'monthlyProjection': '📈 Projeção mês a mês',
    'monthLabel': 'Mês {n}',
    'dataMigrationTitle': 'Migração de dados',
    'importData': 'Importar dados',
    'importDataHint': 'Importe os seus membros a partir de Excel ou CSV. O assistente de mapeamento deteta as colunas automaticamente.',
    'selectFile': 'Selecionar um ficheiro',
    'aiAnalysis': 'Análise IA',
    'detectedFile': 'Ficheiro detetado',
    'detectedRows': 'Linhas detetadas',
    'mappedColumns': 'Colunas mapeadas',
    'mappingConfidence': 'Confiança do mapeamento',
    'launchMigration': 'Iniciar migração',
    'previousMigrations': 'Migrações anteriores',
    'noMigrations': 'Nenhuma migração efetuada',
    'rerun': 'Reiniciar',
    'migrationLaunched': 'Migração iniciada ✅',
    'migrationError': 'Erro durante a migração',
    'rowsCount': '{count} linhas',
    'surveysTitle': 'Inquéritos',
    'surveysEmpty': 'Nenhum inquérito',
    'responsesCount': '{count} respostas',
    'encouragementsTitle': 'Encorajamentos',
    'tabReceived': 'Recebidos ({count})',
    'tabSent': 'Enviados ({count})',
    'tabTeam': 'A minha equipa ({count})',
    'emptyReceivedEnc': 'Nenhum encorajamento recebido',
    'emptySentEnc': 'Nenhum encorajamento enviado',
    'emptyTeam': 'Nenhum membro na sua equipa',
    'composeEncouragement': 'Enviar um encorajamento',
    'encTypePrayer': '🙏 Oração',
    'encTypePraise': '⭐ Louvor',
    'encTypeThanks': '❤️ Obrigado',
    'encTypeSupport': '💪 Apoio',
    'encTypeWelcome': '👋 Boas-vindas',
    'encTypeScripture': '📖 Versículo',
    'writeEncouragementHint': 'Escreva o seu encorajamento...',
    'encouragementSent': 'Encorajamento enviado 🙏',
    'send': 'Enviar',
    'encouragementsReceived': '{count} encorajamentos recebidos',
    'followUpTitle': '🤝 Pedidos de acompanhamento',
    'fuTypeMaker': 'Pedir um faiseur',
    'fuTypeSpiritual': 'Acompanhamento espiritual',
    'fuTypePastoral': 'Aconselhamento pastoral',
    'newFollowUpRequest': 'Novo pedido de acompanhamento',
    'describeNeed': 'Descreva a sua necessidade…',
    'requestSent': 'Pedido enviado ✅',
    'requestFailed': 'Falha no envio',
    'myRequests': 'Os meus pedidos ({count})',
    'assignedToMe': 'Atribuídos a mim ({count})',
    'emptyMyRequests': 'Nenhum pedido. Toque em « Pedir » para começar.',
    'emptyAssignedRequests': 'Nenhum pedido lhe está atribuído.',
    'markComplete': 'Marcar como concluído',
    'askAction': 'Pedir',
    'neighborhoodHealthTitle': '💗 Saúde por bairro',
    'neighborhoodEmpty': 'Nenhuma zona definida. Preencha o campo « zona » das almas.',
    'soulsInZone': '{count} almas',
    'scoreLabel': 'Pontuação: {score}/100',
    'recentContacts': '{count} contactos recentes',
    'sabbathTitle': '🕊️ Painel sabático',
    'globalMaturity': 'Maturidade espiritual global',
    'activeSouls': 'Almas ativas',
    'activeMakers': 'Faiseurs ativos',
    'familiesAtRisk': 'Famílias em risco',
    'twelveAxes': 'Os 12 eixos de maturidade',
    // ---- Lote H1: ecrãs históricos (campos personalizados, fluxos de transferência) ----
    'newField': 'Novo campo',
    'fieldNameLabel': 'Nome do campo',
    'fieldEntityLabel': 'Entidade (SOUL, USER, DEPARTMENT…)',
    'fieldTypeLabel': 'Tipo (TEXT, NUMBER, DATE, SELECT, BOOLEAN)',
    'create': 'Criar',
    'noCustomFields': 'Nenhum campo personalizado',
    'deleteQuestion': 'Eliminar?',
    'transferWorkflowTitle': 'Fluxo de transferência',
    'noConfiguration': 'Nenhuma configuração',
    'statusActive': 'Ativo',
    'statusInactive': 'Inativo',
    'stepsCount': '{count} etapa(s)',
    'initiatorRoles': 'Papéis iniciadores',
    'validationMode': 'Modo de validação',
    'requiredValidations': 'Validações exigidas',
    'delayHours': 'Prazo (horas)',
    'circuitSteps': 'Etapas do circuito',
    'stepLabel': 'Etapa {index} — {label}',
    'configSaved': 'Configuração guardada',
    'saveFailed': 'Erro ao guardar',
    'deleteConfigQuestion': 'Eliminar esta configuração?',
    'deleteBlockedByRequests': 'Eliminação impossível: pedidos usam esta configuração',
    // ---- Lote H1b: pedidos de membros + segurança ----
    'memberRequestsTitle': 'Pedidos',
    'tabMyRequests': 'Meus pedidos',
    'tabInbox': 'Recebidos',
    'newRequest': 'Novo pedido',
    'requestType': 'Tipo',
    'recipient': 'Destinatário',
    'subjectOptional': 'Assunto (opcional)',
    'messageHint': 'Mensagem...',
    'attachments': 'Anexos',
    'sendFailed': 'Erro ao enviar',
    'noSentRequests': 'Nenhum pedido enviado',
    'noReceivedRequests': 'Nenhum pedido recebido',
    'fromLabel': 'De: {name}',
    'resolve': 'Resolver',
    'securityTitle': 'Segurança e privacidade',
    'authSection': 'Autenticação',
    'biometricAuth': 'Autenticação biométrica',
    'biometricSubtitle': 'Impressões digitais ou Face ID',
    'pinCode': 'Código PIN',
    'pinSubtitle': 'Configurar um código PIN de reserva',
    'changePinTitle': 'Alterar código PIN',
    'currentPin': 'Código PIN atual',
    'newPin': 'Novo código PIN',
    'pinUpdated': 'Código PIN atualizado',
    'pinIncorrect': 'Código PIN incorreto',
    'sessionSection': 'Sessão',
    'sessionExpiry': 'Expiração da sessão',
    'sessionExpirySubtitle': 'Desconexão após {value} de inatividade',
    'never': 'Nunca',
    'minutesCount': '{count} minutos',
    'screenProtection': 'Proteção de ecrã',
    'screenProtectionSubtitle': 'Impedir capturas de ecrã',
    'auditSection': 'Auditoria e atividade',
    'auditLogEntries': 'Registo de auditoria ({count} entradas)',
    'viewAuditSubtitle': 'Consultar ações registadas',
    'exportLog': 'Exportar o registo',
    'exportSubtitle': 'CSV ou JSON para arquivo',
    'clearLog': 'Limpar o registo',
    'clearSubtitle': 'Eliminar todas as entradas (RGPD)',
    'accountSection': 'Informações da conta',
    'userLabel': 'Utilizador',
    'orgLabel': 'Organização',
    'activeRoleLabel': 'Papel ativo',
    'rolesLabel': 'Papéis',
    'noEntries': 'Nenhuma entrada',
    'logExported': 'Registo exportado',
    'entriesExported': '{count} entradas exportadas',
    'clearLogQuestion': 'Limpar o registo de auditoria?',
    'clearAction': 'Limpar',
    'churchSettings': 'Configurações da igreja',
    'identity': 'Identidade',
    'slogan': 'Slogan',
    'colorsSection': 'Cores',
    'primaryColorLabel': 'Cor principal',
    'accentColorLabel': 'Cor de destaque',
    'settingsSaved': '✅ Configurações guardadas',
    'tenantsTitle': 'Igrejas (tenants)',
    'noTenants': 'Nenhuma igreja configurada',
    'statusSuspended': 'Suspenso',
    'growthProjectionTitle': '📊 Projeção de crescimento',
    'growthProphecyTitle': 'Profecia de crescimento (análise real)',
    'projectedAnnualGrowth': 'Crescimento anual projetado: {value} %',
    'headcountIn12Months': 'Efetivo em 12 meses: {value}',
    'leadersNeededCount': 'Novos líderes necessários: {value}',
    'simulator': 'Simulador',
    'annualGrowthRate': 'Taxa de crescimento anual (%)',
    'horizonMonths': 'Horizonte (meses)',
    'simulate': 'Simular',
    'simulationSaved': 'Simulação guardada ✅',
    'simulationFailed': 'Falha na simulação',
    'savedProjections': 'Projeções guardadas',
    'myTeamFamilyTitle': '👨‍👩‍👧‍👦 Minha equipa / minha família',
    'teamTabMembers': 'Membros ({count})',
    'teamTabReceived': 'Encorajamentos recebidos ({count})',
    'encourageName': 'Encorajar {name}',
    'noSpiritualFamily': 'Não está ligado a nenhuma família espiritual.',
    'meLabel': '(eu)',
    'encouragementsBadge': '{count} encorajamentos',
    'sendEncouragementTooltip': 'Enviar um encorajamento',
    'noEncouragementsYet': 'Nenhum encorajamento recebido ainda.',
    'myActivities': 'Minhas atividades',
    'filterPresences': 'Presenças',
    'filterEvents': 'Eventos',
    'filterNotes': 'Notas',
    'filterProgression': 'Progressão',
    'noActivities': 'Nenhuma atividade',
    'presenceConfirmed': 'Presença confirmada',
    'absenceRecorded': 'Ausência registrada',
    'weekOf': 'Semana de',
    'makerNote': 'Nota do fazedor',
    'spiritualLevel': 'Nível espiritual: {value}',
    'progressOverview': 'Progressão global',
    'departmentStats': 'Estatísticas do departamento',
    'kpiMembers': 'Membros',
    'kpiActive': 'Ativos',
    'kpiNew': 'Novos',
    'kpiPresence': 'Presença',
    'kpiOverdueTasks': 'Tarefas atrasadas',
    'kpiTeams': 'Equipes',
    'memberBreakdown': 'Distribuição dos membros',
    'integrating': 'Em integração',
    'standby': 'Em standby',
    'droppedOut': 'Desistentes',
    'headcountEvolution': 'Evolução do efetivo (12 meses)',
    'attendanceSection': 'Presença',
    'present': 'Presentes',
    'absent': 'Ausentes',
    'tasksByStatus': 'Tarefas por estado',
    'disciplinaryCategory': 'Disciplina por categoria',
    'noDisciplinary': 'Nenhum evento disciplinar',
    'workloadPerMember': 'Carga de trabalho por membro',
    'noAssignedTasks': 'Nenhuma tarefa aberta atribuída',
    'tasksUnit': 'tarefas',
    'retardUnit': 'atrasadas',
    'organizationSection': 'Organização',
    'activeAssignments': 'Atribuições ativas',
    'activePositions': 'Cargos ativos',
    'evangelizationTitle': 'Evangelismo · {count} almas',
    'funnelOfConversion': 'Funil de conversão',
    'clickStageToSeeSouls': 'Clique numa etapa para ver as almas',
    'soulsAtStage': '{count} alma(s)',
    'noSoulsAtStage': 'Nenhuma alma nesta etapa',
    'advanceTo': 'Avançar → {label}',
    'retreatLabel': '← Retroceder {label}',
    'globalView': 'Vista global',
    'sinceDate': 'Desde {date}',
    'gamificationTitle': 'Gamificação · {count} badges',
    'tabMyBadges': 'As minhas badges',
    'tabLeaderboard': 'Classificação',
    'checkMyBadges': 'Verificar badges',
    'progressionLabel': 'Progressão',
    'percentCompleted': '{pct}% concluído',
    'badgesEarned': '{count} badge(s) ganha(s)',
    'scoresPerCriteria': 'Pontuações por critério',
    'earnedBadges': 'Badges ganhas ({count})',
    'toUnlock': 'Por desbloquear',
    'noLeaderboard': 'Sem classificação disponível',
    'badgesUnit': 'badge(s)',
    'newBadgesEarned': '🎉 {count} nova(s) badge(s) ganha(s)!',
    'noNewBadges': 'Nenhuma badge nova por agora',
    'checkBadgesError': 'Erro ao verificar badges',
    'criteriaVisits': 'Visitas',
    'criteriaInteractions': 'Interações',
    'criteriaEvangelism': 'Evangelismo',
    'criteriaAttendance': 'Presença',
    'criteriaLoyalty': 'Fidelidade',
    'tithesAndOfferings': 'Dízimos e ofertas',
    'giveNow': 'Dar agora',
    'amountXOF': 'Montante (XOF)',
    'invalidAmount': 'Montante inválido',
    'operatorLabel': 'Operador',
    'destinationLabel': 'Destino',
    'mobilePhoneOptional': 'Telemóvel Mobile Money (opcional)',
    'paymentInitiated': 'Pagamento iniciado — referência {ref}',
    'paymentFailed': 'Falha ao iniciar pagamento',
    'paymentConfirmed': 'Pagamento confirmado — obrigado pela sua generosidade!',
    'waitingConfirmation': 'Aguardar confirmação ({ref})…',
    'noDonationsYet': 'Nenhuma doação registada ainda.\nQue o Senhor abençoe a sua generosidade!',
    'statusConfirmed': 'Confirmado',
    'statusPending': 'Pendente',
    'statusFailed': 'Falhado',
    'statusCancelled': 'Cancelado',
    'broadcastTitle': 'Difusão / Broadcast',
    'newBroadcast': 'Nova difusão',
    'sent': 'Enviadas',
    'readRate': 'Lidas',
    'recentBroadcasts': 'Difusões recentes',
    'targeting': 'Segmentação',
    'allMembers': 'Todos os membros',
    'byDepartment': 'Por departamento',
    'byFamily': 'Por família',
    'byRole': 'Por cargo',
    'broadcastEmpty': 'Nenhuma difusão',
    'broadcastError': 'Erro ao carregar difusões',
    'readLabel': 'lido',
    'formsTitle': 'Formulários',
    'createForm': 'Criar formulário',
    'createFormSubtitle': 'Drag & drop com condições lógicas',
    'publishedForms': 'Formulários publicados',
    'fieldText': 'Texto',
    'fieldChoice': 'Escolha múltipla',
    'fieldDate': 'Data',
    'fieldFile': 'Ficheiro',
    'fieldSignature': 'Assinatura',
    'fieldNote': 'Nota',
    'formTitleLabel': 'Título do formulário',
    'addFieldHint': 'Toque num tipo de campo para adicionar',
    'formSaved': 'Formulário guardado!',
    'formsEmpty': 'Nenhum formulário publicado',
    'formsError': 'Erro ao carregar formulários',
    'churchDirectoryTitle': 'Anúncio da Igreja',
    'searchMember': 'Pesquisar membro...',
    'publicProfile': 'Perfil público',
    'privateProfile': 'Perfil privado',
    'directoryEmpty': 'Nenhum membro no anúncio',
    'directoryError': 'Erro ao carregar anúncio',
    'unknownFamily': 'Família desconhecida',
    'unknownRole': 'Cargo desconhecido',

    // ── Lot H6 : dashboards + transferências ──
    'dashGrowth': 'Crescimento',
    'dashPresenceAndReports': 'Presenças e Relatórios',
    'dashGlobalPresence': 'Presença global',
    'dashReportsSubmitted': 'Relatórios submetidos',
    'dashCompletion': 'Conclusão',
    'dashActiveAlerts': 'alerta(s) ativo(s)',
    'attentionRequired': 'Atenção necessária',
    'allUnderControl': 'Tudo sob controlo',
    'dashDepartments': 'Departamentos',
    'dashFamilies': 'Famílias',
    'dashMakers': 'Fazedores',
    'dashRiskFamilies': 'Famílias em risco',
    'dashShepherdsPilot': 'Painel do Pastor',
    'dashMyFamily': 'Minha família',
    'dashMyMaker': 'Meu fazedor',
    'dashMyDepartments': 'Meus departamentos',
    'dashMyProgression': 'Minha progressão',
    'dashMakerNotes': 'Notas do fazedor',
    'dashRecentPresences': 'Minhas presenças recentes',
    'dashUpcomingEvents': 'Eventos próximos',
    'dashQuickActions': 'Ações rápidas',
    'dashMakersCount': 'Fazedores ({count})',
    'dashMakerWorkload': 'Carga de trabalho dos Fazedores',
    'dashDisciplesSplit': 'Distribuição dos discípulos',
    'dashActiveAlertsTitle': 'Alertas ativos',
    'dashUpcomingVisits': 'Visitas próximas',
    'dashRecentPrayers': 'Orações recentes',
    'dashMySpace': 'Meu espaço',
    'dashCurrentLevel': 'Nível atual',
    'dashPresenceThisWeek': 'Presenças esta semana',
    'dashNextStep': 'Próximo passo: {value}',
    'dashCompanion': 'Companheiro',
    'dashChefLabel': 'Chefe: {name}',
    'dashRespLabel': 'Responsável: {name}',
    'dashDeptToAdmin': 'Departamento a administrar',
    'dashOverview': 'Visão geral',
    'dashMyDepartment': 'Meu departamento',
    'dashDeptManagement': 'Gestão do departamento',
    'dashOverdueTasks': 'Tarefas atrasadas',
    'dashBirthdays': 'Aniversários do mês',
    'dashFollowUp': 'Acompanhamento esta semana',
    'dashDiscipline': 'Avaliações',
    'dashTransferRequests': 'Membros desejam mudar de família',
    'dashEventsAndPosts': 'Equipes e Cargos',
    'dashDeptManagementTitle': 'Gestão',
    'dashQRCheckin': 'QR Check-in',
    'dashReport': 'Relatório',
    'dashPresenceLabel': 'Presença',
    'dashActiveLabel': 'Ativos',
    'dashNewLabel': 'Novos',
    'dashPresentLabel': 'Presentes',
    'dashAbsentLabel': 'Ausentes',
    'dashDroppedLabel': 'Desistentes',
    'dashTransferLabel': 'Transferências',
    'dashTeamsLabel': 'Equipes',
    'dashPresentShort': 'Presente',
    'dashAbsentShort': 'Ausente',
    'dashSurcharge': 'Sobrecarregado',
    'dashLightLoad': 'Leve',
    'dashNormalLoad': 'Normal',
    'dashSouls': 'almas',
    'dashConverti': 'Convertido',
    'dashArrivant': 'Recém-chegado',
    'transferCreateTitle': 'Criar pedido de transferência',
    'transferDetailTitle': 'Pedido de transferência',
    'transferType': 'Tipo de transferência',
    'transferDeptConcerned': 'Departamento concernido',
    'transferDeptDestination': 'Departamento de destino',
    'transferDeptRetrait': 'Departamento de retirada',
    'transferFamilyConcerned': 'Família concernida',
    'transferFamilyDestination': 'Família de destino',
    'transferMemberAdd': 'Membro a adicionar',
    'transferMemberRemove': 'Membro a retirar',
    'transferMakerToTransfer': 'Fazedor a transferir',
    'transferDiscipleToTransfer': 'Discípulo a transferir',
    'transferJustification': 'Justificação detalhada *',
    'transferComments': 'Comentários (opcional)',
    'transferSubmitCreate': 'Criar e submeter',
    'transferSaveDraft': 'Guardar rascunho',
    'transferDraftSaved': 'Rascunho guardado',
    'transferSubmitted': 'Pedido submetido ao circuito de validação',
    'transferIncomplete': 'Formulário incompleto',
    'transferCreateError': 'Erro ao criar pedido',
    'transferChoose': 'Escolher',
    'transferCurrentAffectation': 'Atribuição atual',
    'transferCircuit': 'Circuito de validação',
    'transferApprove': 'Aprovar',
    'transferReject': 'Cancelar',
    'transferModify': 'Editar',
    'transferAskInfo': 'Pedir informações',
    'transferExecute': 'Executar',
    'transferArchive': 'Arquivar',
    'transferHistory': 'Histórico',
    'transferDecisions': 'Decisões',
    'transferStep': 'Passo',
    'transferInfo': 'Info',
    'transferJustificationLabel': 'Justificação',
    'transferMotivation': 'Motivação *',
    'transferMotivationOptional': 'Motivação (opcional)',
    'transferCommentsLabel': 'Comentários:',
    'transferConfirm': 'Confirmar',
    'transferCorrection': 'Correção',
    'transferCancelQuestion': 'Cancelar o pedido?',
    'transferCancelExplanation': 'O pedido será devolvido ao requerente.',
    'transferNoDocument': 'Sem documentos no módulo Documentos.',
    'transferNoAttachment': 'Sem anexos.',
    'transferCreateDoc': 'Criar documento',
    'transferNoEvent': 'Sem eventos',
    'transferOperationError': 'Erro na operação',
    'transferAttachments': 'Peças anexas',
    'transferJustificationHint': 'Motivos pastorais, organizacionais...',
    'transferPriority': 'Prioridade',
    'transferPerson': 'Pessoa',
    'transferSubmit': 'Submeter',
    'transferCancel': 'Cancelar',
    'transferConfirmBtn': 'Confirmar',
    'transferApprovalExplanation': 'O pedido avançará no circuito de validação.',
    'transferRejectionExplanation': 'O pedido será definitivamente recusado e notificado.',
    'transferAutoExecution': 'Nenhuma validação necessária — execução automática ao submeter.',
    'transferAttachmentsCount': 'Peças anexas ({count})',
    'transferValidateCount': 'Validar ({count})',
    'transferAttachmentsUpdated': 'Peças anexas atualizadas',
    'transferAttachmentsError': 'Erro ao atualizar peças anexas',
    'transferListTitle': 'Transferências',
    'transferListNewRequest': 'Pedido',
    'transferListStatusFilter': 'Estado',
    'transferListTypeFilter': 'Tipo',
    'transferListEmpty': 'Nenhum pedido de transferência',
    'transferListPersonLabel': 'Pessoa: {name}',
    'transferListValidations': '{approved}/{total} validações',
    'transferListSubmitError': 'Erro ao submeter',
    'transferListCancelSuccess': 'Pedido cancelado',
    'transferListConfigDefault': 'Configuração',
    'statusDraft': 'Rascunho',
    'statusPendingValidation': 'Aguarda validação',
    'statusApproval': 'Aprovação',
    'statusExecuted': 'Executado',
    'statusArchive': 'Arquivado',
    'statusRequestedInfo': 'Informação solicitada',
    'bibleReadingTitle': 'Plano de Leitura Bíblica',
    'myProgress': 'Minha progressão',
    'days': 'dias',
    'consecutiveDays': 'dias consecutivos',
    'todaysReading': 'Leitura do dia',
    'markAsRead': 'Marcar como lido',
    'addNote': 'Adicionar nota',
    'availablePlans': 'Planos disponíveis',
    'noPlans': 'Nenhum plano disponível',
    'familySharing': 'Compartilhado com minha família',
    'devPlanTitle': 'Meu Plano de Desenvolvimento',
    'globalProgress': 'Progressão global',
    'activeObjectives': 'objetivos ativos',
    'completedObjectives': 'Objetivos concluídos',
    'spiritualJournalTitle': 'Diário Espiritual',
    'reflection': 'Reflexão',
    'thanksgiving': 'Agradecimento',
    'praise': 'Louvor',
    'lesson': 'Lição',
    'newEntry': 'Nova entrada',
    'discipleshipPathTitle': 'Caminho do Discipulado',
    'familyCohesionTitle': 'Coesão Familiar',
    'cohesionScore': 'Pontuação de coesão',
    'goodEffort': 'Bom — Manter os esforços',
    'needsImprovement': 'Precisa melhorar',
    'networkFamilies': 'Famílias da rede',
    'noFamilies': 'Nenhuma família',
    'familyResourcesTitle': 'Recursos Familiares',
    'categories': 'Categorias',
    'recentResources': 'Recursos recentes',
    'noResources': 'Nenhum recurso',
    'makerTrackingTitle': 'Minha Jornada de Fazedor',
    'timeline': 'Linha do tempo',
    'noTimeline': 'Nenhuma atividade',
    'sermonTranslationTitle': 'Tradução de sermões',
    'translationInProgress': 'Tradução em andamento',
    'recentTranslations': 'Traduções recentes',
    'noTranslations': 'Nenhuma tradução',
    'skillMatchingTitle': 'Correspondência de Competências',
    'launchAiMatching': 'Iniciar correspondência IA',
    'matchingSubtitle': 'Analisar competências vs necessidades',
    'proposals': 'Propostas',
    'noMatches': 'Nenhuma proposta',
    'skillsMatrixTitle': 'Matriz de Competências',
    'overview': 'Visão geral',
    'skills': 'Competências',
    'evaluatedMembers': 'Membros avaliados',
    'gapsFound': 'Lacunas encontradas',
    'skillsByDepartment': 'Competências por departamento',
    'needsMoreMembers': 'precisa de membros adicionais',
    'read': 'Lido',
    'progression': 'Progressão',
    'prayer': 'Oração',
    'commTitle': 'Anúncios',
    'commManagement': 'Gestão de anúncios',
    'commEmpty': 'Nenhum anúncio. Crie o primeiro com o botão +.',
    'commPublishedEmpty': 'Nenhum anúncio publicado para si ainda',
    'commNew': 'Novo anúncio',
    'commEdit': 'Editar anúncio',
    'commPublished': 'Anúncios publicados',
    'commPublishSuccess': 'Anúncio publicado e enviado para {count} destinatário(s)',
    'commPublishError': 'Erro ao publicar',
    'commDeleteTitle': 'Eliminar anúncio?',
    'commDeleted': 'Anúncio eliminado',
    'commDeleteError': 'Erro ao eliminar',
    'commCreated': 'Anúncio criado',
    'commModified': 'Anúncio atualizado',
    'commSaveError': 'Falha ao guardar',
    'commStatusDraft': 'Rascunho',
    'commStatusPublished': 'Publicado',
    'commStatusArchived': 'Arquivado',
    'commCibleAll': 'Toda a igreja',
    'commCibleRole': 'Por função',
    'commCibleFamily': 'Por família',
    'commCibleDept': 'Por departamento',
    'commBtnCreate': 'Criar',
    'commBtnSave': 'Guardar',
    'commInputTitle': 'Título',
    'commInputContent': 'Conteúdo',
    'commInputCible': 'Público-alvo',
    'commInputFamily': 'Família',
    'commInputDept': 'Departamento',
    'complianceTitle': 'Conformidade RGPD',
    'complianceVerifyAudit': 'Verificar auditoria',
    'complianceTabOverview': 'Visão geral',
    'complianceTabRetention': 'Retenção',
    'complianceTabAudit': 'Auditoria',
    'complianceTabPortability': 'Portabilidade',
    'complianceStatPolicies': 'Políticas ativas',
    'complianceStatConsents': 'Consentimentos',
    'complianceStatAuditEntries': 'Entradas de auditoria',
    'complianceStatGdprRequests': 'Pedidos RGPD',
    'complianceAuditIntegrity': 'Integridade da auditoria',
    'complianceAuditValid': 'Cadeia intacta — sem adulteração',
    'complianceAuditInvalid': 'Adulteração detetada',
    'complianceVerifySuccess': 'Cadeia válida — {count} entradas verificadas',
    'complianceVerifyBroken': '{count} ligação(ões) quebrada(s) na cadeia!',
    'complianceVerifyImpossible': 'Verificação impossível',
    'complianceExportSuccess': 'Exportação descarregada ({count} secções)',
    'complianceExportError': 'Erro na exportação',
    'complianceChecklist': 'Lista de conformidade',
    'complianceCheckPolicy': 'Política de retenção',
    'complianceCheckConsents': 'Consentimentos recolhidos',
    'complianceCheckAudit': 'Registo de auditoria imutável',
    'complianceCheckPortability': 'Portabilidade 1-clique',
    'complianceCheckRightToForget': 'Direito ao esquecimento',
    'complianceCheckEncryption': 'Encriptação AES-256',
    'compliancePurgeTitle': 'Executar purge?',
    'compliancePurgeContent': 'Os dados que excedam o período de retenção serão processados.',
    'compliancePurgeAction': 'Purgar',
    'compliancePurgeSuccess': 'Purge executada',
    'complianceRetentionTitle': 'Políticas de retenção',
    'complianceRetentionEmpty': 'Nenhuma política configurada',
    'complianceRetentionDurations': 'Durações sugeridas',
    'complianceActionAnonymize': 'Anonimizar',
    'complianceActionArchive': 'Arquivar',
    'complianceAuditEmpty': 'Nenhuma entrada de auditoria',
    'complianceExportTitle': 'Exportação de portabilidade',
    'complianceExportSubtitle': 'RGPD Art. 20 — Formato JSON',
    'complianceExportContent': 'Conteúdo da exportação:',
    'complianceExportProfile': 'Perfil do utilizador',
    'complianceExportSouls': 'Almas vinculadas (discípulos)',
    'complianceExportConsents': 'Histórico de consentimentos',
    'complianceExportGdpr': 'Pedidos RGPD anteriores',
    'complianceExportMeta': 'Metadados (formato, versão)',
    'complianceExportBtn': 'Exportar os meus dados',
    'onboardingSkip': 'Pular',
    'onboardingNext': 'Próximo',
    'onboardingStart': 'Começar',
    'onboardingAR': 'Visita AR',
    'onboardingWelcomeTitle': 'Bem-vindo ao Discipolat',
    'onboardingWelcomeDesc': 'A plataforma que facilita o acompanhamento de discípulos da sua igreja.',
    'onboardingTrackingTitle': 'Acompanhamento em tempo real',
    'onboardingTrackingDesc': 'Acesse suas almas, relatórios e eventos de qualquer lugar.',
    'onboardingOfflineTitle': 'Modo offline',
    'onboardingOfflineDesc': 'Todas as funcionalidades críticas funcionam sem conexão.',
    'onboardingNotificationsTitle': 'Notificações inteligentes',
    'onboardingNotificationsDesc': 'Receba alertas personalizados de acordo com o seu papel.',
    'onboardingSecurityTitle': 'Segurança reforçada',
    'onboardingSecurityDesc': 'Autenticação biométrica, sessão segura e proteção de dados.',
    'deptMgmtTitle': 'Gestão do departamento',
    'deptMgmtTooltipStats': 'Estatísticas',
    'deptMgmtTooltipTools': 'Relatórios · Checklists · Inventário',
    'deptMgmtSearchHint': 'Pesquisa rápida: membro, equipe, tarefa…',
    'deptMgmtKpiTeams': 'Equipes',
    'deptMgmtKpiPositions': 'Postos',
    'deptMgmtKpiAssigned': 'Atribuídos',
    'deptMgmtTabMembers': 'Membros',
    'deptMgmtTabOrg': 'Organização',
    'deptMgmtTabTasks': 'Tarefas',
    'deptMgmtTabAssignments': 'Atribuições',
    'deptMgmtTabEvents': 'Eventos',
    'deptMgmtTabActivity': 'Atividade',
    'deptMgmtMembersTitle': 'Membros do departamento ({count})',
    'deptMgmtMembersEmpty': 'Nenhum membro neste departamento',
    'deptMgmtFamilyLabel': 'Família: {name}',
    'deptMgmtMakerLabel': 'Fazedor: {name}',
    'deptMgmtOrgTitle': 'Organograma',
    'deptMgmtOrgEmpty': 'Nenhuma equipe — crie seu primeiro sub-departamento',
    'deptMgmtTeamPermanent': 'Equipe permanente',
    'deptMgmtTeamTemporary': 'Temporária',
    'deptMgmtSubDepartment': 'Sub-departamento',
    'deptMgmtMembersCount': '{count} membros',
    'deptMgmtEventLabel': 'Evento: {title}',
    'deptMgmtNewTeam': 'Nova equipe',
    'deptMgmtTeamNameLabel': 'Nome da equipe',
    'deptMgmtParentTeam': 'Equipe principal',
    'deptMgmtNoParent': '— Nenhuma (raiz) —',
    'deptMgmtLinkedEvent': 'Evento vinculado (opcional)',
    'deptMgmtLoadingEvents': 'Carregando eventos…',
    'deptMgmtNoEvent': '— Nenhum evento —',
    'deptMgmtDateStart': 'Data início (AAAA-MM-DD)',
    'deptMgmtDateEnd': 'Data fim (AAAA-MM-DD)',
    'deptMgmtObjective': 'Objetivo (opcional)',
    'deptMgmtTeamCreateError': 'Falha ao criar equipe',
    'deptMgmtTasksTitle': 'Tarefas do departamento',
    'deptMgmtStatInProgress': 'Em andamento',
    'deptMgmtStatTodo': 'A fazer',
    'deptMgmtStatOverdue': 'Atrasadas',
    'deptMgmtStatDone': 'Concluídas',
    'deptMgmtTaskEmpty': 'Nenhuma tarefa',
    'deptMgmtTaskLoadError': 'Erro ao carregar',
    'deptMgmtNewTask': 'Nova tarefa',
    'deptMgmtAssignedTo': 'Atribuída a',
    'deptMgmtNoAssignee': '— Não atribuída —',
    'deptMgmtPriority': 'Prioridade',
    'deptMgmtPriorityLow': 'Baixa',
    'deptMgmtPriorityMedium': 'Média',
    'deptMgmtPriorityHigh': 'Alta',
    'deptMgmtDeadline': 'Prazo (AAAA-MM-DD, opcional)',
    'deptMgmtTaskCreateError': 'Falha ao criar tarefa',
    'deptMgmtAssignmentsTitle': 'Atribuições ativas',
    'deptMgmtAssignmentsEmpty': 'Nenhuma atribuição ativa',
    'deptMgmtAssignMember': 'Atribuir um membro',
    'deptMgmtRoleChef': 'Líder',
    'deptMgmtRoleAdjunct': 'Adjunto',
    'deptMgmtAssignBtn': 'Atribuir',
    'deptMgmtAssignError': 'Falha na atribuição',
    'deptMgmtEndAssignment': 'Encerrar',
    'deptMgmtEventsTitle': 'Eventos do departamento',
    'deptMgmtEventsUpcoming': 'Próximos ({count})',
    'deptMgmtEventsPast': 'Passados ({count})',
    'deptMgmtEventsEmpty': 'Nenhum evento planejado. Crie o primeiro evento do seu departamento.',
    'deptMgmtNewEvent': 'Novo evento do departamento',
    'deptMgmtTitleRequired': 'O título é obrigatório',
    'deptMgmtEventCreateError': 'Falha ao criar evento',
    'deptMgmtActivityEmpty': 'Nenhuma atividade ainda',
    'deptMgmtSearchResults': '{count} resultado(s) para "{query}"',
    'deptMgmtSearchNoResults': 'Nenhum resultado para "{query}"',
    'deptMgmtSearchHintDetail': 'Tente um nome, posto, equipe, tarefa ou evento.',
    'deptMgmtNoTeam': 'Sem equipe',
    'deptMgmtUnassigned': 'Não atribuída',
    'secTitle': 'Segurança & Configurações',
    'secSessionSection': 'Sessão & Inatividade',
    'secAutoLogout': 'Desconexão automática',
    'secAfterInactivity': 'Após {min} min de inatividade',
    'secSessionDuration': 'Duração da sessão:',
    'secBiometricSection': 'Autenticação Biométrica',
    'secBiometricLogin': 'Login por biometria',
    'secBiometricUnavailable': 'Não disponível neste dispositivo',
    'secBiometricError': 'Falha na ativação biométrica',
    'secDataSaverSection': 'Economizador de dados',
    'secDataSaverMode': 'Modo economizador',
    'secNetworkLabel': 'Rede: {label}',
    'secAutoMode': 'Modo automático',
    'secAutoModeDesc': 'Ativa automaticamente em dados móveis',
    'secImgLoading': 'Carregamento de imagens',
    'secCacheStrategy': 'Estratégia de cache',
    'secRefreshInterval': 'Intervalo de atualização',
    'secOrientationSection': 'Orientação da tela',
    'secScreenshotSection': 'Proteção contra capturas de tela',
    'secScreenshotToggle': 'Proteção anti-captura de tela',
    'secScreenshotDesc': 'Impede capturas de tela em ecrãs sensíveis',
    'secAuditSection': 'Registo de auditoria móvel',
    'secAuditEvents': 'Eventos registados',
    'secAuditCount': '{count} eventos no registo',
    'secAuditLogTitle': 'Registo de auditoria',
    'secAuditClose': 'Fechar',
    'secAuditExportCsv': 'Exportar CSV',
    'secAuditExported': 'Registo exportado ({count} caracteres)',
    'secAuditClear': 'Limpar',
    'secAuditViewLog': 'Ver registo',
    'secInfoPersisted': 'As configurações são guardadas localmente e persistem entre sessões.',
    'dictTitle': 'Dicionários',
    'dictEmpty': 'Nenhum dicionário',
    'dictEntriesCount': '{count} entrada(s)',
    'integTitle': 'Integrações',
    'integSmtp': 'SMTP / Email',
    'integStorage': 'Armazenamento / MinIO',
    'integJwt': 'JWT / Auth',
    'integRateLimiting': 'Rate Limiting',
    'integEnabled': 'Ativado',
    'integDisabled': 'Desativado',
    'integTestConn': 'Testar conexão',
    'integTestResultOk': 'Conexão bem-sucedida',
    'integTestResultFail': 'Erro: {msg}',
    'evtTitle': 'Eventos',
    'evtStatTotal': 'Total',
    'evtStatUpcoming': 'Próximos',
    'evtStatDone': 'Concluídos',
    'evtFilterAll': 'Todos',
    'evtFilterUpcoming': 'Próximos',
    'evtFilterOngoing': 'Em andamento',
    'evtEmpty': 'Nenhum evento',
    'evtNew': 'Novo evento',
    'evtTitleRequired': 'O título é obrigatório',
    'evtCreated': 'Evento criado',
    'evtCreateError': 'Erro ao criar',
    'evtInscrits': '{count}/{total} inscritos',
    'discTitle': 'Disciplina',
    'discStatTotal': 'Total',
    'discStatOngoing': 'Em andamento',
    'discStatResolved': 'Resolvidos',
    'discFilterAll': 'Todos',
    'discFilterOngoing': 'Em andamento',
    'discFilterResolved': 'Resolvidos',
    'discCatAll': 'Todas',
    'discSearchHint': 'Pesquisar por nome, título…',
    'discResolveTitle': 'Resolver este evento?',
    'discResolveConfirm': 'Marcar "{title}" como resolvido?',
    'discResolveAction': 'Resolver',
    'discResolveSuccess': 'Evento resolvido',
    'discResolveError': 'Falha na resolução',
    'discDeleteTitle': 'Eliminar este evento?',
    'discDeleteConfirm': 'Eliminar permanentemente "{title}"?',
    'discDeleteAction': 'Eliminar',
    'discDeleteSuccess': 'Evento eliminado',
    'discDeleteError': 'Falha na eliminação',
    'discCreateTitle': 'Novo evento disciplinar',
    'discMemberLabel': 'Membro *',
    'discSelectMember': 'Selecionar um membro',
    'discCatLabel': 'Categoria *',
    'discTypeLabel': 'Tipo *',
    'discTitleLabel': 'Título *',
    'discDescLabel': 'Descrição',
    'discGravityLabel': 'Gravidade',
    'discDateLabel': 'Data',
    'discCreateError': 'Falha ao criar',
    'discCreateSuccess': 'Evento disciplinar registado',
    'discCreateValidation': 'Selecione um membro e insira um título',
    'discResolved': 'Resolvido',
    'discNewEvent': 'Novo evento',
    'discEmptySearch': 'Nenhum resultado para "{query}"',
    'discEmpty': 'Nenhum evento disciplinar',
    'discClearSearch': 'Limpar pesquisa',
  };
}

class _AppLocalizationsDelegate extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) =>
      ['fr', 'en', 'pt'].contains(locale.languageCode);

  @override
  Future<AppLocalizations> load(Locale locale) async => AppLocalizations(locale);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}
