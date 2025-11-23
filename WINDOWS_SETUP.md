# 🪟 Windows Setup Guide - Complete Beginner Version

## You're on Windows! Here's what to do:

---

## ✅ **Option 1: Simple Windows Setup** (Recommended)

### **Step 1: Install Required Software**

#### **1.1 Install Python**
1. Go to https://www.python.org/downloads/
2. Download **Python 3.11** (or latest)
3. **IMPORTANT:** Check ✅ **"Add Python to PATH"** during installation
4. Click "Install Now"

**Verify it worked:**
```cmd
python --version
```
Should show: `Python 3.11.x`

#### **1.2 Install PostgreSQL**
1. Go to https://www.postgresql.org/download/windows/
2. Download the installer (latest version)
3. Run installer
4. **Remember the password you set!** (suggestion: `postgres`)
5. Keep default port: `5432`
6. Install "pgAdmin 4" (check the box)

**Verify it worked:**
```cmd
psql --version
```
Should show: `psql (PostgreSQL) 16.x`

---

### **Step 2: Run the Migration**

Open **Command Prompt** and navigate to your project:

```cmd
cd C:\Users\rommel\Downloads\Banelomobile-main\Banelomobile-main
```

Then run:
```cmd
QUICKSTART.bat
```

**That's it!** The batch file will:
- ✅ Install Python packages
- ✅ Guide you to setup database
- ✅ Generate all data
- ✅ Import everything

---

## 🐧 **Option 2: Install Bash on Windows** (Advanced)

If you want to use the Linux/Mac commands, install one of these:

### **A) Git Bash** (Easiest)
1. Download from https://git-scm.com/download/win
2. Install with default settings
3. Right-click in your folder → "Git Bash Here"
4. Now you can run:
   ```bash
   bash QUICKSTART.sh
   ```

### **B) WSL (Windows Subsystem for Linux)** (More powerful)
1. Open PowerShell as Administrator
2. Run:
   ```powershell
   wsl --install
   ```
3. Restart computer
4. Open "Ubuntu" from Start menu
5. Now you have a full Linux terminal!

---

## 📝 **Manual Step-by-Step** (If scripts don't work)

### **Step 1: Setup PostgreSQL Database**

Open **pgAdmin 4** (installed with PostgreSQL):

1. Connect to PostgreSQL (use password you set during install)
2. Right-click "Databases" → Create → Database
   - Name: `banelo_db`
   - Owner: postgres
3. Right-click "Login/Group Roles" → Create → Login/Group Role
   - General tab → Name: `banelo_user`
   - Definition tab → Password: `banelo_password_2024`
   - Privileges tab → Check all boxes
4. Click Save

**OR use psql command line:**

```cmd
psql -U postgres

-- Then paste these commands:
DROP DATABASE IF EXISTS banelo_db;
DROP USER IF EXISTS banelo_user;
CREATE USER banelo_user WITH PASSWORD 'banelo_password_2024';
CREATE DATABASE banelo_db OWNER banelo_user;
GRANT ALL PRIVILEGES ON DATABASE banelo_db TO banelo_user;
\c banelo_db
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
GRANT ALL ON SCHEMA public TO banelo_user;
\q
```

---

### **Step 2: Install Python Packages**

```cmd
cd C:\Users\rommel\Downloads\Banelomobile-main\Banelomobile-main\migration\01_setup
python -m pip install -r requirements.txt
```

---

### **Step 3: Generate Data**

```cmd
cd ..\03_transform
python transform_dataset.py
```

Wait for it to finish (creates CSV files).

---

### **Step 4: Create Database Schema**

```cmd
cd ..\02_schema
set PGPASSWORD=banelo_password_2024
psql -U banelo_user -d banelo_db -f schema.sql
```

---

### **Step 5: Import Data**

```cmd
cd ..\05_csv_import
set PGPASSWORD=banelo_password_2024
psql -U banelo_user -d banelo_db -f import_all.sql
```

---

### **Step 6: Verify It Worked**

```cmd
set PGPASSWORD=banelo_password_2024
psql -U banelo_user -d banelo_db
```

Then in psql:
```sql
SELECT COUNT(*) FROM products;
SELECT COUNT(*) FROM sales;
\q
```

---

## 🐛 **Common Windows Issues**

### **"python is not recognized"**
**Fix:**
1. Reinstall Python, check ✅ "Add Python to PATH"
2. OR manually add to PATH:
   - Search "Environment Variables" in Windows
   - Edit PATH variable
   - Add: `C:\Users\rommel\AppData\Local\Programs\Python\Python311`

### **"psql is not recognized"**
**Fix:**
1. Add PostgreSQL to PATH:
   - Search "Environment Variables"
   - Edit PATH
   - Add: `C:\Program Files\PostgreSQL\16\bin`
2. Restart Command Prompt

### **"Permission denied" on PostgreSQL**
**Fix:**
- Run Command Prompt as Administrator
- OR use pgAdmin GUI instead

### **"Module not found" errors**
**Fix:**
```cmd
python -m pip install --upgrade pip
python -m pip install pandas numpy psycopg2-binary
```

### **"Encoding errors" with CSV import**
**Fix:**
- Open CSV files in Notepad
- Save As → Encoding: UTF-8

---

## 🎯 **What Should You Do Now?**

### **For Quickest Results:**

1. **Install Python** (if not installed)
   - https://www.python.org/downloads/
   - ✅ Check "Add to PATH"

2. **Install PostgreSQL** (if not installed)
   - https://www.postgresql.org/download/windows/
   - Remember your password!

3. **Run the Windows batch file:**
   ```cmd
   cd C:\Users\rommel\Downloads\Banelomobile-main\Banelomobile-main
   QUICKSTART.bat
   ```

4. **Follow the prompts** (it will guide you)

---

## 💡 **My Recommendation**

**Easiest path for Windows:**
1. Install Python + PostgreSQL (links above)
2. Run `QUICKSTART.bat`
3. Follow on-screen instructions
4. Done in 15 minutes!

**OR if you're comfortable with Linux:**
1. Install Git Bash
2. Run `bash QUICKSTART.sh`
3. Done!

---

## 📞 **Still Stuck?**

**Tell me:**
1. Did Python install? (run `python --version`)
2. Did PostgreSQL install? (run `psql --version`)
3. What error message do you see?

I'll help you fix it! 😊
