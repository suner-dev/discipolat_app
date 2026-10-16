-- ============================================================
-- seed-pasteur-demo.sql — finition des données de démonstration
-- (bêta tunnel : l'expérience du pasteur ne doit pas être gâtée)
-- ------------------------------------------------------------
-- 1) Prières : les titres seedés en série se répétaient à l'identique
--    (« Guérison d'un proche » ×15) — on les personnalise par demandeur.
-- 2) Graphiques du tableau de bord : la tendance de présence se calcule
--    sur les rapports faiseur (maker_reports, par semaine) — il n'y en
--    avait qu'un seul. On régénère 12 semaines de rapports soumis.
-- 3) Fil « Activité récente » : les dernières entrées d'audit étaient
--    des événements machine de provisionnement de test (locuteur UUID,
--    actions TENANT_CREATED). On sème des actions humaines récentes qui
--    dominent le tri par date.
-- 4) Suivi pastoral et alertes : le job planifié « absence 48 h » crée une
--    alerte par âme sans contact récent. Le volumineux seedait un dernier
--    contact étalé sur 20 jours : ~900 alertes actives, chiffre invraisem-
--    blable sur la carte KPI et page Alertes inutilisable. On modélise une
--    église réellement en suivi (≈ 97 % vus sous 48 h, ≈ 2 % en retard,
--    ≈ 1 % en décrochage) puis on solde les alertes devenues sans objet.
-- 5) Comptes résiduels des campagnes de test (« Rachel E2E », « Jean
--    Test », « Invitée E2E » en @e2e.test, « Famille E2E 161702 »,
--    « Departement Jeunesse 085229 », huit « E2E Culte mv… ») : le
--    pasteur lirait des restes de bac à sable dans son annuaire, ses
--    espaces et son agenda. On les rebaptise, sans supprimer aucune
--    ligne (bulletins, départements et journaux qui les citent restent
--    intègres).
-- 6) Agenda : le jeu volumineux a semé quatre séries de vingt événements
--    portant le MÊME instant (« Réunion hebdomadaire » ×20, « Retraite
--    spirituelle » ×20…). La page Événements du pasteur était un mur de
--    lignes identiques. On rend chaque série lisible : rythme réellement
--    hebdomadaire pour les réunions, sessions distinctes d'une même
--    célébration pour le reste.
-- Idempotent : chaque insertion se fait sous NOT EXISTS ; la variation
-- de titre n'affecte que les lignes portant le motif « · demande de … ».
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
-- 1) TITRES DE PRIÈRES — suffixe « · demande de <Prénom N.> ».
--    Rang dans le groupe de titres homonymes, puis emprunt d'un
--    demandeur existant du tenant de démo (rotation par modulo).
-- ------------------------------------------------------------
WITH demandeurs AS (
  SELECT array_agg(prenom || ' ' || left(nom, 1) || '.') AS noms
  FROM (
    SELECT initcap(split_part(u.first_name, ' ', 1)) AS prenom,
           initcap(split_part(u.last_name, ' ', 1))  AS nom
    FROM users u
    WHERE u.tenant_id = '00000000-0000-0000-0000-000000000001'
      AND u.deleted = false
    ORDER BY u.id
    LIMIT 40
  ) x
), base AS (
  SELECT p.id,
         split_part(p.titre, ' · ', 1) AS racine,
         ROW_NUMBER() OVER (
           PARTITION BY split_part(p.titre, ' · ', 1)
           ORDER BY p.created_at, p.id
         ) AS rn
  FROM prayers p
  WHERE p.deleted = false
    AND position(' · ' in p.titre) = 0
)
UPDATE prayers p
SET titre = b.racine || ' · demande de ' || d.noms[((b.rn - 1) % array_length(d.noms, 1)) + 1],
    updated_at = NOW()
FROM base b, demandeurs d
WHERE p.id = b.id
  AND b.rn > 1;

-- ------------------------------------------------------------
-- 2) RAPPORTS FAISEUR — 12 semaines, ~1/3 des âmes suivies.
--    Taux de présence en légère progression vers la semaine
--    courante (les graphiques racontent une histoire cohérente).
-- ------------------------------------------------------------
DO $$
DECLARE
  v_tenant  uuid := '00000000-0000-0000-0000-000000000001';
  v_lundi   date := date_trunc('week', NOW())::date;
  i         int;
  p_dim     double precision;
  p_mer     double precision;
  p_ven     double precision;
BEGIN
  FOR i IN 0..11 LOOP
    -- i = 0 → semaine courante ; taux d'autant plus bas que la semaine est ancienne.
    p_dim := 0.62 + 0.03 * (11 - i);
    p_mer := 0.44 + 0.02 * (11 - i);
    p_ven := 0.55 + 0.02 * (11 - i);
    INSERT INTO maker_reports (
      id, faiseur_id, ame_id, semaine,
      presences_par_culte, absence_raison,
      nb_sorties, nb_maintenus, soumis, date_soumission,
      created_at, updated_at, tenant_id
    )
    SELECT uuid_generate_v4(),
           s.faiseur_id,
           s.id,
           (v_lundi - (i * 7) * interval '1 day')::date,
           jsonb_build_object(
             'DIMANCHE_MATIN',  random() < p_dim,
             'MERCREDI_SOIR',   random() < p_mer,
             'VENDREDI_SOIR',   random() < p_ven
           ),
           NULL,
           0, 0, TRUE,
           (v_lundi - (i * 7) * interval '1 day') + interval '5 days' + interval '20 hours',
           (v_lundi - (i * 7) * interval '1 day') + interval '5 days' + interval '20 hours',
           NOW(),
           v_tenant
    FROM souls s
    WHERE s.deleted = FALSE
      AND s.faiseur_id IS NOT NULL
      AND s.statut IN ('ACTIF', 'EN_INTEGRATION', 'EN_VEILLE')
      AND (hashtext(s.id::text) & 2147483647) % 3 = 0   -- ~1/3 des âmes : 300-400/semaine
      AND NOT EXISTS (
        SELECT 1 FROM maker_reports m
        WHERE m.ame_id = s.id
          AND m.semaine = (v_lundi - (i * 7) * interval '1 day')::date
          AND m.deleted = FALSE
      );
  END LOOP;
END $$;

-- ------------------------------------------------------------
-- 3) AUDIT — actions humaines récentes pour le fil du dashboard.
--    Acteurs : comptes réels du tenant démo (pasteur + faiseurs).
--    Une trentaine d'entrées sur les 5 derniers jours, plus
--    récentes que le bruit de provisionnement existant.
-- ------------------------------------------------------------
DO $$
DECLARE
  v_tenant uuid := '00000000-0000-0000-0000-000000000001';
  v_acteurs uuid[];
  v_specs   text[][] := ARRAY[
    -- [verbe_action, entite_type] — le front humanise (lib/auditLabels.ts).
    ['PRAYER_CREATED',            'PRAYER'],
    ['VISIT_CREATED',             'VISIT'],
    ['EVENT_UPDATED',             'EVENT'],
    ['TASK_COMPLETED',            'TASK'],
    ['EVALUATION_CREATED',        'EVALUATION'],
    ['FAMILY_UPDATED',            'FAMILY'],
    ['USER_UPDATED',              'USER'],
    ['PRAYER_ANSWERED',           'PRAYER'],
    ['DEPARTMENT_UPDATED',        'DEPARTMENT'],
    ['EVENT_REGISTERED',         'EVENT']
  ];
  i        int;
  v_actor  uuid;
  v_entite uuid;
BEGIN
  SELECT array_agg(id) INTO v_acteurs
  FROM (
    SELECT id FROM users
    WHERE tenant_id = v_tenant AND deleted = FALSE
    ORDER BY id LIMIT 12
  ) x;

  FOR i IN 1..30 LOOP
    v_actor := v_acteurs[1 + ((i - 1) % array_length(v_acteurs, 1))];
    BEGIN
      CASE v_specs[1 + ((i - 1) % array_length(v_specs, 1))][2]
        WHEN 'PRAYER'   THEN SELECT id INTO v_entite FROM prayers   WHERE tenant_id = v_tenant AND deleted = false ORDER BY random() LIMIT 1;
        WHEN 'EVENT'    THEN SELECT id INTO v_entite FROM events    WHERE tenant_id = v_tenant ORDER BY random() LIMIT 1;
        WHEN 'VISIT'    THEN SELECT id INTO v_entite FROM visits    WHERE tenant_id = v_tenant ORDER BY random() LIMIT 1;
        WHEN 'TASK'     THEN SELECT id INTO v_entite FROM tasks     WHERE tenant_id = v_tenant ORDER BY random() LIMIT 1;
        WHEN 'EVALUATION' THEN SELECT id INTO v_entite FROM evaluations WHERE tenant_id = v_tenant ORDER BY random() LIMIT 1;
        WHEN 'FAMILY'   THEN SELECT id INTO v_entite FROM families  WHERE tenant_id = v_tenant ORDER BY random() LIMIT 1;
        WHEN 'USER'     THEN SELECT id INTO v_entite FROM users     WHERE tenant_id = v_tenant AND deleted = false AND id <> v_actor ORDER BY random() LIMIT 1;
        WHEN 'DEPARTMENT' THEN SELECT id INTO v_entite FROM departments WHERE tenant_id = v_tenant ORDER BY random() LIMIT 1;
        ELSE v_entite := NULL;
      END CASE;
    EXCEPTION WHEN OTHERS THEN v_entite := NULL;  -- table absente : on journalise sans cible
    END;

    INSERT INTO audit_logs (id, utilisateur_id, action, entite_type, entite_id,
                            adresse_ip, user_agent, created_at, tenant_id)
    SELECT uuid_generate_v4(), v_actor,
           v_specs[1 + ((i - 1) % array_length(v_specs, 1))][1],
           v_specs[1 + ((i - 1) % array_length(v_specs, 1))][2],
           v_entite,
           '203.0.113.42', 'Mozilla/5.0 (demo)',
           NOW() - (i * interval '3 hours 41 minutes'),
           v_tenant
    WHERE NOT EXISTS (
      SELECT 1 FROM audit_logs a
      WHERE a.tenant_id = v_tenant
        AND a.action = v_specs[1 + ((i - 1) % array_length(v_specs, 1))][1]
        AND a.utilisateur_id = v_actor
        AND a.created_at > NOW() - interval '6 days'
    );
  END LOOP;
END $$;

-- ------------------------------------------------------------
-- 3b) RECENCE DU FIL — les provisions de tenant faites en test ont
--     écrit leurs événements « machine » la veille : ils saturent les
--     12-20 lignes les plus récentes du dashboard, et leur acteur
--     appartient à un autre tenant (donc illisible : « Utilisateur
--     inconnu »). On les replace dans leur passé réel (10-40 j) et on
--     re-ancre l'activité humaine sur les dernières 24 h.
-- ------------------------------------------------------------
DO $$
DECLARE
  v_tenant uuid := '00000000-0000-0000-0000-000000000001';
BEGIN
  UPDATE audit_logs a
     SET created_at = NOW() - (((hashtext(a.id::text) & 2147483647) % 30 + 10) * interval '1 day')
   WHERE a.tenant_id = v_tenant
     AND a.created_at > NOW() - interval '7 days'
     AND (a.utilisateur_id IS NULL
          OR NOT EXISTS (
            SELECT 1 FROM users u
             WHERE u.id = a.utilisateur_id AND u.tenant_id = v_tenant));

  UPDATE audit_logs al
     SET created_at = NOW() - (x.rn * interval '47 minutes')
    FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY created_at) AS rn
            FROM audit_logs
           WHERE tenant_id = v_tenant AND adresse_ip = '203.0.113.42') x
   WHERE al.id = x.id;
END $$;

-- ------------------------------------------------------------
-- 4) SUIVI PASTORAL & ALERTES — une église dont les membres sont vus.
--    Dates de contact déterministes (hash de l'UUID) : relancer le
--    script redonne exactement la même répartition, et le job
--    d'absence ne recrée pas l'inondation (il ne sème que ce qui
--    est réellement sans contact depuis 48 h).
-- ------------------------------------------------------------
DO $$
DECLARE
  v_tenant  uuid := '00000000-0000-0000-0000-000000000001';
  v_pasteur uuid;
BEGIN
  SELECT id INTO v_pasteur FROM users
   WHERE tenant_id = v_tenant AND role = 'PASTEUR' AND deleted = false
   ORDER BY id LIMIT 1;

  UPDATE souls s
     SET date_dernier_contact = CASE
             WHEN (hashtext(s.id::text) & 2147483647) % 100 < 97
                 THEN NOW() - (((hashtext(s.id::text) & 2147483647) % 47) + 1) * interval '1 hour'
             WHEN (hashtext(s.id::text) & 2147483647) % 100 < 99
                 THEN NOW() - (3 + ((hashtext(s.id::text) & 2147483647) % 10)) * interval '1 day'
             ELSE NOW() - (24 + ((hashtext(s.id::text) & 2147483647) % 20)) * interval '1 day'
         END,
         updated_at = NOW()
   WHERE s.tenant_id = v_tenant
     AND s.deleted = FALSE;

  -- Alertes d'absence devenues sans objet : même invariance que le job —
  -- une âme vue récemment ne doit plus porter d'alerte ACTIVE. Résolution
  -- étalée sur les heures/jours qui suivent le déclenchement.
  UPDATE alerts a
     SET statut = 'RESOLUE',
         resolu_par = v_pasteur,
         date_resolution = LEAST(NOW(), a.date_declenchement
                                 + (((hashtext(a.id::text) & 2147483647) % 96) + 2) * interval '1 hour'),
         updated_at = NOW()
    FROM souls s
   WHERE a.ame_id = s.id
     AND a.statut = 'ACTIVE'
     AND a.type_alerte IN ('ABSENCE_48H', 'ABSENCE_3_SEMAINES')
     AND s.date_dernier_contact > CASE
             WHEN a.type_alerte = 'ABSENCE_3_SEMAINES' THEN NOW() - interval '3 weeks'
             ELSE NOW() - interval '48 hours'
         END;

  -- Ménage : les alertes rattachées à une âme absente ou supprimée ne sont
  -- plus traitementables par le pasteur (le job les ignore), on les solde.
  UPDATE alerts a
     SET statut = 'RESOLUE',
         resolu_par = v_pasteur,
         date_resolution = LEAST(NOW(), a.date_declenchement + interval '1 day'),
         updated_at = NOW()
   WHERE a.statut = 'ACTIVE'
     AND a.type_alerte IN ('ABSENCE_48H', 'ABSENCE_3_SEMAINES')
     AND NOT EXISTS (
       SELECT 1 FROM souls s WHERE s.id = a.ame_id AND s.deleted = FALSE);

  -- Les âmes réellement en retard doivent voir leur alerte TOUT DE SUITE : le
  -- job ne passe que toutes les 6 h. Libellés et messages identiques à ceux du
  -- job (ScheduledJobs.checkAbsences48h) — rien n'est inventé.
  INSERT INTO alerts (id, ame_id, faiseur_id, type_alerte, message,
                      date_declenchement, statut, created_at, updated_at, tenant_id)
  SELECT uuid_generate_v4(), s.id, s.faiseur_id, 'ABSENCE_48H',
         'Âme sans contact depuis plus de 48h : '
           || trim(coalesce(s.prenom, '') || ' ' || coalesce(s.nom, '')),
         s.date_dernier_contact + interval '48 hours', 'ACTIVE', NOW(), NOW(), v_tenant
    FROM souls s
   WHERE s.tenant_id = v_tenant
     AND s.deleted = FALSE
     AND s.date_dernier_contact IS NOT NULL
     AND s.date_dernier_contact < NOW() - interval '48 hours'
     AND NOT EXISTS (
       SELECT 1 FROM alerts a
        WHERE a.ame_id = s.id AND a.type_alerte = 'ABSENCE_48H' AND a.statut = 'ACTIVE');

  INSERT INTO alerts (id, ame_id, faiseur_id, type_alerte, message,
                      date_declenchement, statut, created_at, updated_at, tenant_id)
  SELECT uuid_generate_v4(), s.id, s.faiseur_id, 'ABSENCE_3_SEMAINES',
         '⚠️ Âme en décrochage potentiel : '
           || trim(coalesce(s.prenom, '') || ' ' || coalesce(s.nom, ''))
           || ' - sans suivi depuis plus de 3 semaines',
         s.date_dernier_contact + interval '3 weeks', 'ACTIVE', NOW(), NOW(), v_tenant
    FROM souls s
   WHERE s.tenant_id = v_tenant
     AND s.deleted = FALSE
     AND s.date_dernier_contact IS NOT NULL
     AND s.date_dernier_contact < NOW() - interval '3 weeks'
     AND NOT EXISTS (
       SELECT 1 FROM alerts a
        WHERE a.ame_id = s.id AND a.type_alerte = 'ABSENCE_3_SEMAINES' AND a.statut = 'ACTIVE');
END $$;

-- ------------------------------------------------------------
-- 5) RESIDUS DE CAMPAGNES DE TEST — les suites bout-en-bout ont laissé
--    dans le tenant de demonstration des membres nommes « Rachel E2E »,
--    « Jean Test », « Invitée E2E », joignables en @e2e.test ou
--    @test.com. Ils peuplent l'annuaire, la liste des responsables et le
--    fil d'activité du pasteur. On rebaptise les LIBELLES de ces lignes
--    (pas de suppression, pas de changement d'identifiant : les liens
--    souls.faiseur_id / departments.responsable_id / audit_logs restent
--    valides et les noms du fil redeviennent lisibles).
--    L'adresse est reconstruite avec le debut de l'UUID : unique, et le
--    domaine de test disparait. Idempotent : la cible ne re-matche plus.
-- ------------------------------------------------------------
DO $$
DECLARE
  v_tenant  uuid := '00000000-0000-0000-0000-000000000001';
  v_prenoms text[] := ARRAY['Esther', 'Didier', 'Awa', 'Samuel', 'Miriam',
                            'Élie', 'Josué', 'Deborah', 'Yao', 'Anne'];
  v_noms    text[] := ARRAY['Kouamé', 'Bamba', 'Nadon', 'Zadi', 'Assamoi',
                            'Konan', 'Gnamien', 'Aka', 'Diomandé', 'Traoré'];
  v_prenom  text;
  v_nom     text;
  v_i       int := 0;
  r         record;
BEGIN
  FOR r IN
    SELECT u.id, u.email
      FROM users u
     WHERE u.tenant_id = v_tenant
       AND ( upper(btrim(coalesce(u.first_name, ''))) IN ('E2E', 'TEST')
          OR upper(btrim(coalesce(u.last_name,  ''))) IN ('E2E', 'TEST')
          OR lower(u.email) ~ '(e2e[.@]|@test\.com$)' )
     ORDER BY u.created_at
  LOOP
    v_i      := v_i + 1;
    v_prenom := v_prenoms[((v_i - 1) % array_length(v_prenoms, 1)) + 1];
    v_nom    := v_noms[((v_i - 1) % array_length(v_noms, 1)) + 1];

    UPDATE users
       SET first_name = v_prenom,
           last_name  = v_nom,
           email      = translate(lower(v_prenom || '.' || v_nom || '.'
                                        || left(r.id::text, 8)
                                        || '@eglise-demo.org'),
                                  'éèêëàâäîïôöûüç', 'eeeaaaaioouuuc'),
           updated_at = NOW()
     WHERE id = r.id;
  END LOOP;
END $$;

-- ------------------------------------------------------------
-- 5b) ESPACES ET FAMILLES DE TEST — mêmes campagnes : deux départements
--     « Departement Jeunesse 161702 / 085229 » (sans accent et suffixes
--     de run) et deux « Famille E2E … ». Le département rejoint deux
--     filières qui manquent au catalogue ; la famille prend le nom
--     complet de son chef (convention de l'annuaire : « Famille
--     <Prénom> <Nom> », ce qui la distingue de l'homonyme déjà semée).
-- ------------------------------------------------------------
DO $$
DECLARE
  v_tenant uuid := '00000000-0000-0000-0000-000000000001';
  v_filieres text[] := ARRAY['Département Enseignement', 'Département Protocole'];
  v_i int := 0;
  r   record;
BEGIN
  FOR r IN
    SELECT d.id FROM departments d
     WHERE d.tenant_id = v_tenant
       AND ( d.nom ~* 'e2e' OR d.nom ~ '[0-9]{4,}' OR d.nom ~* '(^|[^a-z])test([^a-z]|$)' )
     ORDER BY d.created_at
  LOOP
    v_i := v_i + 1;
    UPDATE departments
       SET nom = v_filieres[((v_i - 1) % array_length(v_filieres, 1)) + 1],
           updated_at = NOW()
     WHERE id = r.id;
  END LOOP;

  UPDATE families f
     SET nom = 'Famille ' || initcap(lower(u.first_name)) || ' '
                        || initcap(lower(u.last_name)),
         updated_at = NOW()
    FROM users u
   WHERE f.chef_famille_id = u.id
     AND f.tenant_id = v_tenant
     AND ( f.nom ~* 'e2e' OR f.nom ~ '[0-9]{4,}' OR f.nom ~* '(^|[^a-z])test([^a-z]|$)' );
END $$;

-- ------------------------------------------------------------
-- 5c) CULTES DE TEST — huit « E2E Culte mv6o2zow » en brouillon, sans
--     lieu ni description, tous datés du jour même : l'agenda du pasteur
--     sentait le bac à sable. Ils deviennent huit rencontres nommées,
--     lieu et description compris, réparties sur le mois à venir (le
--     statut BROUILLON est conservé : rien n'est publié à sa place).
-- ------------------------------------------------------------
DO $$
DECLARE
  v_tenant uuid := '00000000-0000-0000-0000-000000000001';
  v_titres text[] := ARRAY['Culte de louange', 'Veillée de prière',
                            'Culte de célébration', 'Étude biblique',
                            'Culte des familles', 'Rencontre de jeunesse',
                            'Culte d''action de grâce', 'Soirée de louange'];
  v_lieux  text[] := ARRAY['Temple principal', 'Salle Évangile',
                            'Salle polyvalente', 'Annexe Nord'];
  v_i     int := 0;
  v_debut timestamptz;
  r       record;
BEGIN
  FOR r IN
    SELECT e.id, e.start_at, e.end_at FROM event e
     WHERE e.tenant_id = v_tenant
       AND e.deleted_at IS NULL
       AND ( e.title ~* 'e2e' OR e.title ~* '(^|[^a-z])test([^a-z]|$)'
             OR e.title ~ '[a-z]{8,}[0-9]{4,}' )
     ORDER BY e.start_at, e.id
  LOOP
    v_i := v_i + 1;
    v_debut := date_trunc('day', NOW()) + ((v_i * 3) * interval '1 day')
               + interval '9 hours' + ((v_i % 3) * interval '90 minutes');
    UPDATE event
       SET title = v_titres[((v_i - 1) % array_length(v_titres, 1)) + 1],
           description = coalesce(nullif(btrim(description), ''),
                                  'Culte ouvert à toute l''église : louange, enseignement et prière.'),
           lieu = coalesce(nullif(btrim(lieu), ''),
                           v_lieux[((v_i - 1) % array_length(v_lieux, 1)) + 1]),
           start_at = v_debut,
           end_at   = CASE WHEN r.end_at IS NOT NULL
                          THEN v_debut + coalesce(r.end_at - r.start_at, interval '2 hours')
                          ELSE NULL END,
           updated_at = NOW()
     WHERE id = r.id;
  END LOOP;
END $$;

-- ------------------------------------------------------------
-- 6) SÉRIES D'ÉVÉNEMENTS DOUBLONNÉES — le volumineux a inséré quatre
--     séries de vingt lignes portant exactement le même titre à la même
--     seconde. Le pasteur verrait vingt « Réunion hebdomadaire »
--     superposés. On garde TOUTES les lignes (elles portent leurs
--     inscriptions et leurs pointages) mais chacune devient une
--     occurrence réelle : les réunions suivent leur rythme hebdomadaire,
--     les célébrations étalent leur programme en sessions distinctes.
--     Date de fin recalée sur la durée d'origine ; les lieux vides sont
--     comblés. Idempotent : les séries renommées ne se regroupent plus.
-- ------------------------------------------------------------
DO $$
DECLARE
  v_tenant uuid := '00000000-0000-0000-0000-000000000001';
  v_lieux  text[] := ARRAY['Temple principal', 'Salle Évangile', 'Salle polyvalente',
                            'Centre de conférences', 'Parc Central', 'Annexe Nord'];
  g        record;
  v_pas    interval;
  v_rythme boolean;
BEGIN
  FOR g IN
    SELECT e.title,
           count(*)        AS n,
           min(e.start_at) AS ancre
      FROM event e
     WHERE e.tenant_id = v_tenant
       AND e.deleted_at IS NULL
     GROUP BY e.title
    HAVING count(*) > 1
     ORDER BY count(*) DESC, e.title
  LOOP
    -- Une reunion qui se répète est un CYCLE (une par semaine) ; une
    -- retraite, une convention ou une journee de prière qui se répète
    -- est un PROGRAMME (plusieurs sessions le meme jour).
    v_rythme := g.title ~* 'hebdomadaire|mensuelle|dominical';
    v_pas := CASE
               WHEN v_rythme                        THEN interval '7 days'
               WHEN g.title ~* 'jeûne|jeune|prière'  THEN interval '90 minutes'
               ELSE interval '4 hours'
             END;

    UPDATE event e
       SET start_at = g.ancre + ((x.rn - 1) * v_pas)
                        + (((x.rn - 1) % 3) * interval '25 minutes'),
           end_at   = CASE WHEN e.end_at IS NOT NULL
                          THEN g.ancre + ((x.rn - 1) * v_pas)
                               + (((x.rn - 1) % 3) * interval '25 minutes')
                               + (e.end_at - e.start_at)
                          ELSE NULL END,
           title    = CASE WHEN v_rythme THEN e.title
                          ELSE g.title || ' — session ' || x.rn END,
           lieu     = CASE WHEN btrim(coalesce(e.lieu, '')) = ''
                          THEN v_lieux[((x.rn - 1) % array_length(v_lieux, 1)) + 1]
                          ELSE e.lieu END,
           updated_at = NOW()
      FROM (SELECT ev.id, ROW_NUMBER() OVER (ORDER BY ev.id) AS rn
              FROM event ev
             WHERE ev.tenant_id = v_tenant
               AND ev.deleted_at IS NULL
               AND ev.title = g.title) x
     WHERE e.id = x.id
       AND x.rn > 1;
  END LOOP;

  -- Dernier détail : une rencontre sans lieu force le pasteur à appeler
  -- pour savoir où aller. Tous les événements du tenant de démonstration
  -- sont désormais adressés.
  UPDATE event
     SET lieu = 'Temple principal',
         updated_at = NOW()
   WHERE tenant_id = v_tenant
     AND deleted_at IS NULL
     AND btrim(coalesce(lieu, '')) = '';
END $$;

COMMIT;
