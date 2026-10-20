SOURCE CODE LICENSE AGREEMENT - DISCIPOLAT

This Agreement effective [Date] between:

[Your Organization Name or "Church Owner"] ("Licensor"), and
ART4US, represented by its CEO Rainer Stefane Ngansop ("Licensee").

1. DEFINITION: "Discipolat Source Code"
   All human-readable source code comprising Discipolat Church OS including:
   - Backend: Java/Spring Boot 3.4.7, Hibernate, JPA entities, services, controllers
   - Frontend: React 19, TypeScript, TailwindCSS, Vitest, PWA assets
   - Mobile: Flutter 3.35+, Riverpod, Drift, GoRouter
   - Configuration: .env files, Docker compose, Nginx configs
   - Database: Flyway migrations (204+), PostgreSQL schemas, tenant isolation
   - Excludes: Generated artifacts, compiled binaries, .env with real secrets

2. GRANT
   Non-exclusive, non-transferable, revocable license to use Discipolat Source Code 
   exclusively for: "maintenance et corrections de sécurité sur instance de production ART4US",
   pendant la période de licence de 1 an à compter de la date d'effet.

3. RESTRICTIONS (critical for Discipolat)
   - ❌ Jamais redistribuer à tiers (autres églises, concurrents ART4US)
   - ❌ Jamais extraire clés secrètes (JWT, AES, DB passwords)
   - ❌ Jamais modifier architecture multi-tenant (isolation par schéma PostgreSQL)
   - ❌ Jamais partager .env ou clés de chiffrement
   - ❌ Jamais révéler données d'autres tenants (feature core du produit)
   - ❌ Pas de reverse engineering, décompilation ni désassemblage
   - ❌ Pas création d'œuvres dérivées modifiant le modèle multi-tenant
   - ⬅️ TOUT code modifié doit être soumis sous forme de patches/Diff only

4. ACCESS CONTROL
   - Licensee s'engage à tout travail sur instances Docker gérées par Licensor (ART4US)
   - Pas d'accès SSH/local au code source en dehors des sessions sécurisées
   - Builds de production uniquement fournis par Licensor
   - Territoire de travail: Afrique (au moment de la rédaction)

5. INTELLECTUAL PROPERTY
   - Licensor conserve tout titre et propriété intellectuelle
   - Ce licence ne transfère aucun droit de propriété
   - Discipolat inclut éléments tiers (licences Voir LICENSE.txt)
   - Tous droits réservés - usage strictement cadre licensed (1 an)

6. CONFIDENTIALITY
   Identical to NDA terms above + 3-year post-termination survival protection.

7. TERM
   1 year from Effective Date, renouvelable par accord écrit mutuel.
   Résiliation possible en cas de défaut de conformité aux termes du présent Contrat.

8. LIABILITY
   As standard software license, pas de garantie pour données églises.
   Responsabilité limitée aux dommages directs, exclus les dommages indirects.

_______________________     _______________________
Licensor                  ART4US / Rainer Stefane Ngansop (CEO)