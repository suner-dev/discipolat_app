#!/usr/bin/env python3
"""Apply chef.*, member.* and leave.title keys to all 6 web dictionaries."""
import re
import sys
from pathlib import Path

I18N_DIR = Path('frontend/src/i18n')

# ---------------- chef.* (ChefFamilleDashboardPage) ----------------
CHEF = {
    'chef.greetingMorning': {'fr': 'Bonjour', 'en': 'Good morning', 'pt': 'Bom dia', 'es': 'Buenos días', 'sw': 'Habari za asubuhi', 'ar': 'صباح الخير'},
    'chef.greetingAfternoon': {'fr': 'Bon après-midi', 'en': 'Good afternoon', 'pt': 'Boa tarde', 'es': 'Buenas tardes', 'sw': 'Habari za mchana', 'ar': 'مساء الخير'},
    'chef.greetingEvening': {'fr': 'Bonsoir', 'en': 'Good evening', 'pt': 'Boa noite', 'es': 'Buenas noches', 'sw': 'Habari za jioni', 'ar': 'مساء الخير'},
    'chef.family': {'fr': 'Famille', 'en': 'Family', 'pt': 'Família', 'es': 'Familia', 'sw': 'Familia', 'ar': 'عائلة'},
    'chef.ofDisciples': {'fr': 'De disciples', 'en': 'of disciples', 'pt': 'de discípulos', 'es': 'de discípulos', 'sw': 'ya wanafunzi', 'ar': 'من التلاميذ'},
    'chef.subtitle': {'fr': 'Supervision des faiseurs et disciples', 'en': 'Oversight of makers and disciples', 'pt': 'Supervisão dos fazedores e discípulos', 'es': 'Supervisión de hacedores y discípulos', 'sw': 'Usimamizi wa wafanya na wanafunzi', 'ar': 'الإشراف على الصنّاع والتلاميذ'},
    'chef.export': {'fr': 'Exporter', 'en': 'Export', 'pt': 'Exportar', 'es': 'Exportar', 'sw': 'Hamisha', 'ar': 'تصدير'},
    'chef.noFamily': {'fr': 'Aucune famille assignée', 'en': 'No family assigned', 'pt': 'Nenhuma família atribuída', 'es': 'Ninguna familia asignada', 'sw': 'Hakuna familia iliyopangiwa', 'ar': 'لا توجد عائلة معيّنة'},
    'chef.noFamilyDesc': {'fr': "Vous n'êtes pas encore chef d'une famille de disciples.", 'en': 'You are not yet the head of a disciples family.', 'pt': 'Ainda não é chefe de uma família de discípulos.', 'es': 'Todavía no es jefe de una familia de discípulos.', 'sw': 'Bado wewe si mkuu wa familia ya wanafunzi.', 'ar': 'لست بعد رئيساً لعائلة تلاميذ.'},
    'chef.disciples': {'fr': 'Disciples', 'en': 'Disciples', 'pt': 'Discípulos', 'es': 'Discípulos', 'sw': 'Wanafunzi', 'ar': 'التلاميذ'},
    'chef.faiseurs': {'fr': 'Faiseurs', 'en': 'Makers', 'pt': 'Fazedores', 'es': 'Hacedores', 'sw': 'Wafanya', 'ar': 'الصنّاع'},
    'chef.actifs': {'fr': 'Actifs', 'en': 'Active', 'pt': 'Ativos', 'es': 'Activos', 'sw': 'Wanaoendelea', 'ar': 'نشطون'},
    'chef.alertes': {'fr': 'Alertes', 'en': 'Alerts', 'pt': 'Alertas', 'es': 'Alertas', 'sw': 'Tahadhari', 'ar': 'تنبيهات'},
    'chef.visites': {'fr': 'Visites', 'en': 'Visits', 'pt': 'Visitas', 'es': 'Visitas', 'sw': 'Ziara', 'ar': 'زيارات'},
    'chef.prieres': {'fr': 'Prières', 'en': 'Prayers', 'pt': 'Orações', 'es': 'Oraciones', 'sw': 'Maombi', 'ar': 'صلوات'},
    'chef.activeAlerts': {'fr': 'Alertes actives', 'en': 'Active alerts', 'pt': 'Alertas ativos', 'es': 'Alertas activas', 'sw': 'Tahadhari zinazoendelea', 'ar': 'تنبيهات نشطة'},
    'chef.viewAll': {'fr': 'Voir tout', 'en': 'View all', 'pt': 'Ver tudo', 'es': 'Ver todo', 'sw': 'Ona yote', 'ar': 'عرض الكل'},
    'chef.quickActions': {'fr': 'Actions rapides', 'en': 'Quick actions', 'pt': 'Ações rápidas', 'es': 'Acciones rápidas', 'sw': 'Vitendo vya haraka', 'ar': 'إجراءات سريعة'},
    'chef.familyReport': {'fr': 'Rapport famille', 'en': 'Family report', 'pt': 'Relatório da família', 'es': 'Informe familiar', 'sw': 'Ripoti ya familia', 'ar': 'تقرير العائلة'},
    'chef.events': {'fr': 'Événements', 'en': 'Events', 'pt': 'Eventos', 'es': 'Eventos', 'sw': 'Matukio', 'ar': 'فعاليات'},
    'chef.disciplesRepartition': {'fr': 'Répartition des disciples', 'en': 'Disciples breakdown', 'pt': 'Distribuição dos discípulos', 'es': 'Distribución de discípulos', 'sw': 'Mgawanyo wa wanafunzi', 'ar': 'توزيع التلاميذ'},
    'chef.noData': {'fr': 'Aucune donnée', 'en': 'No data', 'pt': 'Sem dados', 'es': 'Sin datos', 'sw': 'Hakuna data', 'ar': 'لا توجد بيانات'},
    'chef.weekReports': {'fr': 'Rapports de la semaine', 'en': 'Weekly reports', 'pt': 'Relatórios da semana', 'es': 'Informes de la semana', 'sw': 'Ripoti za wiki', 'ar': 'تقارير الأسبوع'},
    'chef.detail': {'fr': 'Détail', 'en': 'Detail', 'pt': 'Detalhe', 'es': 'Detalle', 'sw': 'Maelezo', 'ar': 'تفاصيل'},
    'chef.submitted': {'fr': 'Soumis', 'en': 'Submitted', 'pt': 'Submetido', 'es': 'Enviado', 'sw': 'Imewasilishwa', 'ar': 'مُرسل'},
    'chef.pending': {'fr': 'En attente', 'en': 'Pending', 'pt': 'Em espera', 'es': 'Pendiente', 'sw': 'Inasubiri', 'ar': 'قيد الانتظار'},
    'chef.upcomingVisits': {'fr': 'Prochaines visites', 'en': 'Upcoming visits', 'pt': 'Próximas visitas', 'es': 'Próximas visitas', 'sw': 'Ziara zijazo', 'ar': 'الزيارات القادمة'},
    'chef.noVisit': {'fr': 'Aucune visite planifiée', 'en': 'No visit planned', 'pt': 'Nenhuma visita planeada', 'es': 'Ninguna visita planificada', 'sw': 'Hakuna ziara iliyopangwa', 'ar': 'لا توجد زيارة مجدولة'},
    'chef.familyPrayers': {'fr': 'Prières de la famille', 'en': 'Family prayers', 'pt': 'Orações da família', 'es': 'Oraciones de la familia', 'sw': 'Maombi ya familia', 'ar': 'صلوات العائلة'},
    'chef.noPrayer': {'fr': 'Aucune prière', 'en': 'No prayer', 'pt': 'Nenhuma oração', 'es': 'Ninguna oración', 'sw': 'Hakuna ombi', 'ar': 'لا توجد صلاة'},
    'chef.workload': {'fr': 'Charge de travail des Faiseurs', 'en': 'Makers workload', 'pt': 'Carga de trabalho dos fazedores', 'es': 'Carga de trabajo de los hacedores', 'sw': 'Mzigo wa kazi wa wafanya', 'ar': 'عبء عمل الصنّاع'},
    'chef.light': {'fr': 'Léger', 'en': 'Light', 'pt': 'Leve', 'es': 'Ligero', 'sw': 'Nyepesi', 'ar': 'خفيف'},
    'chef.overloaded': {'fr': 'Surchargé', 'en': 'Overloaded', 'pt': 'Sobrecarregado', 'es': 'Sobrecargado', 'sw': 'Imezidiwa', 'ar': 'مثقل'},
    'chef.normal': {'fr': 'Normal', 'en': 'Normal', 'pt': 'Normal', 'es': 'Normal', 'sw': 'Kawaida', 'ar': 'عادي'},
    'chef.soulsTracked': {'fr': 'âmes suivies', 'en': 'souls tracked', 'pt': 'almas seguidas', 'es': 'almas seguidas', 'sw': 'roho zinazofuatiliwa', 'ar': 'نفوس تتم متابعتها'},
    'chef.networkView': {'fr': 'Vue réseau — Faiseurs & Disciples', 'en': 'Network view — Makers & Disciples', 'pt': 'Vista de rede — Fazedores e Discípulos', 'es': 'Vista de red — Hacedores y Discípulos', 'sw': 'Mtazamo wa mtandao — Wafanya na Wanafunzi', 'ar': 'عرض الشبكة — الصنّاع والتلاميذ'},
    'chef.disciplesCount': {'fr': 'disciples', 'en': 'disciples', 'pt': 'discípulos', 'es': 'discípulos', 'sw': 'wanafunzi', 'ar': 'تلاميذ'},
    'chef.reportOk': {'fr': 'Rapport OK', 'en': 'Report OK', 'pt': 'Relatório OK', 'es': 'Informe OK', 'sw': 'Ripoti sawa', 'ar': 'التقرير سليم'},
    'chef.others': {'fr': 'autres', 'en': 'others', 'pt': 'outros', 'es': 'otros', 'sw': 'wengine', 'ar': 'آخرون'},
    'chef.noFaiseur': {'fr': 'Aucun faiseur dans cette famille', 'en': 'No maker in this family', 'pt': 'Nenhum fazedor nesta família', 'es': 'Ningún hacedor en esta familia', 'sw': 'Hakuna mfanya katika familia hii', 'ar': 'لا يوجد صانع في هذه العائلة'},
    'chef.spiritualProgress': {'fr': 'Progression spirituelle', 'en': 'Spiritual progress', 'pt': 'Progresso espiritual', 'es': 'Progreso espiritual', 'sw': 'Maendeleo ya kiroho', 'ar': 'التقدم الروحي'},
    'chef.upcomingEvents': {'fr': 'Événements à venir', 'en': 'Upcoming events', 'pt': 'Próximos eventos', 'es': 'Próximos eventos', 'sw': 'Matukio yajayo', 'ar': 'الفعاليات القادمة'},
    'chef.noEvent': {'fr': 'Aucun événement à venir', 'en': 'No upcoming event', 'pt': 'Nenhum evento próximo', 'es': 'Ningún evento próximo', 'sw': 'Hakuna tukio lijalo', 'ar': 'لا توجد فعالية قادمة'},
    'chef.birthdays': {'fr': 'Anniversaires du mois', 'en': 'Birthdays this month', 'pt': 'Aniversários do mês', 'es': 'Cumpleaños del mes', 'sw': 'Siku za kuzaliwa za mwezi huu', 'ar': 'أعياد الميلاد هذا الشهر'},
    'chef.allDisciples': {'fr': 'Tous les disciples', 'en': 'All disciples', 'pt': 'Todos os discípulos', 'es': 'Todos los discípulos', 'sw': 'Wanafunzi wote', 'ar': 'كل التلاميذ'},
    'chef.name': {'fr': 'Nom', 'en': 'Name', 'pt': 'Nome', 'es': 'Nombre', 'sw': 'Jina', 'ar': 'الاسم'},
    'chef.faiseur': {'fr': 'Faiseur', 'en': 'Maker', 'pt': 'Fazedor', 'es': 'Hacedor', 'sw': 'Mfanya', 'ar': 'صانع'},
    'chef.status': {'fr': 'Statut', 'en': 'Status', 'pt': 'Estado', 'es': 'Estado', 'sw': 'Hali', 'ar': 'الحالة'},
    'chef.type': {'fr': 'Type', 'en': 'Type', 'pt': 'Tipo', 'es': 'Tipo', 'sw': 'Aina', 'ar': 'النوع'},
    'chef.level': {'fr': 'Niveau', 'en': 'Level', 'pt': 'Nível', 'es': 'Nivel', 'sw': 'Kiwango', 'ar': 'المستوى'},
    'chef.report': {'fr': 'Rapport', 'en': 'Report', 'pt': 'Relatório', 'es': 'Informe', 'sw': 'Ripoti', 'ar': 'التقرير'},
    'chef.newConvert': {'fr': 'Nv. converti', 'en': 'New convert', 'pt': 'Novo convertido', 'es': 'Nuevo convertido', 'sw': 'Mgeuzwa mpya', 'ar': 'مهتدٍ جديد'},
    'chef.newArrival': {'fr': 'Nv. arrivant', 'en': 'New arrival', 'pt': 'Novo chegado', 'es': 'Nuevo llegado', 'sw': 'Mgeni mpya', 'ar': 'وافد جديد'},
    'chef.active': {'fr': 'Actif', 'en': 'Active', 'pt': 'Ativo', 'es': 'Activo', 'sw': 'Anayeendelea', 'ar': 'نشط'},
    'chef.integration': {'fr': 'Intégration', 'en': 'Integration', 'pt': 'Integração', 'es': 'Integración', 'sw': 'Ujumuishaji', 'ar': 'اندماج'},
    'chef.sleep': {'fr': 'Veille', 'en': 'Standby', 'pt': 'Espera', 'es': 'Espera', 'sw': 'Kulala', 'ar': 'خامل'},
    'chef.dropped': {'fr': 'Décroché', 'en': 'Dropped', 'pt': 'Desistido', 'es': 'Descolgado', 'sw': 'Ameacha', 'ar': 'منقطع'},
    'chef.noDisciple': {'fr': 'Aucun disciple', 'en': 'No disciple', 'pt': 'Nenhum discípulo', 'es': 'Ningún discípulo', 'sw': 'Hakuna mwanafunzi', 'ar': 'لا يوجد تلميذ'},
}

# ---------------- member.* (MemberDashboardPage) ----------------
MEMBER = {
    'member.greetingMorning': {'fr': 'Bonjour', 'en': 'Good morning', 'pt': 'Bom dia', 'es': 'Buenos días', 'sw': 'Habari za asubuhi', 'ar': 'صباح الخير'},
    'member.greetingAfternoon': {'fr': 'Bon après-midi', 'en': 'Good afternoon', 'pt': 'Boa tarde', 'es': 'Buenas tardes', 'sw': 'Habari za mchana', 'ar': 'مساء الخير'},
    'member.greetingEvening': {'fr': 'Bonsoir', 'en': 'Good evening', 'pt': 'Boa noite', 'es': 'Buenas noches', 'sw': 'Habari za jioni', 'ar': 'مساء الخير'},
    'member.space': {'fr': 'Espace', 'en': 'Space', 'pt': 'Espaço', 'es': 'Espacio', 'sw': 'Nafasi', 'ar': 'مساحة'},
    'member.member': {'fr': 'Membre', 'en': 'Member', 'pt': 'Membro', 'es': 'Miembro', 'sw': 'Mwanachama', 'ar': 'عضو'},
    'member.subtitle': {'fr': 'Vos informations, votre famille de disciple et vos départements', 'en': 'Your information, your disciples family and your departments', 'pt': 'As suas informações, a sua família de discípulos e os seus departamentos', 'es': 'Su información, su familia de discípulos y sus departamentos', 'sw': 'Taarifa zako, familia yako ya wanafunzi na idara zako', 'ar': 'معلوماتك وعائلتك التلمذية وأقسامك'},
    'member.editInfo': {'fr': 'Modifier mes informations', 'en': 'Edit my information', 'pt': 'Editar as minhas informações', 'es': 'Editar mi información', 'sw': 'Hariri taarifa zangu', 'ar': 'تعديل معلوماتي'},
    'member.quickAccess': {'fr': 'Accès rapides', 'en': 'Quick access', 'pt': 'Acesso rápido', 'es': 'Acceso rápido', 'sw': 'Ufikiaji wa haraka', 'ar': 'وصول سريع'},
    'member.events': {'fr': 'Événements', 'en': 'Events', 'pt': 'Eventos', 'es': 'Eventos', 'sw': 'Matukio', 'ar': 'فعاليات'},
    'member.prayers': {'fr': 'Prières', 'en': 'Prayers', 'pt': 'Orações', 'es': 'Oraciones', 'sw': 'Maombi', 'ar': 'صلوات'},
    'member.trainings': {'fr': 'Formations', 'en': 'Trainings', 'pt': 'Formações', 'es': 'Formaciones', 'sw': 'Mafunzo', 'ar': 'تدريبات'},
    'member.badges': {'fr': 'Badges', 'en': 'Badges', 'pt': 'Distintivos', 'es': 'Insignias', 'sw': 'Beji', 'ar': 'شارات'},
    'member.profilePhoto': {'fr': 'Photo de profil', 'en': 'Profile photo', 'pt': 'Foto de perfil', 'es': 'Foto de perfil', 'sw': 'Picha ya wasifu', 'ar': 'صورة الملف'},
    'member.addChangePhoto': {'fr': 'Ajouter / changer ma photo', 'en': 'Add / change my photo', 'pt': 'Adicionar / mudar a minha foto', 'es': 'Añadir / cambiar mi foto', 'sw': 'Ongeza / badilisha picha yangu', 'ar': 'إضافة / تغيير صورتي'},
    'member.dearMember': {'fr': 'cher membre', 'en': 'dear member', 'pt': 'caro membro', 'es': 'estimado miembro', 'sw': 'mwanachama mpendwa', 'ar': 'عزيزي العضو'},
    'member.myInfo': {'fr': 'Mes informations', 'en': 'My information', 'pt': 'As minhas informações', 'es': 'Mi información', 'sw': 'Taarifa zangu', 'ar': 'معلوماتي'},
    'member.personalProfile': {'fr': 'Profil personnel', 'en': 'Personal profile', 'pt': 'Perfil pessoal', 'es': 'Perfil personal', 'sw': 'Wasifu binafsi', 'ar': 'ملف شخصي'},
    'member.age': {'fr': 'Âge', 'en': 'Age', 'pt': 'Idade', 'es': 'Edad', 'sw': 'Umri', 'ar': 'العمر'},
    'member.years': {'fr': 'ans', 'en': 'years', 'pt': 'anos', 'es': 'años', 'sw': 'miaka', 'ar': 'سنوات'},
    'member.churchArrival': {'fr': "Arrivée à l'église", 'en': 'Church arrival', 'pt': 'Chegada à igreja', 'es': 'Llegada a la iglesia', 'sw': 'Kufika kanisani', 'ar': 'الوصول إلى الكنيسة'},
    'member.education': {'fr': "Niveau d'étude", 'en': 'Education level', 'pt': 'Nível de estudos', 'es': 'Nivel de estudios', 'sw': 'Kiwango cha elimu', 'ar': 'المستوى الدراسي'},
    'member.profession': {'fr': 'Profession', 'en': 'Profession', 'pt': 'Profissão', 'es': 'Profesión', 'sw': 'Kazi', 'ar': 'المهنة'},
    'member.maritalStatus': {'fr': 'Situation familiale', 'en': 'Marital status', 'pt': 'Situação familiar', 'es': 'Situación familiar', 'sw': 'Hali ya ndoa', 'ar': 'الحالة العائلية'},
    'member.children': {'fr': 'Enfants', 'en': 'Children', 'pt': 'Filhos', 'es': 'Hijos', 'sw': 'Watoto', 'ar': 'الأولاد'},
    'member.myFamily': {'fr': 'Ma famille de disciple', 'en': 'My disciples family', 'pt': 'A minha família de discípulos', 'es': 'Mi familia de discípulos', 'sw': 'Familia yangu ya wanafunzi', 'ar': 'عائلتي التلمذية'},
    'member.family': {'fr': 'Famille', 'en': 'Family', 'pt': 'Família', 'es': 'Familia', 'sw': 'Familia', 'ar': 'العائلة'},
    'member.familyHead': {'fr': 'Chef de famille', 'en': 'Family head', 'pt': 'Chefe de família', 'es': 'Jefe de familia', 'sw': 'Mkuu wa familia', 'ar': 'رئيس العائلة'},
    'member.noFamily': {'fr': 'Aucune famille associée', 'en': 'No associated family', 'pt': 'Nenhuma família associada', 'es': 'Ninguna familia asociada', 'sw': 'Hakuna familia iliyounganishwa', 'ar': 'لا توجد عائلة مرتبطة'},
    'member.myGuidance': {'fr': 'Mon encadrement', 'en': 'My guidance', 'pt': 'O meu acompanhamento', 'es': 'Mi acompañamiento', 'sw': 'Uandamanishi wangu', 'ar': 'مرافقتي'},
    'member.maker': {'fr': 'Faiseur de disciples', 'en': 'Disciple maker', 'pt': 'Fazedor de discípulos', 'es': 'Hacedor de discípulos', 'sw': 'Mfanya wanafunzi', 'ar': 'صانع تلاميذ'},
    'member.yourGuide': {'fr': 'Votre accompagnateur', 'en': 'Your guide', 'pt': 'O seu acompanhador', 'es': 'Su acompañador', 'sw': 'Mwongozaji wako', 'ar': 'مرافقك'},
    'member.noGuide': {'fr': 'Aucun encadrant assigné', 'en': 'No guide assigned', 'pt': 'Nenhum acompanhador atribuído', 'es': 'Ningún acompañador asignado', 'sw': 'Hakuna mwongozaji aliyepangiwa', 'ar': 'لا يوجد مرافق معيّن'},
    'member.myDepartments': {'fr': 'Mes départements', 'en': 'My departments', 'pt': 'Os meus departamentos', 'es': 'Mis departamentos', 'sw': 'Idara zangu', 'ar': 'أقسامي'},
    'member.active': {'fr': 'actif', 'en': 'active', 'pt': 'ativo', 'es': 'activo', 'sw': 'inayotumika', 'ar': 'نشط'},
    'member.responsable': {'fr': 'Responsable', 'en': 'Head', 'pt': 'Responsável', 'es': 'Responsable', 'sw': 'Mwajibikaji', 'ar': 'مسؤول'},
    'member.ministry': {'fr': 'Ministère', 'en': 'Ministry', 'pt': 'Ministério', 'es': 'Ministerio', 'sw': 'Huduma', 'ar': 'خدمة'},
    'member.noDepartment': {'fr': 'Aucun département pour le moment', 'en': 'No department yet', 'pt': 'Nenhum departamento por enquanto', 'es': 'Ningún departamento por ahora', 'sw': 'Hakuna idara kwa sasa', 'ar': 'لا يوجد قسم حالياً'},
    'member.myPresences': {'fr': 'Mes présences hebdomadaires', 'en': 'My weekly attendances', 'pt': 'As minhas presenças semanais', 'es': 'Mis asistencias semanales', 'sw': 'Mahudhurio yangu ya kila wiki', 'ar': 'حضوري الأسبوعي'},
    'member.presencesVisible': {'fr': 'Visibles par votre chef de famille, votre responsable de département et le pasteur', 'en': 'Visible by your family head, department head and the pastor', 'pt': 'Visíveis pelo seu chefe de família, responsável de departamento e pelo pastor', 'es': 'Visibles por su jefe de familia, responsable de departamento y el pastor', 'sw': 'Yanaonekana na mkuu wako wa familia, mwajibikaji wa idara na mchungaji', 'ar': 'يطلع عليه رئيس عائلتك ومسؤول قسمك والراعي'},
    'member.weekOf': {'fr': 'Semaine du', 'en': 'Week of', 'pt': 'Semana de', 'es': 'Semana del', 'sw': 'Wiki ya', 'ar': 'أسبوع'},
    'member.program': {'fr': 'Programme', 'en': 'Program', 'pt': 'Programa', 'es': 'Programa', 'sw': 'Programu', 'ar': 'البرنامج'},
    'member.chooseProgram': {'fr': 'Choisir un programme...', 'en': 'Choose a program...', 'pt': 'Escolher um programa...', 'es': 'Elegir un programa...', 'sw': 'Chagua programu...', 'ar': 'اختر برنامجاً...'},
    'member.subProgram': {'fr': 'Sous-programme', 'en': 'Sub-program', 'pt': 'Sub-programa', 'es': 'Sub-programa', 'sw': 'Programu ndogo', 'ar': 'برنامج فرعي'},
    'member.choose': {'fr': 'Choisir...', 'en': 'Choose...', 'pt': 'Escolher...', 'es': 'Elegir...', 'sw': 'Chagua...', 'ar': 'اختر...'},
    'member.notesOptional': {'fr': 'Notes (facultatif)', 'en': 'Notes (optional)', 'pt': 'Notas (opcional)', 'es': 'Notas (opcional)', 'sw': 'Maelezo (si lazima)', 'ar': 'ملاحظات (اختياري)'},
    'member.saving': {'fr': 'Enregistrement...', 'en': 'Saving...', 'pt': 'A guardar...', 'es': 'Guardando...', 'sw': 'Inahifadhi...', 'ar': 'جارٍ الحفظ...'},
    'member.savePresence': {'fr': 'Enregistrer ma présence', 'en': 'Record my attendance', 'pt': 'Registar a minha presença', 'es': 'Registrar mi asistencia', 'sw': 'Rekodi mahudhurio yangu', 'ar': 'تسجيل حضوري'},
    'member.myHistory': {'fr': 'Mon historique', 'en': 'My history', 'pt': 'O meu histórico', 'es': 'Mi historial', 'sw': 'Historia yangu', 'ar': 'سجلي'},
    'member.programsPresent': {'fr': 'programmes présents', 'en': 'programs attended', 'pt': 'programas presentes', 'es': 'programas presentes', 'sw': 'programu zilizohudhuriwa', 'ar': 'برامج حضرتها'},
    'member.noPresence': {'fr': "Aucune présence enregistrée pour l'instant", 'en': 'No attendance recorded yet', 'pt': 'Nenhuma presença registada por enquanto', 'es': 'Ninguna asistencia registrada por ahora', 'sw': 'Hakuna mahudhurio yaliyorekodiwa bado', 'ar': 'لا يوجد حضور مسجل بعد'},
    'member.checkPrograms': {'fr': 'Cochez les programmes de la semaine et enregistrez', 'en': "Check this week's programs and record", 'pt': 'Marque os programas da semana e registe', 'es': 'Marque los programas de la semana y registre', 'sw': 'Weka alama kwenye programu za wiki na urekodi', 'ar': 'حدد برامج الأسبوع وسجل'},
    'member.requests': {'fr': 'Suggestions, rendez-vous & signalements', 'en': 'Suggestions, appointments & reports', 'pt': 'Sugestões, reuniões e comunicações', 'es': 'Sugerencias, citas y avisos', 'sw': 'Mapendekezo, miadi na taarifa', 'ar': 'اقتراحات ومواعيد وبلاغات'},
    'member.requestsHint': {'fr': 'Envoyez un message au pasteur, à votre responsable de département ou à votre chef de famille', 'en': 'Send a message to the pastor, your department head or your family head', 'pt': 'Envie uma mensagem ao pastor, ao seu responsável de departamento ou ao seu chefe de família', 'es': 'Envíe un mensaje al pastor, a su responsable de departamento o a su jefe de familia', 'sw': 'Tuma ujumbe kwa mchungaji, mwajibikaji wako wa idara au mkuu wako wa familia', 'ar': 'أرسل رسالة إلى الراعي أو مسؤول قسمك أو رئيس عائلتك'},
    'member.type': {'fr': 'Type', 'en': 'Type', 'pt': 'Tipo', 'es': 'Tipo', 'sw': 'Aina', 'ar': 'النوع'},
    'member.recipient': {'fr': 'Destinataire', 'en': 'Recipient', 'pt': 'Destinatário', 'es': 'Destinatario', 'sw': 'Mpokeaji', 'ar': 'المستلم'},
    'member.yourMessage': {'fr': 'Votre message...', 'en': 'Your message...', 'pt': 'A sua mensagem...', 'es': 'Su mensaje...', 'sw': 'Ujumbe wako...', 'ar': 'رسالتك...'},
    'member.attachments': {'fr': 'Pièces jointes', 'en': 'Attachments', 'pt': 'Anexos', 'es': 'Archivos adjuntos', 'sw': 'Viambatisho', 'ar': 'المرفقات'},
    'member.sending': {'fr': 'Envoi...', 'en': 'Sending...', 'pt': 'A enviar...', 'es': 'Enviando...', 'sw': 'Inatuma...', 'ar': 'جارٍ الإرسال...'},
    'member.sendRequest': {'fr': 'Envoyer ma demande', 'en': 'Send my request', 'pt': 'Enviar o meu pedido', 'es': 'Enviar mi solicitud', 'sw': 'Tuma ombi langu', 'ar': 'إرسال طلبي'},
    'member.myRequests': {'fr': 'Mes demandes', 'en': 'My requests', 'pt': 'Os meus pedidos', 'es': 'Mis solicitudes', 'sw': 'Maombi yangu', 'ar': 'طلباتي'},
    'member.answer': {'fr': 'Réponse :', 'en': 'Answer:', 'pt': 'Resposta:', 'es': 'Respuesta:', 'sw': 'Jibu:', 'ar': 'الرد:'},
    'member.noRequest': {'fr': 'Aucune demande envoyée', 'en': 'No request sent', 'pt': 'Nenhum pedido enviado', 'es': 'Ninguna solicitud enviada', 'sw': 'Hakuna ombi lililotumwa', 'ar': 'لا يوجد طلب مُرسل'},
    'member.noRequestHint': {'fr': 'Vos demandes apparaîtront ici avec leur statut et la réponse', 'en': 'Your requests will appear here with their status and the answer', 'pt': 'Os seus pedidos aparecerão aqui com o seu estado e a resposta', 'es': 'Sus solicitudes aparecerán aquí con su estado y la respuesta', 'sw': 'Maombi yako yataonekana hapa pamoja na hali na jibu', 'ar': 'ستظهر طلباتك هنا مع حالتها والرد'},
    'member.upcomingEvents': {'fr': 'Événements à venir', 'en': 'Upcoming events', 'pt': 'Próximos eventos', 'es': 'Próximos eventos', 'sw': 'Matukio yajayo', 'ar': 'الفعاليات القادمة'},
    'member.communityEvents': {'fr': 'Prochains événements de votre communauté', 'en': 'Upcoming events of your community', 'pt': 'Próximos eventos da sua comunidade', 'es': 'Próximos eventos de su comunidad', 'sw': 'Matukio yajayo ya jamii yako', 'ar': 'الفعاليات القادمة لمجتمعك'},
    'member.viewAll': {'fr': 'Voir tout', 'en': 'View all', 'pt': 'Ver tudo', 'es': 'Ver todo', 'sw': 'Ona yote', 'ar': 'عرض الكل'},
    'member.noEvent': {'fr': 'Aucun événement à venir', 'en': 'No upcoming event', 'pt': 'Nenhum evento próximo', 'es': 'Ningún evento próximo', 'sw': 'Hakuna tukio lijalo', 'ar': 'لا توجد فعالية قادمة'},
    'member.prayersThanks': {'fr': 'Prières & actions de grâce', 'en': 'Prayers & thanksgiving', 'pt': 'Orações e ações de graças', 'es': 'Oraciones y acciones de gracias', 'sw': 'Maombi na shukrani', 'ar': 'الصلوات والتشكرات'},
    'member.prayersHint': {'fr': "Demandes de prière de votre famille et de l'église", 'en': 'Prayer requests of your family and the church', 'pt': 'Pedidos de oração da sua família e da igreja', 'es': 'Peticiones de oración de su familia y de la iglesia', 'sw': 'Maombi ya familia yako na kanisa', 'ar': 'طلبات صلاة من عائلتك والكنيسة'},
    'member.answered': {'fr': 'Exaucée', 'en': 'Answered', 'pt': 'Atendida', 'es': 'Respondida', 'sw': 'Limejibiwa', 'ar': 'مستجابة'},
    'member.inProgress': {'fr': 'En cours', 'en': 'In progress', 'pt': 'Em andamento', 'es': 'En curso', 'sw': 'Inaendelea', 'ar': 'قيد التنفيذ'},
    'member.noPrayer': {'fr': 'Aucune prière', 'en': 'No prayer', 'pt': 'Nenhuma oração', 'es': 'Ninguna oración', 'sw': 'Hakuna ombi', 'ar': 'لا توجد صلاة'},
    'member.myProgress': {'fr': 'Ma progression', 'en': 'My progress', 'pt': 'O meu progresso', 'es': 'Mi progreso', 'sw': 'Maendeleo yangu', 'ar': 'تقدمي'},
    'member.progressHint': {'fr': 'Évolution spirituelle et engagement', 'en': 'Spiritual growth and engagement', 'pt': 'Evolução espiritual e envolvimento', 'es': 'Evolución espiritual y compromiso', 'sw': 'Ukuaji wa kiroho na ushiriki', 'ar': 'النمو الروحي والالتزام'},
    'member.spiritualLevel': {'fr': 'Niveau spirituel', 'en': 'Spiritual level', 'pt': 'Nível espiritual', 'es': 'Nivel espiritual', 'sw': 'Kiwango cha kiroho', 'ar': 'المستوى الروحي'},
    'member.presences': {'fr': 'Présences', 'en': 'Attendances', 'pt': 'Presenças', 'es': 'Asistencias', 'sw': 'Mahudhurio', 'ar': 'حضور'},
    'member.reports': {'fr': 'Rapports', 'en': 'Reports', 'pt': 'Relatórios', 'es': 'Informes', 'sw': 'Ripoti', 'ar': 'تقارير'},
    'member.makerNotes': {'fr': 'Notes de mon faiseur', 'en': 'Notes from my maker', 'pt': 'Notas do meu fazedor', 'es': 'Notas de mi hacedor', 'sw': 'Maelezo ya mfanya wangu', 'ar': 'ملاحظات صانعي'},
    'member.note': {'fr': 'Note', 'en': 'Note', 'pt': 'Nota', 'es': 'Nota', 'sw': 'Maelezo', 'ar': 'ملاحظة'},
    'member.editMyInfo': {'fr': 'Modifier mes informations', 'en': 'Edit my information', 'pt': 'Editar as minhas informações', 'es': 'Editar mi información', 'sw': 'Hariri taarifa zangu', 'ar': 'تعديل معلوماتي'},
    'member.profilePhotoLink': {'fr': 'Photo de profil (lien, facultatif)', 'en': 'Profile photo (link, optional)', 'pt': 'Foto de perfil (link, opcional)', 'es': 'Foto de perfil (enlace, opcional)', 'sw': 'Picha ya wasifu (kiungo, si lazima)', 'ar': 'صورة الملف (رابط، اختياري)'},
    'member.preview': {'fr': 'Aperçu', 'en': 'Preview', 'pt': 'Pré-visualização', 'es': 'Vista previa', 'sw': 'Onyesho la awali', 'ar': 'معاينة'},
    'member.updated': {'fr': 'Vos informations ont été mises à jour ✨', 'en': 'Your information has been updated ✨', 'pt': 'As suas informações foram atualizadas ✨', 'es': 'Su información ha sido actualizada ✨', 'sw': 'Taarifa zako zimesasishwa ✨', 'ar': 'تم تحديث معلوماتك ✨'},
    'member.presenceSaved': {'fr': 'Votre présence a été enregistrée ✨', 'en': 'Your attendance has been recorded ✨', 'pt': 'A sua presença foi registada ✨', 'es': 'Su asistencia ha sido registrada ✨', 'sw': 'Mahudhurio yako yamerekodiwa ✨', 'ar': 'تم تسجيل حضورك ✨'},
    'member.requestSent': {'fr': 'Votre demande a été envoyée ✅', 'en': 'Your request has been sent ✅', 'pt': 'O seu pedido foi enviado ✅', 'es': 'Su solicitud ha sido enviada ✅', 'sw': 'Ombi lako limetumwa ✅', 'ar': 'تم إرسال طلبك ✅'},
    'member.checkProgram': {'fr': 'Cochez au moins un programme ou ajoutez une note', 'en': 'Check at least one program or add a note', 'pt': 'Marque pelo menos um programa ou adicione uma nota', 'es': 'Marque al menos un programa o añada una nota', 'sw': 'Weka alama kwenye angalau programu moja au ongeza maelezo', 'ar': 'حدد برنامجاً واحداً على الأقل أو أضف ملاحظة'},
    'member.writeMessage': {'fr': "Écrivez votre message avant d'envoyer", 'en': 'Write your message before sending', 'pt': 'Escreva a sua mensagem antes de enviar', 'es': 'Escriba su mensaje antes de enviar', 'sw': 'Andika ujumbe wako kabla ya kutuma', 'ar': 'اكتب رسالتك قبل الإرسال'},
    'member.loadError': {'fr': 'Impossible de charger votre espace membre', 'en': 'Unable to load your member space', 'pt': 'Impossível carregar o seu espaço de membro', 'es': 'No se puede cargar su espacio de miembro', 'sw': 'Hawezi kupakia nafasi yako ya mwanachama', 'ar': 'تعذر تحميل مساحة عضويتك'},
    'member.retry': {'fr': 'Réessayer', 'en': 'Retry', 'pt': 'Tentar novamente', 'es': 'Reintentar', 'sw': 'Jaribu tena', 'ar': 'إعادة المحاولة'},
    'member.statutIs': {'fr': 'Votre statut est', 'en': 'Your status is', 'pt': 'O seu estado é', 'es': 'Su estado es', 'sw': 'Hali yako ni', 'ar': 'حالتك هي'},
    'member.youAccompany': {'fr': ' — vous accompagnez des disciples dans leur croissance.', 'en': ' — you accompany disciples in their growth.', 'pt': ' — acompanha discípulos no seu crescimento.', 'es': ' — acompaña a discípulos en su crecimiento.', 'sw': ' — unawaandamanisha wanafunzi katika ukuaji wao.', 'ar': ' — ترافق تلاميذ في نموهم.'},
    'member.youAreAccompanied': {'fr': ' — vous êtes accompagné(e) dans votre croissance spirituelle au sein de votre famille de disciple.', 'en': ' — you are accompanied in your spiritual growth within your disciples family.', 'pt': ' — é acompanhado no seu crescimento espiritual na sua família de discípulos.', 'es': ' — es acompañado en su crecimiento espiritual dentro de su familia de discípulos.', 'sw': ' — unaandamanishwa katika ukuaji wako wa kiroho ndani ya familia yako ya wanafunzi.', 'ar': ' — تُرافق في نموك الروحي داخل عائلتك التلمذية.'},
}

# leave.title missing in es/sw/ar
LEAVE_TITLE = {
    'leave.title': {
        'fr': 'Demandes de congé',
        'en': 'Leave requests',
        'pt': 'Pedidos de licença',
        'es': 'Solicitudes de permiso',
        'sw': 'Maombi ya likizo',
        'ar': 'طلبات الإجازة',
    }
}


def load_ts_keyvals(path):
    src = path.read_text()
    src = re.sub(r'//[^\n]*', '', src)
    out = {}
    for m in re.finditer(r"(['\"])([^'\"]+)\1\s*:\s*(['\"])(.*?)\3\s*,?", src, re.S):
        out[m.group(2)] = m.group(4)
    return out


def fmt_entry(k, v):
    if "'" in v:
        return f'  "{k}": "{v}",'
    return f"  '{k}': '{v}',"


def apply(path, table, label):
    src = path.read_text()
    existing = load_ts_keyvals(path)
    missing = {k: v for k, v in table.items() if k not in existing}
    if not missing:
        return
    m = re.search(r'\n\};', src)
    assert m, path
    block = '\n'.join(fmt_entry(k, missing[k]) for k in sorted(missing))
    src = src[:m.start()] + '\n' + block + src[m.start():]
    path.write_text(src)
    print(f'  {path.name} [{label}]: +{len(missing)}')


def main():
    for table, label in [(CHEF, 'chef'), (MEMBER, 'member'), (LEAVE_TITLE, 'leave')]:
        for lang in ['fr', 'en', 'pt', 'es', 'sw', 'ar']:
            per_lang = {k: v[lang] for k, v in table.items()}
            apply(I18N_DIR / f'{lang}.ts', per_lang, label)


if __name__ == '__main__':
    main()