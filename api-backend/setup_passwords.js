/**
 * Setup Authentication for Banelo Database
 *
 * This script:
 * 1. Adds password_hash column to users table
 * 2. Sets default password "admin123" for all existing users
 * 3. Provides helper function to create users with hashed passwords
 *
 * Usage:
 *   node setup_passwords.js
 */

const { Pool } = require('pg');
const bcrypt = require('bcrypt');

// Database connection
const pool = new Pool({
    host: 'localhost',
    port: 5432,
    database: 'banelo_db',
    user: 'postgres',
    password: 'admin123',
});

const SALT_ROUNDS = 10;
const DEFAULT_PASSWORD = 'admin123';

async function setupAuthentication() {
    const client = await pool.connect();

    try {
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🔐 SETTING UP AUTHENTICATION');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

        // Step 1: Add password_hash column if it doesn't exist
        console.log('📝 Step 1: Adding password_hash column...');
        await client.query(`
            ALTER TABLE users
            ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
        `);
        console.log('✅ Column added successfully\n');

        // Step 2: Get all users without passwords
        console.log('👥 Step 2: Finding users without passwords...');
        const usersResult = await client.query(`
            SELECT id, username, fname, lname, role
            FROM users
            WHERE password_hash IS NULL OR password_hash = ''
        `);

        const users = usersResult.rows;
        console.log(`✅ Found ${users.length} users needing passwords\n`);

        if (users.length === 0) {
            console.log('✅ All users already have passwords!');
            return;
        }

        // Step 3: Hash default password
        console.log(`🔒 Step 3: Hashing default password ("${DEFAULT_PASSWORD}")...`);
        const hashedPassword = await bcrypt.hash(DEFAULT_PASSWORD, SALT_ROUNDS);
        console.log('✅ Password hashed successfully\n');

        // Step 4: Update all users with hashed password
        console.log('💾 Step 4: Updating user passwords...');
        for (const user of users) {
            await client.query(`
                UPDATE users
                SET password_hash = $1
                WHERE id = $2
            `, [hashedPassword, user.id]);

            console.log(`  ✅ ${user.username} (${user.fname} ${user.lname}) - ${user.role}`);
        }
        console.log('');

        // Step 5: Make password_hash NOT NULL
        console.log('🔒 Step 5: Making password_hash required...');
        await client.query(`
            ALTER TABLE users
            ALTER COLUMN password_hash SET NOT NULL;
        `);
        console.log('✅ Password is now required for all users\n');

        // Step 6: Create index for faster lookups
        console.log('⚡ Step 6: Creating performance index...');
        await client.query(`
            CREATE INDEX IF NOT EXISTS idx_users_username
            ON users(username);
        `);
        console.log('✅ Index created successfully\n');

        // Summary
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✅ AUTHENTICATION SETUP COMPLETE!');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

        // Show login credentials
        console.log('📋 DEFAULT LOGIN CREDENTIALS:');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        const allUsers = await client.query(`
            SELECT username, fname, lname, role, status
            FROM users
            WHERE status = 'active'
            ORDER BY role, username
        `);

        console.log('\n👤 ACTIVE USERS:\n');
        allUsers.rows.forEach(user => {
            console.log(`   Username: ${user.username}`);
            console.log(`   Password: ${DEFAULT_PASSWORD}`);
            console.log(`   Name: ${user.fname} ${user.lname}`);
            console.log(`   Role: ${user.role}`);
            console.log('   ─────────────────────────');
        });

        console.log('\n⚠️  IMPORTANT SECURITY NOTE:');
        console.log('   All users have the default password: "admin123"');
        console.log('   Users should change their passwords immediately!');
        console.log('   This is for development/testing only.\n');

    } catch (error) {
        console.error('❌ Error setting up authentication:', error);
        throw error;
    } finally {
        client.release();
        await pool.end();
    }
}

// Helper function to create a new user with hashed password
async function createUserWithPassword(userData) {
    const { username, password, fname, lname, mname, role, auth_email } = userData;

    const hashedPassword = await bcrypt.hash(password, SALT_ROUNDS);

    const result = await pool.query(`
        INSERT INTO users (fname, lname, mname, username, auth_email, role, status, password_hash)
        VALUES ($1, $2, $3, $4, $5, $6, 'active', $7)
        RETURNING *
    `, [fname, lname, mname || '', username, auth_email || '', role, hashedPassword]);

    return result.rows[0];
}

// Run setup
if (require.main === module) {
    setupAuthentication()
        .then(() => {
            console.log('✅ Setup completed successfully!');
            process.exit(0);
        })
        .catch((error) => {
            console.error('❌ Setup failed:', error);
            process.exit(1);
        });
}

module.exports = { createUserWithPassword };
