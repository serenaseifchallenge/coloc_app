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
    birthday        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE task (
    id              BIGSERIAL PRIMARY KEY,
    shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
    assigned_id     BIGINT REFERENCES roommate(id) ON DELETE SET NULL, 
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
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
    expense_date    DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE contribution (
    expense_id      BIGINT NOT NULL REFERENCES expense(id) ON DELETE CASCADE,
    roommate_id     BIGINT NOT NULL REFERENCES roommate(id),
    status          BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (expense_id, roommate_id)
);

CREATE TABLE note (
    id              BIGSERIAL PRIMARY KEY,
    shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
    author_id       BIGINT NOT NULL REFERENCES roommate(id),
    content         TEXT NOT NULL,
    type            VARCHAR(20) NOT NULL DEFAULT 'AFFICHAGE' CHECK (type IN ('AFFICHAGE','VOTE')),
    creation_date   TIMESTAMP NOT NULL DEFAULT NOW(),
    vote_end_date   TIMESTAMP,                      
    CHECK (type = 'VOTE' OR vote_end_date IS NULL),
    CHECK (vote_end_date >= creation_date)
);

CREATE TABLE choice (
    id              BIGSERIAL PRIMARY KEY,
    note_id         BIGINT NOT NULL REFERENCES note(id) ON DELETE CASCADE,
    choice_option   VARCHAR(150) NOT NULL
);

CREATE TABLE vote (
    roommate_id  BIGINT NOT NULL REFERENCES roommate(id) ON DELETE CASCADE,
    choice_id    BIGINT NOT NULL REFERENCES choice(id) ON DELETE CASCADE,
    PRIMARY KEY (roommate_id, choice_id)
);

CREATE TABLE article (
    id              BIGSERIAL PRIMARY KEY,
    shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
    owner_id        BIGINT REFERENCES roommate(id) ON DELETE CASCADE, -- si NULL : liste commune
    name            VARCHAR(100) NOT NULL,
    bought          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE event (
    id              BIGSERIAL PRIMARY KEY,
    shared_house_id BIGINT NOT NULL REFERENCES shared_house(id) ON DELETE CASCADE,
    creator_id      BIGINT NOT NULL REFERENCES roommate(id),
    title           VARCHAR(150) NOT NULL,
    start_date      TIMESTAMP NOT NULL,
    end_date        TIMESTAMP NOT NULL,
    CHECK (end_date >= start_date)
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