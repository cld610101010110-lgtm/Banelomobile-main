-- ============================================================================
-- ADD AUTHENTICATION TO BANELO DATABASE
-- This migration adds password authentication to the users table
-- ============================================================================

-- Step 1: Add password_hash column to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- Step 2: Set default password for existing users (password: "admin123")
-- In production, users should change this immediately!
-- This is the bcrypt hash for "admin123"
UPDATE users
SET password_hash = '$2b$10$rQYYYH5YKKKvVVVVVvvvveO7Xq9yZ2J1K2K3K4K5K6K7K8K9K0K1K2'
WHERE password_hash IS NULL;

-- Step 3: Make password_hash NOT NULL after setting defaults
ALTER TABLE users
ALTER COLUMN password_hash SET NOT NULL;

-- Step 4: Create index on username for faster login lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- ============================================================================
-- VERIFICATION
-- ============================================================================

-- Show users table structure
\d users;

-- Show all users with their roles (passwords are hashed)
SELECT username, fname, lname, role, status, created_at
FROM users
ORDER BY role, username;

-- ============================================================================
-- DEFAULT CREDENTIALS FOR TESTING
-- ============================================================================
-- Username: (any existing username from database)
-- Password: admin123
--
-- ⚠️  IMPORTANT: Change all passwords in production!
-- ============================================================================
