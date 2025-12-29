-- DDZ Game Database Schema
-- Phase 4: PostgreSQL Database Setup

-- Table: users
CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT username_length CHECK (LENGTH(username) >= 3),
    CONSTRAINT username_alphanumeric CHECK (username ~ '^[a-zA-Z0-9_]+$')
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_last_seen ON users(last_seen_at);

-- Table: games
CREATE TABLE IF NOT EXISTS games (
    game_id VARCHAR(50) PRIMARY KEY,
    join_code VARCHAR(4) UNIQUE NOT NULL,
    max_players INTEGER NOT NULL,
    current_phase VARCHAR(20) NOT NULL,
    is_paused BOOLEAN NOT NULL DEFAULT false,
    game_state_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT valid_phase CHECK (current_phase IN ('LOBBY', 'DEAL', 'BIDDING', 'PLAY', 'SCORING', 'TERMINATED')),
    CONSTRAINT valid_player_count CHECK (max_players BETWEEN 3 AND 12)
);

CREATE INDEX IF NOT EXISTS idx_games_join_code ON games(join_code);
CREATE INDEX IF NOT EXISTS idx_games_phase ON games(current_phase);
CREATE INDEX IF NOT EXISTS idx_games_updated ON games(updated_at);
CREATE INDEX IF NOT EXISTS idx_games_not_completed ON games(completed_at) WHERE completed_at IS NULL;

-- Table: game_participants
CREATE TABLE IF NOT EXISTS game_participants (
    id BIGSERIAL PRIMARY KEY,
    game_id VARCHAR(50) NOT NULL REFERENCES games(game_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    player_id UUID NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,

    CONSTRAINT unique_user_per_game UNIQUE(game_id, user_id),
    CONSTRAINT unique_player_id_per_game UNIQUE(game_id, player_id)
);

CREATE INDEX IF NOT EXISTS idx_game_participants_game ON game_participants(game_id);
CREATE INDEX IF NOT EXISTS idx_game_participants_user ON game_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_game_participants_active ON game_participants(user_id, left_at) WHERE left_at IS NULL;

-- Table: game_results
CREATE TABLE IF NOT EXISTS game_results (
    id BIGSERIAL PRIMARY KEY,
    game_id VARCHAR(50) NOT NULL REFERENCES games(game_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    player_id UUID NOT NULL,
    final_score INTEGER NOT NULL,
    was_landlord BOOLEAN NOT NULL,
    completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_user_game_result UNIQUE(game_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_game_results_user ON game_results(user_id);
CREATE INDEX IF NOT EXISTS idx_game_results_game ON game_results(game_id);
CREATE INDEX IF NOT EXISTS idx_game_results_completed ON game_results(completed_at);

-- Migration: Add session support (Phase 2)
-- Add session_id and round_number to games table
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='games' AND column_name='session_id') THEN
        ALTER TABLE games ADD COLUMN session_id VARCHAR(50);
        -- Backfill existing data: use game_id as session_id
        UPDATE games SET session_id = game_id WHERE session_id IS NULL;
        ALTER TABLE games ALTER COLUMN session_id SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='games' AND column_name='round_number') THEN
        ALTER TABLE games ADD COLUMN round_number INTEGER DEFAULT 1 NOT NULL;
    END IF;
END $$;

-- Add indexes for session queries
CREATE INDEX IF NOT EXISTS idx_games_session_id ON games(session_id);
CREATE INDEX IF NOT EXISTS idx_games_session_round ON games(session_id, round_number);

-- Add session_id to game_results table
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='game_results' AND column_name='session_id') THEN
        ALTER TABLE game_results ADD COLUMN session_id VARCHAR(50);
        -- Backfill from games table
        UPDATE game_results gr
        SET session_id = g.session_id
        FROM games g
        WHERE gr.game_id = g.game_id AND gr.session_id IS NULL;
        -- Set NOT NULL after backfill
        ALTER TABLE game_results ALTER COLUMN session_id SET NOT NULL;
    END IF;
END $$;

-- Add index for session-based leaderboard queries
CREATE INDEX IF NOT EXISTS idx_game_results_session ON game_results(session_id);

-- Note: The unique_user_game_result constraint remains as-is
-- It allows one result per user per game_id (round)
-- Multiple rounds in a session will have different game_ids
