# 📖 Guide Utilisateur — Discipolat

> Plateforme de gestion d'église multi-tenant. Ce guide vous accompagne dans la prise en main de toutes les fonctionnalités.

---

## 🚀 Démarrage rapide

### 1. Connexion
1. Ouvrez l'application (web ou mobile)
2. Saisissez votre **email** et **mot de passe**
3. Si c'est votre première connexion, suivez le **wizard d'onboarding** (voir ci-dessous)

### 2. Premier accès — Wizard d'onboarding
Le wizard vous guide étape par étape :
- **PASTEUR/ADMIN** : Identité → Structure → Rôles → Import → Modules
- **CHEF DE FAMILLE** : Famille → Disciples → Événements → Dashboard
- **FAISEUR** : Disciples → Contacts → Défis → Prières
- **MEMBRE** : Profil → Famille → Événements → Sondages

### 3. Navigation
- **Web** : Menu latéral gauche avec tous les modules activés
- **Mobile** : Navigation par onglets (Accueil, Église, Communique, Plus)

---

## 👥 Gestion des âmes (disciples)

### Créer une âme
1. Menu **Âmes** → **Nouvelle âme**
2. Remplir les informations (nom, prénom, téléphone, email)
3. Assigner une **famille spirituelle** et un **faiseur**
4. Cliquer **Enregistrer**

### Dossier 360°
Chaque âme a un dossier complet :
- **Profil** : informations personnelles
- **Timeline** : historique des interactions
- **Score spirituel** : présence, prière, engagement, service
- **Alertes** : signaux de décrochage
- **Notes pastorales** : privées, visibles par le pasteur seul

### Recherche et filtres
Utilisez la barre de recherche ou filtrez par :
- Statut (Actif, Inactif, Nouveau converti)
- Famille spirituelle
- État spirituel (Tiède, Actif, Brûlant)
- Faiseur assigné

---

## 📅 Événements

### Créer un événement
1. Menu **Événements** → **Nouvel événement**
2. Remplir : titre, description, date, lieu, type
3. Définir la **limite de places** (optionnel)
4. Publier ou sauvegarder en brouillon

### RSVP (Confirmations)
- Les membres voient les événements à venir dans leur **calendrier personnel**
- Ils peuvent répondre : **J'y vais** / **Intéressé** / **Annulé**
- Les organisateurs voient la liste des inscrits en temps réel

### Rappels WhatsApp
Les rappels sont envoyés automatiquement **24h avant** l'événement aux membres inscrits.

---

## 📊 Rapports pastoraux

### Soumettre un rapport
1. Menu **Rapports** → **Nouveau rapport**
2. Choisir le type (hebdomadaire, mensuel, spécial)
3. Remplir les sections : présences, activités, alertes, prières
4. Soumettre → le pasteur reçoit une notification

### Rapports exécutifs (IA)
L'assistant IA génère automatiquement un **rapport exécutif** mensuel avec :
- Tendances de présence
- Nouveaux convertis
- Alertes prioritaires
- Recommandations d'actions

---

## 🤖 Assistant IA Pastoral

### Chat IA
1. Menu **IA** → **Chat**
2. Posez une question : *"Quelles sont les familles en décrochement ?"*
3. L'IA analyse les données et propose des actions

### Commandes vocales (PasteurBot)
Sur mobile, utilisez le micro pour poser des questions vocales :
- 🗣️ *"Montre-moi les familles en décrochement"*
- 🗣️ *"Combien de nouveaux convertis ce mois ?"*
- 🗣️ *"Génère un rapport de la semaine"*

---

## 💬 Messagerie

### Messagerie 1:1
- Cliquez sur un membre → **Envoyer un message**
- Les messages sont en temps réel (WebSocket)

### Broadcast ciblé
- Menu **Communications** → **Nouveau broadcast**
- Choisissez la cible : tous, par département, par famille, par segment
- Les membres reçoivent le message dans l'app + email + WhatsApp (si configuré)

---

## 💰 Finances

### Enregistrer une transaction
1. Menu **Finances** → **Nouvelle transaction**
2. Type : Recette ou Dépense
3. Montant, catégorie, description, date
4. La devise de votre église est appliquée automatiquement

### Rapport financier multi-devise
- Consultez les stats dans votre devise locale
- Convertissez instantanément en USD, EUR, ou toute autre devise
- Les rapports s'affichent avec le symbole de votre devise

### Budget
1. Menu **Finances** → **Budgets**
2. Définir les budgets par catégorie
3. Suivre la consommation en temps réel

---

## 📱 Mobile

### Fonctionnalités hors-ligne
- Consultez les âmes, rapports et événements **sans connexion**
- Les modifications sont synchronisées automatiquement
- Un indicateur vous informe du statut de synchronisation

### Géolocalisation
- Pointage par géolocalisation lors des événements
- Carte interactive des secteurs de l'église

---

## 🔒 Sécurité et confidentialité

### Authentification
- **JWT RS256** : tokens sécurisés à durée limitée (15 min)
- **2FA** : authentification à deux facteurs (TOTP)
- **Multi-rôles** : changez de rôle en un clic

### Données personnelles (RGPD)
- **Export de données** : demandez l'export de vos données via le profil
- **Suppression** : demandez la suppression de votre compte
- **Consentements** : gérez vos préférences de données

---

## ⚙️ Configuration (Admin)

### Paramètres de l'église
- Menu **Administration** → **Paramètres**
- Nom, logo, devise, contact, fuseau horaire

### Modules
- Activez/désactivez les modules selon vos besoins
- Chaque module est indépendant et n'affecte pas les autres

### Connecteurs tiers
- **Zapier/Make** : transférez les événements métier vers vos outils
- **Google Calendar** : synchronisez les événements
- **QuickBooks/Xero** : exportez les transactions financières

### WhatsApp Business
1. Menu **Administration** → **WhatsApp**
2. Configurer le **Phone Number ID** et le **Token d'accès**
3. Tester la connexion
4. Activer → les annonces et rappels sont envoyés par WhatsApp

---

## 🆘 Aide et support

### Aide intégrée
- **Tooltips** : survolez les éléments pour des explications
- **Wizard** : le guide de première connexion vous accompagne
- **Commande vocale** : dites *"Aide"* au PasteurBot

### Contact support
- Email : support@discipolat.com
- Documentation API : `/swagger-ui.html`
- FAQ : https://discipolat.com/faq

---

*Guide v2.0 — Août 2026. Pour la dernière version, consultez https://docs.discipolat.com*
