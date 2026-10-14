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

COMMIT;
