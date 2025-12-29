-- Development seed data for DDZ game
-- This script populates the database with 10 test players for local development
-- Run this manually against your local PostgreSQL database using:
--   psql -h localhost -p 5432 -U ddz_dev -d ddz_dev -f server/src/main/resources/db/seed-dev-data.sql

-- Insert 10 test users
INSERT INTO users (user_id, username, display_name, created_at, last_seen_at)
VALUES
  (gen_random_uuid(), 'player1', 'Alice Chen', NOW(), NOW()),
  (gen_random_uuid(), 'player2', 'Bob Zhang', NOW(), NOW()),
  (gen_random_uuid(), 'player3', 'Charlie Wang', NOW(), NOW()),
  (gen_random_uuid(), 'player4', 'Diana Liu', NOW(), NOW()),
  (gen_random_uuid(), 'player5', 'Eddie Lin', NOW(), NOW()),
  (gen_random_uuid(), 'player6', 'Fiona Ma', NOW(), NOW()),
  (gen_random_uuid(), 'player7', 'George Wu', NOW(), NOW()),
  (gen_random_uuid(), 'player8', 'Helen Zhao', NOW(), NOW()),
  (gen_random_uuid(), 'player9', 'Ivan Song', NOW(), NOW()),
  (gen_random_uuid(), 'player10', 'Jenny Huang', NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

-- Verify the data was inserted
SELECT username, display_name FROM users ORDER BY username;
