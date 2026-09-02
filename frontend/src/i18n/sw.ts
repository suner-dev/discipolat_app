const sw: Record<string, string> = {
  // Urambazaji
  'nav.dashboard': 'Dashibodi',
  'nav.souls': 'Nafsi',
  'nav.families': 'Familia',
  'nav.departments': 'Idara',
  'nav.reports': 'Ripoti',
  'nav.prayers': 'Maombi',
  'nav.events': 'Matukio',
  'nav.alerts': 'Tahadhari',
  'nav.search': 'Tafuta',
  'nav.messages': 'Ujumbe',
  'nav.profile': 'Wasifu',
  'nav.settings': 'Mipangilio',
  'nav.admin': 'Usimamizi',
  'nav.logout': 'Ondoka',

  // Utambulisho
  'auth.login': 'Ingia',
  'auth.email': 'Barua pepe',
  'auth.password': 'Nenosiri',
  'auth.forgotPassword': 'Umesahau nenosiri?',
  'auth.loginWith': 'Ingia na',
  'auth.google': 'Google',
  'auth.magicLink': 'Kiungo maalum',
  'auth.magicLinkSent': 'Kiungo cha kuingia kimetumwa kwenye barua pepe yako',
  'auth.googleUnavailable': 'Kuingia na Google haijawekwa kwenye seva hii.',
  'auth.noAccount': 'Huna akaunti bado?',
  'auth.createAccount': 'Fungua akaunti',
  'auth.welcome': 'Karibu Discipolat',
  'auth.welcomeMessage': 'Ingia kusimamia kanisa lako',

  // Dashibodi
  'dashboard.title': 'Dashibodi',
  'dashboard.totalSouls': 'Jumla ya nafsi',
  'dashboard.activeSouls': 'Nafsi hai',
  'dashboard.newConverts': 'Waliobadilishwa',
  'dashboard.activeAlerts': 'Tahadhari hai',
  'dashboard.pendingReports': 'Ripoti zinazosubiri',
  'dashboard.familyRisk': 'Familia hatarini',
  'dashboard.presenceRate': 'Kiwango cha uwepo',

  // Nafsi
  'souls.title': 'Nafsi / Wanafunzi',
  'souls.create': 'Nafsi mpya',
  'souls.search': 'Tafuta nafsi...',
  'souls.name': 'Jina',
  'souls.firstName': 'Jina la kwanza',
  'souls.phone': 'Simu',
  'souls.email': 'Barua pepe',
  'souls.status': 'Hali',
  'souls.faiseur': 'Mshauri',
  'souls.family': 'Familia',
  'souls.department': 'Idara',
  'souls.integrationDate': 'Tarehe ya kujiunga',
  'souls.spiritualState': 'Hali ya kiroho',

  // Hali
  'status.active': 'Hai',
  'status.integration': 'Inaungana',
  'status.sleep': 'Inalala',
  'status.dropped': 'Imeanguka',

  // Ripoti
  'reports.title': 'Ripoti',
  'reports.makerReport': 'Ripoti ya mshauri',
  'reports.familyReport': 'Ripoti ya familia',
  'reports.submit': 'Wasilisha',
  'reports.draft': 'Rasimu',
  'reports.submitted': 'Imewasilishwa',
  'reports.validated': 'Imethibitishwa',
  'reports.week': 'Wiki',
  'reports.present': 'Yupo',
  'reports.absent': 'Hapo',

  // Maombi
  'prayers.title': 'Maombi',
  'prayers.create': 'Ombi jipya',
  'prayers.priority': 'Kipaumbele',
  'prayers.category': 'Aina',
  'prayers.visibility': 'Uonekano',
  'prayers.answered': 'Imejibiwa',
  'prayers.pending': 'Inasubiri',
  'prayers.testimony': 'Ushahidi',

  // Matukio
  'events.title': 'Matukio',
  'events.create': 'Tukio jipya',
  'events.date': 'Tarehe',
  'events.location': 'Mahali',
  'events.register': 'Jiandikishe',
  'events.attendees': 'Washiriki',

  // Arifa
  'notifications.title': 'Arifa',
  'notifications.markAllRead': 'Weka zote kama zimesomwa',
  'notifications.empty': 'Hakuna arifa',

  // Mipangilio
  'settings.title': 'Mipangilio',
  'settings.language': 'Lugha',
  'settings.theme': 'Mandhari',
  'settings.darkMode': 'Hali ya giza',
  'settings.notifications': 'Arifa',
  'settings.privacy': 'Faragha',

  // Lugha
  'lang.fr': 'Français',
  'lang.en': 'English',
  'lang.pt': 'Português',
  'lang.es': 'Español',
  'lang.sw': 'Kiswahili',

  // Kawaida
  'common.save': 'Hifadhi',
  'common.cancel': 'Ghairi',
  'common.delete': 'Futa',
  'common.edit': 'Hariri',
  'common.create': 'Unda',
  'common.search': 'Tafuta',
  'common.filter': 'Chuja',
  'common.loading': 'Inapakia...',
  'common.noData': 'Hakuna data',
  'common.confirm': 'Thibitisha',
  'common.back': 'Rudi',
  'common.next': 'Ifuatayo',
  'common.previous': 'Iliyotangulia',
  'common.success': 'Imefanikiwa',
  'common.error': 'Hitilafu',
  'common.warning': 'Onyo',
  'common.info': 'Taarifa',
  'common.yes': 'Ndiyo',
  'common.no': 'Hapana',
  'common.all': 'Zote',
  'common.none': 'Hakuna',
  'common.total': 'Jumla',
  'common.actions': 'Vitendo',
  'common.details': 'Maelezo',
  'common.status': 'Hali',
  'common.date': 'Tarehe',
  'common.amount': 'Kiasi',
  'common.description': 'Maelezo',

  // Malipo
  'payments.title': 'Zaka & Zawadi',
  'payments.give': 'Toa sasa',
  'payments.amount': 'Kiasi (XOF)',
  'payments.operator': 'Opereta',
  'payments.destination': 'Lengo',
  'payments.phone': 'Simu ya Mobile Money',
  'payments.history': 'Historia ya zawadi',
  'payments.confirmed': 'Imethibitishwa',
  'payments.pending': 'Inasubiri',
  'payments.failed': 'Imeshindwa',

  // Msaidizi wa AI
  'ai.title': 'Msaidizi wa AI wa Kiroho',
  'ai.placeholder': 'Uliza maswali kwa msaidizi...',
  'ai.thinking': 'AI inafikiria...',
  'ai.send': 'Tuma',
  'ai.history': 'Historia',
  'ai.suggestions': 'Mapendekezo',

  // Tiketi
  'tickets.title': 'Tiketi na Msaada',
  'tickets.create': 'Tiketi mpya',
  'tickets.open': 'Fungua',
  'tickets.closed': 'Funga',

  // Uchaguzi
  'surveys.title': 'Uchaguzi wa haraka',
  'surveys.create': 'Uchaguzi mpya',
  'surveys.vote': 'Kura',
  'surveys.results': 'Matokeo',

  // Ushahidi
  'testimonials.title': 'Ukumbusho wa ushahidi',
  'testimonials.create': 'Shiriki ushahidi',
  'testimonials.approved': 'Imekubalika',
  'testimonials.pending': 'Inasubiri',

  'leaveRequests.title': 'Maombi ya likizo',
  'leaveRequests.create': 'Ombi jipya',
  'leaveRequests.approved': 'Imekubalika',
  'leaveRequests.rejected': 'Imekataliwa',

  'referrals.title': 'Mapendekezo',
  'referrals.invite': 'Alika',
  'referrals.rewards': 'Zawadi',

  'calendar.title': 'Muunganisho wa kalenda',
  'calendar.sync': 'Sawazisha',
  'calendar.connect': 'Unganisha',

  'skillsMatrix.title': 'Matriisi ya ujuzi',
  'skillsMatrix.evaluate': 'Tathmini',
  'skillsMatrix.level': 'Kiwango',

  'teamGantt.title': 'Chati ya Gantt ya timu',
  'teamGantt.tasks': 'Kazi',
  'teamGantt.timeline': 'Ratiba',

  'compliance.title': 'Dashibodi ya kufuata sheria',
  'compliance.export': 'Hamisha',
  'compliance.delete': 'Futa data',

  'apiDocs.title': 'API na Nyaraka',
  'apiDocs.keys': 'Ufunguo wa API',
  'apiDocs.swagger': 'Swagger',

  'onboarding.title': 'Msaidizi wa usanidi',
  'onboarding.step1': 'Utambulisho',
  'onboarding.step2': 'Ingiza wanachama',
  'onboarding.step3': 'Muundo',
  'onboarding.step4': 'Majukumu',
  'onboarding.step5': 'Tukio la kwanza',

  'cercleFaiseurs.title': 'Duara la washauri',
  'cercleFaiseurs.members': 'Wanachama',
  'cercleFaiseurs.sessions': 'Vikao',

  'bibleReading.title': 'Mpango wa kusoma Biblia',
  'bibleReading.progress': 'Maendeleo',
  'bibleReading.daily': 'Kila siku',

  'prayerJournal.title': 'Diari ya maombi',
  'prayerJournal.add': 'Ongeza',

  'spiritualChallenges.title': 'Changamoto za kiroho',
  'spiritualChallenges.create': 'Unda changamoto',

  'directory.title': 'Orodha ya kanisa',
  'spiritualJourney.title': 'Safari ya kiroho',

  'automations.title': 'Automatiksheni za kihuduma',
  'automations.create': 'Automatiksheni mpya',

  // Streaming
  'nav.streaming': 'Kutiririka moja kwa moja',
  'streaming.live': 'Moja kwa moja',
  'streaming.scheduled': 'Imepangwa',
  'streaming.ended': 'Imekwisha',

  // Broadcast
  'nav.broadcast': 'Kutangaza / Broadcast',
  'broadcast.new': 'Ujumbe mpya',
  'broadcast.send': 'Tuma',
  'broadcast.draft': 'Rasimu',

  // Inventory
  'nav.inventory': 'Hifadhidata akili',
  'inventory.lowStock': 'Hifadhi ndogo',
  'inventory.categories': 'Aina',

  // Department KPIs
  'nav.departmentKpis': 'Vipimo vya Idara',
  'kpi.progress': 'Maendeleo',
  'kpi.target': 'Lengo',

  // Rewards
  'nav.rewards': 'Zawadi na Gamification',
  'rewards.points': 'Alama zangu',
  'rewards.claimed': 'Imepatikana',
  'rewards.locked': 'Imefungwa',

  // Marketplace
  'nav.marketplace': 'Soko la jamii',
  'marketplace.offer': 'Ofa',
  'marketplace.request': 'Ombi',
  'marketplace.service': 'Huduma',
  'marketplace.free': 'Bure',

  // Community
  'nav.community': 'Jamii',
  'community.testimony': 'Ushahidi',
  'community.prayer': 'Maombi',
  'community.encouragement': 'Uhimilisho',

  // AI Predictions
  'nav.aiPredictions': 'Utabiri wa AI',
  'aiPredictions.title': 'Utabiri na uchambuzi wa AI',
  'aiPredictions.growth': 'Kuongoza ukuaji',
  'aiPredictions.risk': 'Hatari ya kuondoka',
  'aiPredictions.trends': 'Mwenendo',

  // Automatisations — new keys
  'nav.automations': 'Automatiksheni',
  'automations.trigger': 'Kiendeshaji',
  'automations.action': 'Kitendo',
  'automations.active': 'Hai',
  'automations.paused': 'Imesimamishwa',
  'automations.executions': 'utekelezaji',

  // Mentorat IA
  'nav.mentoring': 'Ushauri wa AI',
  'mentoring.title': 'Ushauri wa AI — Viongozi wa Familia',
  'mentoring.generate': 'Zalisha mapendekezo',
  'mentoring.accompaniment': 'Ufuatiliaji',
  'mentoring.formation': 'Mafunzo',
  'mentoring.delegation': 'Uwakilishi',
  'mentoring.recognition': 'Utambuzi',
  'mentoring.actionRecommanded': 'Kitendo kilichopendekezwa',
  'mentoring.reasoning': 'Sababu',
  'mentoring.confidence': 'Ujasiri',

  // KPI Drill-down
  'nav.kpiDrilldown': 'Uchambuzi wa KPI',
  'kpiDrilldown.title': 'Uchambuzi wa Hadithi wa KPI',
  'kpiDrilldown.clickToAnalyze': 'Bofya ili uchambue',
  'kpiDrilldown.narrative': 'Hadithi',
  'kpiDrilldown.causes': 'Sababu zilizotambuliwa',
  'kpiDrilldown.recommendations': 'Mapendekezo',
  'kpiDrilldown.trend': 'Mwenendo',

  // Prayer Journal — new keys
  'nav.prayerJournal': 'Diari ya Maombi',
  'prayerJournal.answered': 'Imejibiwa',
  'prayerJournal.remembered': 'Imekumbukwa',
  'prayerJournal.inProgress': 'Inaendelea',

  // Spiritual Challenges — new keys
  'nav.spiritualChallenges': 'Changamoto za Kiroho',
  'spiritualChallenges.progress': '+1 siku',

  // Skills Matrix — new keys
  'nav.skillsMatrix': 'Matriisi ya Ujuzi',
  'skills.evaluate': 'Tathmini',
  'skills.level': 'Kiwango',

  // Team Gantt — new keys
  'nav.teamGantt': 'Ratiba ya Timu',
  'gantt.team': 'Timu',
  'gantt.timeline': 'Muda',

  // Calendar Integration — new keys
  'nav.calendar': 'Kalenda',
  'calendar.export': 'Hamisha iCal',

  // Broadcast — new keys
  'broadcast.title': 'Utangazaji / Broadcast',
  'broadcast.sent': 'Imetumwa',
  'broadcast.scheduled': 'Imepangwa',
  'broadcast.readReceipts': 'Ushahidi wa kusoma',

  // Church Directory — new keys
  'nav.directory': 'Orodha',
  'directory.myProfile': 'Wasifu wangu',
  'directory.public': 'Ya umma',
  'directory.private': 'Ya kibinafsi',

  // Personal Objectives
  'nav.personalObjectives': 'Malengo ya Kiroho',
  'personalObjectives.title': 'Malengo ya Kiroho Binafsi',
  'personalObjectives.create': 'Lengo jipya',
  'personalObjectives.progress': '+1 maendeleo',

  // Succession
  'nav.succession': 'Mpango wa Urithi',
  'succession.title': 'Mpango wa Urithi',
  'succession.create': 'Mpango mpya',
  'succession.readiness': 'Uko tayari',

  // Pastoral Visits
  'nav.pastoralVisits': 'Ziara za Kihuduma',
  'pastoralVisits.title': 'Ziara za Kihuduma',
  'pastoralVisits.autoGenerate': 'Zalisha moja kwa moja',
  'pastoralVisits.completed': 'Imekamilika',

  // Family Resources
  'nav.familyResources': 'Rasilimali za Familia',
  'familyResources.title': 'Benki ya Rasilimali',
  'familyResources.share': 'Shiriki',
  'familyResources.document': 'Hati',
  'familyResources.video': 'Video',

  // Family Cohesion
  'nav.familyCohesion': 'Umoja wa Familia',
  'familyCohesion.title': 'Umoja wa Familia',
  'familyCohesion.score': 'Alama ya umoja',
  'familyCohesion.recommendations': 'Mapendekezo',

  // Currency
  'nav.currency': 'Sarafu Nyingi & Muda',
  'currency.primary': 'Sarafu kuu',
  'currency.all': 'Sarafu zilizowekwa',
  'currency.add': 'Ongeza',
  'currency.timezones': 'Muda wa eneo',

  // Content Moderation
  'nav.moderation': 'Ufilisi wa AI',
  'moderation.title': 'Kichujio cha moderation AI',
  'moderation.pending': 'Inasubiri',
  'moderation.approved': 'Imekubalika',
  'moderation.rejected': 'Imekataliwa',

  // Predictions ML
  'nav.predictionsMl': 'Utabiri ML',
  'predictions.title': 'Utabiri ML',
  'predictions.growth': 'Ukuaji',
  'predictions.risk': 'Hatari',
  'predictions.trends': 'Mwenendo',

  // Intelligence Center
  'nav.intelligenceCenter': 'Kituo cha Akili',
  'intelligence.title': 'Kituo cha Akili (50+ KPIs)',
  'intelligence.alerts': 'Tahadhari hai',

  // Engagement Analytics
  'nav.engagementAnalytics': 'Uchambuzi Ushiriki',
  'engagement.title': 'Uchambuzi wa ushiriki',
  'engagement.pages': 'Kurasa zilizotazamwa',
  'engagement.actions': 'Vitendo vya mtumiaji',
  'engagement.funnels': 'Funnel',
  'engagement.retention': 'Kushikilia',

  // Scheduled Announcements
  'nav.scheduledAnnouncements': 'Tangazo zilizopangwa',
  'announcements.title': 'Tangazo zilizopangwa',
  'announcements.draft': 'Rasimu',
  'announcements.scheduled': 'Imepangwa',
  'announcements.published': 'Imetolewa',

  // Event Checklists
  'nav.eventChecklists': 'Orodha ya matukio',
  'checklist.title': 'Orodha za matukio',
  'checklist.generate': 'Zalisha',
  'checklist.completed': 'Imekamilika',

  // Group Messages
  'nav.groupMessages': 'Ujumbe wa kikundi',
  'groupMessages.title': 'Ujumbe wa kikundi',
  'groupMessages.groups': 'Vikundi',
  'groupMessages.placeholder': 'Andika ujumbe...',
  'groupMessages.empty': 'Hakuna ujumbe',

  // Weekly Challenges
  'nav.weeklyChallenges': 'Changamoto za wiki',
  'challenges.title': 'Changamoto za wiki',
  'challenges.progress': 'Maendeleo',
  'challenges.active': 'Hai',
  'challenges.completed': 'Imekamilika',

  // Discipleship Path
  'nav.discipleshipPath': 'Njia ya ufunzi',
  'discipleship.title': 'Njia ya ufunzi ya AI',
  'discipleship.progress': 'Maendeleo',
  'discipleship.recommendation': 'Mapendekezo ya AI',

  // AI Visit Notes
  'nav.aiVisitNotes': 'Vidokezo vya AI Ziara',
  'visitNotes.title': 'Vidokezo vya AI Ziara',
  'visitNotes.summary': 'Muhtasari wa AI',
  'visitNotes.actions': 'Vitendo vinavyopendekezwa',
  'visitNotes.sentiment': 'Hisia',

  // Reverse Mentoring
  'nav.reverseMentoring': 'Ushauri wa kinyume',
  'reverseMentoring.title': 'Ushauri wa kinyume',
  'reverseMentoring.request': 'Omba msaada',
  'reverseMentoring.pending': 'Inasubiri',

  // Family Meetings
  'nav.familyMeetings': 'Mikutano ya familia auto',
  'familyMeeting.title': 'Mikutano ya familia iliyotolewa kiotomatiki',
  'familyMeeting.agenda': 'Agenda ya kiotomatiki',
  'familyMeeting.generate': 'Zalisha',

  // Executive Insights
  'nav.executiveInsights': 'Ufahamu wa Utendaji AI',
  'executiveInsights.title': 'Ufahamu wa Utendaji AI',
  'executiveInsights.critical': 'Hatari',
  'executiveInsights.warning': 'Tahadhari',
  'executiveInsights.opportunity': 'Fursa',
  'executiveInsights.action': 'Kitendo kilichopendekezwa',

  // Upcoming Events
  'nav.upcomingEvents': 'Matukio yajayo',
  'upcomingEvents.title': 'Matukio yangu yajayo',
  'upcomingEvents.going': 'Nitaenda',
  'upcomingEvents.interested': 'Ninapenda',

  // My Team/Family
  'nav.myTeam': 'Timu / Familia yangu',
  'myTeam.title': 'Timu / Familia yangu',
  'myTeam.message': 'Tuma ujumbe',
  'myTeam.encourage': 'Himiza',

  // P1 — Fomu
  'nav.forms': 'Fomu',
  'forms.title': 'Mtengenezaji wa fomu',
  'forms.create': 'Unda fomu',
  'forms.dragDrop': 'Buruta na kudondosha sehemu',

  // P1 — Tafsiri mahubiri
  'nav.sermonTranslations': 'Tafsiri ya mahubiri',
  'sermonTranslations.title': 'Tafsiri ya moja kwa moja',
  'sermonTranslations.whisper': 'Whisper → LLM → manukuu',

  // P1 — Jarida la kiroho
  'nav.spiritualJournal': 'Jarida la kiroho',
  'spiritualJournal.title': 'Jarida langu la kiroho',
  'spiritualJournal.prayer': 'Maombi',
  'spiritualJournal.reflection': 'Kutafakari',
  'spiritualJournal.streak': 'Mfululizo',

  // P1 — Maombi ya usimamizi
  'nav.adminRequests': 'Maombi ya usimamizi',
  'adminRequests.title': 'Maombi ya usimamizi',
  'adminRequests.baptism': 'Ubatizo',
  'adminRequests.dedication': 'Kujitolea',

  // P1 — Mpango wa ukuaji
  'nav.devPlans': 'Mpango wa maendeleo',
  'devPlans.title': 'Mpango wangu wa maendeleo',
  'devPlans.progress': 'Maendeleo',
  'devPlans.autoGenerate': 'Zalisha kiotomatiki',

  // P1 — Fuatiliaji wa mhudumu
  'nav.makerTracking': 'Fuatiliaji wa mhudumu',
  'makerTracking.title': 'Njia yangu ya mhudumu',
  'makerTracking.formations': 'Mafunzo',
  'makerTracking.skills': 'Ujuzi',

  // P1 — Makadirio ya ukuaji
  'nav.growthProjection': 'Makadirio ya ukuaji',
  'growthProjection.title': 'Makadirio ya ukuaji',
  'growthProjection.simulate': 'Simulate',

  // P1 — Linganisha kanisa
  'nav.churchComparison': 'Linganisha kanisa',
  'churchComparison.title': 'Benchmark ya mtandao',
  'churchComparison.benchmark': 'Linganisha',

  // P1 — Kujitolea
  'nav.volunteers': 'Wajitolea',
  'volunteers.title': 'Usimamizi wa wajitolea',
  'volunteers.match': 'Linganisha wajitolea',

  // P1 — Linganisha ujuzi
  'nav.skillMatching': 'Ulinganishaji wa ujuzi',
  'skillMatching.title': 'Ulinganishaji wa AI',
  'skillMatching.accept': 'Kubali',
  'skillMatching.decline': 'Kataa',

  // P1 — Udhibiti wa maudhui
  'nav.contentModeration': 'Udhibiti wa maudhui',
  'contentModeration.title': 'Udhibiti kwa AI',
  'contentModeration.approve': 'Idhinisha',
  'contentModeration.reject': 'Kataa',



  // Dîmes & offrandes (Mobile Money)
  'giving.title': 'Zaka na Sadaka',
  'giving.subtitle': 'Pesa ya simu ya papo hapo — M-Pesa, MTN, Orange, Wave…',
  'giving.purpose': 'Aina ya mchango',
  'giving.operator': 'Mtoa huduma',
  'giving.amount': 'Kiasi (XOF)',
  'giving.phone': 'Nambari ya pesa ya simu (si lazima)',
  'giving.phonePlaceholder': '+225 07 xx xx xx xx',
  'giving.donate': 'Toa sasa',
  'giving.pending': 'Malipo yanathibitishwa na mtoa huduma…',
  'giving.byOperator': 'Mgawanyo kwa njia ya mchango',
  'giving.lastPayments': 'Malipo ya hivi karibuni',
  'giving.noPayments': 'Hakuna malipo bado.',
  'giving.initiated': 'Malipo yameanzishwa — rejeleo {ref}',
  'giving.confirmed': 'Malipo yamethibitishwa — risiti imetolewa!',
  'giving.status.confirmed': 'Imethibitishwa',
  'giving.status.pending': 'Inasubiri',
  'giving.status.failed': 'Imeshindwa',
  'giving.status.cancelled': 'Imeghairiwa',
  'giving.cancel': 'Ghairi',
  'giving.cancelled': 'Malipo yameghairiwa.',
  'giving.receipt': 'Risiti ya kodi',
  'giving.receiptUnavailable': 'Risiti haipatikani.',
  'giving.purpose.dime': 'Zaka',
  'giving.purpose.offrande': 'Sadaka',
  'giving.purpose.promesse': 'Ahadi',
  'giving.purpose.projet': 'Mradi maalum',
  'giving.purpose.diaspora': 'Mchango wa diaspora',
  'giving.operator.urban': 'Orange Money',
  'giving.recurring.title': 'Michango ya mara kwa mara',
  'giving.recurring.new': 'Mchango mpya wa mara kwa mara',
  'giving.recurring.hide': 'Ficha',
  'giving.recurring.create': 'Unda mchango wa mara kwa mara',
  'giving.recurring.frequency': 'Mzunguko',
  'giving.recurring.frequency.weekly': 'Kila wiki',
  'giving.recurring.frequency.bimonthly': 'Kila wiki mbili',
  'giving.recurring.frequency.monthly': 'Kila mwezi',
  'giving.recurring.frequency.quarterly': 'Kila robo mwaka',
  'giving.recurring.frequency.yearly': 'Kila mwaka',
  'giving.recurring.created': 'Mchango wa mara kwa mara umefanikiwa kuundwa!',
  'giving.recurring.cancelled': 'Mchango wa mara kwa mara umefutwa.',
  'giving.recurring.cancel': 'Ghairi',
  'giving.recurring.active': 'Hai',
  'giving.recurring.inactive': 'Haijatimizwa',
  'giving.recurring.empty': 'Hakuna michango ya mara kwa mara. Unda moja ili kujitolea kiotomatiki.',
  'giving.recurring.nextDate': 'Tarehe inayofuata',
  'giving.recurring.totalDonated': 'Jumla ya michango',
  'giving.recurring.donations': 'michango',

  // ── Dashibodi ya malipo ya admin ──
  'paymentDashboard.title': 'Dashibodi ya malipo',
  'paymentDashboard.subtitle': 'Muhtasari wa mtiririko wa fedha',
  'paymentDashboard.totalPayments': 'Jumla ya malipo',
  'paymentDashboard.confirmed': 'Imethibitishwa',
  'paymentDashboard.pending': 'Inasubiri',
  'paymentDashboard.failed': 'Imeshindwa',
  'paymentDashboard.cancelled': 'Imeghairiwa',
  'paymentDashboard.confirmationRate': 'Kiwango cha uthibitisho',
  'paymentDashboard.avgAmount': 'Wastani',
  'paymentDashboard.maxAmount': 'Kiwango cha juu',
  'paymentDashboard.minAmount': 'Kiwango cha chini',
  'paymentDashboard.byOperator': 'Kwa mtendaji',
  'paymentDashboard.byPurpose': 'Kwa madhumuni',
  'paymentDashboard.byProvider': 'Kwa mtoaji',
  'paymentDashboard.monthlyTrend': 'Mwelekeo wa kila mwezi',
  'paymentDashboard.dailyTrend': 'Mwelekeo wa kila siku (30siku)',
  'paymentDashboard.recurring': 'Michango ya mara kwa mara',
  'paymentDashboard.activeRecurring': 'Hai',
  'paymentDashboard.monthlyCommitment': 'Ahadi ya kila mwezi',
  'paymentDashboard.avgCommitment': 'Ahadi ya wastani',
  'paymentDashboard.totalProcessed': 'Jumla iliyosindikwa',
  'paymentDashboard.totalRecurringDonated': 'Jumla iliyotolewa (recurring)',
  'paymentDashboard.recurringByFrequency': 'Kwa masafa',
  'paymentDashboard.recurringByOperator': 'Kwa mtendaji',
  'paymentDashboard.payments': 'malipo',
  'paymentDashboard.noData': 'Hakuna data inayopatikana',
  'paymentDashboard.export': 'Hamisha',
};

export default sw;
