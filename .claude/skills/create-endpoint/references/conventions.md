# Conventions du back Coloc

## Architecture 3-tiers

| Couche | Dossier | Rôle | Interdit |
|---|---|---|---|
| Présentation | `controller/` | reçoit la requête HTTP, valide (`@Valid`), appelle le service, renvoie un DTO | logique métier, accès aux repositories, `@Transactional`, entités JPA |
| Métier | `service/` | règles métier, droits, transactions | dépendre d'un contrôleur |
| Données | `repository/`, `entity/` | accès à PostgreSQL via Spring Data JPA | règles métier |

Les DTO (`dto/`) sont des `record` ; les entités ne sortent jamais du service.

## URLs et verbes REST

- Ressources au **pluriel**, préfixe `/api` : `/api/expenses`, `/api/pots`, `/api/tasks`.
- `GET /api/x` liste, `GET /api/x/{id}` détail, `POST /api/x` création, `PUT /api/x/{id}` remplacement complet, `PATCH /api/x/{id}` modification partielle, `DELETE /api/x/{id}` suppression.
- Sous-ressource : `/api/pots/{id}/payments`, `/api/pots/{id}/payments/{paymentId}`.
- Une action qui n'est pas un simple CRUD reste exceptionnelle et se fait en `PATCH`/`PUT` sur la ressource (ex. `PATCH /api/tasks/{id}/complete`, `PUT /api/pots/{id}/purchased`).

## Codes HTTP

| Situation | Code |
|---|---|
| Lecture ou modification réussie | 200 |
| Création (`POST`) | 201 (`@ResponseStatus(HttpStatus.CREATED)`) |
| Suppression réussie sans corps | 204 (`@ResponseStatus(HttpStatus.NO_CONTENT)`) |
| Données invalides, règle métier violée | 400 |
| Pas de jeton ou jeton invalide | 401 (géré par la configuration de sécurité) |
| Connecté mais pas le droit (pas l'auteur, pas participant) | 403 |
| Ressource inexistante **ou d'une autre colocation** | 404 |

## Sécurité par colocation

- La colocation et l'utilisateur viennent de `CurrentUserService` (`getCurrentSharedHouse()`, `getCurrentRoommateId()`), jamais du corps de la requête.
- Toute lecture ou écriture filtre par colocation (`findByIdAndSharedHouseId(...)`). Une ressource d'une autre colocation renvoie 404, pas 403, pour ne pas révéler son existence.
- Les droits d'édition (auteur seul, participant seul) sont vérifiés dans le service.
- Les montants sont revalidés côté serveur (`BigDecimal`, 2 décimales, plafond).

## Erreurs

Les services lancent `ResponseStatusException(HttpStatus.X, "message en français")`. `GlobalExceptionHandler` les transforme en JSON `{timestamp, status, error, message, path}`. Les erreurs de validation des DTO deviennent un 400 avec la liste `champ : message`.

## Base de données

- `database/01_schema.sql` est la source de vérité du schéma (base neuve, exécutée par Docker au premier lancement).
- `back/src/main/resources/db/migrations.sql` met à niveau une base existante au démarrage : uniquement des instructions **rejouables** (`IF NOT EXISTS`).
- Hibernate est en `ddl-auto=validate` : l'entité doit refléter exactement le schéma.

## Style

- Injection par constructeur avec Lombok `@RequiredArgsConstructor`.
- Messages d'erreur destinés à l'utilisateur en français.
- `@Transactional` sur les méthodes de service qui écrivent, `@Transactional(readOnly = true)` sur celles qui lisent.
