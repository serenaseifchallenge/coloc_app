-- Migrations rejouables (idempotentes) : elles s'exécutent à chaque démarrage du back,
-- avant la validation du schéma par Hibernate. Elles mettent à niveau une base créée
-- avec une ancienne version de database/01_schema.sql, sans commande à lancer à la main.
-- Une base neuve (docker compose up) contient déjà ces colonnes via 01_schema.sql.

-- Cagnottes : l'achat a-t-il été effectué ?
ALTER TABLE IF EXISTS pot ADD COLUMN IF NOT EXISTS purchased BOOLEAN NOT NULL DEFAULT FALSE;
