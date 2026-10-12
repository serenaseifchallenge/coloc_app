-- Schéma de l'application de gestion de colocation

CREATE TABLE shared_house (
                              id               BIGSERIAL PRIMARY KEY,
                              name             VARCHAR(100) NOT NULL,
                              address          VARCHAR(255) NOT NULL,
                              invitation_code  VARCHAR(20)  NOT NULL UNIQUE,
                              creation_date    TIMESTAMP NOT NULL DEFAULT NOW(),
                              description      TEXT NOT NULL
);

CREATE TABLE roommate (
                          id              BIGSERIAL PRIMARY KEY,
                          shared_house_id BIGINT REFERENCES shared_house(id) ON DELETE SET NULL,
                          name            VARCHAR(100) NOT NULL,
                          surname         VARCHAR(100) NOT NULL,
                          email           VARCHAR(255) NOT NULL UNIQUE,
                          password        VARCHAR(255) NOT NULL,
                          points          INTEGER,
                          creation_date   TIMESTAMP NOT NULL DEFAULT NOW(),
                          birthday        TIMESTAMP NOT NULL DEFAULT NOW(),
                          avatar_color    VARCHAR(7),
                          avatar_image    TEXT
);

CREATE TABLE task (
                      id              BIGSERIAL PRIMARY KEY,
                      shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
                      assigned_id     BIGINT REFERENCES roommate(id) ON DELETE SET NULL,
                      name            VARCHAR(150) NOT NULL,
                      completion_date DATE,
                      deadline        DATE,
                      status          BOOLEAN NOT NULL DEFAULT FALSE,
                      points          INTEGER NOT NULL DEFAULT 10
);

CREATE TABLE expense (
                         id              BIGSERIAL PRIMARY KEY,
                         shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
                         payer_id        BIGINT NOT NULL REFERENCES roommate(id),
                         name            VARCHAR(150) NOT NULL,
                         amount          NUMERIC(10,2) NOT NULL CHECK (amount > 0),
                         expense_date    DATE NOT NULL DEFAULT CURRENT_DATE,
                         category        VARCHAR(20) NOT NULL DEFAULT 'COURANTE'
                             CHECK (category IN ('COURANTE', 'EAU', 'INTERNET', 'AUTRE'))
);

CREATE TABLE contribution (
                              expense_id      BIGINT NOT NULL REFERENCES expense(id) ON DELETE CASCADE,
                              roommate_id     BIGINT NOT NULL REFERENCES roommate(id),
                              status          BOOLEAN NOT NULL DEFAULT FALSE,
                              PRIMARY KEY (expense_id, roommate_id)
);

CREATE TABLE reimbursement (
                               id                 BIGSERIAL PRIMARY KEY,
                               shared_house_id    BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
                               payer_id           BIGINT NOT NULL REFERENCES roommate(id),
                               receiver_id        BIGINT NOT NULL REFERENCES roommate(id),
                               amount             NUMERIC(10,2) NOT NULL CHECK (amount > 0),
                               method             VARCHAR(20) NOT NULL DEFAULT 'VIREMENT'
                                   CHECK (method IN ('VIREMENT', 'ESPECES', 'PAYPAL_WERO', 'AUTRE')),
                               reimbursement_date DATE NOT NULL DEFAULT CURRENT_DATE,
                               CHECK (payer_id <> receiver_id)
);

CREATE TABLE pot (
                     id              BIGSERIAL PRIMARY KEY,
                     shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
                     created_by      BIGINT NOT NULL REFERENCES roommate(id),
                     name            VARCHAR(150) NOT NULL,
                     type            VARCHAR(20) NOT NULL DEFAULT 'AUTRE'
                         CHECK (type IN ('VOYAGE', 'CADEAU', 'EVENEMENT', 'MATERIEL', 'AUTRE')),
                     target_amount   NUMERIC(10,2) NOT NULL CHECK (target_amount > 0),
                     deadline        DATE,
                     purchased       BOOLEAN NOT NULL DEFAULT FALSE,
                     creation_date   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE pot_participant (
                                 pot_id      BIGINT NOT NULL REFERENCES pot(id) ON DELETE CASCADE,
                                 roommate_id BIGINT NOT NULL REFERENCES roommate(id) ON DELETE CASCADE,
                                 PRIMARY KEY (pot_id, roommate_id)
);

CREATE TABLE pot_payment (
                             id           BIGSERIAL PRIMARY KEY,
                             pot_id       BIGINT NOT NULL REFERENCES pot(id) ON DELETE CASCADE,
                             roommate_id  BIGINT NOT NULL REFERENCES roommate(id),
                             amount       NUMERIC(10,2) NOT NULL CHECK (amount > 0),
                             method       VARCHAR(20) NOT NULL DEFAULT 'VIREMENT'
                                 CHECK (method IN ('VIREMENT', 'ESPECES', 'PAYPAL_WERO', 'AUTRE')),
                             payment_date DATE NOT NULL DEFAULT CURRENT_DATE
);


CREATE TABLE article (
                         id              BIGSERIAL PRIMARY KEY,
                         shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
                         owner_id        BIGINT REFERENCES roommate(id) ON DELETE CASCADE,
                         name            VARCHAR(100) NOT NULL,
                         bought          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE event (
    id              BIGSERIAL PRIMARY KEY,
    shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
    creator_id      BIGINT NOT NULL REFERENCES roommate(id),
    title           VARCHAR(150) NOT NULL,
    description     TEXT,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    all_day         BOOLEAN NOT NULL DEFAULT FALSE,
    start_time      TIME,
    end_time        TIME,
    CHECK (end_date >= start_date),
    CHECK (all_day = TRUE OR (start_time IS NOT NULL AND end_time IS NOT NULL)),
    CHECK (start_date <> end_date OR all_day = TRUE OR end_time > start_time)
);

CREATE TABLE hidden_task_name (
                                  id              BIGSERIAL PRIMARY KEY,
                                  shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
                                  name            VARCHAR(150) NOT NULL,
                                  UNIQUE (shared_house_id, name)
);

CREATE TABLE custom_task_name (
                                  id              BIGSERIAL PRIMARY KEY,
                                  shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
                                  name            VARCHAR(150) NOT NULL,
                                  UNIQUE (shared_house_id, name)
);

CREATE TABLE monthly_score (
                               id              BIGSERIAL PRIMARY KEY,
                               shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
                               roommate_id     BIGINT NOT NULL REFERENCES roommate(id) ON DELETE CASCADE,
                               month_start     DATE NOT NULL,
                               points          INTEGER NOT NULL,
                               UNIQUE (roommate_id, month_start)
);

CREATE TABLE note (
    id BIGSERIAL PRIMARY KEY,
    shared_house_id BIGINT NOT NULL
        REFERENCES shared_house(id) ON DELETE CASCADE,
    creator_id BIGINT NOT NULL REFERENCES roommate(id),
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL
        CHECK (type IN ('ANNONCE', 'SONDAGE')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    multiple_answers BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE poll_option (
    id BIGSERIAL PRIMARY KEY,
    note_id BIGINT NOT NULL
        REFERENCES note(id) ON DELETE CASCADE,
    label VARCHAR(250) NOT NULL
);

CREATE TABLE poll_vote (
    id BIGSERIAL PRIMARY KEY,
    option_id BIGINT NOT NULL
        REFERENCES poll_option(id) ON DELETE CASCADE,
    roommate_id BIGINT NOT NULL
        REFERENCES roommate(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_poll_vote_option_roommate
        UNIQUE (option_id, roommate_id)
);

CREATE INDEX idx_note_shared_house_created
    ON note(shared_house_id, created_at DESC);

CREATE INDEX idx_poll_option_note
    ON poll_option(note_id);

CREATE INDEX idx_poll_vote_roommate
    ON poll_vote(roommate_id);