-- ============================================================================
-- seed-volumineux.sql — Jeu de données MASSIF 100 % fictif pour les tests
-- ============================================================================
-- Remplit la base de DEV avec un volume réaliste :
--   • ~1000 âmes/disciples      • ~56 familles          • 16 départements
--   • ~96 équipes               • ~96 postes            • ~160 tâches
--   • ~1000 affectations        • ~64 événements        • ~1400 pointages
--   • présences hebdo, rapports, checklists, matériel, activité
--
-- Usage :  PGPASSWORD=discipolat_secret psql -h localhost -p 5433 \
--            -U discipolat -d discipolat -f scripts/seed-volumineux.sql
--
-- Ré-exécutable : tous les inserts utilisent des IDs déterministes
-- (md5(...)::uuid) + ON CONFLICT DO NOTHING → relancer ne duplique rien.
-- N'efface AUCUNE donnée existante.
-- ============================================================================

\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- 1. USERS — 12 responsables + 16 chefs de famille + 30 faiseurs
--    (mot de passe : password123 — hash BCrypt identique aux comptes démo)
-- ============================================================================
DO $$
DECLARE
    pwd TEXT := '$2a$12$A9Wsk6xn4ZDxK8eqHfXg.e9Yktv3msuB/IPuP9nfGNaNxjONSkfSW';
    i INT;
    n INT;
    v_id UUID;
    v_nom TEXT;
    v_prenom TEXT;
    admin_id UUID;
BEGIN
    SELECT id INTO admin_id FROM users WHERE email = 'admin@discipolat.com' LIMIT 1;
    IF admin_id IS NULL THEN SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' LIMIT 1; END IF;

    -- 12 responsables (responsable10 → responsable21)
    FOR i IN 10..21 LOOP
        v_id := md5('seed:resp:' || i)::uuid;
        v_nom := (ARRAY['Kouassi','Traoré','Bamba','Ouattara','Touré','Sanogo','Coulibaly','Koné','Diabaté','N''Guessan','Yao','Assi'])[i - 9];
        INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, est_chef_de_famille, statut, created_at, updated_at, created_by)
        VALUES (v_id, 'responsable' || i || '@discipolat.com', pwd, 'Responsable', v_nom, '0700000' || lpad(i::text, 3, '0'),
                'RESPONSABLE', false, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_id)
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO user_roles (user_id, role) VALUES (v_id, 'RESPONSABLE'), (v_id, 'FAISEUR') ON CONFLICT DO NOTHING;
    END LOOP;

    -- 16 chefs de famille (chef10 → chef25)
    FOR i IN 10..25 LOOP
        v_id := md5('seed:chef:' || i)::uuid;
        v_prenom := (ARRAY['Esther','David','Mariam','Salif','Nadège','Moïse','Grâce','Emmanuel','Prisca','Josué','Léa','Samuel','Awa','Dieudonné','Chantal','Yannick'])[i - 9];
        v_nom := (ARRAY['Koné','Bamba','Touré','Coulibaly','Sanogo','Yao','Kouadio','Assi','Diabaté','Traoré','Ouattara','N''Guessan','Koffi','Aka','Bédié','Dembélé'])[i - 9];
        INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, est_chef_de_famille, statut, created_at, updated_at, created_by)
        VALUES (v_id, 'chef' || i || '@discipolat.com', pwd, v_prenom, v_nom, '0700001' || lpad(i::text, 3, '0'),
                'FAISEUR', true, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_id)
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO user_roles (user_id, role) VALUES (v_id, 'FAISEUR'), (v_id, 'CHEF_DE_FAMILLE') ON CONFLICT DO NOTHING;
    END LOOP;

    -- 30 faiseurs (faiseur10 → faiseur39)
    FOR i IN 10..39 LOOP
        v_id := md5('seed:faiseur:' || i)::uuid;
        v_prenom := (ARRAY['Adama','Aya','Boris','Clarisse','Frédéric','Ibrahim','Judith','Kader','Landry','Mireille','Olivier','Rachel','Serge','Thierry','Victor','Aminata','Ange','Armand','Assetou','Bintou','Christian','Clément','Élise','Fatoumata','Grégoire','Issa','Julien','Karim','Laeticia','Lionel'])[i - 9];
        v_nom := (ARRAY['Cissé','Sangaré','Keïta','Doumbia','Sylla','Fofana','Camara','Diallo','Barry','Bah','Soumah','Kaba','Konaté','Sidibé','Diomandé','Cissoko','Mendy','Ndiaye','Sarr','Kouassi','Traoré','Bamba','Ouattara','Touré','Sanogo','Coulibaly','Koné','Diabaté','Yao','Assi'])[i - 9];
        INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, est_chef_de_famille, statut, created_at, updated_at, created_by)
        VALUES (v_id, 'faiseur' || i || '@discipolat.com', pwd, v_prenom, v_nom, '0700002' || lpad(i::text, 3, '0'),
                'FAISEUR', false, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_id)
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO user_roles (user_id, role) VALUES (v_id, 'FAISEUR') ON CONFLICT DO NOTHING;
    END LOOP;
END $$;

-- ============================================================================
-- 2. DÉPARTEMENTS — 12 nouveaux (16 au total avec les 4 existants)
-- ============================================================================
DO $$
DECLARE
    i INT;
    v_id UUID;
    v_resp UUID;
    admin_id UUID;
    depts TEXT[][] := ARRAY[
        ['Intercession', 'Prière, jeûne et intercession pour l''église et la ville'],
        ['Accueil & Ushers', 'Réception des visiteurs et placement durant les cultes'],
        ['Ministère des Enfants', 'École du dimanche et encadrement des enfants'],
        ['Logistique', 'Montage, transport et logistique des événements'],
        ['Média & Réseaux sociaux', 'Communication digitale, photos et réseaux sociaux'],
        ['Sport & Loisirs', 'Activités sportives et récréatives des jeunes'],
        ['Entrepreneuriat & Commerce', 'Encadrement des entrepreneurs et commerçants'],
        ['Santé & Bien-être', 'Visites aux malades et sensibilisation santé'],
        ['Sécurité', 'Sécurité des personnes et des biens pendant les cultes'],
        ['Maintenance & Bâtiment', 'Entretien du temple et des bâtiments'],
        ['Évangélisation & Mission', 'Évangélisation de rue et missions'],
        ['Administration', 'Secrétariat, gestion et archives du département']
    ];
BEGIN
    SELECT id INTO admin_id FROM users WHERE email = 'admin@discipolat.com' LIMIT 1;
    IF admin_id IS NULL THEN SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' LIMIT 1; END IF;

    FOR i IN 1..12 LOOP
        v_id := md5('seed:dept:' || i)::uuid;
        v_resp := md5('seed:resp:' || (9 + ((i - 1) % 12) + 1))::uuid;
        INSERT INTO departments (id, nom, description, responsable_id, statut, created_at, updated_at, created_by)
        VALUES (v_id, depts[i][1], depts[i][2], v_resp, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_id)
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO user_departments (user_id, department_id, date_affectation, role_dans_dept)
        VALUES (v_resp, v_id, CURRENT_DATE - 30, 'RESPONSABLE') ON CONFLICT DO NOTHING;
    END LOOP;
END $$;

-- ============================================================================
-- 3. FAMILLES — 40 nouvelles (56 au total)
-- ============================================================================
DO $$
DECLARE
    i INT;
    v_id UUID;
    v_chef UUID;
    v_nom TEXT;
    admin_id UUID;
    noms TEXT[] := ARRAY['Kouassi','Traoré','Diabaté','Koné','Bamba','Ouattara','Touré','Sanogo','Coulibaly','Koffi','Yao','N''Guessan','Assi','Aka','Kouadio','N''Dri','Bédié','Gbagbo','Guéi','Dembélé','Cissé','Sangaré','Keïta','Doumbia','Sylla','Fofana','Camara','Diallo','Barry','Bah','Soumah','Kaba','Konaté','Sidibé','Diomandé','Cissoko','Mendy','Ndiaye','Sarr','Zadi','Kablan'];
BEGIN
    SELECT id INTO admin_id FROM users WHERE email = 'admin@discipolat.com' LIMIT 1;
    IF admin_id IS NULL THEN SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' LIMIT 1; END IF;

    FOR i IN 1..40 LOOP
        v_id := md5('seed:famille:' || i)::uuid;
        v_chef := md5('seed:chef:' || (9 + ((i - 1) % 16) + 1))::uuid;
        v_nom := noms[i];
        INSERT INTO families (id, nom, chef_famille_id, date_creation, statut, niveau_risque, zone, created_at, updated_at, created_by)
        VALUES (v_id, 'Famille ' || v_nom, v_chef, CURRENT_DATE - 120, 'ACTIVE', 'NORMAL',
                (ARRAY['Koumassi','Yopougon','Marcory','Cocody','Adjamé','Treichville','Abobo','Plateau'])[1 + ((i - 1) % 8)],
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_id)
        ON CONFLICT (id) DO NOTHING;
        -- Chaque chef gère sa première famille
        UPDATE users SET famille_geree_id = v_id
        WHERE id = v_chef AND famille_geree_id IS NULL;
    END LOOP;
END $$;

-- ============================================================================
-- 4. ÂMES / DISCIPLES — 1000 nouvelles
-- ============================================================================
DO $$
DECLARE
    i INT;
    v_id UUID;
    v_nom TEXT;
    v_prenom TEXT;
    v_email TEXT;
    v_tel TEXT;
    v_statut TEXT;
    v_type TEXT;
    v_etat TEXT;
    v_sit TEXT;
    v_faiseur UUID;
    v_famille UUID;
    admin_id UUID;
    faiseurs UUID[] := ARRAY(SELECT id FROM users WHERE role = 'FAISEUR' ORDER BY created_at);
    familles UUID[] := ARRAY(SELECT id FROM families ORDER BY created_at);
    prenoms TEXT[] := ARRAY['Adama','Aya','Aïcha','Aminata','Ange','Armand','Assetou','Awa','Bintou','Boris','Chantal','Christian','Clarisse','Clément','David','Dieudonné','Edwige','Élise','Emmanuel','Esther','Évelyne','Fatou','Fatoumata','Félicité','Frédéric','Grâce','Grégoire','Hadja','Ibrahim','Issa','Jean','Josué','Judith','Julien','Kader','Karim','Koffi','Koumba','Laeticia','Landry','Léa','Lionel','Maimouna','Mariam','Marius','Mathias','Mireille','Moïse','Nadège','Nathalie','Noël','Olivier','Prisca','Rachel','Salif','Samuel','Sarah','Serge','Sonia','Thierry','Victor','Yannick'];
    noms TEXT[] := ARRAY['Kouassi','Traoré','Diabaté','Koné','Bamba','Ouattara','Touré','Sanogo','Coulibaly','Koffi','Yao','N''Guessan','Assi','Aka','Kouadio','N''Dri','Bédié','Gbagbo','Guéi','Dembélé','Cissé','Sangaré','Keïta','Doumbia','Sylla','Fofana','Camara','Diallo','Barry','Bah','Soumah','Kaba','Konaté','Sidibé','Diomandé','Cissoko','Mendy','Ndiaye','Sarr','Coulibaly'];
BEGIN
    SELECT id INTO admin_id FROM users WHERE email = 'admin@discipolat.com' LIMIT 1;
    IF admin_id IS NULL THEN SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' LIMIT 1; END IF;
    IF array_length(faiseurs, 1) IS NULL THEN faiseurs := ARRAY['a0000000-0000-0000-0000-000000000006'::uuid]; END IF;
    IF array_length(familles, 1) IS NULL THEN familles := ARRAY['c0000000-0000-0000-0000-000000000001'::uuid]; END IF;

    FOR i IN 1..1000 LOOP
        v_id := md5('seed:soul:' || i)::uuid;
        v_prenom := prenoms[1 + ((i - 1) % array_length(prenoms, 1))];
        v_nom := noms[1 + (((i - 1) / 60)::int % array_length(noms, 1))];
        v_email := lower(regexp_replace(v_prenom, '[^a-zà-ÿ]', '', 'g') || '.' || regexp_replace(v_nom, '[^a-zà-ÿ]', '', 'g') || i) || '@mail.com';
        v_tel := '07' || lpad((10000000 + i)::text, 8, '0');

        -- Répartition réaliste des statuts
        v_statut := CASE
            WHEN i % 100 BETWEEN 0 AND 54 THEN 'ACTIF'
            WHEN i % 100 BETWEEN 55 AND 66 THEN 'EN_INTEGRATION'
            WHEN i % 100 BETWEEN 67 AND 76 THEN 'EN_VEILLE'
            WHEN i % 100 BETWEEN 77 AND 84 THEN 'DECROCHE'
            WHEN i % 100 BETWEEN 85 AND 94 THEN 'NOUVEAU_CONVERTI'
            ELSE 'NOUVEL_ARRIVANT'
        END;
        v_type := CASE WHEN i % 2 = 0 THEN 'NOUVEAU_CONVERTI' ELSE 'NOUVEL_ARRIVANT' END;
        v_etat := CASE
            WHEN i % 5 = 0 THEN 'NOUVEAU_CONVERTI'
            WHEN i % 5 IN (1, 2) THEN 'EN_CROISSANCE'
            WHEN i % 5 = 3 THEN 'MATURE'
            ELSE 'EN_DIFFICULTE'
        END;
        v_sit := CASE
            WHEN i % 10 BETWEEN 0 AND 3 THEN 'CELIBATAIRE'
            WHEN i % 10 BETWEEN 4 AND 7 THEN 'MARIE'
            WHEN i % 10 = 8 THEN 'DIVORCE'
            ELSE 'VEUF'
        END;
        v_faiseur := faiseurs[1 + (i % array_length(faiseurs, 1))];
        v_famille := CASE WHEN i % 5 < 3 THEN familles[1 + (i % array_length(familles, 1))] ELSE NULL END;

        INSERT INTO souls (id, nom, prenom, email, telephone, adresse, date_naissance, profession,
                           type_disciple, date_integration, date_conversion, statut, faiseur_id, famille_id,
                           situation_familiale, etat_spirituel, niveau_croissance, niveau_etude, nb_enfants, zone,
                           date_dernier_contact, created_at, updated_at, created_by)
        VALUES (v_id, v_nom, v_prenom, v_email, v_tel,
                (ARRAY['Abidjan','Yamoussoukro','Bouaké','San Pedro','Korhogo','Daloa','Man','Gagnoa'])[1 + ((i - 1) % 8)] || ', quartier ' || (1 + (i % 20)),
                CURRENT_DATE - (18 + (i % 45)) * INTERVAL '1 year',
                (ARRAY['Étudiant','Commerçant','Enseignant','Agent de santé','Artisan','Informaticien','Chauffeur','Sans emploi'])[1 + ((i - 1) % 8)],
                v_type,
                CURRENT_DATE - (i % 400) * INTERVAL '1 day',
                CURRENT_DATE - (i % 400 + 60) * INTERVAL '1 day',
                v_statut, v_faiseur, v_famille,
                v_sit, v_etat, 1 + (i % 5),
                (ARRAY['PRIMAIRE','SECONDAIRE','BACCALAUREAT','LICENCE','MASTER'])[1 + ((i - 1) % 5)],
                CASE WHEN i % 3 = 0 THEN 1 + (i % 4) ELSE 0 END,
                (ARRAY['Koumassi','Yopougon','Marcory','Cocody','Adjamé','Treichville','Abobo','Plateau'])[1 + ((i - 1) % 8)],
                CURRENT_DATE - (i % 20) * INTERVAL '1 day',
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_id)
        ON CONFLICT (id) DO NOTHING;
    END LOOP;
END $$;

-- ============================================================================
-- 5. APPARTENANCE AUX DÉPARTEMENTS (~1000 liens + 25 % second département)
-- ============================================================================
DO $$
DECLARE
    i INT;
    v_soul UUID;
    v_dept1 UUID;
    v_dept2 UUID;
    depts UUID[] := ARRAY(SELECT id FROM departments ORDER BY created_at);
    admin_id UUID;
BEGIN
    SELECT id INTO admin_id FROM users WHERE email = 'admin@discipolat.com' LIMIT 1;
    IF admin_id IS NULL THEN SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' LIMIT 1; END IF;

    FOR i IN 1..1000 LOOP
        v_soul := md5('seed:soul:' || i)::uuid;
        IF NOT EXISTS (SELECT 1 FROM souls WHERE id = v_soul) THEN CONTINUE; END IF;
        v_dept1 := depts[1 + ((i - 1) % array_length(depts, 1))];
        INSERT INTO soul_departments (soul_id, department_id, date_affectation, actif, created_by, origine)
        VALUES (v_soul, v_dept1, CURRENT_TIMESTAMP - (i % 300) * INTERVAL '1 day', true, admin_id, 'MANUEL')
        ON CONFLICT (soul_id, department_id) DO NOTHING;
        INSERT INTO member_departments (id, soul_id, department_id, created_at)
        VALUES (md5('seed:md:' || i || ':1')::uuid, v_soul, v_dept1, CURRENT_TIMESTAMP)
        ON CONFLICT (id) DO NOTHING;

        IF i % 4 = 0 THEN
            v_dept2 := depts[1 + ((i + 7) % array_length(depts, 1))];
            IF v_dept2 <> v_dept1 THEN
                INSERT INTO soul_departments (soul_id, department_id, date_affectation, actif, created_by, origine)
                VALUES (v_soul, v_dept2, CURRENT_TIMESTAMP - (i % 150) * INTERVAL '1 day', true, admin_id, 'MANUEL')
                ON CONFLICT (soul_id, department_id) DO NOTHING;
                INSERT INTO member_departments (id, soul_id, department_id, created_at)
                VALUES (md5('seed:md:' || i || ':2')::uuid, v_soul, v_dept2, CURRENT_TIMESTAMP)
                ON CONFLICT (id) DO NOTHING;
            END IF;
        END IF;
    END LOOP;
END $$;

-- ============================================================================
-- 6. ÉQUIPES — 4 permanentes par département (+ temporaires liées aux événements)
-- ============================================================================
DO $$
DECLARE
    d RECORD;
    i INT;
    v_id UUID;
    v_chef UUID;
    admin_id UUID;
    team_names TEXT[] := ARRAY['Équipe Alpha','Équipe Béta','Équipe Omega','Sous-département 1','Sous-département 2','Noyau de prière','Brigade d''accueil','Cellule de suivi'];
BEGIN
    SELECT id INTO admin_id FROM users WHERE email = 'admin@discipolat.com' LIMIT 1;
    IF admin_id IS NULL THEN SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' LIMIT 1; END IF;

    FOR d IN SELECT id, responsable_id, nom FROM departments ORDER BY created_at LOOP
        FOR i IN 1..4 LOOP
            v_id := md5('seed:team:' || d.id || ':' || i)::uuid;
            v_chef := (SELECT soul_id FROM soul_departments WHERE department_id = d.id AND actif ORDER BY date_affectation LIMIT 1 OFFSET (i - 1));
            INSERT INTO department_teams (id, department_id, parent_id, date_debut, created_at, updated_at,
                                          chef_id, nom, type, description, statut, objectif)
            VALUES (v_id, d.id, NULL, CURRENT_DATE - 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, v_chef,
                    d.nom || ' — ' || team_names[i],
                    CASE WHEN i <= 2 THEN 'EQUIPE_PERMANENTE' ELSE 'SOUS_DEPARTEMENT' END,
                    'Équipe du département ' || d.nom,
                    'ACTIVE', 'Servir dans la vision du département')
            ON CONFLICT (id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- ============================================================================
-- 7. POSTES — 6 par département
-- ============================================================================
DO $$
DECLARE
    d RECORD;
    i INT;
    v_id UUID;
    admin_id UUID;
    positions TEXT[] := ARRAY['Chef de département adjoint','Chef d''équipe','Adjoint','Membre actif','Trésorier','Responsable matériel','Secrétaire'];
BEGIN
    SELECT id INTO admin_id FROM users WHERE email = 'admin@discipolat.com' LIMIT 1;
    IF admin_id IS NULL THEN SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' LIMIT 1; END IF;

    FOR d IN SELECT id FROM departments ORDER BY created_at LOOP
        FOR i IN 1..6 LOOP
            v_id := md5('seed:position:' || d.id || ':' || i)::uuid;
            INSERT INTO department_positions (id, department_id, created_at, updated_at, competences_requises, statut, nom, description)
            VALUES (v_id, d.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Leadership, organisation, esprit d''équipe',
                    'ACTIVE', positions[i], 'Poste de ' || lower(positions[i]) || ' du département')
            ON CONFLICT (id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- ============================================================================
-- 8. TÂCHES — ~10 par département
-- ============================================================================
DO $$
DECLARE
    d RECORD;
    i INT;
    v_id UUID;
    v_team UUID;
    v_soul UUID;
    v_statut TEXT;
    v_echeance DATE;
    admin_id UUID;
    task_titles TEXT[] := ARRAY['Préparer la salle pour le culte','Faire le bilan des membres','Organiser la réunion d''équipe','Mettre à jour la liste des présences','Contacter les membres en veille','Préparer le rapport hebdomadaire','Planifier la formation du mois','Ranger le matériel','Relancer les retardataires','Préparer l''événement du mois'];
BEGIN
    SELECT id INTO admin_id FROM users WHERE email = 'admin@discipolat.com' LIMIT 1;
    IF admin_id IS NULL THEN SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' LIMIT 1; END IF;

    FOR d IN SELECT id, nom FROM departments ORDER BY created_at LOOP
        v_team := (SELECT id FROM department_teams WHERE department_id = d.id ORDER BY created_at LIMIT 1);
        FOR i IN 1..10 LOOP
            v_id := md5('seed:task:' || d.id || ':' || i)::uuid;
            v_soul := (SELECT soul_id FROM soul_departments WHERE department_id = d.id AND actif ORDER BY date_affectation LIMIT 1 OFFSET (i % 10));
            v_statut := CASE
                WHEN i % 20 IN (0,1,2,3,4,5,6) THEN 'A_FAIRE'
                WHEN i % 20 IN (7,8,9,10,11) THEN 'EN_COURS'
                WHEN i % 20 IN (12,13,14,15) THEN 'TERMINEE'
                WHEN i % 20 IN (16,17) THEN 'BLOQUEE'
                WHEN i % 20 = 18 THEN 'VALIDEE'
                ELSE 'ANNULEE'
            END;
            v_echeance := CASE
                WHEN i % 3 = 0 THEN CURRENT_DATE - (5 + (i % 10))   -- en retard
                WHEN i % 3 = 1 THEN CURRENT_DATE + (3 + (i % 12))
                ELSE CURRENT_DATE + (20 + (i % 30))
            END;
            INSERT INTO department_tasks (id, department_id, team_id, avancement, created_by, created_at, updated_at,
                                          assigned_to, date_debut, echeance, titre, description, statut, priorite)
            VALUES (v_id, d.id, v_team, CASE WHEN v_statut = 'TERMINEE' OR v_statut = 'VALIDEE' THEN 100 WHEN v_statut = 'EN_COURS' THEN 30 + (i % 50) ELSE 0 END,
                    admin_id, CURRENT_TIMESTAMP - (i * 3) * INTERVAL '1 day', CURRENT_TIMESTAMP,
                    v_soul, CURRENT_DATE - 7, v_echeance,
                    task_titles[i], 'Tâche de ' || d.nom || ' générée pour les tests',
                    v_statut, (ARRAY['BASSE','MOYENNE','HAUTE'])[1 + (i % 3)])
            ON CONFLICT (id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- ============================================================================
-- 9. AFFECTATIONS — chaque membre actif est affecté à une équipe + un poste
-- ============================================================================
DO $$
DECLARE
    i INT;
    v_soul UUID;
    v_dept UUID;
    v_team UUID;
    v_pos UUID;
    v_role TEXT;
    v_id UUID;
    admin_id UUID;
BEGIN
    SELECT id INTO admin_id FROM users WHERE email = 'admin@discipolat.com' LIMIT 1;
    IF admin_id IS NULL THEN SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' LIMIT 1; END IF;

    FOR i IN 1..1000 LOOP
        v_soul := md5('seed:soul:' || i)::uuid;
        IF NOT EXISTS (SELECT 1 FROM souls WHERE id = v_soul AND statut <> 'DECROCHE') THEN CONTINUE; END IF;
        v_dept := (SELECT department_id FROM soul_departments WHERE soul_id = v_soul AND actif ORDER BY date_affectation LIMIT 1);
        IF v_dept IS NULL THEN CONTINUE; END IF;
        v_team := (SELECT id FROM department_teams WHERE department_id = v_dept ORDER BY created_at LIMIT 1 OFFSET (i % 4));
        v_pos := (SELECT id FROM department_positions WHERE department_id = v_dept ORDER BY created_at LIMIT 1 OFFSET (i % 6));
        v_role := CASE WHEN i % 20 = 0 THEN 'CHEF' WHEN i % 20 IN (1,2) THEN 'ADJOINT' ELSE 'MEMBRE' END;
        v_id := md5('seed:assign:' || i)::uuid;
        INSERT INTO department_assignments (id, department_id, team_id, position_id, member_id, date_debut, date_fin, actif, created_by, created_at, role)
        VALUES (v_id, v_dept, v_team, v_pos, v_soul, CURRENT_DATE - (i % 300), NULL, true, admin_id, CURRENT_TIMESTAMP, v_role)
        ON CONFLICT (id) DO NOTHING;
    END LOOP;
END $$;

-- ============================================================================
-- 10. ÉVÉNEMENTS — 4 par département (passés + à venir)
-- ============================================================================
DO $$
DECLARE
    d RECORD;
    i INT;
    v_id UUID;
    v_type TEXT;
    v_statut TEXT;
    v_titre TEXT;
    v_date TIMESTAMP;
    types TEXT[] := ARRAY['CULTE','VEILLEE','FORMATION','REUNION','CONFERENCE','RETRAITE','SORTIE','EVANGELISATION','PRIERE','ETUDE_BIBLIQUE'];
    titres TEXT[] := ARRAY['Culte de dimanche','Veillée de prière','Formation des équipes','Réunion hebdomadaire','Conférence ouverte','Retraite spirituelle','Sortie d''évangélisation','Journée de jeûne et prière','Étude biblique du département','Convention annuelle'];
BEGIN
    FOR d IN SELECT id, responsable_id, nom FROM departments ORDER BY created_at LOOP
        FOR i IN 1..4 LOOP
            v_id := md5('seed:event:' || d.id || ':' || i)::uuid;
            v_type := types[1 + ((i * 3 + 2) % array_length(types, 1))];
            v_titre := titres[1 + ((i * 2 + 1) % array_length(titres, 1))];
            IF i % 4 IN (0, 3) THEN
                -- événement passé
                v_statut := CASE WHEN i = 3 THEN 'ANNULE' ELSE 'TERMINE' END;
                v_date := CURRENT_TIMESTAMP - (20 + i * 15) * INTERVAL '1 day';
            ELSE
                -- événement à venir
                v_statut := 'PLANIFIE';
                v_date := CURRENT_TIMESTAMP + (5 + i * 12) * INTERVAL '1 day';
            END IF;
            INSERT INTO events (id, organisateur_id, department_id, type_evenement, titre, description, lieu,
                                date_debut, date_fin, nb_inscrits, statut, created_at, updated_at)
            VALUES (v_id, d.responsable_id, d.id, v_type, v_titre, 'Événement ' || v_type || ' du département ' || d.nom,
                    (ARRAY['Temple principal','Salle polyvalente','Champ de prière','Centre de conférences'])[1 + (i % 4)],
                    v_date, v_date + INTERVAL '3 hours', 0, v_statut, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- Équipes temporaires liées aux événements (1 pour les événements passés TERMINE)
DO $$
DECLARE
    e RECORD;
    v_id UUID;
    v_soul UUID;
BEGIN
    FOR e IN SELECT id, department_id, titre FROM events WHERE statut = 'TERMINE' ORDER BY created_at LIMIT 16 LOOP
        v_id := md5('seed:team:event:' || e.id)::uuid;
        v_soul := (SELECT soul_id FROM soul_departments WHERE department_id = e.department_id AND actif ORDER BY date_affectation LIMIT 1);
        INSERT INTO department_teams (id, department_id, parent_id, date_debut, date_fin, created_at, updated_at,
                                      event_id, chef_id, nom, type, description, statut)
        VALUES (v_id, e.department_id, NULL, CURRENT_DATE - 30, CURRENT_DATE - 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                e.id, v_soul, 'Équipe ' || e.titre, 'EQUIPE_TEMPORAIRE', 'Équipe temporaire liée à l''événement', 'ACTIVE')
        ON CONFLICT (id) DO NOTHING;
    END LOOP;
END $$;

-- ============================================================================
-- 11. PRÉSENCE AUX ÉVÉNEMENTS (~1400 pointages) + INSCRIPTIONS
-- ============================================================================
DO $$
DECLARE
    e RECORD;
    s RECORD;
    i INT;
    v_id UUID;
    v_user UUID;
    v_present BOOLEAN;
    n_faiseurs INT;
BEGIN
    SELECT count(*) INTO n_faiseurs FROM users WHERE role = 'FAISEUR';
    IF n_faiseurs < 8 THEN n_faiseurs := 8; END IF;
    FOR e IN SELECT id, department_id FROM events WHERE statut IN ('TERMINE', 'EN_COURS') LOOP
        i := 0;
        FOR s IN SELECT soul_id FROM soul_departments WHERE department_id = e.department_id AND actif ORDER BY date_affectation LOOP
            i := i + 1;
            IF i > 50 THEN EXIT; END IF;
            v_present := (i % 5 <> 0);  -- ~80 % présents
            v_id := md5('seed:att:' || e.id || ':' || s.soul_id)::uuid;
            INSERT INTO department_event_attendance (id, department_id, event_id, soul_id, present, marked_by, created_at, updated_at)
            VALUES (v_id, e.department_id, e.id, s.soul_id, v_present,
                    (SELECT responsable_id FROM departments WHERE id = e.department_id),
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO NOTHING;
        END LOOP;
    END LOOP;

    -- Inscriptions aux événements à venir
    FOR e IN SELECT id, department_id, organisateur_id FROM events WHERE statut = 'PLANIFIE' LIMIT 24 LOOP
        FOR i IN 1..8 LOOP
            v_id := md5('seed:reg:' || e.id || ':' || i)::uuid;
            SELECT id INTO v_user FROM users WHERE role = 'FAISEUR'
            ORDER BY id LIMIT 1 OFFSET (((abs(hashtext(e.id::text)) % n_faiseurs) + i - 1) % n_faiseurs);
            INSERT INTO event_registrations (id, event_id, utilisateur_id, date_inscription, statut_inscription, created_at)
            VALUES (v_id, e.id, v_user,
                    CURRENT_TIMESTAMP - i * INTERVAL '1 day',
                    CASE WHEN i % 3 = 0 THEN 'EN_ATTENTE' ELSE 'INSCRIT' END,
                    CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- ============================================================================
-- 12. PRÉSENCES HEBDOMADAIRES (membres + faiseurs) — ~5 semaines
-- ============================================================================
DO $$
DECLARE
    w INT;
    s RECORD;
    i INT;
    v_id UUID;
    v_present BOOLEAN;
    v_user UUID;
BEGIN
    FOR w IN 1..5 LOOP
        i := 0;
        FOR s IN SELECT id AS soul_id, faiseur_id FROM souls WHERE statut IN ('ACTIF', 'EN_INTEGRATION', 'EN_VEILLE') AND id IN (SELECT soul_id FROM soul_departments) ORDER BY id LIMIT 600 OFFSET 0 LOOP
            i := i + 1;
            IF i % 3 = 0 THEN CONTINUE; END IF;  -- ~2/3 des membres ont une fiche
            v_present := (i % 4 <> 0);
            -- Pointage par âme (modèle V31) : user_id NULL, une présence par (soul_id, semaine)
            v_user := NULL;
            v_id := md5('seed:mp:' || s.soul_id || ':' || w)::uuid;
            INSERT INTO member_presences (id, user_id, soul_id, semaine, presences, created_at, updated_at, present, type_programme, sous_programme)
            VALUES (v_id, v_user, s.soul_id,
                    (CURRENT_DATE - w * 7)::date,
                    jsonb_build_object('CULTE', v_present, 'ETUDE_BIBLIQUE', i % 3 <> 0, 'PRIERE', i % 2 = 0),
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, v_present,
                    CASE WHEN i % 3 = 0 THEN 'ETUDE_BIBLIQUE' ELSE 'CULTE' END,
                    CASE WHEN i % 3 = 0 THEN 'Étude du livre des Actes' ELSE 'Culte dominical' END)
            ON CONFLICT (id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- ============================================================================
-- 13. RAPPORTS DE DÉPARTEMENT — 3 par département
-- ============================================================================
DO $$
DECLARE
    d RECORD;
    i INT;
    v_id UUID;
    admin_id UUID;
BEGIN
    SELECT id INTO admin_id FROM users WHERE email = 'admin@discipolat.com' LIMIT 1;
    IF admin_id IS NULL THEN SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' LIMIT 1; END IF;

    FOR d IN SELECT id, responsable_id, nom FROM departments ORDER BY created_at LOOP
        -- Rapport hebdomadaire soumis
        v_id := md5('seed:report:' || d.id || ':1')::uuid;
        INSERT INTO department_reports (id, department_id, auteur_id, periode_debut, periode_fin, created_at, updated_at, type, titre, contenu, statut)
        VALUES (v_id, d.id, d.responsable_id, CURRENT_DATE - 7, CURRENT_DATE,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'HEBDOMADAIRE',
                'Rapport hebdomadaire — ' || d.nom,
                'Cette semaine : réunion d''équipe tenue, tâches réparties, présence moyenne de 85 %. Deux membres en veille à relancer.',
                'SOUMIS')
        ON CONFLICT (id) DO NOTHING;

        -- Rapport mensuel soumis
        v_id := md5('seed:report:' || d.id || ':2')::uuid;
        INSERT INTO department_reports (id, department_id, auteur_id, periode_debut, periode_fin, created_at, updated_at, type, titre, contenu, statut)
        VALUES (v_id, d.id, d.responsable_id, CURRENT_DATE - 30, CURRENT_DATE,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'MENSUEL',
                'Rapport mensuel — ' || d.nom,
                'Bilan du mois : effectif stable, 3 nouveaux intégrés, 12 tâches terminées, 2 en retard. Prochain événement en préparation.',
                'SOUMIS')
        ON CONFLICT (id) DO NOTHING;

        -- Brouillon de synthèse
        v_id := md5('seed:report:' || d.id || ':3')::uuid;
        INSERT INTO department_reports (id, department_id, auteur_id, periode_debut, periode_fin, created_at, updated_at, type, titre, contenu, statut)
        VALUES (v_id, d.id, d.responsable_id, CURRENT_DATE - 90, CURRENT_DATE,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYNTHESE',
                'Synthèse trimestrielle — ' || d.nom,
                'Ébauche de synthèse trimestrielle à compléter.',
                'BROUILLON')
        ON CONFLICT (id) DO NOTHING;
    END LOOP;
END $$;

-- ============================================================================
-- 14. PARAMÈTRES + CHECKLISTS + MATÉRIEL + ACTIVITÉ + RAPPORTS MEMBRES
-- ============================================================================
DO $$
DECLARE
    d RECORD;
    i INT;
    v_id UUID;
    v_soul UUID;
    admin_id UUID;
BEGIN
    SELECT id INTO admin_id FROM users WHERE email = 'admin@discipolat.com' LIMIT 1;
    IF admin_id IS NULL THEN SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' LIMIT 1; END IF;

    FOR d IN SELECT id, responsable_id, nom FROM departments ORDER BY created_at LOOP
        -- Paramètres
        INSERT INTO department_settings (department_id, absence_seuil, absence_periode, inactivite_mois, tache_retard_alerte, event_rappel_jours, created_at, updated_at)
        VALUES (d.id, 3, 4, 2, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (department_id) DO NOTHING;

        v_soul := (SELECT soul_id FROM soul_departments WHERE department_id = d.id AND actif ORDER BY date_affectation LIMIT 1);

        -- Checklists (générale + équipe + événement)
        FOR i IN 1..3 LOOP
            v_id := md5('seed:checklist:' || d.id || ':' || i)::uuid;
            INSERT INTO department_checklists (id, department_id, created_by, created_at, updated_at, cible_id, titre, cible_type, statut)
            VALUES (v_id, d.id, admin_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                    CASE i WHEN 1 THEN NULL WHEN 2 THEN (SELECT id FROM department_teams WHERE department_id = d.id LIMIT 1) ELSE (SELECT id FROM events WHERE department_id = d.id LIMIT 1) END,
                    CASE i WHEN 1 THEN 'Préparation du culte' WHEN 2 THEN 'Réunion d''équipe' ELSE 'Organisation de l''événement' END,
                    CASE i WHEN 1 THEN 'GENERAL' WHEN 2 THEN 'EQUIPE' ELSE 'EVENEMENT' END,
                    CASE WHEN i = 3 THEN 'TERMINEE' ELSE 'OUVERTE' END)
            ON CONFLICT (id) DO NOTHING;
        END LOOP;

        -- Matériel (5 items)
        FOR i IN 1..5 LOOP
            v_id := md5('seed:equip:' || d.id || ':' || i)::uuid;
            INSERT INTO department_equipment (id, department_id, date_acquisition, created_by, created_at, quantite, responsable_id, affecte_a_id, nom, description, localisation, etat)
            VALUES (v_id, d.id, CURRENT_DATE - (i * 60), admin_id, CURRENT_TIMESTAMP, 1 + (i % 3),
                    d.responsable_id, CASE WHEN i % 2 = 0 THEN v_soul ELSE NULL END,
                    (ARRAY['Caméra Sony','Micro sans fil','Enceinte portative','Table et chaises','Tente de campagne','Lumière LED','Bible de référence','Mégaphone','Ordinateur portable','Clé USB'])[1 + ((i + (abs(hashtext(d.id::text)) % 5)) % 10)],
                    'Équipement du département ' || d.nom,
                    (ARRAY['Local département','Salle de stockage','Champ de prière'])[1 + (i % 3)],
                    (ARRAY['NEUF','BON','USAGE','REPARATION','HORS_SERVICE'])[1 + ((i + (abs(hashtext(d.id::text)) % 5)) % 5)])
            ON CONFLICT (id) DO NOTHING;
        END LOOP;

        -- Activité récente
        FOR i IN 1..3 LOOP
            v_id := md5('seed:activity:' || d.id || ':' || i)::uuid;
            INSERT INTO department_activity (id, department_id, actor_id, entity_id, created_at, entity_type, details, actor_nom, action)
            VALUES (v_id, d.id, d.responsable_id, d.id, CURRENT_TIMESTAMP - i * INTERVAL '2 day',
                    CASE i WHEN 1 THEN 'MEMBER' WHEN 2 THEN 'TEAM' ELSE 'EVENT' END,
                    'Création enregistrée (seed de test)',
                    (SELECT first_name || ' ' || last_name FROM users WHERE id = d.responsable_id),
                    CASE i WHEN 1 THEN 'MEMBER_CREATED' WHEN 2 THEN 'TEAM_CREATED' ELSE 'EVENT_ATTENDANCE_MARK_ALL' END)
            ON CONFLICT (id) DO NOTHING;
        END LOOP;

        -- Rapport membre (2)
        FOR i IN 1..2 LOOP
            v_id := md5('seed:memberreport:' || d.id || ':' || i)::uuid;
            INSERT INTO department_member_reports (id, department_id, member_id, auteur_id, created_at, updated_at, type, contenu)
            VALUES (v_id, d.id, v_soul, d.responsable_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                    CASE WHEN i = 1 THEN 'ASSIDUITE' ELSE 'COMPORTEMENT' END,
                    'Évaluation ' || CASE WHEN i = 1 THEN 'd''assiduité' ELSE 'de comportement' END || ' : membre régulier, présence satisfaisante aux réunions.')
            ON CONFLICT (id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- ============================================================================
-- 15. RAPPORTS DE FAMILLE — 2 semaines pour les familles
-- ============================================================================
DO $$
DECLARE
    f RECORD;
    w INT;
    v_id UUID;
    v_total INT;
    v_presents INT;
BEGIN
    FOR f IN SELECT id, chef_famille_id FROM families ORDER BY created_at LIMIT 40 LOOP
        FOR w IN 1..2 LOOP
            v_id := md5('seed:famreport:' || f.id || ':' || w)::uuid;
            v_total := 8 + (w * 2);
            v_presents := v_total - (w % 3);
            INSERT INTO family_reports (id, famille_id, chef_famille_id, semaine, stats_agregees, presence_moyenne,
                                        total_presents, total_absents, total_sorties, date_soumission, statut_validation,
                                        created_at, updated_at, commentaire_synthese)
            VALUES (v_id, f.id, f.chef_famille_id, (CURRENT_DATE - w * 7)::date,
                    jsonb_build_object('totalMembres', v_total, 'nouveaux', w, 'tachesTerminees', 3 + w),
                    CASE WHEN v_total > 0 THEN round((v_presents::numeric / v_total) * 100) ELSE 0 END,
                    v_presents, v_total - v_presents, 0,
                    CURRENT_TIMESTAMP - w * 2 * INTERVAL '1 day', 'SOUMIS',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                    'Semaine ' || w || ' : bonne participation, à encourager.')
            ON CONFLICT (id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

COMMIT;

-- ============================================================================
-- RÉCAPITULATIF
-- ============================================================================
SELECT 'users' AS entite, count(*) AS total FROM users
UNION ALL SELECT 'souls', count(*) FROM souls
UNION ALL SELECT 'families', count(*) FROM families
UNION ALL SELECT 'departments', count(*) FROM departments
UNION ALL SELECT 'soul_departments', count(*) FROM soul_departments
UNION ALL SELECT 'department_teams', count(*) FROM department_teams
UNION ALL SELECT 'department_positions', count(*) FROM department_positions
UNION ALL SELECT 'department_tasks', count(*) FROM department_tasks
UNION ALL SELECT 'department_assignments', count(*) FROM department_assignments
UNION ALL SELECT 'events', count(*) FROM events
UNION ALL SELECT 'event_attendance', count(*) FROM department_event_attendance
UNION ALL SELECT 'event_registrations', count(*) FROM event_registrations
UNION ALL SELECT 'member_presences', count(*) FROM member_presences
UNION ALL SELECT 'department_reports', count(*) FROM department_reports
UNION ALL SELECT 'family_reports', count(*) FROM family_reports
UNION ALL SELECT 'department_checklists', count(*) FROM department_checklists
UNION ALL SELECT 'department_equipment', count(*) FROM department_equipment
UNION ALL SELECT 'department_activity', count(*) FROM department_activity
ORDER BY entite;
