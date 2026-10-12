---
name: create-endpoint
description: Ajoute ou modifie une fonctionnalité d'API REST dans le back Spring Boot de Coloc (table, entité, repository, DTO, service, contrôleur, tests) en respectant l'architecture 3-tiers et les règles de sécurité par colocation. À utiliser quand on demande de créer, étendre ou corriger une route /api/... ou une ressource métier dans back/. Ne PAS utiliser pour du code Angular (front/), pour de la simple documentation ou pour une question de déploiement.
---

# create-endpoint

Ce skill guide l'ajout d'une fonctionnalité d'API dans le back de Coloc (Spring Boot 4, Java 17, PostgreSQL) sans casser l'architecture ni les règles de sécurité de l'équipe.

## Déclencheurs

Utiliser ce skill quand la demande ressemble à :
- « Ajoute la possibilité de commenter une dépense » ;
- « Crée une route pour supprimer / modifier / lister X » ;
- « Ajoute un champ Y à la ressource Z » (colonne, DTO, validation) ;
- « Corrige le code HTTP renvoyé par cette route ».

Ne pas l'utiliser pour : l'interface Angular, les textes du README, les questions de Git ou de Docker.

## Procédure

**Étape 1 : cadrer sans tout lire**
1. Lire `references/conventions.md` (URLs, verbes, codes HTTP, sécurité).
2. Ouvrir **une seule** fonctionnalité existante proche comme modèle (par exemple `PotController`, `PotService`, `PotRepository` pour une ressource avec droits) plutôt que parcourir tout le dépôt.
3. Si la demande est ambiguë (qui a le droit ? que se passe-t-il aux limites ?), poser **une question à la fois** avant de coder.

**Étape 2 : base de données**
1. Nouvelle table ou colonne : modifier `database/01_schema.sql` (base neuve).
2. Pour une base déjà créée, ajouter une ligne **rejouable** (`IF NOT EXISTS`) dans `back/src/main/resources/db/migrations.sql`.
3. L'entité JPA doit correspondre exactement au schéma : Hibernate est en `ddl-auto=validate` et refuse de démarrer sinon.

**Étape 3 : les trois couches** (modèles de code dans `assets/templates.md`)
1. `entity/` puis `repository/` : accès aux données uniquement, aucune règle métier.
2. `dto/` : des `record` de requête (annotations de validation) et de réponse. Ne jamais renvoyer une entité JPA.
3. `service/` : toute la logique métier et les contrôles de droits. La colocation vient de `CurrentUserService`, jamais du client.
4. `controller/` : fin et sans logique. `@Valid`, appel du service, bon code HTTP.

**Étape 4 : tests**
1. Logique pure (calculs, règles) : test unitaire dans `back/src/test/java/com/coloc/back/util/`.
2. Route : ajouter un cas à `ColocApiIntegrationTest` (ou un fichier dans `integration/`) couvrant au minimum le cas nominal (200 ou 201), la validation (400), l'absence de jeton (401) et l'accès depuis une autre colocation (404).

**Étape 5 : vérification**
1. Lancer `bash .claude/skills/create-endpoint/scripts/check-layers.sh` : il vérifie l'architecture 3-tiers.
2. Base Docker démarrée (`docker compose up -d`), lancer `cd back && ./mvnw test`.
3. Relire `references/checklist.md` et confirmer chaque point.

**Étape 6 : rendre compte**
Résumer en français : fichiers créés ou modifiés, routes ajoutées (verbe, URL, code de retour), migration éventuelle à connaître pour l'équipe.

## Gestion des erreurs et contraintes

- `Schema validation: missing column/table` au démarrage : le schéma SQL et l'entité divergent. Corriger `01_schema.sql`, `migrations.sql` ou l'entité, puis relancer.
- `check-layers.sh` signale un contrôleur qui importe un repository : déplacer la logique dans un service.
- Un test d'intégration échoue en 401 : le jeton n'est pas envoyé (voir la méthode `authed` du test).
- Port 5432 occupé ou base absente : arrêter le PostgreSQL local, relancer `docker compose up -d`.
- Ne jamais écrire de secret (`.env`, mot de passe, clé JWT) dans le code, dans un test ou dans ce skill.
- Ne pas modifier les fichiers d'un autre module (routes, schéma) au-delà du strict nécessaire : ils sont source de conflits Git.

## Exemple

Demande : « Ajoute des commentaires sur les dépenses. »

Résultat attendu :
- `database/01_schema.sql` : table `expense_comment` liée à `expense` et `roommate` ; ligne `CREATE TABLE IF NOT EXISTS` dans `migrations.sql` ;
- `entity/ExpenseComment`, `repository/ExpenseCommentRepository`, `dto/CommentRequest` et `CommentResponse`, `service/ExpenseCommentService`, route dans `ExpenseController` ;
- routes : `GET /api/expenses/{id}/comments` (200), `POST /api/expenses/{id}/comments` (201), `DELETE /api/expenses/{id}/comments/{commentId}` (204, auteur seulement) ;
- tests : 404 depuis une autre colocation, 400 pour un texte vide, 403 pour la suppression par un tiers.

## Références

- `references/conventions.md` : conventions REST et sécurité du projet
- `references/checklist.md` : liste de contrôle finale
- `assets/templates.md` : squelettes de code
- `scripts/check-layers.sh` : vérification automatique de l'architecture
