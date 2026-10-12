-- À exécuter UNE FOIS sur une base déjà créée (les nouvelles bases utilisent 01_schema.sql).
ALTER TABLE pot ADD COLUMN IF NOT EXISTS purchased BOOLEAN NOT NULL DEFAULT FALSE;
