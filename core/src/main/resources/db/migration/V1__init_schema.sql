-- Full schema up front (see NBA2K Assistant plan §2). Only players/player_attributes/
-- badges/teams are populated by Milestone 1; rosters/sessions are created here too
-- so later milestones are pure application code, no further schema churn.

CREATE TABLE players (
    id              BIGSERIAL PRIMARY KEY,
    external_id     TEXT,
    name            TEXT NOT NULL,
    team            TEXT,
    position        TEXT,
    positions       TEXT[]     NOT NULL DEFAULT '{}',
    overall         SMALLINT   NOT NULL,
    era_tag         TEXT       NOT NULL DEFAULT 'CURRENT',
    source          TEXT       NOT NULL,
    last_synced_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One (source, external_id) identifies a player from a given feed; local-scraper
-- rows have no external_id, so name+team+era_tag is the natural key for those.
CREATE UNIQUE INDEX ux_players_source_external_id
    ON players (source, external_id) WHERE external_id IS NOT NULL;
CREATE UNIQUE INDEX ux_players_source_name_team_era
    ON players (source, name, team, era_tag) WHERE external_id IS NULL;

CREATE INDEX ix_players_position_overall ON players (position, overall);
CREATE INDEX ix_players_era_tag ON players (era_tag);

CREATE TABLE player_attributes (
    player_id           BIGINT PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    three_pt             SMALLINT,
    mid_range             SMALLINT,
    layup                SMALLINT,
    dunk                 SMALLINT,
    speed                SMALLINT,
    strength             SMALLINT,
    post_defense          SMALLINT,
    perimeter_defense      SMALLINT,
    -- Full nba2kapi attributes payload, kept verbatim as the source of truth;
    -- the columns above are a best-effort projection the rules engine reads.
    raw_attributes        JSONB
);

CREATE TABLE badges (
    id           BIGSERIAL PRIMARY KEY,
    slug         TEXT NOT NULL UNIQUE,
    name         TEXT NOT NULL,
    category     TEXT,
    tier_levels  SMALLINT NOT NULL DEFAULT 4
);

CREATE TABLE player_badges (
    player_id  BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    badge_id   BIGINT NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    tier       SMALLINT NOT NULL,
    PRIMARY KEY (player_id, badge_id)
);

CREATE TABLE teams (
    id          BIGSERIAL PRIMARY KEY,
    name        TEXT NOT NULL,
    abbreviation TEXT,
    era_tag     TEXT NOT NULL DEFAULT 'CURRENT',
    team_type   TEXT NOT NULL DEFAULT 'CURR'
);
CREATE UNIQUE INDEX ux_teams_name_era ON teams (name, era_tag);

CREATE TABLE rosters (
    id               BIGSERIAL PRIMARY KEY,
    owner_session_id TEXT,
    name             TEXT NOT NULL,
    era_tag          TEXT NOT NULL DEFAULT 'CURRENT',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE roster_players (
    roster_id      BIGINT NOT NULL REFERENCES rosters(id) ON DELETE CASCADE,
    player_id      BIGINT NOT NULL REFERENCES players(id),
    is_starter     BOOLEAN NOT NULL DEFAULT false,
    position_slot  TEXT,
    PRIMARY KEY (roster_id, player_id)
);

CREATE TABLE sessions (
    id              BIGSERIAL PRIMARY KEY,
    code            TEXT NOT NULL UNIQUE,
    state           TEXT NOT NULL DEFAULT 'LOBBY',
    host_client_id  TEXT,
    guest_client_id TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0
);
