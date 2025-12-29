#!/bin/bash
# Script to seed local development database with test data
# Make sure your local PostgreSQL is running with:
#   docker-compose up -d postgres

set -e

echo "Seeding local development database with test data..."

# Check if PostgreSQL is running
if ! docker-compose ps postgres | grep -q "Up"; then
  echo "Error: PostgreSQL is not running. Start it with: docker-compose up -d postgres"
  exit 1
fi

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
until docker-compose exec -T postgres pg_isready -U ddz_dev > /dev/null 2>&1; do
  sleep 1
done

# Run the seed script
echo "Running seed script..."
docker-compose exec -T postgres psql -U ddz_dev -d ddz_dev < server/src/main/resources/db/seed-dev-data.sql

echo "✓ Development database seeded successfully!"
echo ""
echo "Test users created:"
echo "  - player1 (Alice Chen)"
echo "  - player2 (Bob Zhang)"
echo "  - player3 (Charlie Wang)"
echo "  - player4 (Diana Liu)"
echo "  - player5 (Eddie Lin)"
echo "  - player6 (Fiona Ma)"
echo "  - player7 (George Wu)"
echo "  - player8 (Helen Zhao)"
echo "  - player9 (Ivan Song)"
echo "  - player10 (Jenny Huang)"
