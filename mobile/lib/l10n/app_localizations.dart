import 'package:flutter/material.dart';

/// Comprehensive i18n system for Discipolat.
/// Supports FR, EN, PT with fallback to FR.
/// Add new strings in the _translations map for each locale.
class AppLocalizations {
  final Locale locale;
  AppLocalizations(this.locale);

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
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
  String get offline => translate('offline');

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
    'difficultes': 'Difficultés',
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
