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

  // Streaming
  'nav.streaming': 'Streaming & Live',
  'streaming.live': 'Live',
  'streaming.scheduled': 'Scheduled',
  'streaming.ended': 'Ended',

  // Broadcast
  'nav.broadcast': 'Broadcast Messaging',

  // Inventory
  'nav.inventory': 'Smart Inventory',
  'inventory.lowStock': 'Low Stock',
  'inventory.categories': 'Categories',

  // Department KPIs
  'nav.departmentKpis': 'Department KPIs',
  'kpi.progress': 'Progress',
  'kpi.target': 'Target',

  // Rewards
  'nav.rewards': 'Rewards & Gamification',
  'rewards.points': 'My points',
  'rewards.claimed': 'Claimed',
  'rewards.locked': 'Locked',

  // Marketplace
  'nav.marketplace': 'Community Marketplace',
  'marketplace.offer': 'Offer',
  'marketplace.request': 'Request',
  'marketplace.service': 'Service',
  'marketplace.free': 'Free',

  // Community
  'nav.community': 'Community',
  'community.testimony': 'Testimony',
  'community.prayer': 'Prayer',
  'community.encouragement': 'Encouragement',

    'aiPredictions.title': 'AI Predictions',
  'broadcast.new': 'New broadcast',
  'broadcast.send': 'Send',
  'broadcast.draft': 'Draft',

  // ─────────────────────────── LANDING PAGE ───────────────────────────
  'landing.skipToContent': 'Skip to content',
  'landing.openMenu': 'Open menu',
  'landing.closeMenu': 'Close menu',
  'landing.themeDark': 'Switch to dark mode',
  'landing.themeLight': 'Switch to light mode',

  // Nav
  'landing.nav.product': 'Product',
  'landing.nav.features': 'Features',
  'landing.nav.solutions': 'Solutions',
  'landing.nav.platform': 'Platform',
  'landing.nav.roles': 'Spaces',
  'landing.nav.pricing': 'Pricing',
  'landing.nav.about': 'About',
  'landing.nav.login': 'Sign in',
  'landing.nav.start': 'Get started',

  // Hero
  'landing.hero.eyebrow': 'The modern discipleship platform',
  'landing.hero.headline': 'Your church discipleship,',
  'landing.hero.headlineAccent': 'finally connected',
  'landing.hero.subtitle': 'A centralized platform to support every soul, coordinate your leaders and steer the spiritual growth of your church.',
  'landing.hero.subtitleEmphasis': 'Simple, powerful, human.',
  'landing.hero.ctaDiscover': 'Discover the platform',
  'landing.hero.ctaDemo': 'Request a demo',
  'landing.hero.demoTag': 'Demo',
  'landing.hero.scroll': 'Scroll to explore',
  'landing.hero.statSpaces': 'Dedicated spaces',
  'landing.hero.statModules': 'Configurable modules',
  'landing.hero.statAlerts': 'Auto alerts',

  // Problem
  'landing.problem.eyebrow': 'The reality',
  'landing.problem.title': 'Your church deserves better',
  'landing.problem.titleAccent': 'than scattered tools',
  'landing.problem.subtitle': 'Every leader has their own method, every family their own files — and the big picture fades away.',
  'landing.problem.files': 'Scattered records',
  'landing.problem.filesDesc': 'Follow-ups pile up in Excel files, notebooks and WhatsApp chats — with no overview.',
  'landing.problem.manual': 'Manual tracking',
  'landing.problem.manualDesc': 'Hours lost consolidating reports, hunting for information and chasing people.',
  'landing.problem.visibility': 'No visibility',
  'landing.problem.visibilityDesc': 'No clear view of the spiritual health of each member and each family.',
  'landing.problem.lateAlerts': 'Late alerts',
  'landing.problem.lateAlertsDesc': 'Absences, drop-outs and critical situations detected far too late.',
  'landing.problem.fragmented': 'Fragmented communication',
  'landing.problem.fragmentedDesc': 'Messages lost between WhatsApp, SMS, calls and meetings.',
  'landing.problem.dataLoss': 'Lost data',
  'landing.problem.dataLossDesc': 'History and contacts vanish when a leader changes role or leaves.',

  // Solution
  'landing.solution.eyebrow': 'The solution',
  'landing.solution.title': 'One management hub',
  'landing.solution.titleAccent': 'for your whole church',
  'landing.solution.subtitle': 'Discipolat connects every link of your organisation — from pastor to disciple — in one coherent ecosystem.',
  'landing.solution.1': 'A clear dashboard for every role',
  'landing.solution.2': 'A hierarchy Pastor → Leader → Chief → Maker → Disciple',
  'landing.solution.3': 'Real-time statistics and KPIs',
  'landing.solution.4': 'Smart automatic alerts',
  'landing.solution.5': 'Consolidated weekly reports',
  'landing.solution.6': 'Total security and confidentiality',

  // Ecosystem
  'landing.ecosystem.eyebrow': 'The ecosystem',
  'landing.ecosystem.title': 'From the pastor’s vision',
  'landing.ecosystem.titleAccent': 'to the disciple’s daily life',
  'landing.ecosystem.subtitle': 'A clear hierarchy, fluid connections, responsibility at every level.',
  'landing.ecosystem.pasteur': 'Pastor',
  'landing.ecosystem.pasteurDesc': 'Global vision, final validation',
  'landing.ecosystem.responsable': 'Leader',
  'landing.ecosystem.responsableDesc': 'Department management',
  'landing.ecosystem.chef': 'Family chief',
  'landing.ecosystem.chefDesc': 'Creating and tracking families',
  'landing.ecosystem.faiseur': 'Maker',
  'landing.ecosystem.faiseurDesc': 'Personalised support',
  'landing.ecosystem.disciple': 'Disciple',
  'landing.ecosystem.discipleDesc': 'Spiritual growth',

  // Features
  'landing.features.eyebrow': 'Features',
  'landing.features.title': 'Everything to',
  'landing.features.titleAccent': 'accompany',
  'landing.features.subtitle': 'A complete tool designed for pastors, leaders, family chiefs and discipleship makers.',
  'landing.features.discipolat': 'Discipleship tracking',
  'landing.features.discipolatDesc': 'Families, makers and souls: structured, gentle and human support at every level.',
  'landing.features.reporting': 'Weekly reporting',
  'landing.features.reportingDesc': 'Maker and family reports consolidated and validated at each responsibility level.',
  'landing.features.dashboard': 'Dashboards',
  'landing.features.dashboardDesc': 'Spiritual health KPIs, smart indices and trends to steer growth.',
  'landing.features.org': 'Departments & families',
  'landing.features.orgDesc': 'Department → Family → Maker → Soul, with dedicated leaders at every step.',
  'landing.features.alerts': 'Smart alerts',
  'landing.features.alertsDesc': 'Absences, drop-outs and deadlines detected automatically to act at the right time.',
  'landing.features.security': 'Security & audit',
  'landing.features.securityDesc': 'Role-isolated spaces, immutable audit log, total confidentiality.',
  'landing.features.discover': 'Discover',

  // Modules
  'landing.modules.eyebrow': 'Platform',
  'landing.modules.title': 'One',
  'landing.modules.titleAccent': 'single platform',
  'landing.modules.subtitle': 'Every module your church needs, in one coherent ecosystem.',
  'landing.modules.discipolat': 'Discipleship',
  'landing.modules.discipolatDesc': 'Tracking souls, disciples and makers',
  'landing.modules.members': 'Members',
  'landing.modules.membersDesc': 'Complete member management',
  'landing.modules.departments': 'Departments',
  'landing.modules.departmentsDesc': 'Department organisation',
  'landing.modules.families': 'Families',
  'landing.modules.familiesDesc': 'Discipleship families',
  'landing.modules.reports': 'Reports',
  'landing.modules.reportsDesc': 'Centralised reports',
  'landing.modules.prayers': 'Prayers',
  'landing.modules.prayersDesc': 'Tracking prayer topics',
  'landing.modules.attendance': 'Attendance',
  'landing.modules.attendanceDesc': 'Participation tracking',
  'landing.modules.events': 'Events',
  'landing.modules.eventsDesc': 'Event organisation',
  'landing.modules.alerts': 'Alerts',
  'landing.modules.alertsDesc': 'Automatic detection',
  'landing.modules.stats': 'Statistics',
  'landing.modules.statsDesc': 'Steering your church',

  // Roles spaces
  'landing.roles.eyebrow': 'Your space',
  'landing.roles.title': 'Everyone gets',
  'landing.roles.titleAccent': 'their space',
  'landing.roles.subtitle': 'Select your role to discover the experience built for you.',
  'landing.roles.iAm': 'I am…',
  'landing.roles.pasteur': 'Pastor',
  'landing.roles.pasteurDesc': 'Complete view of the whole church. Final validation. Strategic command centre.',
  'landing.roles.responsable': 'Leader',
  'landing.roles.responsableDesc': 'Manage your department, track families and members, coordinate.',
  'landing.roles.chef': 'Family chief',
  'landing.roles.chefDesc': 'Create families, track disciples, consolidate weekly reports.',
  'landing.roles.faiseur': 'Maker',
  'landing.roles.faiseurDesc': 'Personalised support of each disciple. Notes and reports.',
  'landing.roles.membre': 'Member',
  'landing.roles.membreDesc': 'Personal space. Activities, prayers, events and spiritual journey.',
  'landing.roles.openSpace': 'Access this space',
  'landing.roles.f1': 'Global dashboard', 'landing.roles.f2': 'Alerts and notifications',
  'landing.roles.f3': 'Consolidated reports', 'landing.roles.f4': 'Department management',
  'landing.roles.f5': 'Department view', 'landing.roles.f6': 'Family management',
  'landing.roles.f7': 'Family reports', 'landing.roles.f8': 'Attendance tracking',
  'landing.roles.f9': 'Family management', 'landing.roles.f10': 'Disciple assignments',
  'landing.roles.f11': 'Weekly reports', 'landing.roles.f12': 'Spiritual tracking',
  'landing.roles.f13': 'Disciple list', 'landing.roles.f14': 'Note taking',
  'landing.roles.f15': 'Weekly report', 'landing.roles.f16': 'Follow-up history',
  'landing.roles.f17': 'My journey', 'landing.roles.f18': 'Personal activities',
  'landing.roles.f19': 'Prayers and praises', 'landing.roles.f20': 'Upcoming events',

  // Before / After
  'landing.beforeAfter.eyebrow': 'Transformation',
  'landing.beforeAfter.title': 'Before',
  'landing.beforeAfter.titleAccent': 'Discipolat',
  'landing.beforeAfter.beforeTitle': 'Before',
  'landing.beforeAfter.afterTitle': 'After',
  'landing.beforeAfter.b1': 'Scattered Excel files', 'landing.beforeAfter.b2': 'Time-consuming manual tracking',
  'landing.beforeAfter.b3': 'Lost paper reports', 'landing.beforeAfter.b4': 'No visibility',
  'landing.beforeAfter.b5': 'Late alerts', 'landing.beforeAfter.b6': 'Scattered data',
  'landing.beforeAfter.a1': 'Centralised platform', 'landing.beforeAfter.a2': 'Smart automation',
  'landing.beforeAfter.a3': 'Real-time consolidated reports', 'landing.beforeAfter.a4': 'Instant global visibility',
  'landing.beforeAfter.a5': 'Proactive automatic alerts', 'landing.beforeAfter.a6': 'Secure, traceable history',

  // Stats
  'landing.stats.demo': 'Demo data',
  'landing.stats.s1': 'Dedicated workspaces', 'landing.stats.s2': 'Configurable modules',
  'landing.stats.s3': 'Hierarchy levels', 'landing.stats.s4': 'Availability',

  // How it works
  'landing.how.eyebrow': 'Getting started',
  'landing.how.title': 'How it',
  'landing.how.titleAccent': 'works',
  'landing.how.subtitle': 'Go from scattered to a single platform in a few simple steps.',
  'landing.how.s1Title': 'Configure', 'landing.how.s1Desc': 'Define your organisation, departments and your church roles.',
  'landing.how.s2Title': 'Organise', 'landing.how.s2Desc': 'Create your discipleship families, set chiefs and makers in a few clicks.',
  'landing.how.s3Title': 'Support', 'landing.how.s3Desc': 'Track every soul, record reports and attendance. Alerts work for you.',
  'landing.how.s4Title': 'Steer', 'landing.how.s4Desc': 'KPIs, trends and measurable goals for guided spiritual growth.',

  // Customisation
  'landing.custom.eyebrow': 'Customisation',
  'landing.custom.title': 'Every church is',
  'landing.custom.titleAccent': 'unique',
  'landing.custom.desc': 'Discipolat adapts to your organisation. Configure colours, logos, departments, roles, permissions and features without writing a single line of code.',
  'landing.custom.strap': 'Every church runs differently. Discipolat adapts to yours.',
  'landing.custom.c1': 'Logo and visual identity', 'landing.custom.c2': 'Brand colours',
  'landing.custom.c3': 'Custom departments', 'landing.custom.c4': 'Roles and permissions',
  'landing.custom.c5': 'Toggleable modules', 'landing.custom.c6': 'Custom fields',

  // Security
  'landing.security.eyebrow': 'Security',
  'landing.security.title': 'Your data,',
  'landing.security.titleAccent': 'protected',
  'landing.security.subtitle': 'Enterprise-grade security designed to reassure church leaders.',
  'landing.security.s1': 'Data isolation', 'landing.security.s1Desc': 'Every role only sees the data that concerns them.',
  'landing.security.s2': 'Audit log', 'landing.security.s2Desc': 'Every action is traced, timestamped and tamper-proof.',
  'landing.security.s3': 'Access control', 'landing.security.s3Desc': 'Granular permissions by role, department and family.',
  'landing.security.s4': 'Confidentiality', 'landing.security.s4Desc': 'Sensitive data is isolated and encrypted.',
  'landing.security.s5': 'Authentication', 'landing.security.s5Desc': 'Secure JWT, 2FA, magic link and temporary passwords.',
  'landing.security.s6': 'Backups', 'landing.security.s6Desc': 'Automatic backups and one-click restore.',

  // Multi-device
  'landing.device.title': 'Your church follows you everywhere',
  'landing.device.subtitle': 'Access your space from your phone, tablet or computer. The experience is optimal on every screen.',
  'landing.device.phone': 'Phone', 'landing.device.tablet': 'Tablet', 'landing.device.desktop': 'Desktop',

  // Final CTA
  'landing.cta.title': 'Your church deserves more than a multitude of scattered tools.',
  'landing.cta.subtitle': 'Start today or request a personalised demonstration.',
  'landing.cta.primary': 'Get started now',
  'landing.cta.secondary': 'Request a demo',

  // Demo modal
  'landing.demo.title': 'Request a demonstration',
  'landing.demo.subtitle': 'Leave your details and our team will contact you to arrange a personalised demonstration for your church.',
  'landing.demo.name': 'Your name', 'landing.demo.namePh': 'John Doe',
  'landing.demo.email': 'Work email', 'landing.demo.emailPh': 'you@church.org',
  'landing.demo.church': 'Church name', 'landing.demo.churchPh': 'Local church',
  'landing.demo.role': 'Your role', 'landing.demo.rolePh': 'Pastor, leader…',
  'landing.demo.message': 'Your message (optional)', 'landing.demo.messagePh': 'Tell us about your church…',
  'landing.demo.send': 'Send request', 'landing.demo.sending': 'Sending…', 'landing.demo.close': 'Close',
  'landing.demo.successTitle': 'Request sent',
  'landing.demo.successBody': 'Thank you! Your demo request has been recorded. We will get back to you soon.',
  'landing.demo.fallback': 'Your mail app will open to send your request.',
  'landing.demo.required': 'Please fill in your name, email and church name.',

  // Footer
  'landing.footer.tagline': 'Making disciples of Jesus Christ',
  'landing.footer.product': 'Product', 'landing.footer.company': 'Company', 'landing.footer.legal': 'Legal',
  'landing.footer.rights': 'All rights reserved.', 'landing.footer.links': 'Useful links',
  'landing.footer.about': 'About', 'landing.footer.contact': 'Contact',
  'landing.footer.privacy': 'Privacy', 'landing.footer.terms': 'Terms',


  // Dîmes & offrandes (Mobile Money)
  'giving.title': 'Tithes & Offerings',
  'giving.subtitle': 'Instant Mobile Money — M-Pesa, MTN, Orange, Wave…',
  'giving.purpose': 'Donation type',
  'giving.operator': 'Operator',
  'giving.amount': 'Amount (XOF)',
  'giving.phone': 'Mobile Money number (optional)',
  'giving.phonePlaceholder': '+225 07 xx xx xx xx',
  'giving.donate': 'Give now',
  'giving.pending': 'Payment being confirmed by the operator…',
  'giving.byOperator': 'Breakdown by giving method',
  'giving.lastPayments': 'Latest payments',
  'giving.noPayments': 'No payments yet.',
  'giving.initiated': 'Payment initiated — reference {ref}',
  'giving.confirmed': 'Payment confirmed — receipt generated!',
  'giving.status.confirmed': 'Confirmed',
  'giving.status.pending': 'Pending',
  'giving.status.failed': 'Failed',
  'giving.status.cancelled': 'Cancelled',
  'giving.receipt': 'Tax receipt',
  'giving.receiptUnavailable': 'Receipt unavailable.',
  'giving.purpose.dime': 'Tithe',
  'giving.purpose.offrande': 'Offering',
  'giving.purpose.promesse': 'Pledge',
  'giving.purpose.projet': 'Special project',
  'giving.purpose.diaspora': 'Diaspora gift',
  'giving.operator.urban': 'Orange Money',
  'giving.recurring.title': 'Recurring gifts',
  'giving.recurring.new': 'New recurring gift',
  'giving.recurring.hide': 'Hide',
  'giving.recurring.create': 'Create recurring gift',
  'giving.recurring.frequency': 'Frequency',
  'giving.recurring.frequency.weekly': 'Weekly',
  'giving.recurring.frequency.bimonthly': 'Bi-monthly',
  'giving.recurring.frequency.monthly': 'Monthly',
  'giving.recurring.frequency.quarterly': 'Quarterly',
  'giving.recurring.frequency.yearly': 'Yearly',
  'giving.recurring.created': 'Recurring gift created successfully!',
  'giving.recurring.cancelled': 'Recurring gift cancelled.',
  'giving.recurring.cancel': 'Cancel',
  'giving.recurring.active': 'Active',
  'giving.recurring.inactive': 'Inactive',
  'giving.recurring.empty': 'No recurring gifts. Create one to automate your donations.',
  'giving.recurring.nextDate': 'Next due date',
  'giving.recurring.totalDonated': 'Total donated',
  'giving.recurring.donations': 'donations',

  // ── Payment admin dashboard ──
  'paymentDashboard.title': 'Payment Dashboard',
  'paymentDashboard.subtitle': 'Overview of financial flows',
  'paymentDashboard.totalPayments': 'Total payments',
  'paymentDashboard.confirmed': 'Confirmed',
  'paymentDashboard.pending': 'Pending',
  'paymentDashboard.failed': 'Failed',
  'paymentDashboard.cancelled': 'Cancelled',
  'paymentDashboard.confirmationRate': 'Confirmation rate',
  'paymentDashboard.avgAmount': 'Average amount',
  'paymentDashboard.maxAmount': 'Max amount',
  'paymentDashboard.minAmount': 'Min amount',
  'paymentDashboard.byOperator': 'By operator',
  'paymentDashboard.byPurpose': 'By purpose',
  'paymentDashboard.byProvider': 'By provider',
  'paymentDashboard.monthlyTrend': 'Monthly trend',
  'paymentDashboard.dailyTrend': 'Daily trend (30d)',
  'paymentDashboard.recurring': 'Recurring donations',
  'paymentDashboard.activeRecurring': 'Active',
  'paymentDashboard.monthlyCommitment': 'Monthly commitment',
  'paymentDashboard.avgCommitment': 'Average commitment',
  'paymentDashboard.totalProcessed': 'Total processed',
  'paymentDashboard.totalRecurringDonated': 'Total donated (recurring)',
  'paymentDashboard.recurringByFrequency': 'By frequency',
  'paymentDashboard.recurringByOperator': 'By operator',
  'paymentDashboard.payments': 'payments',
  'paymentDashboard.noData': 'No data available',
  'paymentDashboard.export': 'Export',
};

export default en;
