# 🚀 Quick Start Guide - Banelo POS System

## ⚡ Setup in 3 Steps

### **Step 1: Setup Database & Passwords**
```bash
cd api-backend
npm install
node setup_passwords.js
```

### **Step 2: Start API Server**
```bash
node server.js
```

You should see:
```
✅ Connected to PostgreSQL database: banelo_db
🚀 Server running on: http://localhost:3000
```

### **Step 3: Run Android App**
- Open project in Android Studio
- Build and run the app
- Login with default credentials:
  - **Username:** (any username from database, e.g., `manager1`, `staff1`)
  - **Password:** `admin123`

---

## 🎯 Default Login Credentials

After running `setup_passwords.js`, you'll see something like:

```
👤 ACTIVE USERS:

   Username: manager1
   Password: admin123
   Name: John Doe
   Role: manager
   ─────────────────────────

   Username: staff1
   Password: admin123
   Name: Jane Smith
   Role: staff
   ─────────────────────────
```

Use any of these usernames with password `admin123`.

---

## 📱 App Setup

The app is already configured to connect to the local API server:
- **Emulator:** `http://10.0.2.2:3000/` ✅
- **Physical Device:** Change URL in `BaneloApiService.kt` to your computer's IP

---

## ✅ Verification

### API Server Running?
```bash
curl http://localhost:3000/api/users
```

Should return JSON with user data.

### Database Connected?
```bash
psql -U postgres -d banelo_db -c "SELECT username, role FROM users LIMIT 5;"
```

Should show users from database.

### Android App?
- Build should succeed without errors
- Login screen should appear
- Login with `admin123` should work

---

## 🐛 Troubleshooting

### "CLEARTEXT communication not permitted"
✅ **FIXED!** Network security config added in:
- `app/src/main/res/xml/network_security_config.xml`
- Allows HTTP to localhost for development

### "Connection refused" or "Cannot connect to server"
1. Check API server is running: `node server.js`
2. For physical device: Update IP in `BaneloApiService.kt` line 161
3. Check firewall isn't blocking port 3000

### "Invalid username or password"
1. Run `node setup_passwords.js` first
2. Use exact password: `admin123`
3. Check API logs for error details

### "bcrypt not found"
```bash
cd api-backend
npm install
```

### Database errors
1. Ensure PostgreSQL is running
2. Database `banelo_db` exists
3. Run: `psql -U postgres -l` to list databases

---

## 📂 Project Structure

```
Banelomobile-main/
├── api-backend/           # Node.js REST API
│   ├── server.js          # Main API server
│   ├── setup_passwords.js # Password setup script
│   └── package.json       # Dependencies
│
├── app/                   # Android app (Kotlin)
│   └── src/main/
│       ├── java/...       # App code
│       ├── res/           # Resources
│       └── AndroidManifest.xml
│
└── migration/             # Database setup
    ├── 02_schema/schema.sql
    └── add_authentication.sql
```

---

## 🔐 Security Features

✅ Bcrypt password hashing
✅ HTTP allowed for local development only
✅ Password verification on every login
✅ Failed login attempt logging
✅ HTTPS ready for production

---

## 📚 Full Documentation

- **Authentication:** `AUTHENTICATION_SETUP.md`
- **Migration:** `README_MIGRATION.md`
- **API Reference:** `API_QUICK_REFERENCE.md`

---

## 🎉 You're Ready!

1. ✅ Database with authentication
2. ✅ API server running
3. ✅ Android app connected
4. ✅ Login working

**Try it now:**
- Open the Android app
- Login with username + password `admin123`
- Start managing your pastry shop! 🥐

---

## ⚠️ For Production

Before deploying to production:

1. **Change all passwords** from `admin123`
2. **Use HTTPS** instead of HTTP
3. **Remove cleartext traffic permission**
4. **Use environment variables** for credentials
5. **Add rate limiting** to API
6. **Enable Firebase Analytics** (optional)
7. **Test on real devices**

