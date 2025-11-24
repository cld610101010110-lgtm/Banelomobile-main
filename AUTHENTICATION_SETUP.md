# 🔐 Authentication Setup Guide

## Overview

The Banelo application now has **proper password authentication** using bcrypt for secure password hashing.

---

## ⚠️ **CRITICAL**: You Found a Security Vulnerability!

**Good catch!** The original implementation had **NO authentication** - anyone could login with just a username. This has now been fixed with:

- ✅ Secure password hashing using bcrypt
- ✅ Password verification on login
- ✅ Protected API endpoints
- ✅ Proper error handling

---

## 🚀 Quick Setup

### 1. Install Dependencies

```bash
cd api-backend
npm install
```

This will install `bcrypt` for password hashing.

### 2. Setup Database Authentication

Run the setup script to add passwords to existing users:

```bash
node setup_passwords.js
```

This script will:
- Add `password_hash` column to the `users` table
- Set default password `"admin123"` for all existing users
- Create performance indexes
- Display all user credentials

### 3. Start the API Server

```bash
node server.js
```

---

## 📱 Login Credentials

After running the setup script, all users will have:

**Password:** `admin123`

**Usernames:** (whatever usernames exist in your database)

Example:
```
Username: manager1
Password: admin123

Username: staff1
Password: admin123
```

---

## 🔧 How It Works

### Backend (API)

**File:** `api-backend/server.js`

```javascript
// Login endpoint with password verification
app.post('/api/users/login', async (req, res) => {
    const { username, password } = req.body;

    // Get user from database
    const user = await pool.query('SELECT * FROM users WHERE username = $1', [username]);

    // Verify password using bcrypt
    const passwordMatch = await bcrypt.compare(password, user.password_hash);

    if (passwordMatch) {
        // Login successful
        res.json({ success: true, data: user });
    } else {
        // Invalid password
        res.status(401).json({ success: false, error: 'Invalid credentials' });
    }
});
```

### Android App

**Files Updated:**
1. `BaneloApiService.kt` - Added password field to `LoginRequest`
2. `UserRepository.kt` - Sends password to API
3. `login.kt` - Already had password input field

---

## 🔒 Security Features

### Password Hashing
- Uses **bcrypt** with 10 salt rounds
- Passwords are NEVER stored in plain text
- Hash format: `$2b$10$...` (60 characters)

### API Protection
- Validates username AND password
- Returns generic error messages (doesn't reveal if username exists)
- Logs failed login attempts

### Best Practices
- Password hash never sent to client
- HTTPS recommended for production
- Rate limiting should be added for production

---

## 🛠️ Common Tasks

### Add a New User with Password

Use the helper script:

```javascript
const { createUserWithPassword } = require('./setup_passwords');

await createUserWithPassword({
    username: 'newuser',
    password: 'SecurePass123!',
    fname: 'John',
    lname: 'Doe',
    mname: 'M',
    role: 'staff',
    auth_email: 'john@example.com'
});
```

### Change a User's Password

```sql
-- First, hash the new password using bcrypt (use Node.js or bcrypt CLI)
-- Then update the database:

UPDATE users
SET password_hash = '$2b$10$...'  -- Insert hashed password here
WHERE username = 'username';
```

Or use the API (you'll need to add this endpoint):

```javascript
app.post('/api/users/:id/change-password', async (req, res) => {
    const { currentPassword, newPassword } = req.body;

    // Verify current password
    // Hash new password
    // Update database
});
```

### Reset All Passwords to Default

```bash
node setup_passwords.js
```

---

## 📊 Database Schema

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_id VARCHAR(255) UNIQUE,
    fname VARCHAR(100) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    mname VARCHAR(100),
    username VARCHAR(100) NOT NULL UNIQUE,
    auth_email VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,  -- ⬅️ NEW
    role VARCHAR(50) DEFAULT 'Staff',
    status VARCHAR(50) DEFAULT 'active',
    joined_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Performance index
CREATE INDEX idx_users_username ON users(username);
```

---

## ⚠️ Production Deployment

### Before going to production:

1. **Change ALL passwords** from default `admin123`
2. **Use environment variables** for database credentials
3. **Enable HTTPS** (TLS/SSL)
4. **Add rate limiting** to prevent brute force attacks
5. **Implement password complexity requirements**
6. **Add "forgot password" functionality**
7. **Enable audit logging** for security events
8. **Regular security audits**

---

## 🐛 Troubleshooting

### "Invalid username or password" error
- Check that `setup_passwords.js` was run
- Verify the username exists in database
- Ensure password is exactly `admin123`
- Check API logs for detailed error messages

### "bcrypt not found" error
```bash
cd api-backend
npm install bcrypt
```

### "password_hash column doesn't exist"
```bash
node setup_passwords.js
```

---

## 📝 Files Modified

### Backend
- ✅ `api-backend/package.json` - Added bcrypt dependency
- ✅ `api-backend/server.js` - Added password verification
- ✅ `api-backend/setup_passwords.js` - NEW: Setup script

### Android App
- ✅ `BaneloApiService.kt` - Added password to LoginRequest
- ✅ `UserRepository.kt` - Sends password to API
- ✅ `login.kt` - Already had password input (now functional!)

### Database
- ✅ `migration/add_authentication.sql` - SQL migration script

---

## ✅ Testing the Authentication

1. **Start the API server:**
   ```bash
   cd api-backend
   node setup_passwords.js
   node server.js
   ```

2. **Run the Android app**

3. **Login with:**
   - Username: (any username from your database)
   - Password: `admin123`

4. **Success indicators:**
   - ✅ Login successful message
   - ✅ API logs show: `✅ Successful login: username (role)`
   - ✅ App navigates to dashboard

5. **Failure test:**
   - Try wrong password: `wrongpassword`
   - Should show: "Invalid username or password"
   - API logs show: `❌ Failed login attempt for user: username`

---

## 🎯 Summary

**Before:** No authentication - anyone could login with just a username ❌

**After:** Secure bcrypt password authentication ✅

Your application now has proper security! 🔒

