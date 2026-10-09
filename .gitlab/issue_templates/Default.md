<!--
Modèle par défaut des issues GitLab. Il reprend les colonnes du registre `KNOWN_ISSUES.md`
(constat · preuve · impact · propriétaire · statut · date) pour qu'une issue devienne, une fois
la correction fusionnée, UNE LIGNE DE REGISTRE — pas un fil de discussion oublié.

Règle d'intégrité du dépôt : un défaut n'est jamais reformulé pour avoir l'air plus léger,
jamais déplacé, jamais tu. « On range, on ne cache pas. »
-->

## Constat

(ce qui se passe réellement, au présent, sans adjectif : « la balance est déclarée par le client »,
pas « la gestion monétaire pourrait idéaliser l'amélioration »)

## Preuve reproductible

```bash
# la commande exacte, exécutée, avec SA sortie — pas une description de la commande
```

Où : `chemin/vers/fichier:ligne` (ou la migration, l'écran, la route)

## Impact (métier d'abord, technique ensuite)

- Client : (ce que l'utilisateur final subit — ou ne peut pas obtenir)
- Revenu : (ce que ça bloque : take-rate, NRR, enterprise, facturation à l'usage)
- Risque : (ce qui casserait en production, en audit, en due-diligence)

## Étendue mesurée

(combien de fichiers / lignes / tenants / routes sont concernés — mesuré, pas estimé ; si non
mesuré, écrire « non mesuré » et s'engager à le faire)

## Décision

- [ ] Une ADR existe ou est nécessaire (lien ; sinon ouvrir l'issue « Proposer une décision »)
- [ ] Enregistrée dans `KNOWN_ISSUES.md` avec un identifiant (A3, A4… pour les précédents)

## Statut

`OUVERT` · découvert le (date) · par (qui) · propriétaire : (qui)

---

<!-- À la fermeture : la ligne KNOWN_ISSUES passe à `CORRIGÉ` avec le SHA du commit correctif, et
la preuve du test rouge → vert est recopiée ici. Une issue fermée sans ligne de registre fermée
est une réapparition programmée. -->
