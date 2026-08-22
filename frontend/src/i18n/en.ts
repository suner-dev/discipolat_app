const en: Record<string, string> = {
  // Navigation
  'nav.dashboard': 'Dashboard',
  'nav.souls': 'Souls',
  'nav.families': 'Families',
  'nav.departments': 'Departments',
  'nav.reports': 'Reports',
  'nav.prayers': 'Prayers',
  'nav.events': 'Events',
  'nav.alerts': 'Alerts',
  'nav.search': 'Search',
  'nav.messages': 'Messages',
  'nav.profile': 'Profile',
  'nav.settings': 'Settings',
  'nav.admin': 'Administration',
  'nav.logout': 'Logout',

  // Auth
  'auth.login': 'Login',
  'auth.email': 'Email',
  'auth.password': 'Password',
  'auth.forgotPassword': 'Forgot password?',
  'auth.loginWith': 'Login with',
  'auth.google': 'Google',
  'auth.magicLink': 'Magic Link',
  'auth.magicLinkSent': 'A login link has been sent to your email',
  'auth.googleUnavailable': 'Google sign-in is not configured on this server.',
  'auth.noAccount': 'No account yet?',
  'auth.createAccount': 'Create account',
  'auth.welcome': 'Welcome to Discipolat',
  'auth.welcomeMessage': 'Login to manage your church',

  // Dashboard
  'dashboard.title': 'Dashboard',
  'dashboard.totalSouls': 'Total souls',
  'dashboard.activeSouls': 'Active souls',
  'dashboard.newConverts': 'New converts',
  'dashboard.activeAlerts': 'Active alerts',
  'dashboard.pendingReports': 'Pending reports',
  'dashboard.familyRisk': 'Families at risk',
  'dashboard.presenceRate': 'Attendance rate',

  // Souls
  'souls.title': 'Souls / Disciples',
  'souls.create': 'New soul',
  'souls.search': 'Search for a soul...',
  'souls.name': 'Name',
  'souls.firstName': 'First name',
  'souls.phone': 'Phone',
  'souls.email': 'Email',
  'souls.status': 'Status',
  'souls.faiseur': 'Mentor',
  'souls.family': 'Family',
  'souls.department': 'Department',
  'souls.integrationDate': 'Integration date',
  'souls.spiritualState': 'Spiritual state',

  // Status
  'status.active': 'Active',
  'status.integration': 'In integration',
  'status.sleep': 'Sleeping',
  'status.dropped': 'Dropped',

  // Reports
  'reports.title': 'Reports',
  'reports.makerReport': 'Mentor report',
  'reports.familyReport': 'Family report',
  'reports.submit': 'Submit',
  'reports.draft': 'Draft',
  'reports.submitted': 'Submitted',
  'reports.validated': 'Validated',
  'reports.week': 'Week',
  'reports.present': 'Present',
  'reports.absent': 'Absent',

  // Prayers
  'prayers.title': 'Prayers',
  'prayers.create': 'New prayer',
  'prayers.priority': 'Priority',
  'prayers.category': 'Category',
  'prayers.visibility': 'Visibility',
  'prayers.answered': 'Answered',
  'prayers.pending': 'Pending',
  'prayers.testimony': 'Testimony',

  // Events
  'events.title': 'Events',
  'events.create': 'New event',
  'events.date': 'Date',
  'events.location': 'Location',
  'events.register': 'Register',
  'events.attendees': 'Attendees',

  // Notifications
  'notifications.title': 'Notifications',
  'notifications.markAllRead': 'Mark all as read',
  'notifications.empty': 'No notifications',

  // Settings
  'settings.title': 'Settings',
  'settings.language': 'Language',
  'settings.theme': 'Theme',
  'settings.darkMode': 'Dark mode',
  'settings.notifications': 'Notifications',
  'settings.privacy': 'Privacy',

  // Languages
  'lang.fr': 'Français',
  'lang.en': 'English',
  'lang.pt': 'Português',

  // Common
  'common.save': 'Save',
  'common.cancel': 'Cancel',
  'common.delete': 'Delete',
  'common.edit': 'Edit',
  'common.create': 'Create',
  'common.search': 'Search',
  'common.filter': 'Filter',
  'common.loading': 'Loading...',
  'common.noData': 'No data',
  'common.confirm': 'Confirm',
  'common.back': 'Back',
  'common.next': 'Next',
  'common.previous': 'Previous',
  'common.success': 'Success',
  'common.error': 'Error',
  'common.warning': 'Warning',
  'common.info': 'Information',
  'common.yes': 'Yes',
  'common.no': 'No',
  'common.all': 'All',
  'common.none': 'None',
  'common.total': 'Total',
  'common.actions': 'Actions',
  'common.details': 'Details',
  'common.status': 'Status',
  'common.date': 'Date',
  'common.amount': 'Amount',
  'common.description': 'Description',

  // Payments
  'payments.title': 'Tithes & Offerings',
  'payments.give': 'Give now',
  'payments.amount': 'Amount (XOF)',
  'payments.operator': 'Operator',
  'payments.destination': 'Destination',
  'payments.phone': 'Mobile Money phone',
  'payments.history': 'Giving history',
  'payments.confirmed': 'Confirmed',
  'payments.pending': 'Pending',
  'payments.failed': 'Failed',

  // AI Assistant
  'ai.title': 'Pastoral AI Assistant',
  'ai.placeholder': 'Ask the assistant a question...',
  'ai.thinking': 'AI is thinking...',
  'ai.send': 'Send',
  'ai.history': 'History',
  'ai.suggestions': 'Suggestions',

  // Tickets
  'tickets.title': 'Tickets & Support',
  'tickets.create': 'New ticket',

  // Surveys
  'surveys.title': 'Surveys',
  'surveys.create': 'New survey',

  // Testimonials
  'testimonials.title': 'Testimonials',
  'testimonials.share': 'Share testimony',

  // Leave requests
  'leave.title': 'Leave Requests',
  'leave.create': 'New request',

  // Referrals
  'referrals.title': 'Referrals',
  'referrals.invite': 'Invite',

  // Calendar
  'calendar.title': 'Calendar',
  'calendar.create': 'New event',

  // Skills Matrix
  'skills.title': 'Skills Matrix',

  // Team Gantt
  'gantt.title': 'Team Planning',

  // Compliance
  'compliance.title': 'GDPR Compliance',

  // API Docs
  'apiDocs.title': 'API & Documentation',

  // Portal
  'portal.title': 'Public Portal',

  // Cercle Faiseurs
  'cercle.title': 'Mentors Circle',
  'cercle.share': 'Share',

  // Bible Reading
  'bibleReading.title': 'Bible Reading Plans',
  'bibleReading.create': 'New plan',

  // Prayer Journal
  'prayerJournal.title': 'Prayer Journal',
  'prayerJournal.add': 'Add',

  // Spiritual Challenges
  'spiritualChallenges.title': 'Spiritual Challenges',
  'spiritualChallenges.create': 'Create challenge',

  // Church Directory
  'directory.title': 'Church Directory',

  // Spiritual Journey
  'spiritualJourney.title': 'Spiritual Journey',

  // Automations
  'automations.title': 'Pastoral Automations',
  'automations.create': 'New automation',

  // Onboarding
  'onboarding.title': 'Setup Wizard',
  'onboarding.step1': 'Identity',
  'onboarding.step2': 'Import members',
  'onboarding.step3': 'Structure',
  'onboarding.step4': 'Roles',
  'onboarding.step5': 'First event',
};

export default en;
