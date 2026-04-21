CREATE TABLE sessions (
    session_key         INT PRIMARY KEY,
    circuit_short_name  VARCHAR(100),
    country_code        VARCHAR(10),
    country_name        VARCHAR(100),
    date_end            TIMESTAMPTZ,
    date_start          TIMESTAMPTZ,
    session_name        VARCHAR(100),
    session_type        VARCHAR(50),
    year                INT NOT NULL,
    fastest_lap_driver_number INT
);

CREATE TABLE drivers (
    id             BIGSERIAL PRIMARY KEY,
    session_key    INT NOT NULL REFERENCES sessions(session_key) ON DELETE CASCADE,
    driver_number  INT NOT NULL,
    country_code   VARCHAR(10),
    first_name     VARCHAR(100),
    full_name      VARCHAR(100),
    last_name      VARCHAR(100),
    headshot_url   VARCHAR(500),
    name_acronym   VARCHAR(10),
    team_colour    VARCHAR(10),
    team_name      VARCHAR(100),
    UNIQUE (session_key, driver_number)
);

CREATE TABLE session_results (
    id             BIGSERIAL PRIMARY KEY,
    session_key    INT NOT NULL REFERENCES sessions(session_key) ON DELETE CASCADE,
    driver_number  INT NOT NULL,
    dnf            BOOLEAN NOT NULL DEFAULT FALSE,
    dns            BOOLEAN NOT NULL DEFAULT FALSE,
    dsq            BOOLEAN NOT NULL DEFAULT FALSE,
    duration       DOUBLE PRECISION,
    gap_to_leader  DOUBLE PRECISION,
    number_of_laps INT NOT NULL DEFAULT 0,
    position       INT,
    UNIQUE (session_key, driver_number)
);

CREATE INDEX idx_sessions_year      ON sessions(year);
CREATE INDEX idx_sessions_year_type ON sessions(year, session_type);
CREATE INDEX idx_drivers_session    ON drivers(session_key);
CREATE INDEX idx_results_session    ON session_results(session_key);
CREATE INDEX idx_results_driver     ON session_results(driver_number);
