# Coloc

Coloc est une application web qui aide les colocataires à organiser leur vie commune. Elle réunit au même endroit ce qui, d'habitude, se perd dans les discussions de groupe : le partage des tâches ménagères, la liste de courses, les dépenses communes, le calendrier de la maison, les notes et les sondages. Un système de points récompense ceux qui s'investissent dans les tâches et alimente un classement mensuel, pour que l'organisation reste équitable et un peu ludique.

Le projet a été réalisé par Emma Lehec, Serena Seif, Mila Ambrosi et Sonja Dia dans le cadre du cours de Java de l'EPF, en 2026.

## Contexte et objectifs

Vivre en colocation, c'est partager un logement mais aussi une multitude de petites responsabilités : qui sort les poubelles, qui a acheté le dernier paquet de pâtes, combien chacun doit à l'autre après les courses du week-end, quand est prévue la prochaine soirée. Ces informations circulent d'ordinaire dans des messageries, des tableaux improvisés ou de simples souvenirs, et finissent par créer des malentendus. Notre objectif était de centraliser tout cela dans une seule application, simple à utiliser au quotidien, où chaque colocataire retrouve l'état réel de la maison et sait ce qu'il lui reste à faire.

Nous avons aussi voulu que l'application soit équitable et motivante. Les tâches rapportent des points, les points alimentent un classement mensuel, et les comptes de la colocation sont calculés automatiquement pour que personne n'ait à tenir une comptabilité à la main.

## Fonctionnalités

L'application propose les fonctionnalités suivantes, accessibles depuis le menu principal une fois connecté à une colocation :

- **Compte et colocation** : chacun crée son compte, puis crée une colocation ou en rejoint une existante. Il peut modifier son profil, son adresse e-mail et son mot de passe, quitter la colocation ou supprimer son compte.
- **Accueil** : une page de synthèse qui résume en un coup d'œil l'activité de la maison (points, courses à faire, dépenses du mois, prochains événements, dernières notes).
- **Tâches ménagères** : les tâches sont créées, assignées à un colocataire, dotées d'une échéance et d'un nombre de points, et peuvent se répéter automatiquement. Les tâches terminées sont nettoyées au bout d'un certain temps.
- **Points et classement** : accomplir une tâche rapporte des points. Un classement mensuel et le podium du mois précédent récompensent les plus investis, avec une clôture automatique à chaque fin de mois.
- **Courses** : une liste partagée où chacun ajoute, modifie ou supprime des articles ; les articles achetés disparaissent automatiquement.
- **Dépenses partagées** : les dépenses communes sont réparties entre les participants, les soldes de chacun sont calculés et l'application propose le plus petit nombre de remboursements pour équilibrer les comptes. Elle gère aussi des cagnottes collectives.
- **Calendrier** : un agenda commun, en vue mensuelle ou hebdomadaire, pour les événements de la maison.
- **Notes et sondages** : des notes partagées et des sondages à choix multiples pour décider ensemble.

## Présentation technique

L'application est composée de trois parties indépendantes qui communiquent entre elles. Le back-end est une API REST écrite en Java 17 avec Spring Boot 4.1.1, qui utilise Spring Data JPA et Hibernate pour accéder aux données, Spring Security avec des jetons JWT pour authentifier les utilisateurs, Lombok pour alléger le code et Maven pour la construction. Le front-end est une application Angular 20 écrite en TypeScript, organisée en composants autonomes. Les données sont stockées dans une base PostgreSQL 16 qui tourne dans un conteneur Docker, ce qui évite à chacun d'installer et de configurer la base sur sa machine.

Chaque colocataire se connecte avec son compte et ne voit que les informations de sa propre colocation. Cette colocation est toujours déduite de l'utilisateur connecté côté serveur, jamais fournie par le navigateur, et une ressource appartenant à une autre colocation est traitée comme inexistante.

## Prérequis

Avant de lancer le projet, assurez-vous d'avoir installé les outils suivants :

- **JDK 17 ou plus récent**, nécessaire pour compiler et exécuter le back-end.
- **Node.js (version LTS) et npm**, ainsi qu'Angular CLI (`npm install -g @angular/cli`), pour le front-end.
- **Docker Desktop**, qui fournit la base de données PostgreSQL sans installation supplémentaire.
- **Git**, pour récupérer le projet et suivre les modifications.

Le port 5432 doit également être libre. Si PostgreSQL est déjà installé directement sur votre machine, arrêtez ce service avant de commencer, sinon l'application se connectera à cette base locale plutôt qu'à celle de Docker et échouera au démarrage.

## Installation et lancement

Commencez par créer votre fichier de configuration. À la racine du projet, copiez le modèle fourni puis remplissez-le avec vos propres valeurs.

```bash
cp .env.example .env
```

Le fichier `.env` contient le nom de la base, l'utilisateur et le mot de passe de la base de données ainsi que la clé secrète utilisée pour signer les jetons JWT. Cette clé doit être une longue chaîne aléatoire d'au moins 32 caractères. Ce fichier est personnel et ne doit jamais être envoyé sur Git.

```
DATABASE_NAME=coloc
DATABASE_USER=coloc_user
DATABASE_PASSWORD=un_mot_de_passe
JWT_SECRET=une_longue_chaine_aleatoire_d_au_moins_32_caracteres
```

Démarrez ensuite la base de données depuis la racine du projet. Au tout premier lancement, Docker exécute automatiquement les scripts du dossier `database/` : il crée les tables, puis insère quelques données de démonstration.

```bash
docker compose up -d
```

Lancez ensuite le serveur depuis le dossier `back/`. Il écoute sur http://localhost:8080.

```bash
./mvnw spring-boot:run        # sous Windows : .\mvnw.cmd spring-boot:run
```

Enfin, lancez l'interface depuis le dossier `front/`. L'installation des dépendances n'est nécessaire qu'à la première utilisation.

```bash
npm install
ng serve
```

L'application est alors accessible sur http://localhost:4200, où vous pouvez créer un compte et rejoindre ou créer une colocation. Si vous voulez repartir d'une base entièrement vide, par exemple après une modification du schéma, utilisez `docker compose down -v` puis `docker compose up -d`. Attention, cette commande supprime toutes les données existantes.

## Premiers pas dans l'application

Au premier lancement, la page de connexion s'affiche. Créez un compte avec votre prénom, votre nom, votre adresse e-mail et un mot de passe, puis connectez-vous. Vous arrivez alors sur un écran qui vous propose soit de créer une nouvelle colocation, soit d'en rejoindre une existante. Une fois dans une colocation, vous accédez à l'accueil et au menu principal qui donne accès à toutes les fonctionnalités. Pour tester le fonctionnement à plusieurs, créez un deuxième compte (dans une fenêtre de navigation privée par exemple) et faites-le rejoindre la même colocation : vous pourrez alors vous assigner des tâches, partager des dépenses et voter aux sondages comme le feraient de vrais colocataires.

## Sécurité

L'accès à l'API repose sur des jetons JWT : à la connexion, le serveur remet un jeton signé que le front-end joint ensuite à chaque requête, et seules les routes d'authentification sont accessibles sans jeton. Les mots de passe sont stockés sous forme chiffrée, jamais en clair. Les règles d'accès sont vérifiées côté serveur et pas seulement dans l'interface : un utilisateur ne peut consulter ou modifier que les données de sa colocation, et certaines actions sont réservées à leur auteur. Les montants et les champs saisis sont aussi revalidés par le serveur. Enfin, les secrets (accès à la base, clé de signature) sont placés dans le fichier `.env`, exclu de Git.

## Tests

Les tests du back-end se lancent depuis le dossier `back/` avec `./mvnw test`, et ceux du front-end depuis le dossier `front/` avec `ng test`.

## Structure du projet

Le dépôt est organisé en quatre blocs. Le dossier `back/` contient l'API Spring Boot, découpée en couches (contrôleurs, services, accès aux données, entités, objets de transfert, sécurité et tâches planifiées). Le dossier `front/` contient l'application Angular, avec un dossier `core` pour l'authentification et les échanges avec l'API, un dossier `shared` pour les éléments réutilisables et un dossier par grande fonctionnalité. Le dossier `database/` regroupe le script de création du schéma (`01_schema.sql`) et celui des données de démonstration (`02_data.sql`). Le fichier `docker-compose.yml` décrit le conteneur PostgreSQL. Le schéma de la base est entièrement défini dans le script SQL : Hibernate se contente de vérifier qu'il correspond bien aux entités Java.

## Utilisation de l'intelligence artificielle

L'équipe a utilisé des assistants d'IA générative comme outils d'aide au développement, dans un cadre que nous avons voulu clair : l'IA assiste, mais c'est l'équipe qui conçoit, vérifie et reste responsable de ce qui est livré.

### Pour quoi nous l'avons utilisée

Nous avons fait appel à l'IA pour plusieurs types de tâches, toujours avec un objectif précis :

- **Génération de code** à partir de spécifications écrites par l'équipe : entités, services, contrôleurs, composants Angular et algorithmes de calcul. Nous décrivions le besoin et la structure attendue, puis l'IA produisait une première version.
- **Compréhension et correction d'erreurs** : lecture des messages d'erreur, recherche de la cause et proposition de correctifs, par exemple pour un conflit de port avec une base PostgreSQL installée en local.
- **Tests unitaires** sur les calculs les plus sensibles, afin de vérifier leur exactitude sur des cas limites que nous aurions pu oublier.
- **Organisation du travail Git** : découpage des modifications en commits cohérents, rédaction de messages de commit clairs et de descriptions de pull requests.
- **Documentation** : rédaction et mise en forme de ce README.

### Ce que nous avons gardé entre nos mains

L'IA n'a jamais défini à notre place la direction du projet. L'architecture, le choix des technologies, le modèle de données et les maquettes ont été décidés par l'équipe avant de solliciter un outil. Les règles de fonctionnement de l'application, comme les droits de chacun ou la manière de calculer certaines valeurs, ont elles aussi été fixées par nous. Quand du code était généré, nous l'avons adapté à l'existant pour qu'il respecte la même organisation en couches et les mêmes conventions de sécurité que le reste du projet.

### Comment nous avons vérifié le code

Un code généré peut sembler correct sans l'être, nous avons donc multiplié les vérifications. Les calculs sensibles sont couverts par des tests unitaires. Chaque parcours a été testé à la main dans l'application complète, avec la base, le back et le front en fonctionnement. Le travail est ensuite relu par un autre membre de l'équipe lors de la pull request, avant d'être fusionné dans la branche principale. Nous avons enfin contrôlé les points de sécurité : isolation des données entre colocations, droits de modification et validation des données côté serveur.

### Nos règles d'usage

Pour garder la maîtrise de notre projet, nous nous sommes fixé trois règles :

- **Aucun secret transmis** : le fichier `.env`, les mots de passe et la clé JWT ne sont jamais partagés avec un outil d'IA.
- **Un code compris par son auteur** : chaque membre doit être capable d'expliquer le code de sa partie, qu'il l'ait écrit seul ou avec de l'aide, et de le modifier sans l'outil.
- **Un esprit critique** : quand l'IA proposait quelque chose de faux ou d'inadapté, nous l'avons corrigé ou écarté plutôt que de l'accepter tel quel.

### Limites et enseignements

L'IA accélère nettement l'écriture du code répétitif et aide à débloquer des situations, mais elle ne connaît pas notre projet par elle-même : elle a besoin d'un cadre précis et de consignes claires. Ses résultats doivent toujours être testés et relus. Dans notre expérience, c'est ce travail de vérification qui garantit la qualité du résultat, et non l'outil seul.

## Organisation du travail avec Git

Pour travailler à quatre sur le même code sans nous gêner, nous avons suivi quelques conventions simples :

- **Une branche par fonctionnalité ou correction**, nommée `feat/...` ou `fix/...`, créée à partir d'un `main` à jour.
- **Une pull request relue** par un autre membre de l'équipe avant toute fusion dans `main`, ce qui permet de repérer les erreurs et de partager la connaissance du code.
- **Des messages de commit explicites** au format `type(portée) : description`, par exemple `feat(back) : ajout d'un article` ou `fix(front) : correction de l'affichage`.
- **Des fichiers personnels exclus du suivi** : le dossier `.idea/` et le fichier `.env` ne doivent jamais être envoyés sur Git.

Cette organisation nous a surtout servi à limiter les conflits de merge, qui apparaissent surtout dans quelques fichiers que tout le monde modifie (les routes du front et les scripts SQL). La section suivante explique comment les résoudre.

## Problèmes fréquents

Voici les problèmes que nous avons le plus souvent rencontrés pendant le développement, avec leur cause probable et la façon de les résoudre.

| Problème | Cause probable | Solution |
|---|---|---|
| Le back ne démarre pas : `Schema validation: missing table [...]` | Le back est connecté à un autre PostgreSQL que celui de Docker, généralement un service installé sur la machine qui occupe déjà le port 5432. | Arrêter le service PostgreSQL local, puis recréer la base avec `docker compose down -v` suivi de `docker compose up -d`. |
| « Une erreur est survenue » à l'inscription ou à la connexion | Le back n'est pas lancé, ou le front n'est pas ouvert sur l'adresse autorisée par le serveur. | Vérifier que le back tourne sur le port 8080 et ouvrir le front sur exactement `http://localhost:4200`. |
| Le back s'arrête au démarrage avec une variable introuvable | Le fichier `.env` est absent ou incomplet. | Le recopier depuis `.env.example` et renseigner les quatre valeurs. |
| **Conflit de merge** : `CONFLICT (content): Merge conflict in ...` | Deux personnes ont modifié les mêmes lignes d'un fichier, souvent `app.routes.ts`, `01_schema.sql` ou `02_data.sql`. | Mettre sa branche à jour avec `git fetch` puis `git merge origin/main`. Ouvrir chaque fichier en conflit, garder les deux versions quand ce sont des ajouts, supprimer les marqueurs `<<<<<<<`, `=======`, `>>>>>>>`, puis `git add <fichier>` et `git commit`. Pour tout annuler : `git merge --abort`. |
| **Erreur de commit** : `Please tell me who you are`, ou fichier ajouté par erreur | L'identité Git n'est pas configurée, ou un fichier non voulu (`.env`, `.idea/`) a été ajouté à l'index. | Configurer son identité avec `git config --global user.name "Prénom Nom"` et `git config --global user.email "adresse@mail"`. Retirer un fichier de l'index avec `git restore --staged <fichier>`. Corriger le dernier message, s'il n'est pas poussé, avec `git commit --amend`. Vérifier avec `git status` avant chaque commit. |
| **Erreur de push** : `rejected ... non-fast-forward` ou `failed to push some refs` | Le dépôt distant contient des commits absents en local, ou l'historique de la branche a été réécrit. | Récupérer les commits avec `git pull --rebase origin <branche>`, résoudre les conflits éventuels, puis `git push`. Si Git indique `no upstream branch` : `git push -u origin <branche>`. Après un `--amend` ou un rebase volontaire : `git push --force-with-lease`, jamais sur `main`. |
