const es: Record<string, string> = {
  // Navegación
  'nav.dashboard': 'Panel de control',
  'nav.souls': 'Almas',
  'nav.families': 'Familias',
  'nav.departments': 'Departamentos',
  'nav.reports': 'Informes',
  'nav.prayers': 'Oraciones',
  'nav.events': 'Eventos',
  'nav.alerts': 'Alertas',
  'nav.search': 'Buscar',
  'nav.messages': 'Mensajes',
  'nav.profile': 'Perfil',
  'nav.settings': 'Configuración',
  'nav.admin': 'Administración',
  'nav.logout': 'Cerrar sesión',

  // Auth
  'auth.login': 'Iniciar sesión',
  'auth.email': 'Correo electrónico',
  'auth.password': 'Contraseña',
  'auth.forgotPassword': '¿Olvidaste tu contraseña?',
  'auth.loginWith': 'Iniciar sesión con',
  'auth.google': 'Google',
  'auth.magicLink': 'Enlace mágico',
  'auth.magicLinkSent': 'Se ha enviado un enlace de inicio de sesión a tu correo',
  'auth.googleUnavailable': 'El inicio de sesión con Google no está configurado en este servidor.',
  'auth.noAccount': '¿No tienes cuenta?',
  'auth.createAccount': 'Crear cuenta',
  'auth.welcome': 'Bienvenido a Discipolat',
  'auth.welcomeMessage': 'Inicia sesión para gestionar tu iglesia',

  // Panel de control
  'dashboard.title': 'Panel de control',
  'dashboard.totalSouls': 'Total de almas',
  'dashboard.activeSouls': 'Almas activas',
  'dashboard.newConverts': 'Nuevos convertidos',
  'dashboard.activeAlerts': 'Alertas activas',
  'dashboard.pendingReports': 'Informes pendientes',
  'dashboard.familyRisk': 'Familias en riesgo',
  'dashboard.presenceRate': 'Tasa de asistencia',

  // Almas
  'souls.title': 'Almas / Discípulos',
  'souls.create': 'Nueva alma',
  'souls.search': 'Buscar un alma...',
  'souls.name': 'Nombre',
  'souls.firstName': 'Nombre',
  'souls.phone': 'Teléfono',
  'souls.email': 'Correo',
  'souls.status': 'Estado',
  'souls.faiseur': 'Mentor',
  'souls.family': 'Familia',
  'souls.department': 'Departamento',
  'souls.integrationDate': 'Fecha de integración',
  'souls.spiritualState': 'Estado espiritual',

  // Estado
  'status.active': 'Activo',
  'status.integration': 'En integración',
  'status.sleep': 'En espera',
  'status.dropped': 'Desertor',

  // Informes
  'reports.title': 'Informes',
  'reports.makerReport': 'Informe del mentor',
  'reports.familyReport': 'Informe familiar',
  'reports.submit': 'Enviar',
  'reports.draft': 'Borrador',
  'reports.submitted': 'Enviado',
  'reports.validated': 'Validado',
  'reports.week': 'Semana',
  'reports.present': 'Presente',
  'reports.absent': 'Ausente',

  // Oraciones
  'prayers.title': 'Oraciones',
  'prayers.create': 'Nueva oración',
  'prayers.priority': 'Prioridad',
  'prayers.category': 'Categoría',
  'prayers.visibility': 'Visibilidad',
  'prayers.answered': 'Atendida',
  'prayers.pending': 'Pendiente',
  'prayers.testimony': 'Testimonio',

  // Eventos
  'events.title': 'Eventos',
  'events.create': 'Nuevo evento',
  'events.date': 'Fecha',
  'events.location': 'Lugar',
  'events.register': 'Inscribirse',
  'events.attendees': 'Participantes',

  // Notificaciones
  'notifications.title': 'Notificaciones',
  'notifications.markAllRead': 'Marcar todas como leídas',
  'notifications.empty': 'Sin notificaciones',

  // Configuración
  'settings.title': 'Configuración',
  'settings.language': 'Idioma',
  'settings.theme': 'Tema',
  'settings.darkMode': 'Modo oscuro',
  'settings.notifications': 'Notificaciones',
  'settings.privacy': 'Privacidad',

  // Idiomas
  'lang.fr': 'Français',
  'lang.en': 'English',
  'lang.pt': 'Português',
  'lang.es': 'Español',
  'lang.sw': 'Kiswahili',

  // Común
  'common.save': 'Guardar',
  'common.cancel': 'Cancelar',
  'common.delete': 'Eliminar',
  'common.edit': 'Editar',
  'common.create': 'Crear',
  'common.search': 'Buscar',
  'common.filter': 'Filtrar',
  'common.loading': 'Cargando...',
  'common.noData': 'Sin datos',
  'common.confirm': 'Confirmar',
  'common.back': 'Atrás',
  'common.next': 'Siguiente',
  'common.previous': 'Anterior',
  'common.success': 'Éxito',
  'common.error': 'Error',
  'common.warning': 'Advertencia',
  'common.info': 'Información',
  'common.yes': 'Sí',
  'common.no': 'No',
  'common.all': 'Todos',
  'common.none': 'Ninguno',
  'common.total': 'Total',
  'common.actions': 'Acciones',
  'common.details': 'Detalles',
  'common.status': 'Estado',
  'common.date': 'Fecha',
  'common.amount': 'Monto',
  'common.description': 'Descripción',

  // Pagos
  'payments.title': 'Diezmos & Ofrendas',
  'payments.give': 'Dar ahora',
  'payments.amount': 'Monto (XOF)',
  'payments.operator': 'Operador',
  'payments.destination': 'Destino',
  'payments.phone': 'Teléfono Mobile Money',
  'payments.history': 'Historial de donaciones',
  'payments.confirmed': 'Confirmado',
  'payments.pending': 'Pendiente',
  'payments.failed': 'Fallido',

  // Asistente IA
  'ai.title': 'Asistente IA Pastoral',
  'ai.placeholder': 'Haz una pregunta al asistente...',
  'ai.thinking': 'La IA está pensando...',
  'ai.send': 'Enviar',
  'ai.history': 'Historial',
  'ai.suggestions': 'Sugerencias',

  // Tickets
  'tickets.title': 'Tickets & Soporte',
  'tickets.create': 'Nuevo ticket',
  'tickets.open': 'Abierto',
  'tickets.closed': 'Cerrado',

  // Sondages
  'surveys.title': 'Encuestas rápidas',
  'surveys.create': 'Nueva encuesta',
  'surveys.vote': 'Votar',
  'surveys.results': 'Resultados',

  // Témoignages
  'testimonials.title': 'Galería de testimonios',
  'testimonials.create': 'Compartir testimonio',
  'testimonials.approved': 'Aprobado',
  'testimonials.pending': 'Pendiente',

  // Absences
  'leaveRequests.title': 'Solicitudes de ausencia',
  'leaveRequests.create': 'Nueva solicitud',
  'leaveRequests.approved': 'Aprobada',
  'leaveRequests.rejected': 'Rechazada',

  // Parrainage
  'referrals.title': 'Referidos',
  'referrals.invite': 'Invitar',
  'referrals.rewards': 'Recompensas',

  // Calendrier
  'calendar.title': 'Integración de calendario',
  'calendar.sync': 'Sincronizar',
  'calendar.connect': 'Conectar',

  // Compétences
  'skillsMatrix.title': 'Matriz de competencias',
  'skillsMatrix.evaluate': 'Evaluar',
  'skillsMatrix.level': 'Nivel',

  // Gantt
  'teamGantt.title': 'Diagrama Gantt de equipos',
  'teamGantt.tasks': 'Tareas',
  'teamGantt.timeline': 'Cronograma',

  // Conformité
  'compliance.title': 'Panel de conformidad RGPD',
  'compliance.export': 'Exportar',
  'compliance.delete': 'Eliminar datos',

  // API Docs
  'apiDocs.title': 'API & Documentación',
  'apiDocs.keys': 'Claves API',
  'apiDocs.swagger': 'Swagger',

  // Onboarding
  'onboarding.title': 'Asistente de configuración',
  'onboarding.step1': 'Identidad',
  'onboarding.step2': 'Importar miembros',
  'onboarding.step3': 'Estructura',
  'onboarding.step4': 'Roles',
  'onboarding.step5': 'Primer evento',

  // Cercle Faiseurs
  'cercleFaiseurs.title': 'Círculo de mentores',
  'cercleFaiseurs.members': 'Miembros',
  'cercleFaiseurs.sessions': 'Sesiones',

  // Bible Reading
  'bibleReading.title': 'Plan de lectura bíblica',
  'bibleReading.progress': 'Progreso',
  'bibleReading.daily': 'Diario',

  // Prayer Journal
  'prayerJournal.title': 'Diario de oración',
  'prayerJournal.add': 'Agregar',

  // Spiritual Challenges
  'spiritualChallenges.title': 'Desafíos espirituales',
  'spiritualChallenges.create': 'Crear desafío',

  // Directory
  'directory.title': 'Directorio de la iglesia',

  // Spiritual Journey
  'spiritualJourney.title': 'Viaje espiritual',

  // Automations
  'automations.title': 'Automatizaciones pastorales',
  'automations.create': 'Nueva automatización',

  // Streaming
  'nav.streaming': 'Streaming en vivo',
  'streaming.live': 'En vivo',
  'streaming.scheduled': 'Programado',
  'streaming.ended': 'Finalizado',

  // Broadcast
  'nav.broadcast': 'Difusión / Broadcast',
  'broadcast.new': 'Nueva difusión',
  'broadcast.send': 'Enviar',
  'broadcast.draft': 'Borrador',

  // Inventory
  'nav.inventory': 'Inventario inteligente',
  'inventory.lowStock': 'Stock bajo',
  'inventory.categories': 'Categorías',

  // Department KPIs
  'nav.departmentKpis': 'KPIs departamentales',
  'kpi.progress': 'Progreso',
  'kpi.target': 'Objetivo',

  // Rewards
  'nav.rewards': 'Recompensas y gamificación',
  'rewards.points': 'Mis puntos',
  'rewards.claimed': 'Obtenido',
  'rewards.locked': 'Bloqueado',

  // Marketplace
  'nav.marketplace': 'Marketplace comunitario',
  'marketplace.offer': 'Oferta',
  'marketplace.request': 'Solicitud',
  'marketplace.service': 'Servicio',
  'marketplace.free': 'Gratis',

  // Community
  'nav.community': 'Comunidad',
  'community.testimony': 'Testimonio',
  'community.prayer': 'Oración',
  'community.encouragement': 'Animación',

  // AI Predictions
  'nav.aiPredictions': 'Predicciones IA',
  'aiPredictions.title': 'Predicciones y análisis IA',
  'aiPredictions.growth': 'Crecimiento previsto',
  'aiPredictions.risk': 'Riesgo de abandono',
  'aiPredictions.trends': 'Tendencias',

  // Automatisations — new keys
  'nav.automations': 'Automatizaciones',
  'automations.trigger': 'Desencadenante',
  'automations.action': 'Acción',
  'automations.active': 'Activa',
  'automations.paused': 'En pausa',
  'automations.executions': 'ejecuciones',

  // Mentorat IA
  'nav.mentoring': 'Mentoría IA',
  'mentoring.title': 'Mentoría IA — Jefes de Familia',
  'mentoring.generate': 'Generar sugerencias',
  'mentoring.accompaniment': 'Acompañamiento',
  'mentoring.formation': 'Formación',
  'mentoring.delegation': 'Delegación',
  'mentoring.recognition': 'Reconocimiento',
  'mentoring.actionRecommanded': 'Acción recomendada',
  'mentoring.reasoning': 'Razonamiento',
  'mentoring.confidence': 'Confianza',

  // KPI Drill-down
  'nav.kpiDrilldown': 'Análisis KPI',
  'kpiDrilldown.title': 'Análisis Narrativo de KPI',
  'kpiDrilldown.clickToAnalyze': 'Haz clic para analizar',
  'kpiDrilldown.narrative': 'Narrativa',
  'kpiDrilldown.causes': 'Causas identificadas',
  'kpiDrilldown.recommendations': 'Recomendaciones',
  'kpiDrilldown.trend': 'Tendencia',

  // Prayer Journal — new keys
  'nav.prayerJournal': 'Diario de Oración',
  'prayerJournal.answered': 'Atendida',
  'prayerJournal.remembered': 'Recordada',
  'prayerJournal.inProgress': 'En curso',

  // Spiritual Challenges — new keys
  'nav.spiritualChallenges': 'Desafíos Espirituales',
  'spiritualChallenges.progress': '+1 día',

  // Skills Matrix — new keys
  'nav.skillsMatrix': 'Matriz de Competencias',
  'skills.evaluate': 'Evaluar',
  'skills.level': 'Nivel',

  // Team Gantt — new keys
  'nav.teamGantt': 'Planeación de Equipos',
  'gantt.team': 'Equipo',
  'gantt.timeline': 'Cronograma',

  // Calendar Integration — new keys
  'nav.calendar': 'Calendario',
  'calendar.export': 'Exportar iCal',

  // Broadcast — new keys
  'broadcast.title': 'Difusión / Broadcast',
  'broadcast.sent': 'Enviado',
  'broadcast.scheduled': 'Programado',
  'broadcast.readReceipts': 'Acuse de recibo',

  // Church Directory — new keys
  'nav.directory': 'Directorio',
  'directory.myProfile': 'Mi perfil',
  'directory.public': 'Público',
  'directory.private': 'Privado',

  // Personal Objectives
  'nav.personalObjectives': 'Objetivos Espirituales',
  'personalObjectives.title': 'Objetivos Espirituales Personales',
  'personalObjectives.create': 'Nuevo objetivo',
  'personalObjectives.progress': '+1 progreso',

  // Succession
  'nav.succession': 'Plan de Sucesión',
  'succession.title': 'Plan de Sucesión',
  'succession.create': 'Nuevo plan',
  'succession.readiness': 'Preparación',

  // Pastoral Visits
  'nav.pastoralVisits': 'Visitas Pastorales',
  'pastoralVisits.title': 'Visitas Pastorales',
  'pastoralVisits.autoGenerate': 'Generación automática',
  'pastoralVisits.completed': 'Realizada',

  // Family Resources
  'nav.familyResources': 'Recursos Familiares',
  'familyResources.title': 'Banco de Recursos',
  'familyResources.share': 'Compartir',
  'familyResources.document': 'Documento',
  'familyResources.video': 'Video',

  // Family Cohesion
  'nav.familyCohesion': 'Cohesión Familiar',
  'familyCohesion.title': 'Cohesión Familiar',
  'familyCohesion.score': 'Puntuación de cohesión',
  'familyCohesion.recommendations': 'Recomendaciones',

  // Currency
  'nav.currency': 'Multi-divisa & Zonas',
  'currency.primary': 'Divisa principal',
  'currency.all': 'Divisas configuradas',
  'currency.add': 'Agregar',
  'currency.timezones': 'Zonas horarias',

  // Content Moderation
  'nav.moderation': 'Moderación IA',
  'moderation.title': 'Filtro de moderación IA',
  'moderation.pending': 'Pendiente',
  'moderation.approved': 'Aprobado',
  'moderation.rejected': 'Rechazado',

  // Predictions ML
  'nav.predictionsMl': 'Predicciones ML',
  'predictions.title': 'Predicciones ML',
  'predictions.growth': 'Crecimiento',
  'predictions.risk': 'Riesgo',
  'predictions.trends': 'Tendencias',

  // Intelligence Center
  'nav.intelligenceCenter': 'Centro Inteligencia',
  'intelligence.title': 'Centro de Inteligencia (50+ KPIs)',
  'intelligence.alerts': 'Alertas activas',

  // Engagement Analytics
  'nav.engagementAnalytics': 'Analytics Engagement',
  'engagement.title': 'Analytics de engagement',
  'engagement.pages': 'Páginas vistas',
  'engagement.actions': 'Acciones usuario',
  'engagement.funnels': 'Funnel',
  'engagement.retention': 'Retención',

  // Scheduled Announcements
  'nav.scheduledAnnouncements': 'Anuncios programados',
  'announcements.title': 'Anuncios programados',
  'announcements.draft': 'Borrador',
  'announcements.scheduled': 'Programado',
  'announcements.published': 'Publicado',

  // Event Checklists
  'nav.eventChecklists': 'Checklists eventos',
  'checklist.title': 'Checklists de eventos',
  'checklist.generate': 'Generar',
  'checklist.completed': 'Completado',

  // Group Messages
  'nav.groupMessages': 'Mensajes de grupo',
  'groupMessages.title': 'Mensajes de grupo',
  'groupMessages.groups': 'Grupos',
  'groupMessages.placeholder': 'Escribir mensaje...',
  'groupMessages.empty': 'Sin mensajes',

  // Weekly Challenges
  'nav.weeklyChallenges': 'Desafíos semanales',
  'challenges.title': 'Desafíos semanales',
  'challenges.progress': 'Progreso',
  'challenges.active': 'Activo',
  'challenges.completed': 'Completado',

  // Discipleship Path
  'nav.discipleshipPath': 'Camino discipulado',
  'discipleship.title': 'Camino de discipulado IA',
  'discipleship.progress': 'Progreso',
  'discipleship.recommendation': 'Recomendación IA',

  // AI Visit Notes
  'nav.aiVisitNotes': 'Notas IA Visitas',
  'visitNotes.title': 'Notas IA Visitas',
  'visitNotes.summary': 'Resumen IA',
  'visitNotes.actions': 'Acciones recomendadas',
  'visitNotes.sentiment': 'Sentimiento',

  // Reverse Mentoring
  'nav.reverseMentoring': 'Mentoría inversa',
  'reverseMentoring.title': 'Mentoría inversa',
  'reverseMentoring.request': 'Pedir ayuda',
  'reverseMentoring.pending': 'Pendiente',

  // Family Meetings
  'nav.familyMeetings': 'Reuniones familia auto',
  'familyMeeting.title': 'Reuniones familia automatizadas',
  'familyMeeting.agenda': 'Orden del día auto',
  'familyMeeting.generate': 'Generar',

  // Executive Insights
  'nav.executiveInsights': 'Insights ejecutivos IA',
  'executiveInsights.title': 'Insights ejecutivos IA',
  'executiveInsights.critical': 'Crítico',
  'executiveInsights.warning': 'Atención',
  'executiveInsights.opportunity': 'Oportunidad',
  'executiveInsights.action': 'Acción recomendada',

  // Upcoming Events
  'nav.upcomingEvents': 'Próximos eventos',
  'upcomingEvents.title': 'Mis próximos eventos',
  'upcomingEvents.going': 'Asistiré',
  'upcomingEvents.interested': 'Interesado',

  // My Team/Family
  'nav.myTeam': 'Mi equipo / familia',
  'myTeam.title': 'Mi equipo / Mi familia',
  'myTeam.message': 'Enviar mensaje',
  'myTeam.encourage': 'Animar',
};

export default es;
