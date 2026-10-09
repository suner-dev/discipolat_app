<!--
Modèle « proposer une décision ». Une ADR de ce dépôt se tient en quatre sections et meurt rarement
d'un excès de longueur : Contexte mesuré · Décision · Conséquences · Options rejetées. Le registre
`docs/architecture/ADR-REGISTRE.md` est le seul endroit qui dit quelles décisions sont en vigueur ;
une ADR non listée dedans n'existe pas.
-->

## Titre de la décision

(une phrase au présent : « le solde est dérivé d'un journal, jamais déclaré par le client »)

## Contexte — mesuré, pas ressenti

| Élément | État vérifié | Comment revérifier |
|---|---|---|
| (brique concernée) | (ce que le code fait aujourd'hui, chiffres à l'appui) | `commande exacte` |

Ce qui rend la situation intenable maintenant (et pas dans six mois) :

## Options envisagées

| Option | Coût | Réversibilité | Pourquoi elle n'est pas retenue |
|---|---|---|---|
| | | | |

## Décision

(numéroter les règles — une règle = une phrase impérative vérifiable)

1. …

## Conséquences

- Sur le code : (quels modules, quels contrats, quelles migrations)
- Sur la valorisation : (quel levier : NRR, take-rate, fossé de données, décote risque)
- Sur ce qui devient **impossible** : (toute décision qui n'interdit rien n'est une préférence)

## Lots exécutables, rouge puis verte

| Lot | Contenu | Rouge (preuve que ça casse aujourd'hui) | Verte (preuve que c'est tenu) |
|---|---|---|---|
| | | | |

## Après coup

- [ ] `docs/architecture/ADR-0xx-….md` écrit et accepté
- [ ] Ligne ajoutée dans `docs/architecture/ADR-REGISTRE.md`
- [ ] `VALORISATION-PLATEFORME.md` §2 mis à jour si une plaque change d'ordre
- [ ] Défauts découverts enregistrés dans `KNOWN_ISSUES.md` (pas seulement racontés ici)
