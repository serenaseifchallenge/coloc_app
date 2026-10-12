-- Migrations rejouables (idempotentes) : elles s'exécutent à chaque démarrage du back,
-- avant la validation du schéma par Hibernate. Elles mettent à niveau une base créée
-- avec une ancienne version de database/01_schema.sql, sans commande à lancer à la main.
-- Une base neuve (docker compose up) contient déjà ces colonnes via 01_schema.sql.

-- Cagnottes : l'achat a-t-il été effectué ?
ALTER TABLE IF EXISTS pot ADD COLUMN IF NOT EXISTS purchased BOOLEAN NOT NULL DEFAULT FALSE;

-- Cagnottes : personnes à qui la cagnotte est cachée (cadeau surprise).
CREATE TABLE IF NOT EXISTS pot_hidden_member (
    pot_id      BIGINT NOT NULL REFERENCES pot(id) ON DELETE CASCADE,
    roommate_id BIGINT NOT NULL REFERENCES roommate(id) ON DELETE CASCADE,
    PRIMARY KEY (pot_id, roommate_id)
);

-- Notes : date de fin de vote des sondages (ajoutée après la première version du schéma).
ALTER TABLE IF EXISTS note ADD COLUMN IF NOT EXISTS poll_ends_at TIMESTAMP NULL;

-- Colocataires : photo de profil ou couleur d'avatar.
ALTER TABLE IF EXISTS roommate ADD COLUMN IF NOT EXISTS avatar_color VARCHAR(7);
ALTER TABLE IF EXISTS roommate ADD COLUMN IF NOT EXISTS avatar_image TEXT;
