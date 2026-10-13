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


-- Suppression de compte : l'historique (dépenses, remboursements, versements) reste,
-- avec NULL à la place de la personne supprimée. Ses événements et notes sont supprimés.
ALTER TABLE IF EXISTS expense ALTER COLUMN payer_id DROP NOT NULL;
ALTER TABLE IF EXISTS expense DROP CONSTRAINT IF EXISTS expense_payer_id_fkey;
ALTER TABLE IF EXISTS expense ADD CONSTRAINT expense_payer_id_fkey
    FOREIGN KEY (payer_id) REFERENCES roommate(id) ON DELETE SET NULL;

ALTER TABLE IF EXISTS reimbursement ALTER COLUMN payer_id DROP NOT NULL;
ALTER TABLE IF EXISTS reimbursement ALTER COLUMN receiver_id DROP NOT NULL;
ALTER TABLE IF EXISTS reimbursement DROP CONSTRAINT IF EXISTS reimbursement_payer_id_fkey;
ALTER TABLE IF EXISTS reimbursement ADD CONSTRAINT reimbursement_payer_id_fkey
    FOREIGN KEY (payer_id) REFERENCES roommate(id) ON DELETE SET NULL;
ALTER TABLE IF EXISTS reimbursement DROP CONSTRAINT IF EXISTS reimbursement_receiver_id_fkey;
ALTER TABLE IF EXISTS reimbursement ADD CONSTRAINT reimbursement_receiver_id_fkey
    FOREIGN KEY (receiver_id) REFERENCES roommate(id) ON DELETE SET NULL;

ALTER TABLE IF EXISTS pot_payment ALTER COLUMN roommate_id DROP NOT NULL;
ALTER TABLE IF EXISTS pot_payment DROP CONSTRAINT IF EXISTS pot_payment_roommate_id_fkey;
ALTER TABLE IF EXISTS pot_payment ADD CONSTRAINT pot_payment_roommate_id_fkey
    FOREIGN KEY (roommate_id) REFERENCES roommate(id) ON DELETE SET NULL;

ALTER TABLE IF EXISTS event DROP CONSTRAINT IF EXISTS event_creator_id_fkey;
ALTER TABLE IF EXISTS event ADD CONSTRAINT event_creator_id_fkey
    FOREIGN KEY (creator_id) REFERENCES roommate(id) ON DELETE CASCADE;

ALTER TABLE IF EXISTS note DROP CONSTRAINT IF EXISTS note_creator_id_fkey;
ALTER TABLE IF EXISTS note ADD CONSTRAINT note_creator_id_fkey
    FOREIGN KEY (creator_id) REFERENCES roommate(id) ON DELETE CASCADE;

-- Dépenses : la part de chacun est enregistrée (calculée ici pour les anciennes dépenses,
-- même règle que ExpenseSplitter : les centimes en trop vont aux plus petits id).
ALTER TABLE IF EXISTS contribution ADD COLUMN IF NOT EXISTS share NUMERIC(10,2);
UPDATE contribution AS c
SET share = ((s.cents / s.n) + CASE WHEN s.pos < MOD(s.cents, s.n) THEN 1 ELSE 0 END) / 100.0
FROM (SELECT c2.expense_id,
             c2.roommate_id,
             ROUND(e.amount * 100)::BIGINT AS cents,
             COUNT(*) OVER (PARTITION BY c2.expense_id) AS n,
             ROW_NUMBER() OVER (PARTITION BY c2.expense_id ORDER BY c2.roommate_id) - 1 AS pos
      FROM contribution c2
      JOIN expense e ON e.id = c2.expense_id) AS s
WHERE c.share IS NULL
  AND c.expense_id = s.expense_id
  AND c.roommate_id = s.roommate_id;
ALTER TABLE IF EXISTS contribution ALTER COLUMN share SET NOT NULL;

ALTER TABLE IF EXISTS contribution DROP CONSTRAINT IF EXISTS contribution_pkey;
ALTER TABLE IF EXISTS contribution ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE IF EXISTS contribution DROP CONSTRAINT IF EXISTS contribution_id_pkey;
ALTER TABLE IF EXISTS contribution ADD CONSTRAINT contribution_id_pkey PRIMARY KEY (id);
ALTER TABLE IF EXISTS contribution DROP CONSTRAINT IF EXISTS contribution_expense_roommate_key;
ALTER TABLE IF EXISTS contribution ADD CONSTRAINT contribution_expense_roommate_key UNIQUE (expense_id, roommate_id);
ALTER TABLE IF EXISTS contribution ALTER COLUMN roommate_id DROP NOT NULL;
ALTER TABLE IF EXISTS contribution DROP CONSTRAINT IF EXISTS contribution_roommate_id_fkey;
ALTER TABLE IF EXISTS contribution ADD CONSTRAINT contribution_roommate_id_fkey
    FOREIGN KEY (roommate_id) REFERENCES roommate(id) ON DELETE SET NULL;