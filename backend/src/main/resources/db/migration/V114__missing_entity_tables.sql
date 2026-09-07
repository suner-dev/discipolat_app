-- V114 — Restitution des tables d'entités JPA absentes des migrations.
--
-- Audit (ddl-auto=validate en dev) : 65 entités mappées à des tables qui
-- n'avaient jamais été créées par une migration Flyway. Elles provenaient
-- d'un passé où le schéma était géré par JPA (create/update), ce qui cassait
-- tout démarrage en dev (validate) et tout déploiement frais (ddl-auto=none).
-- Ces instructions sont reproductibles (IF NOT EXISTS) : sans effet sur les
-- bases existantes, requises pour les installations propres.
--
-- modules/volunteers/domain/Volunteer.java

CREATE TABLE IF NOT EXISTS volunteers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    membre_id UUID NOT NULL,
    statut VARCHAR(255) NOT NULL,
    disponibilite VARCHAR(255),
    competences_json TEXT,
    domaines_interet_json TEXT,
    heures_mois INTEGER,
    nb_evenements INTEGER,
    depuis DATE,
    inscrit_le TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_volunteers_tenant ON volunteers (tenant_id);

-- modules/referrals/domain/Referral.java

CREATE TABLE IF NOT EXISTS referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    nom_complet VARCHAR(255) NOT NULL,
    telephone VARCHAR(255),
    email VARCHAR(255),
    statut VARCHAR(255) NOT NULL,
    parrain_id UUID NOT NULL,
    famille_geree_id UUID,
    notes TEXT,
    points INTEGER,
    created_at TIMESTAMP NOT NULL,
    converted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_referrals_tenant ON referrals (tenant_id);

-- modules/usageAnalytics/domain/UsageEvent.java

CREATE TABLE IF NOT EXISTS usage_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID,
    page VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    referrer VARCHAR(255),
    duration_ms BIGINT,
    device VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_usage_events_tenant ON usage_events (tenant_id);

-- modules/backups/domain/BackupRecord.java

CREATE TABLE IF NOT EXISTS backup_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    cron_expression VARCHAR(255),
    size_bytes BIGINT,
    checksum TEXT,
    storage_location VARCHAR(255),
    retention_days INTEGER NOT NULL,
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    verified_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_backup_records_tenant ON backup_records (tenant_id);

-- modules/personalObjectives/domain/PersonalObjective.java

CREATE TABLE IF NOT EXISTS personal_objectives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    membre_id UUID NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    catégorie VARCHAR(255) NOT NULL,
    statut VARCHAR(255) NOT NULL,
    objectif_cible INTEGER,
    progression_actuelle INTEGER,
    created_at TIMESTAMP NOT NULL,
    deadline TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_personal_objectives_tenant ON personal_objectives (tenant_id);

-- modules/tickets/domain/Ticket.java

CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    categorie VARCHAR(255) NOT NULL,
    priorite VARCHAR(255) NOT NULL,
    statut VARCHAR(255) NOT NULL,
    cree_par UUID NOT NULL,
    assignea UUID,
    messages VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tickets_tenant ON tickets (tenant_id);

-- modules/tickets/domain/TicketMessage.java

CREATE TABLE IF NOT EXISTS ticket_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contenu TEXT NOT NULL,
    auteur_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    ticket_id UUID
);

CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket_id ON ticket_messages (ticket_id);

-- modules/discipleshipPath/domain/DiscipleshipPath.java

CREATE TABLE IF NOT EXISTS discipleship_paths (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    member_id UUID NOT NULL,
    current_stage VARCHAR(255) NOT NULL,
    recommended_next_step TEXT,
    progress_percent DOUBLE PRECISION,
    status VARCHAR(255),
    ai_notes TEXT,
    created_at TIMESTAMP NOT NULL,
    start_date TIMESTAMP,
    last_activity_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_discipleship_paths_tenant ON discipleship_paths (tenant_id);

-- modules/messages/domain/GroupConversation.java

CREATE TABLE IF NOT EXISTS group_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    group_type VARCHAR(255) NOT NULL,
    created_by UUID NOT NULL,
    avatar_url VARCHAR(255),
    last_message_at TIMESTAMP,
    last_message VARCHAR(255),
    last_message_sender_id UUID,
    is_archived BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_group_conversations_tenant ON group_conversations (tenant_id);

-- modules/messages/domain/MessageReaction.java

CREATE TABLE IF NOT EXISTS message_reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL,
    user_id UUID NOT NULL,
    emoji VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);


-- modules/messages/domain/GroupConversationMember.java

CREATE TABLE IF NOT EXISTS group_conversation_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(255) NOT NULL,
    is_muted BOOLEAN NOT NULL,
    last_read_at TIMESTAMP,
    unread_count INTEGER,
    joined_at TIMESTAMP NOT NULL
);


-- modules/testimonials/domain/Testimony.java

CREATE TABLE IF NOT EXISTS testimonies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    titre VARCHAR(255) NOT NULL,
    contenu TEXT NOT NULL,
    categorie VARCHAR(255) NOT NULL,
    statut VARCHAR(255) NOT NULL,
    auteur_id UUID NOT NULL,
    likes INTEGER,
    commentaires INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_testimonies_tenant ON testimonies (tenant_id);

-- modules/succession/domain/SuccessionPlan.java

CREATE TABLE IF NOT EXISTS succession_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    candidat_id UUID NOT NULL,
    rôle_cible VARCHAR(255) NOT NULL,
    mentor_id UUID,
    readiness VARCHAR(255) NOT NULL,
    statut VARCHAR(255) NOT NULL,
    plan_formation TEXT,
    commentaires TEXT,
    created_at TIMESTAMP NOT NULL,
    target_date TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_succession_plans_tenant ON succession_plans (tenant_id);

-- modules/familyMeeting/domain/FamilyMeeting.java

CREATE TABLE IF NOT EXISTS family_meetings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    family_id UUID,
    organised_by UUID,
    agenda TEXT,
    minutes TEXT,
    status VARCHAR(255),
    scheduled_at TIMESTAMP,
    attendees_count INTEGER,
    is_auto_generated BOOLEAN NOT NULL,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_family_meetings_tenant ON family_meetings (tenant_id);

-- modules/streaming/domain/StreamChatMessage.java

CREATE TABLE IF NOT EXISTS stream_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stream_id BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    sender_name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(255) NOT NULL,
    emoji VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_stream_chat_messages_tenant ON stream_chat_messages (tenant_id);

-- modules/streaming/domain/LiveStream.java

CREATE TABLE IF NOT EXISTS live_streams (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    stream_url VARCHAR(255),
    thumbnail_url VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    tenant_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    viewer_count INTEGER,
    recording_url VARCHAR(255),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_streams_tenant ON live_streams (tenant_id);

-- modules/leaveRequests/domain/LeaveRequest.java

CREATE TABLE IF NOT EXISTS leave_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    type VARCHAR(255) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    motif TEXT NOT NULL,
    statut VARCHAR(255) NOT NULL,
    demandeur_id UUID NOT NULL,
    valide_par_id UUID,
    commentaire TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_leave_requests_tenant ON leave_requests (tenant_id);

-- modules/executiveInsights/domain/ExecutiveInsight.java

CREATE TABLE IF NOT EXISTS executive_insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    recommended_action TEXT,
    metric_value VARCHAR(255),
    metric_change VARCHAR(255),
    is_read BOOLEAN,
    is_dismissed BOOLEAN,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_executive_insights_tenant ON executive_insights (tenant_id);

-- modules/sermon/domain/SermonTranslation.java

CREATE TABLE IF NOT EXISTS sermon_translations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    sermon_id UUID NOT NULL,
    langue_cible VARCHAR(255) NOT NULL,
    statut VARCHAR(255) NOT NULL,
    transcription_originale TEXT,
    traduction_texte TEXT,
    subtitles_json TEXT,
    confiance DOUBLE PRECISION,
    cree_le TIMESTAMP,
    termine_le TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sermon_translations_tenant ON sermon_translations (tenant_id);

-- modules/announcements/domain/ScheduledAnnouncement.java

CREATE TABLE IF NOT EXISTS scheduled_announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    target VARCHAR(255) NOT NULL,
    target_id UUID,
    status VARCHAR(255) NOT NULL,
    scheduled_at TIMESTAMP,
    published_at TIMESTAMP,
    expires_at TIMESTAMP,
    author_id UUID,
    created_at TIMESTAMP,
    pin_to_top BOOLEAN NOT NULL,
    send_notification BOOLEAN
);

CREATE INDEX IF NOT EXISTS idx_scheduled_announcements_tenant ON scheduled_announcements (tenant_id);

-- modules/members/domain/GeofencePing.java

CREATE TABLE IF NOT EXISTS geofence_pings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    accuracy DOUBLE PRECISION,
    distance_meters INTEGER,
    in_zone BOOLEAN,
    kind VARCHAR(255),
    power_mode VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_geofence_pings_tenant ON geofence_pings (tenant_id);

-- modules/skillsMatrix/domain/SkillEvaluation.java

CREATE TABLE IF NOT EXISTS skill_evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    membre_id UUID NOT NULL,
    compétence VARCHAR(255) NOT NULL,
    niveau VARCHAR(255) NOT NULL,
    évalué_par UUID,
    commentaire TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_skill_evaluations_tenant ON skill_evaluations (tenant_id);

-- modules/growthProjection/domain/GrowthProjection.java

CREATE TABLE IF NOT EXISTS growth_projections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    type_projection VARCHAR(255) NOT NULL,
    cible_id UUID,
    nom VARCHAR(255) NOT NULL,
    effectif_actuel INTEGER NOT NULL,
    effectif_projete INTEGER NOT NULL,
    taux_croissance_annuel DOUBLE PRECISION,
    mois_projection INTEGER,
    hypotheses TEXT,
    recommandations TEXT,
    calcule_le TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_growth_projections_tenant ON growth_projections (tenant_id);

-- modules/churchComparison/domain/ChurchComparison.java

CREATE TABLE IF NOT EXISTS church_comparisons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    nom_eglise VARCHAR(255) NOT NULL,
    effectif INTEGER,
    taux_presence DOUBLE PRECISION,
    taux_conversion DOUBLE PRECISION,
    taux_retention DOUBLE PRECISION,
    score_spirituel_moyen DOUBLE PRECISION,
    generosite_moyenne DOUBLE PRECISION,
    nb_departements INTEGER,
    nb_familles INTEGER,
    categorie VARCHAR(255),
    pays VARCHAR(255),
    denomination VARCHAR(255),
    analyse_le TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_church_comparisons_tenant ON church_comparisons (tenant_id);

-- modules/automations/domain/AutomationExecution.java

CREATE TABLE IF NOT EXISTS automation_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    contexte TEXT,
    statut VARCHAR(255) NOT NULL,
    résultat TEXT,
    exécuté_le TIMESTAMP NOT NULL,
    rule_id UUID
);

CREATE INDEX IF NOT EXISTS idx_automation_executions_tenant ON automation_executions (tenant_id);
CREATE INDEX IF NOT EXISTS idx_automation_executions_rule_id ON automation_executions (rule_id);

-- modules/automations/domain/AutomationRule.java

CREATE TABLE IF NOT EXISTS automation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    trigger_event VARCHAR(255) NOT NULL,
    trigger_params TEXT,
    action_type VARCHAR(255) NOT NULL,
    action_params TEXT,
    statut VARCHAR(255) NOT NULL,
    total_exécutions INTEGER,
    dernière_exécution TIMESTAMP,
    max_exécutions INTEGER,
    créé_par UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_automation_rules_tenant ON automation_rules (tenant_id);

-- modules/predictions/domain/Prediction.java

CREATE TABLE IF NOT EXISTS predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    prediction_type VARCHAR(255) NOT NULL,
    period_months INTEGER,
    current_value DOUBLE PRECISION,
    predicted_value DOUBLE PRECISION,
    growth_rate DOUBLE PRECISION,
    trend VARCHAR(255),
    confidence VARCHAR(255),
    narrative TEXT,
    factors TEXT,
    department_id UUID,
    created_at TIMESTAMP,
    period_start TIMESTAMP,
    period_end TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_predictions_tenant ON predictions (tenant_id);

-- modules/makerTracking/domain/MakerTracking.java

CREATE TABLE IF NOT EXISTS maker_trackings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    faiseur_id UUID NOT NULL,
    type_evenement VARCHAR(255) NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    ref_id UUID,
    ref_type VARCHAR(255),
    points_gagnes INTEGER,
    date_evenement DATE,
    cree_le TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_maker_trackings_tenant ON maker_trackings (tenant_id);

-- modules/spiritualChallenges/domain/SpiritualChallenge.java

CREATE TABLE IF NOT EXISTS spiritual_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(255) NOT NULL,
    statut VARCHAR(255) NOT NULL,
    created_by UUID NOT NULL,
    assignéà UUID,
    objectif_jours INTEGER,
    jours_complétés INTEGER,
    created_at TIMESTAMP NOT NULL,
    deadline TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_spiritual_challenges_tenant ON spiritual_challenges (tenant_id);

-- modules/surveys/domain/SurveyResponse.java

CREATE TABLE IF NOT EXISTS survey_responses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auteur_id UUID,
    reponse TEXT,
    created_at TIMESTAMP NOT NULL,
    survey_id UUID
);

CREATE INDEX IF NOT EXISTS idx_survey_responses_survey_id ON survey_responses (survey_id);

CREATE TABLE IF NOT EXISTS survey_response_selections (
    response_id UUID NOT NULL,
    selection VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_survey_response_selections_response_id ON survey_response_selections (response_id);

-- modules/surveys/domain/Survey.java

CREATE TABLE IF NOT EXISTS surveys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(255) NOT NULL,
    statut VARCHAR(255) NOT NULL,
    total_reponses INTEGER,
    anonyme BOOLEAN,
    cree_par UUID NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_surveys_tenant ON surveys (tenant_id);

CREATE TABLE IF NOT EXISTS survey_options (
    survey_id UUID NOT NULL,
    option_value VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_survey_options_survey_id ON survey_options (survey_id);

-- modules/trainings/domain/TrainingCertificate.java

CREATE TABLE IF NOT EXISTS training_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    numero VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    score_final INTEGER NOT NULL,
    delivre_le TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_training_certificates_tenant ON training_certificates (tenant_id);

-- modules/workflow/domain/Automation.java

CREATE TABLE IF NOT EXISTS automations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    trigger_type VARCHAR(255) NOT NULL,
    trigger_config TEXT,
    action_type VARCHAR(255) NOT NULL,
    action_config TEXT,
    statut VARCHAR(255) NOT NULL,
    nombre_executions INTEGER,
    derniere_execution TIMESTAMP,
    cree_par UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_automations_tenant ON automations (tenant_id);

-- modules/followUpRequests/domain/FollowUpRequest.java

CREATE TABLE IF NOT EXISTS follow_up_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    requester_id UUID NOT NULL,
    requester_name VARCHAR(255),
    type VARCHAR(255) NOT NULL,
    message TEXT,
    preferred_family_id UUID,
    status VARCHAR(255) NOT NULL,
    assigned_to_id UUID,
    assigned_to_name VARCHAR(255),
    resolution_notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_follow_up_requests_tenant ON follow_up_requests (tenant_id);

-- modules/familyResources/domain/FamilyResource.java

CREATE TABLE IF NOT EXISTS family_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    famille_id UUID NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(255) NOT NULL,
    url VARCHAR(255),
    uploadé_par UUID,
    created_at TIMESTAMP NOT NULL,
    téléchargements INTEGER
);

CREATE INDEX IF NOT EXISTS idx_family_resources_tenant ON family_resources (tenant_id);

-- modules/notifications/domain/NotificationPreference.java

CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    email_enabled BOOLEAN,
    push_enabled BOOLEAN,
    sms_enabled BOOLEAN,
    whatsapp_enabled BOOLEAN,
    in_app_enabled BOOLEAN,
    quiet_hours_start INTEGER,
    quiet_hours_end INTEGER,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_preferences_tenant ON notification_preferences (tenant_id);

-- modules/broadcast/domain/BroadcastMessage.java

CREATE TABLE IF NOT EXISTS broadcast_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    titre VARCHAR(255) NOT NULL,
    contenu TEXT NOT NULL,
    cible VARCHAR(255) NOT NULL,
    cible_ids TEXT,
    statut VARCHAR(255) NOT NULL,
    envoyé_par UUID NOT NULL,
    programmé_at TIMESTAMP,
    envoyé_at TIMESTAMP,
    expires_at TIMESTAMP,
    total_envoyé INTEGER,
    total_lu INTEGER,
    receipts VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_broadcast_messages_tenant ON broadcast_messages (tenant_id);

-- modules/broadcast/domain/BroadcastReceipt.java

CREATE TABLE IF NOT EXISTS broadcast_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    membre_id UUID NOT NULL,
    lu BOOLEAN,
    lu_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    broadcast_id UUID
);

CREATE INDEX IF NOT EXISTS idx_broadcast_receipts_broadcast_id ON broadcast_receipts (broadcast_id);

-- modules/devPlan/domain/DevelopmentPlan.java

CREATE TABLE IF NOT EXISTS development_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    membre_id UUID NOT NULL,
    departement_id UUID,
    cree_par_id UUID,
    objectif VARCHAR(255) NOT NULL,
    description TEXT,
    statut VARCHAR(255) NOT NULL,
    priorite VARCHAR(255) NOT NULL,
    date_debut DATE,
    date_echeance DATE,
    progression INTEGER NOT NULL,
    commentaire TEXT,
    cree_le TIMESTAMP,
    modifie_le TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_development_plans_tenant ON development_plans (tenant_id);

-- modules/gantt/domain/TeamTask.java

CREATE TABLE IF NOT EXISTS team_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    statut VARCHAR(255) NOT NULL,
    priorite VARCHAR(255) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    assignea UUID,
    department_id UUID,
    evenement_id UUID,
    progression INTEGER,
    cree_par UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_team_tasks_tenant ON team_tasks (tenant_id);

-- modules/gantt/domain/TeamAssignment.java

CREATE TABLE IF NOT EXISTS team_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    équipe_id UUID NOT NULL,
    événement_id UUID,
    rôle VARCHAR(255) NOT NULL,
    membre_id UUID,
    début TIMESTAMP NOT NULL,
    fin TIMESTAMP NOT NULL,
    statut VARCHAR(255) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_team_assignments_tenant ON team_assignments (tenant_id);

-- modules/engagementAnalytics/domain/EngagementAnalytics.java

CREATE TABLE IF NOT EXISTS engagement_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    metric_name VARCHAR(255),
    metric_category VARCHAR(255),
    metric_value DOUBLE PRECISION,
    previous_value DOUBLE PRECISION,
    change_percentage DOUBLE PRECISION,
    period_days INTEGER,
    dimensions VARCHAR(255),
    recorded_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_engagement_analytics_tenant ON engagement_analytics (tenant_id);

-- modules/eventChecklist/domain/EventChecklistItem.java

CREATE TABLE IF NOT EXISTS event_checklists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    event_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(255) NOT NULL,
    assigned_to UUID,
    order_index INTEGER,
    created_at TIMESTAMP,
    completed_at TIMESTAMP,
    is_auto_generated BOOLEAN NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_event_checklists_tenant ON event_checklists (tenant_id);

-- modules/prayerJournal/domain/PrayerJournalEntry.java

CREATE TABLE IF NOT EXISTS prayer_journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    membre_id UUID NOT NULL,
    contenu TEXT NOT NULL,
    statut VARCHAR(255) NOT NULL,
    visibilité VARCHAR(255) NOT NULL,
    réponse TEXT,
    category VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    exaucée_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prayer_journal_entries_tenant ON prayer_journal_entries (tenant_id);

-- modules/rewards/domain/Reward.java

CREATE TABLE IF NOT EXISTS rewards (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    icon_url VARCHAR(255),
    points_required INTEGER NOT NULL,
    reward_type VARCHAR(255),
    tenant_id BIGINT NOT NULL,
    is_active BOOLEAN,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rewards_tenant ON rewards (tenant_id);

-- modules/rewards/domain/UserRewardClaim.java

CREATE TABLE IF NOT EXISTS user_reward_claims (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reward_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    points_spent INTEGER NOT NULL,
    total_points INTEGER,
    claimed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_reward_claims_tenant ON user_reward_claims (tenant_id);

-- modules/intelligence/domain/IntelligenceKpi.java

CREATE TABLE IF NOT EXISTS intelligence_kpis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(255) NOT NULL,
    current_value DOUBLE PRECISION,
    previous_value DOUBLE PRECISION,
    target_value DOUBLE PRECISION,
    percentage_change DOUBLE PRECISION,
    trend VARCHAR(255),
    unit VARCHAR(255),
    display_order INTEGER,
    narrative TEXT,
    is_alert BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_intelligence_kpis_tenant ON intelligence_kpis (tenant_id);

-- modules/moderation/domain/ModerationItem.java

CREATE TABLE IF NOT EXISTS moderation_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    source VARCHAR(255) NOT NULL,
    source_id UUID,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(255) NOT NULL,
    risk_level VARCHAR(255),
    ai_confidence DOUBLE PRECISION,
    ai_flags TEXT,
    moderator_notes TEXT,
    reviewed_by UUID,
    created_at TIMESTAMP,
    reviewed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_moderation_items_tenant ON moderation_items (tenant_id);

-- modules/calendar/domain/CalendarEvent.java

CREATE TABLE IF NOT EXISTS calendar_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    début TIMESTAMP NOT NULL,
    fin TIMESTAMP NOT NULL,
    lieu VARCHAR(255),
    événement_id UUID,
    source VARCHAR(255) NOT NULL,
    statut VARCHAR(255) NOT NULL,
    external_id VARCHAR(255),
    rappel_activé BOOLEAN,
    rappel_minutes_avant INTEGER,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_calendar_events_tenant ON calendar_events (tenant_id);

-- modules/departmentKpi/domain/DepartmentKpi.java

CREATE TABLE IF NOT EXISTS department_kpis (
    id BIGSERIAL PRIMARY KEY,
    department_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    target_value DOUBLE PRECISION,
    current_value DOUBLE PRECISION,
    unit VARCHAR(255),
    period VARCHAR(255),
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_department_kpis_tenant ON department_kpis (tenant_id);

-- modules/forms/domain/FormTemplate.java

CREATE TABLE IF NOT EXISTS form_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    statut VARCHAR(255) NOT NULL,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    fields_json TEXT,
    categorie VARCHAR(255),
    anonyme BOOLEAN,
    cree_par UUID,
    cree_le TIMESTAMP,
    publie_le TIMESTAMP,
    expire_le TIMESTAMP,
    nb_reponses INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_form_templates_tenant ON form_templates (tenant_id);

-- modules/forms/domain/FormResponse.java

CREATE TABLE IF NOT EXISTS form_responses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    form_template_id UUID NOT NULL,
    submitted_by UUID,
    answers_json TEXT,
    submitted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_form_responses_tenant ON form_responses (tenant_id);

-- modules/groupMessages/domain/GroupMessage.java

CREATE TABLE IF NOT EXISTS group_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    group_id UUID NOT NULL,
    group_type VARCHAR(255) NOT NULL,
    sender_id UUID NOT NULL,
    content TEXT,
    message_type VARCHAR(255),
    file_name VARCHAR(255),
    reply_to_id VARCHAR(255),
    reaction_count INTEGER,
    is_deleted BOOLEAN NOT NULL,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_group_messages_tenant ON group_messages (tenant_id);

-- modules/skillMatching/domain/SkillMatch.java

CREATE TABLE IF NOT EXISTS skill_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    membre_id UUID NOT NULL,
    departement_id UUID NOT NULL,
    competence VARCHAR(255) NOT NULL,
    score_match INTEGER NOT NULL,
    statut VARCHAR(255) NOT NULL,
    justification TEXT,
    cree_par UUID,
    cree_le TIMESTAMP,
    repondu_le TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_skill_matches_tenant ON skill_matches (tenant_id);

-- modules/encouragements/domain/Encouragement.java

CREATE TABLE IF NOT EXISTS encouragements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    kind VARCHAR(255) NOT NULL,
    message TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_encouragements_tenant ON encouragements (tenant_id);

-- modules/directory/domain/DirectoryEntry.java

CREATE TABLE IF NOT EXISTS directory_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    membre_id UUID NOT NULL,
    public_profil BOOLEAN,
    photo_url VARCHAR(255),
    bio TEXT,
    téléphone VARCHAR(255),
    email VARCHAR(255),
    département VARCHAR(255),
    rôle VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_directory_entries_tenant ON directory_entries (tenant_id);

-- modules/familyCohesion/domain/FamilyCohesion.java

CREATE TABLE IF NOT EXISTS family_cohesion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    famille_id UUID NOT NULL,
    taux_participation DOUBLE PRECISION,
    diversité_âmes INTEGER,
    équilibre_charges INTEGER,
    score_cohésion DOUBLE PRECISION,
    recommandations TEXT,
    calculé_le TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_family_cohesion_tenant ON family_cohesion (tenant_id);

-- modules/weeklyChallenges/domain/WeeklyChallenge.java

CREATE TABLE IF NOT EXISTS weekly_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(255) NOT NULL,
    difficulty VARCHAR(255),
    xp_reward INTEGER,
    is_auto_generated BOOLEAN NOT NULL,
    assigned_to_id UUID,
    week_number INTEGER,
    year INTEGER,
    status VARCHAR(255),
    progress INTEGER,
    created_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_weekly_challenges_tenant ON weekly_challenges (tenant_id);

-- modules/mentoring/domain/MentorSuggestion.java

CREATE TABLE IF NOT EXISTS mentor_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    chef_de_famille_id UUID NOT NULL,
    faiseur_id UUID NOT NULL,
    priorité VARCHAR(255) NOT NULL,
    catégorie VARCHAR(255) NOT NULL,
    titre TEXT NOT NULL,
    "analyse" TEXT NOT NULL,
    action_recommandée TEXT,
    raisonnement TEXT,
    confiance DOUBLE PRECISION,
    statut VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mentor_suggestions_tenant ON mentor_suggestions (tenant_id);

-- modules/dataMigration/domain/DataMigrationJob.java

CREATE TABLE IF NOT EXISTS data_migration_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    source_type VARCHAR(255) NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    field_mapping TEXT,
    total_rows INTEGER,
    imported_rows INTEGER,
    error_rows INTEGER,
    errors_log TEXT,
    created_by UUID,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_data_migration_jobs_tenant ON data_migration_jobs (tenant_id);

-- modules/kpiNarrative/domain/KpiNarrative.java

CREATE TABLE IF NOT EXISTS kpi_narratives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    typekpi VARCHAR(255) NOT NULL,
    période VARCHAR(255) NOT NULL,
    valeur_actuelle DOUBLE PRECISION,
    valeur_précédente DOUBLE PRECISION,
    tendance VARCHAR(255) NOT NULL,
    variation_pct DOUBLE PRECISION,
    narration TEXT NOT NULL,
    causes TEXT,
    recommandations TEXT,
    département_id UUID,
    généré_le TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_kpi_narratives_tenant ON kpi_narratives (tenant_id);

-- modules/marketplace/domain/MarketplaceListing.java

CREATE TABLE IF NOT EXISTS marketplace_listings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    listing_type VARCHAR(255),
    category VARCHAR(255),
    price_cents BIGINT,
    image_url VARCHAR(255),
    seller_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    is_active BOOLEAN,
    contact_info VARCHAR(255),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_marketplace_listings_tenant ON marketplace_listings (tenant_id);

-- modules/spiritualJournal/domain/SpiritualJournal.java

CREATE TABLE IF NOT EXISTS spiritual_journals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    auteur_id UUID NOT NULL,
    type_entree VARCHAR(255) NOT NULL,
    titre VARCHAR(255) NOT NULL,
    contenu TEXT,
    date_entree DATE,
    publique BOOLEAN,
    favori BOOLEAN,
    cree_le TIMESTAMP,
    modifie_le TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_spiritual_journals_tenant ON spiritual_journals (tenant_id);

-- modules/reverseMentoring/domain/ReverseMentoringRequest.java

CREATE TABLE IF NOT EXISTS reverse_mentoring_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    requester_id UUID NOT NULL,
    assigned_mentor_id UUID,
    topic VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(255) NOT NULL,
    outcome TEXT,
    urgency_level INTEGER,
    created_at TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reverse_mentoring_requests_tenant ON reverse_mentoring_requests (tenant_id);

-- modules/visits/domain/PastoralVisit.java

CREATE TABLE IF NOT EXISTS pastoral_visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    visiteur_id UUID NOT NULL,
    membre_id UUID NOT NULL,
    motif VARCHAR(255) NOT NULL,
    statut VARCHAR(255) NOT NULL,
    prévu_le TIMESTAMP NOT NULL,
    réalisé_le TIMESTAMP,
    notes TEXT,
    auto_généré BOOLEAN,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pastoral_visits_tenant ON pastoral_visits (tenant_id);
