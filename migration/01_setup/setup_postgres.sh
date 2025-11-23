#!/bin/bash

##############################################################################
# PostgreSQL Setup Script for Banelo Migration
# Creates database, user, and initial configuration
##############################################################################

set -e  # Exit on error

echo "🐘 PostgreSQL Setup for Banelo Pastry Shop"
echo "================================================================"

# Configuration
DB_NAME="banelo_db"
DB_USER="banelo_user"
DB_PASSWORD="banelo_password_2024"
DB_HOST="localhost"
DB_PORT="5432"

# Check if PostgreSQL is installed
if ! command -v psql &> /dev/null; then
    echo "❌ PostgreSQL is not installed!"
    echo ""
    echo "Install PostgreSQL:"
    echo "  Ubuntu/Debian: sudo apt install postgresql postgresql-contrib"
    echo "  macOS: brew install postgresql"
    echo "  Windows: Download from https://www.postgresql.org/download/windows/"
    echo ""
    exit 1
fi

echo "✅ PostgreSQL is installed"
echo ""

# Check if PostgreSQL service is running
if ! sudo systemctl is-active --quiet postgresql; then
    echo "⚠️  PostgreSQL service is not running. Starting..."
    sudo systemctl start postgresql
    echo "✅ PostgreSQL service started"
fi
echo ""

# Create database and user
echo "Creating database and user..."
echo ""

sudo -u postgres psql <<EOF
-- Drop existing database if exists (careful!)
DROP DATABASE IF EXISTS ${DB_NAME};
DROP USER IF EXISTS ${DB_USER};

-- Create new user
CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASSWORD}';

-- Create database
CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};

-- Connect to the new database and grant schema privileges
\c ${DB_NAME}
GRANT ALL ON SCHEMA public TO ${DB_USER};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ${DB_USER};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ${DB_USER};

-- Enable UUID extension (needed for UUID primary keys)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";  -- For gen_random_uuid()

-- Verify setup
\l ${DB_NAME}
\du ${DB_USER}
EOF

echo ""
echo "================================================================"
echo "✅ PostgreSQL Setup Complete!"
echo ""
echo "📋 Database Details:"
echo "  Host:     ${DB_HOST}"
echo "  Port:     ${DB_PORT}"
echo "  Database: ${DB_NAME}"
echo "  User:     ${DB_USER}"
echo "  Password: ${DB_PASSWORD}"
echo ""

# Create .env file for Python scripts
cat > ../.env <<EOF
# PostgreSQL Configuration for Banelo Migration
DB_HOST=${DB_HOST}
DB_PORT=${DB_PORT}
DB_NAME=${DB_NAME}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
DB_URL=postgresql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}
EOF

echo "✅ Environment file created: migration/.env"
echo ""

# Test connection
echo "Testing database connection..."
PGPASSWORD=${DB_PASSWORD} psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "SELECT version();" || {
    echo "❌ Connection test failed!"
    exit 1
}

echo ""
echo "✅ Connection test successful!"
echo ""
echo "Next steps:"
echo "1. Create schema: cd ../02_schema && psql -U ${DB_USER} -d ${DB_NAME} -f schema.sql"
echo "2. Transform data: cd ../03_transform && python3 transform_dataset.py"
echo "3. Import data: cd ../05_csv_import && psql -U ${DB_USER} -d ${DB_NAME} -f import_all.sql"
echo ""
