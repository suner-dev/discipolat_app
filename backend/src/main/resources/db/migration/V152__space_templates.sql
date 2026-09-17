-- V152__space_templates.sql
-- ============================================================
-- G2.3 — Template Engine (départements, familles, dress code)
-- Contrat : Annexe A §A.1 (space_template)
-- 20 templates métier seedés, chacun avec modules, workflows, statuts, dashboards par défaut
-- ============================================================

CREATE TABLE IF NOT EXISTS space_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(100),
    color VARCHAR(7),
    version INTEGER NOT NULL DEFAULT 1,
    modules_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    default_workflows_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    default_statuses_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    default_dashboards_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_space_template_code ON space_templates(code);

COMMENT ON TABLE space_templates IS 'G2.3 : templates d''espaces (département/famille/sous-équipe) — création en 1 clic, modifiable après';
COMMENT ON COLUMN space_templates.modules_json IS 'Liste des modules activés par défaut (codes)';
COMMENT ON COLUMN space_templates.default_workflows_json IS 'Workflows par défaut pour ce template';
COMMENT ON COLUMN space_templates.default_statuses_json IS 'Statuts par défaut pour ce template';
COMMENT ON COLUMN space_templates.default_dashboards_json IS 'Dashboards/widgets par défaut pour ce template';

-- ============================================================
-- SEED : 20 templates métier (Annexe A + exigences utilisateur)
-- ============================================================

-- 1. AUDIOVISUAL
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('AUDIOVISUAL', 'Audiovisuel', 'Gestion technique son/vidéo/streaming', 'Video', '#3b82f6',
 '["people", "teams", "assets", "inventory", "maintenance", "events", "tasks", "finance", "reports", "archive"]',
 '[{"code": "equipment_checkout", "name": "Sortie matériel", "steps": [{"name": "Demande", "type": "FORM"}, {"name": "Validation responsable", "type": "APPROVAL"}, {"name": "Préparation", "type": "AUTO_ACTION"}, {"name": "Sortie", "type": "ASSET_STATUS"}]}]',
 '[{"code": "AVAILABLE", "name": "Disponible", "color": "#22c55e", "initial": true}, {"code": "CHECKED_OUT", "name": "Sorti", "color": "#3b82f6"}, {"code": "MAINTENANCE", "name": "Maintenance", "color": "#f59e0b"}, {"code": "DAMAGED", "name": "Endommagé", "color": "#ef4444"}]',
 '[{"widget": "equipment_status", "title": "État du matériel"}, {"widget": "upcoming_events", "title": "Événements à venir"}, {"widget": "maintenance_alerts", "title": "Alertes maintenance"}, {"widget": "team_schedule", "title": "Planning équipe"}]')
ON CONFLICT (code) DO NOTHING;

-- 2. CHOIR
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('CHOIR', 'Chorale', 'Gestion répétitions, répertoire, partitions', 'Music', '#8b5cf6',
 '["people", "teams", "events", "rehearsal", "repertoire", "tasks", "archive"]',
 '[{"code": "rehearsal_planning", "name": "Planification répétition", "steps": [{"name": "Proposition créneau", "type": "FORM"}, {"name": "Validation chef", "type": "APPROVAL"}, {"name": "Notification membres", "type": "NOTIFY"}]}]',
 '[{"code": "PLANNED", "name": "Planifiée", "color": "#8b5cf6", "initial": true}, {"code": "IN_PROGRESS", "name": "En cours", "color": "#3b82f6"}, {"code": "COMPLETED", "name": "Terminée", "color": "#22c55e"}, {"code": "CANCELLED", "name": "Annulée", "color": "#ef4444"}]',
 '[{"widget": "next_rehearsal", "title": "Prochaine répétition"}, {"widget": "repertoire_progress", "title": "Avancement répertoire"}, {"widget": "attendance", "title": "Présence"}, {"widget": "upcoming_services", "title": "Services à venir"}]')
ON CONFLICT (code) DO NOTHING;

-- 3. LOGISTICS
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('LOGISTICS', 'Logistique', 'Matériel, transport, montage/démontage', 'Truck', '#f59e0b',
 '["people", "teams", "assets", "inventory", "events", "tasks", "finance", "archive"]',
 '[{"code": "setup_teardown", "name": "Montage/Démontage", "steps": [{"name": "Planning", "type": "FORM"}, {"name": "Assignation équipe", "type": "AUTO_ACTION"}, {"name": "Validation fin", "type": "APPROVAL"}]}]',
 '[{"code": "PENDING", "name": "En attente", "color": "#94a3b8", "initial": true}, {"code": "IN_PROGRESS", "name": "En cours", "color": "#3b82f6"}, {"code": "COMPLETED", "name": "Terminé", "color": "#22c55e"}, {"code": "ISSUE", "name": "Problème", "color": "#ef4444"}]',
 '[{"widget": "inventory_levels", "title": "Niveaux stock"}, {"widget": "event_logistics", "title": "Logistique événements"}, {"widget": "team_assignments", "title": "Assignations équipe"}, {"widget": "budget_tracking", "title": "Suivi budget"}]')
ON CONFLICT (code) DO NOTHING;

-- 4. FINANCE
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('FINANCE', 'Finances', 'Budget, dépenses, recettes, rapports', 'DollarSign', '#22c55e',
 '["people", "finance", "reports", "archive", "budget"]',
 '[{"code": "expense_approval", "name": "Validation dépense", "steps": [{"name": "Soumission", "type": "FORM"}, {"name": "Validation responsable", "type": "APPROVAL"}, {"name": "Validation finance", "type": "APPROVAL"}, {"name": "Paiement", "type": "AUTO_ACTION"}]}]',
 '[{"code": "DRAFT", "name": "Brouillon", "color": "#94a3b8", "initial": true}, {"code": "SUBMITTED", "name": "Soumis", "color": "#3b82f6"}, {"code": "APPROVED", "name": "Approuvé", "color": "#22c55e"}, {"code": "PAID", "name": "Payé", "color": "#8b5cf6"}, {"code": "REJECTED", "name": "Rejeté", "color": "#ef4444"}]',
 '[{"widget": "budget_overview", "title": "Vue budget"}, {"widget": "pending_expenses", "title": "Dépenses en attente"}, {"widget": "cash_flow", "title": "Flux trésorerie"}, {"widget": "monthly_report", "title": "Rapport mensuel"}]')
ON CONFLICT (code) DO NOTHING;

-- 5. PRAYER
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('PRAYER', 'Prière', 'Programmes, créneaux, requêtes, jeûnes', 'Heart', '#ec4899',
 '["people", "prayer", "events", "archive"]',
 '[{"code": "prayer_slot", "name": "Créneau prière", "steps": [{"name": "Inscription", "type": "FORM"}, {"name": "Confirmation", "type": "AUTO_ACTION"}, {"name": "Rappel", "type": "NOTIFY"}]}]',
 '[{"code": "OPEN", "name": "Ouvert", "color": "#22c55e", "initial": true}, {"code": "FILLED", "name": "Pourvu", "color": "#3b82f6"}, {"code": "COMPLETED", "name": "Effectué", "color": "#8b5cf6"}, {"code": "MISSED", "name": "Manqué", "color": "#ef4444"}]',
 '[{"widget": "prayer_schedule", "title": "Planning prière"}, {"widget": "active_requests", "title": "Requêtes actives"}, {"widget": "fasting_calendar", "title": "Calendrier jeûnes"}, {"widget": "participation", "title": "Participation"}]')
ON CONFLICT (code) DO NOTHING;

-- 6. CHILDREN
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('CHILDREN', 'Enfants', 'Ministère enfants, sécurité, activités', 'Baby', '#f97316',
 '["people", "events", "tasks", "checklists", "attendance", "archive", "safety"]',
 '[{"code": "activity_approval", "name": "Validation activité", "steps": [{"name": "Proposition", "type": "FORM"}, {"name": "Validation sécurité", "type": "APPROVAL"}, {"name": "Communication parents", "type": "NOTIFY"}]}]',
 '[{"code": "PLANNED", "name": "Planifiée", "color": "#3b82f6", "initial": true}, {"code": "READY", "name": "Prête", "color": "#22c55e"}, {"code": "IN_PROGRESS", "name": "En cours", "color": "#f59e0b"}, {"code": "COMPLETED", "name": "Terminée", "color": "#8b5cf6"}]',
 '[{"widget": "upcoming_activities", "title": "Activités à venir"}, {"widget": "attendance_trends", "title": "Tendances présence"}, {"widget": "safety_checklist", "title": "Checklist sécurité"}, {"widget": "volunteer_schedule", "title": "Planning bénévoles"}]')
ON CONFLICT (code) DO NOTHING;

-- 7. YOUTH
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('YOUTH', 'Jeunes', 'Ministère jeunesse, événements, discipleship', 'Users', '#ec4899',
 '["people", "events", "discipleship", "tasks", "archive", "prayer"]',
 '[{"code": "event_approval", "name": "Validation événement jeunes", "steps": [{"name": "Proposition", "type": "FORM"}, {"name": "Validation pasteur", "type": "APPROVAL"}, {"name": "Logistique", "type": "AUTO_ACTION"}, {"name": "Communication", "type": "NOTIFY"}]}]',
 '[{"code": "IDEA", "name": "Idée", "color": "#94a3b8", "initial": true}, {"code": "PLANNING", "name": "En préparation", "color": "#3b82f6"}, {"code": "READY", "name": "Prêt", "color": "#22c55e"}, {"code": "DONE", "name": "Réalisé", "color": "#8b5cf6"}]',
 '[{"widget": "youth_events", "title": "Événements jeunes"}, {"widget": "discipleship_groups", "title": "Groupes discipleship"}, {"widget": "attendance", "title": "Présence"}, {"widget": "prayer_requests", "title": "Requêtes prière"}]')
ON CONFLICT (code) DO NOTHING;

-- 8. FAMILY
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('FAMILY', 'Famille', 'Suivi des âmes, visites, réceptions, comptes-rendus', 'Home', '#10b981',
 '["people", "visits", "receptions", "events", "reports", "archive", "discipleship"]',
 '[{"code": "visit_followup", "name": "Suivi visite", "steps": [{"name": "Programmation", "type": "FORM"}, {"name": "Réalisation", "type": "AUTO_ACTION"}, {"name": "Compte-rendu", "type": "FORM"}, {"name": "Prochaine étape", "type": "AUTO_ACTION"}]}]',
 '[{"code": "NEW", "name": "Nouvelle", "color": "#3b82f6", "initial": true}, {"code": "IN_PROGRESS", "name": "En suivi", "color": "#f59e0b"}, {"code": "STABLE", "name": "Stable", "color": "#22c55e"}, {"code": "ATTENTION", "name": "Attention", "color": "#ef4444"}]',
 '[{"widget": "souls_overview", "title": "Vue d''ensemble âmes"}, {"widget": "pending_visits", "title": "Visites en attente"}, {"widget": "receptions", "title": "Réceptions récentes"}, {"widget": "followup_timeline", "title": "Timeline suivi"}]')
ON CONFLICT (code) DO NOTHING;

-- 9. EVANGELISM
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('EVANGELISM', 'Évangélisation', 'Campagnes, sorties, suivi nouveaux convertis', 'Megaphone', '#f43f5e',
 '["people", "events", "tasks", "discipleship", "reports", "archive"]',
 '[{"code": "campaign_followup", "name": "Suivi campagne", "steps": [{"name": "Inscription", "type": "FORM"}, {"name": "Premier contact", "type": "AUTO_ACTION"}, {"name": "Suivi 48h", "type": "AUTO_ACTION"}, {"name": "Intégration", "type": "APPROVAL"}]}]',
 '[{"code": "CONTACTED", "name": "Contacté", "color": "#3b82f6", "initial": true}, {"code": "INTERESTED", "name": "Intéressé", "color": "#f59e0b"}, {"code": "CONVERTED", "name": "Converti", "color": "#22c55e"}, {"code": "INTEGRATED", "name": "Intégré", "color": "#8b5cf6"}]',
 '[{"widget": "campaign_stats", "title": "Stats campagne"}, {"widget": "new_contacts", "title": "Nouveaux contacts"}, {"widget": "followup_funnel", "title": "Entonnoir suivi"}, {"widget": "conversion_rate", "title": "Taux conversion"}]')
ON CONFLICT (code) DO NOTHING;

-- 10. MAINTENANCE
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('MAINTENANCE', 'Maintenance', 'Maintenance préventive/corrective, tickets', 'Wrench', '#6b7280',
 '["assets", "maintenance", "inventory", "tasks", "finance", "archive"]',
 '[{"code": "maintenance_ticket", "name": "Ticket maintenance", "steps": [{"name": "Signalement", "type": "FORM"}, {"name": "Diagnostic", "type": "APPROVAL"}, {"name": "Intervention", "type": "AUTO_ACTION"}, {"name": "Validation", "type": "APPROVAL"}, {"code": "CLOSURE", "name": "Clôture", "type": "AUTO_ACTION"}]}]',
 '[{"code": "OPEN", "name": "Ouvert", "color": "#ef4444", "initial": true}, {"code": "DIAGNOSING", "name": "Diagnostic", "color": "#f59e0b"}, {"code": "IN_PROGRESS", "name": "En cours", "color": "#3b82f6"}, {"code": "TESTING", "name": "Test", "color": "#8b5cf6"}, {"code": "CLOSED", "name": "Fermé", "color": "#22c55e"}]',
 '[{"widget": "open_tickets", "title": "Tickets ouverts"}, {"widget": "preventive_schedule", "title": "Planning préventif"}, {"widget": "asset_health", "title": "Santé équipements"}, {"widget": "cost_tracking", "title": "Coûts maintenance"}]')
ON CONFLICT (code) DO NOTHING;

-- 11. BOOKSTORE
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('BOOKSTORE', 'Librairie', 'Stock livres, ventes, commande, inventaire', 'BookOpen', '#84cc16',
 '["inventory", "finance", "sales", "tasks", "reports", "archive"]',
 '[{"code": "restock_order", "name": "Commande réapprovisionnement", "steps": [{"name": "Détection seuil", "type": "AUTO_ACTION"}, {"name": "Validation commande", "type": "APPROVAL"}, {"name": "Réception", "type": "AUTO_ACTION"}, {"name": "Mise en rayon", "type": "AUTO_ACTION"}]}]',
 '[{"code": "IN_STOCK", "name": "En stock", "color": "#22c55e", "initial": true}, {"code": "LOW_STOCK", "name": "Stock bas", "color": "#f59e0b"}, {"code": "OUT_OF_STOCK", "name": "Rupture", "color": "#ef4444"}, {"code": "ORDERED", "name": "Commandé", "color": "#3b82f6"}]',
 '[{"widget": "stock_levels", "title": "Niveaux stock"}, {"widget": "sales_dashboard", "title": "Tableau ventes"}, {"widget": "top_sellers", "title": "Meilleures ventes"}, {"widget": "pending_orders", "title": "Commandes en attente"}]')
ON CONFLICT (code) DO NOTHING;

-- 12. MEDIA
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('MEDIA', 'Médias', 'Photos, vidéos, streaming, archives', 'Camera', '#6366f1',
 '["media", "events", "tasks", "archive", "streaming"]',
 '[{"code": "media_production", "name": "Production média", "steps": [{"name": "Brief", "type": "FORM"}, {"name": "Capture", "type": "AUTO_ACTION"}, {"name": "Montage", "type": "APPROVAL"}, {"name": "Publication", "type": "AUTO_ACTION"}]}]',
 '[{"code": "DRAFT", "name": "Brouillon", "color": "#94a3b8", "initial": true}, {"code": "IN_PRODUCTION", "name": "En production", "color": "#3b82f6"}, {"code": "REVIEW", "name": "Revue", "color": "#f59e0b"}, {"code": "PUBLISHED", "name": "Publié", "color": "#22c55e"}]',
 '[{"widget": "recent_media", "title": "Médias récents"}, {"widget": "storage_usage", "title": "Utilisation stockage"}, {"widget": "streaming_stats", "title": "Stats streaming"}, {"widget": "pending_review", "title": "En attente revue"}]')
ON CONFLICT (code) DO NOTHING;

-- 13. DISCIPLESHIP
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('DISCIPLESHIP', 'Discipleship', 'Parcours, mentorat, groupes, étapes', 'Compass', '#14b8a6',
 '["people", "discipleship", "events", "tasks", "prayer", "archive"]',
 '[{"code": "journey_progression", "name": "Progression parcours", "steps": [{"name": "Évaluation", "type": "FORM"}, {"name": "Validation mentor", "type": "APPROVAL"}, {"name": "Célébration", "type": "NOTIFY"}, {"name": "Nouvelle étape", "type": "AUTO_ACTION"}]}]',
 '[{"code": "VISITOR", "name": "Visiteur", "color": "#94a3b8", "initial": true}, {"code": "SEEKER", "name": "Cherchant", "color": "#3b82f6"}, {"code": "NEW_BELIEVER", "name": "Nouveau croyant", "color": "#22c55e"}, {"code": "DISCIPLE", "name": "Disciple", "color": "#8b5cf6"}, {"code": "LEADER", "name": "Leader", "color": "#f59e0b"}]',
 '[{"widget": "journey_distribution", "title": "Répartition parcours"}, {"widget": "mentor_pairs", "title": "Binômes mentor/disciple"}, {"widget": "group_attendance", "title": "Présence groupes"}, {"widget": "stage_progression", "title": "Progression étapes"}]')
ON CONFLICT (code) DO NOTHING;

-- 14. ACCUEIL
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('ACCUEIL', 'Accueil', 'Accueil dominical, dress code, orientation', 'Handshake', '#06b6d4',
 '["people", "dresscode", "events", "tasks", "checklists", "archive"]',
 '[{"code": "service_preparation", "name": "Préparation service", "steps": [{"name": "Planning équipe", "type": "FORM"}, {"name": "Validation dress code", "type": "APPROVAL"}, {"name": "Brief équipe", "type": "NOTIFY"}, {"name": "Service", "type": "AUTO_ACTION"}]}]',
 '[{"code": "SCHEDULED", "name": "Programmé", "color": "#3b82f6", "initial": true}, {"code": "READY", "name": "Prêt", "color": "#22c55e"}, {"code": "ACTIVE", "name": "Actif", "color": "#f59e0b"}, {"code": "COMPLETED", "name": "Terminé", "color": "#8b5cf6"}]',
 '[{"widget": "team_schedule", "title": "Planning équipe"}, {"widget": "dress_code_today", "title": "Tenue du jour"}, {"widget": "visitor_count", "title": "Visiteurs attendus"}, {"widget": "team_checkin", "title": "Check-in équipe"}]')
ON CONFLICT (code) DO NOTHING;

-- 15. PROTOCOLE
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('PROTOCOLE', 'Protocole', 'Ordre de service, officiels, cadeaux', 'Scroll', '#a855f7',
 '["people", "events", "tasks", "checklists", "archive", "gifts"]',
 '[{"code": "protocol_preparation", "name": "Préparation protocole", "steps": [{"name": "Réception demande", "type": "FORM"}, {"name": "Validation pasteur", "type": "APPROVAL"}, {"name": "Préparation logistique", "type": "AUTO_ACTION"}, {"name": "Exécution", "type": "AUTO_ACTION"}]}]',
 '[{"code": "REQUESTED", "name": "Demandé", "color": "#94a3b8", "initial": true}, {"code": "APPROVED", "name": "Approuvé", "color": "#3b82f6"}, {"code": "PREPARING", "name": "En préparation", "color": "#f59e0b"}, {"code": "EXECUTED", "name": "Exécuté", "color": "#22c55e"}]',
 '[{"widget": "pending_requests", "title": "Demandes en attente"}, {"widget": "upcoming_officials", "title": "Officiels à venir"}, {"widget": "gift_registry", "title": "Registre cadeaux"}, {"widget": "protocol_checklist", "title": "Checklist protocole"}]')
ON CONFLICT (code) DO NOTHING;

-- 16. COORDINATION
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('COORDINATION', 'Coordination', 'Coordination inter-départements, planning global', 'Grid', '#3b82f6',
 '["events", "tasks", "calendar", "teams", "reports", "archive"]',
 '[{"code": "cross_dept_coordination", "name": "Coordination interdépartement", "steps": [{"name": "Réunion préparation", "type": "FORM"}, {"name": "Validation leads", "type": "APPROVAL"}, {"name": "Communication", "type": "NOTIFY"}, {"name": "Suivi actions", "type": "AUTO_ACTION"}]}]',
 '[{"code": "PLANNED", "name": "Planifié", "color": "#3b82f6", "initial": true}, {"code": "IN_PROGRESS", "name": "En cours", "color": "#f59e0b"}, {"code": "COMPLETED", "name": "Terminé", "color": "#22c55e"}, {"code": "BLOCKED", "name": "Bloqué", "color": "#ef4444"}]',
 '[{"widget": "global_calendar", "title": "Calendrier global"}, {"widget": "dept_status", "title": "Statut départements"}, {"widget": "action_items", "title": "Points d''action"}, {"widget": "resource_conflicts", "title": "Conflits ressources"}]')
ON CONFLICT (code) DO NOTHING;

-- 17. INTERCESSION
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('INTERCESSION', 'Intercession', 'Prière d''intercession, jeûnes, veilles', 'Flame', '#f97316',
 '["people", "prayer", "events", "fasting", "archive"]',
 '[{"code": "intercession_chain", "name": "Chaîne intercession", "steps": [{"name": "Inscription", "type": "FORM"}, {"name": "Assignation créneau", "type": "AUTO_ACTION"}, {"name": "Rappel", "type": "NOTIFY"}, {"name": "Témoignage", "type": "FORM"}]}]',
 '[{"code": "OPEN", "name": "Ouvert", "color": "#22c55e", "initial": true}, {"code": "ASSIGNED", "name": "Assigné", "color": "#3b82f6"}, {"code": "COMPLETED", "name": "Effectué", "color": "#8b5cf6"}, {"code": "MISSED", "name": "Manqué", "color": "#ef4444"}]',
 '[{"widget": "intercession_schedule", "title": "Planning intercession"}, {"widget": "active_fastings", "title": "Jeûnes actifs"}, {"widget": "prayer_burden", "title": "Sujets prière"}, {"widget": "testimonies", "title": "Témoignages"}]')
ON CONFLICT (code) DO NOTHING;

-- 18. SECURITE
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('SECURITE', 'Sécurité', 'Sécurité physique, contrôle accès, incidents', 'Shield', '#ef4444',
 '["people", "events", "tasks", "checklists", "incidents", "archive"]',
 '[{"code": "incident_report", "name": "Rapport incident", "steps": [{"name": "Signalement", "type": "FORM"}, {"name": "Évaluation", "type": "APPROVAL"}, {"name": "Action corrective", "type": "AUTO_ACTION"}, {"name": "Clôture", "type": "APPROVAL"}]}]',
 '[{"code": "REPORTED", "name": "Signalé", "color": "#ef4444", "initial": true}, {"code": "ASSESSING", "name": "Évaluation", "color": "#f59e0b"}, {"code": "RESOLVING", "name": "Résolution", "color": "#3b82f6"}, {"code": "CLOSED", "name": "Clos", "color": "#22c55e"}]',
 '[{"widget": "active_incidents", "title": "Incidents actifs"}, {"widget": "shift_schedule", "title": "Planning gardes"}, {"widget": "access_logs", "title": "Logs accès"}, {"widget": "safety_checklist", "title": "Checklist sécurité"}]')
ON CONFLICT (code) DO NOTHING;

-- 19. COMMUNICATION
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('COMMUNICATION', 'Communication', 'Annonces, réseaux sociaux, newsletter, site web', 'MessageSquare', '#06b6d4',
 '["people", "announcements", "social", "newsletter", "website", "tasks", "archive"]',
 '[{"code": "announcement_approval", "name": "Validation annonce", "steps": [{"name": "Rédaction", "type": "FORM"}, {"name": "Relecture", "type": "APPROVAL"}, {"name": "Validation pasteur", "type": "APPROVAL"}, {"name": "Publication", "type": "AUTO_ACTION"}]}]',
 '[{"code": "DRAFT", "name": "Brouillon", "color": "#94a3b8", "initial": true}, {"code": "REVIEW", "name": "Relecture", "color": "#3b82f6"}, {"code": "APPROVED", "name": "Approuvé", "color": "#22c55e"}, {"code": "PUBLISHED", "name": "Publié", "color": "#8b5cf6"}]',
 '[{"widget": "pending_announcements", "title": "Annonces en attente"}, {"widget": "social_stats", "title": "Stats réseaux sociaux"}, {"widget": "newsletter_performance", "title": "Performance newsletter"}, {"widget": "website_analytics", "title": "Analytics site"}]')
ON CONFLICT (code) DO NOTHING;

-- 20. HEALTH (Infirmerie - exigence utilisateur)
INSERT INTO space_templates (code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json) VALUES
('HEALTH', 'Infirmerie / Santé', 'Dossiers patients, pharmacie, gardes, campagnes médicales, kits', 'HeartPulse', '#dc2626',
 '["people", "patients", "consultations", "pharmacy", "campaigns", "kits", "staff_duty", "reports", "archive"]',
 '[{"code": "consultation_flow", "name": "Flux consultation", "steps": [{"name": "Tri", "type": "FORM"}, {"name": "Consultation", "type": "AUTO_ACTION"}, {"name": "Prescription", "type": "FORM"}, {"name": "Distribution", "type": "AUTO_ACTION"}, {"name": "Suivi", "type": "APPROVAL"}]},
  {"code": "campaign_execution", "name": "Exécution campagne", "steps": [{"name": "Planification", "type": "FORM"}, {"name": "Validation médicale", "type": "APPROVAL"}, {"name": "Logistique", "type": "AUTO_ACTION"}, {"name": "Terrain", "type": "AUTO_ACTION"}, {"name": "Rapport", "type": "FORM"}]}]',
 '[{"code": "WAITING", "name": "Attente", "color": "#f59e0b", "initial": true}, {"code": "IN_CONSULTATION", "name": "En consultation", "color": "#3b82f6"}, {"code": "PRESCRIBED", "name": "Prescrit", "color": "#8b5cf6"}, {"code": "DISPENSED", "name": "Distribué", "color": "#22c55e"}, {"code": "FOLLOWUP", "name": "Suivi", "color": "#06b6d4"}]',
 '[{"widget": "waiting_patients", "title": "Patients en attente"}, {"widget": "pharmacy_alerts", "title": "Alertes pharmacie"}, {"widget": "active_campaigns", "title": "Campagnes actives"}, {"widget": "staff_duty", "title": "Gardes personnel"}, {"widget": "kit_stock", "title": "Stock kits"}]')
ON CONFLICT (code) DO NOTHING;

-- Trigger updated_at
DROP TRIGGER IF EXISTS update_space_templates_updated_at ON space_templates;
CREATE TRIGGER update_space_templates_updated_at
    BEFORE UPDATE ON space_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();