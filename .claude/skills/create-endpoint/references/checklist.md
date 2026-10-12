# Checklist finale

À confirmer point par point avant de rendre compte.

**Architecture**
- [ ] Le contrôleur n'appelle que le service (aucun repository, aucune entité).
- [ ] La logique métier et les droits sont dans le service.
- [ ] Les réponses sont des DTO, pas des entités.
- [ ] `check-layers.sh` termine sans erreur.

**REST**
- [ ] URL au pluriel sous `/api`, verbe HTTP adapté.
- [ ] 201 pour une création, 204 pour une suppression sans corps.
- [ ] Erreurs en `ResponseStatusException` avec un message en français.

**Sécurité**
- [ ] La colocation est déduite de `CurrentUserService`.
- [ ] Une ressource d'une autre colocation renvoie 404.
- [ ] Les droits d'auteur / de participant sont contrôlés.
- [ ] Les entrées sont validées (annotations sur le DTO + contrôle côté service).

**Base de données**
- [ ] `01_schema.sql` mis à jour.
- [ ] `migrations.sql` mis à jour avec une instruction rejouable.
- [ ] L'entité correspond au schéma (le back démarre).

**Tests**
- [ ] Test unitaire pour la logique pure.
- [ ] Test d'intégration : nominal, 400, 401, 404 autre colocation.
- [ ] `./mvnw test` passe (base Docker démarrée).

**Hygiène**
- [ ] Aucun secret dans le code ou les tests.
- [ ] Aucun fichier `.env` ou `.idea/` ajouté à Git.
