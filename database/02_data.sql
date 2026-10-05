--  Données de test

INSERT INTO shared_house (name, address, invitation_code, description) VALUES
  ('La Coloc du 3e', '12 rue des Lilas, 75011 Paris', 'LILAS2026', 'Coloc de 3 étudiants, ambiance calme en semaine.'),
  ('Villa Soleil',   '4 avenue du Port, 13002 Marseille', 'SOLEIL26', 'Grande maison avec jardin.');

INSERT INTO roommate (shared_house_id, name, surname, email, password, points, birthday) VALUES
  (1,    'Alice', 'Martin', 'alice@test.fr', '$2b$10$R/3Qpl1ZIkEw7WfXzxvngemw97dv2y7srn/fWyo3Aq8HK3sKp/eh6', 10, '2002-03-14'),
  (1,    'Bob',   'Durand', 'bob@test.fr',   '$2b$10$R/3Qpl1ZIkEw7WfXzxvngemw97dv2y7srn/fWyo3Aq8HK3sKp/eh6', 0,  '2001-11-02'),
  (1,    'Chloé', 'Petit',  'chloe@test.fr', '$2b$10$R/3Qpl1ZIkEw7WfXzxvngemw97dv2y7srn/fWyo3Aq8HK3sKp/eh6', 40, '2003-07-25'),
  (NULL, 'Dan',   'Leroy',  'dan@test.fr',   '$2b$10$R/3Qpl1ZIkEw7WfXzxvngemw97dv2y7srn/fWyo3Aq8HK3sKp/eh6', 0,  '2000-01-30');

INSERT INTO task (shared_house_id, assigned_id, name, description, deadline, status, completion_date, points) VALUES
  (1, 2,    'Sortir les poubelles',      'Jaunes et noires',          CURRENT_DATE + 1, FALSE, NULL,         5),
  (1, 3,    'Nettoyer la salle de bain', NULL,                        CURRENT_DATE + 3, FALSE, NULL,         20),
  (1, NULL, 'Arroser les plantes',       'Personne n''est assigné',   CURRENT_DATE + 2, FALSE, NULL,         5),
  (1, 1,    'Faire la vaisselle',        NULL,                        CURRENT_DATE,     TRUE,  CURRENT_DATE, 10),
  (1, 3,    'Passer l''aspirateur',      NULL,                        CURRENT_DATE,     TRUE,  CURRENT_DATE, 15),
  (1, 3,    'Nettoyer le frigo',         NULL,                        CURRENT_DATE,     TRUE,  CURRENT_DATE, 25);

INSERT INTO expense (shared_house_id, payer_id, name, amount, expense_date) VALUES
  (1, 1, 'Courses Carrefour', 60.00, CURRENT_DATE - 2),
  (1, 3, 'Facture internet',  30.00, CURRENT_DATE - 5);

INSERT INTO contribution (expense_id, roommate_id, status) VALUES
  (1, 1, TRUE),
  (1, 2, TRUE),
  (1, 3, FALSE),
  (2, 1, FALSE),
  (2, 2, FALSE),
  (2, 3, TRUE);

INSERT INTO note (shared_house_id, author_id, content, type, vote_end_date) VALUES
  (1, 1, 'Le proprio passe jeudi pour le chauffe-eau !', 'AFFICHAGE', NULL),
  (1, 2, 'On achète un lave-vaisselle ? Environ 120 € chacun', 'VOTE', NOW() + INTERVAL '7 days'),
  (1, 3, 'Quel soir pour la soirée coloc ?', 'VOTE', NOW() + INTERVAL '3 days');

INSERT INTO choice (note_id, choice_option) VALUES
  (2, 'Oui'), (2, 'Non'), (2, 'Plus tard'),
  (3, 'Vendredi'), (3, 'Samedi');

INSERT INTO vote (roommate_id, choice_id) VALUES
  (1, 1), (3, 1), (2, 2), 
  (1, 5), (2, 5);

INSERT INTO article (shared_house_id, owner_id, name, bought) VALUES
  (1, NULL, 'Papier toilette',   FALSE),
  (1, NULL, 'Liquide vaisselle', TRUE),
  (1, NULL, 'Éponges',           FALSE),
  (1, 2,    'Céréales',          FALSE),
  (1, 2,    'Lait d''avoine',    FALSE),
  (1, 3,    'Pommes',            TRUE);

INSERT INTO event (shared_house_id, creator_id, title, start_date, end_date) VALUES
  (1, 3, 'Soirée coloc',          NOW() + INTERVAL '5 days', NOW() + INTERVAL '5 days 4 hours'),
  (1, 1, 'Visite du proprio',     NOW() + INTERVAL '3 days', NOW() + INTERVAL '3 days 1 hour'),
  (1, 2, 'Grand ménage mensuel',  NOW() + INTERVAL '10 days', NOW() + INTERVAL '10 days 3 hours');
