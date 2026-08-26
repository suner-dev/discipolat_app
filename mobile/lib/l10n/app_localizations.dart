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

  // ==================== NAVIGATION ====================
  String get navDashboard => translate('navDashboard');
  String get navSouls => translate('navSouls');
  String get navFamilies => translate('navFamilies');
  String get navDepartments => translate('navDepartments');
  String get navReports => translate('navReports');
  String get navPrayers => translate('navPrayers');
  String get navEvents => translate('navEvents');
  String get navAlerts => translate('navAlerts');
  String get navMessages => translate('navMessages');
  String get navNotifications => translate('navNotifications');
  String get navProfile => translate('navProfile');
  String get navSettings => translate('navSettings');
  String get navUsers => translate('navUsers');
  String get navBadges => translate('navBadges');
  String get navEvangelism => translate('navEvangelism');
  String get navDiscipline => translate('navDiscipline');
  String get navPresence => translate('navPresence');
  String get navTrainings => translate('navTrainings');
  String get navFinances => translate('navFinances');
  String get navCommunications => translate('navCommunications');
  String get navMap => translate('navMap');
  String get navSearch => translate('navSearch');
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
  String get launchMigration => translate('launchMigration');
  String get previousMigrations => translate('previousMigrations');
  String get noMigrations => translate('noMigrations');
  String get rerun => translate('rerun');
  String get migrationLaunched => translate('migrationLaunched');
  String get migrationError => translate('migrationError');
  String rowsCount(int count) => translate('rowsCount').replaceAll('{count}', '$count');
  String get surveysTitle => translate('surveysTitle');
  String get surveysEmpty => translate('surveysEmpty');
  String responsesCount(int count) => translate('responsesCount').replaceAll('{count}', '$count');
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
    'navCommunications': 'Communications',
    'navMap': 'Carte',
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
