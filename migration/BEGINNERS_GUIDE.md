# 🎓 Complete Beginner's Guide to Firestore → PostgreSQL Migration

**"I don't know anything about databases or migration. Help!"**

Don't worry! This guide assumes **zero prior knowledge** and will walk you through everything step by step.

---

## 📚 Table of Contents

1. [What is this migration about?](#what-is-this-migration-about)
2. [What you need (Prerequisites)](#what-you-need)
3. [Understanding the basics](#understanding-the-basics)
4. [Step-by-step migration](#step-by-step-migration)
5. [Troubleshooting](#troubleshooting)
6. [FAQs](#faqs)

---

## 🤔 What is this migration about?

### Your Current Setup (Firestore)
Your Banelo Pastry Shop app currently stores data in **Firebase Firestore**, which is Google's cloud database.

```
Your Android App ←→ Firebase Firestore (Google's Cloud)
```

**Problems with Firestore:**
- 💰 Expensive as you grow (pays per read/write)
- ⚡ Limited query capabilities
- 📊 Hard to do complex analytics

### What We're Migrating To (PostgreSQL)
**PostgreSQL** is a traditional database that you host yourself.

```
Your Android App ←→ Your Server ←→ PostgreSQL Database
```

**Benefits:**
- 💵 Fixed cost (hosting only)
- 🚀 Powerful queries and joins
- 📈 Better for analytics and reporting
- 🔧 Full control over your data

---

## 🛠️ What You Need

### 1. A Computer/Server to Run PostgreSQL

**Options:**

**Option A: Your Own Computer (Easiest for testing)**
- Ubuntu/Linux PC
- macOS (via Homebrew)
- Windows (via WSL2 or native installer)

**Option B: Cloud Server (For production)**
- DigitalOcean ($6/month) - [Sign up](https://www.digitalocean.com/)
- AWS EC2 (Free tier available) - [Sign up](https://aws.amazon.com/)
- Google Cloud (Free tier available) - [Sign up](https://cloud.google.com/)
- Heroku (Easy setup) - [Sign up](https://www.heroku.com/)

**Recommendation for beginners:** Start with your own computer, then move to a cloud server later.

### 2. Software to Install

| Software | Purpose | Download Link |
|----------|---------|---------------|
| **PostgreSQL** | The database | [postgresql.org](https://www.postgresql.org/download/) |
| **Python 3.8+** | Run transformation scripts | [python.org](https://www.python.org/downloads/) |
| **Git** | Download this code | [git-scm.com](https://git-scm.com/downloads) |
| **Kaggle Account** | Download datasets | [kaggle.com](https://www.kaggle.com/) |

### 3. Basic Command Line Knowledge

You'll be using the terminal/command prompt. Don't worry - we'll show you exactly what to type!

**Windows:** Open "Command Prompt" or "PowerShell"
**Mac:** Open "Terminal" (Applications → Utilities → Terminal)
**Linux:** Open your terminal app

---

## 📖 Understanding the Basics

### What is a Database Migration?

Think of it like moving houses:
1. **Old house (Firestore)**: Where your data currently lives
2. **New house (PostgreSQL)**: Where you want to move your data
3. **Moving truck (Migration scripts)**: Tools to transfer everything
4. **Change address (Android app update)**: Tell your app where to find data

### What is a Schema?

A **schema** is like a blueprint for your database. It defines:
- What tables exist (like `products`, `sales`, `users`)
- What fields each table has (like `name`, `price`, `quantity`)
- What type of data each field holds (text, numbers, dates)

### What is CSV?

**CSV** = "Comma Separated Values"

It's a simple spreadsheet format:
```csv
name,price,quantity
Croissant,3.50,100
Coffee,2.00,50
```

We'll transform Kaggle datasets into CSV files, then import them into PostgreSQL.

---

## 🚀 Step-by-Step Migration

### Phase 1: Setup Your Environment (30 minutes)

#### Step 1.1: Install PostgreSQL

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

**macOS:**
```bash
brew install postgresql@15
brew services start postgresql@15
```

**Windows:**
1. Download installer from [postgresql.org](https://www.postgresql.org/download/windows/)
2. Run installer
3. Remember your password!
4. Default port: 5432

**Verify installation:**
```bash
psql --version
```
You should see: `psql (PostgreSQL) 15.x`

#### Step 1.2: Install Python

**Check if already installed:**
```bash
python3 --version
```

If not installed:
- **Ubuntu/Debian:** `sudo apt install python3 python3-pip`
- **macOS:** `brew install python3`
- **Windows:** Download from [python.org](https://www.python.org/downloads/)

#### Step 1.3: Install Python Dependencies

Navigate to the migration folder:
```bash
cd migration/01_setup
pip3 install -r requirements.txt
```

This installs tools for:
- Connecting to PostgreSQL (`psycopg2`)
- Processing data (`pandas`, `numpy`)
- Downloading datasets (`kaggle`)

#### Step 1.4: Setup Kaggle API

1. Go to [kaggle.com](https://www.kaggle.com/) and create account
2. Click your profile picture → "Settings"
3. Scroll to "API" section
4. Click "Create New Token"
5. Download `kaggle.json`

Place the file:
```bash
mkdir -p ~/.kaggle
mv ~/Downloads/kaggle.json ~/.kaggle/
chmod 600 ~/.kaggle/kaggle.json
```

---

### Phase 2: Download Datasets (15 minutes)

```bash
cd migration/01_setup
bash download_datasets.sh
```

This downloads:
- **French Bakery Sales** (~234K transactions) - For products and sales
- **Food.com Recipes** (~230K recipes) - For recipes and ingredients

**What if it fails?**
- Check Kaggle API credentials
- Manually download from:
  - [Bakery Sales](https://www.kaggle.com/datasets/matthieugimbert/french-bakery-daily-sales)
  - [Food.com Recipes](https://www.kaggle.com/datasets/shuyangli94/food-com-recipes-and-interactions)
- Extract to `migration/datasets/` folder

---

### Phase 3: Setup PostgreSQL Database (10 minutes)

```bash
cd migration/01_setup
bash setup_postgres.sh
```

This creates:
- Database: `banelo_db`
- User: `banelo_user`
- Password: `banelo_password_2024`

**Test the connection:**
```bash
psql -U banelo_user -d banelo_db
```

You should see:
```
banelo_db=>
```

Type `\q` to exit.

**Troubleshooting:**
If you get "peer authentication failed":
```bash
sudo nano /etc/postgresql/15/main/pg_hba.conf
```
Change `peer` to `md5`, then:
```bash
sudo systemctl restart postgresql
```

---

### Phase 4: Transform Datasets (20 minutes)

This converts Kaggle data into your PostgreSQL format.

```bash
cd migration/03_transform
python3 transform_dataset.py
```

**What this does:**
1. ✅ Creates 15 users (admin, managers, staff)
2. ✅ Generates 500 products (pastries, beverages, ingredients)
3. ✅ Creates 300 recipes with ingredients
4. ✅ Generates 10,000 sales transactions
5. ✅ Creates 500 waste log entries

**Output:**
```
📁 Generated 5 CSV files:
  • users.csv
  • products.csv
  • recipes.csv
  • recipe_ingredients.csv
  • sales.csv
  • waste_logs.csv
```

**You can customize:**
Edit each `generate_*.py` file to change:
- Number of records
- Product names
- Price ranges
- Date ranges

---

### Phase 5: Create Database Schema (5 minutes)

Create the database structure:

```bash
cd migration/02_schema
psql -U banelo_user -d banelo_db -f schema.sql
```

**What this creates:**
- 7 tables: `users`, `products`, `recipes`, `recipe_ingredients`, `sales`, `waste_logs`, `audit_logs`
- Indexes for fast queries
- Views for common reports
- Automatic timestamp updates

**Verify:**
```bash
psql -U banelo_user -d banelo_db
```

```sql
-- List all tables
\dt

-- See products table structure
\d products
```

---

### Phase 6: Import CSV Data (10 minutes)

```bash
cd migration/05_csv_import
psql -U banelo_user -d banelo_db -f import_all.sql
```

**This imports:**
```
✅ Users: 15 records
✅ Products: 500 records
✅ Recipes: 300 records
✅ Recipe Ingredients: ~2,000 records
✅ Sales: 10,000 records
✅ Waste Logs: 500 records
```

**Verify the import:**
```sql
-- Count all records
SELECT 'products' as table_name, COUNT(*) FROM products
UNION ALL
SELECT 'sales', COUNT(*) FROM sales
UNION ALL
SELECT 'users', COUNT(*) FROM users;
```

---

### Phase 7: Generate ML Datasets (5 minutes)

```bash
cd migration/06_ml_datasets
python3 generate_ml_datasets.py
```

Creates analytics-ready CSVs:
- `sales_analysis.csv` - Daily sales by product
- `waste_prediction.csv` - Waste patterns
- `inventory_forecast.csv` - Reorder points
- `product_recommendations.csv` - Products bought together

---

### Phase 8: Test Your Database (10 minutes)

Connect to PostgreSQL:
```bash
psql -U banelo_user -d banelo_db
```

**Try these queries:**

```sql
-- See all products
SELECT name, category, price, quantity FROM products LIMIT 10;

-- Check sales summary
SELECT * FROM v_sales_by_product LIMIT 10;

-- See waste by product
SELECT * FROM v_waste_by_product LIMIT 10;

-- Total revenue
SELECT
    SUM(quantity * price) as total_revenue,
    COUNT(*) as total_transactions
FROM sales;

-- Top selling products
SELECT
    product_name,
    SUM(quantity) as total_sold,
    SUM(quantity * price) as revenue
FROM sales
GROUP BY product_name
ORDER BY revenue DESC
LIMIT 10;

-- Products low on stock
SELECT name, category, quantity, inventory_a, inventory_b
FROM products
WHERE quantity < 50
ORDER BY quantity;
```

---

## 🔧 Updating Your Android App

### Option 1: Keep it Simple (Testing Only)

Use the provided adapter for direct PostgreSQL access:

1. Copy files to your Android project:
```
migration/07_android_adapter/PostgresConnection.kt
migration/07_android_adapter/DatabaseAdapter.kt
```

2. Add dependency to `app/build.gradle.kts`:
```kotlin
implementation("org.postgresql:postgresql:42.6.0")
```

3. Update connection in `PostgresConnection.kt`:
```kotlin
private const val DB_HOST = "YOUR_SERVER_IP"  // e.g., "192.168.1.100"
```

4. Replace Firestore calls:
```kotlin
// OLD
firestore.collection("products").get()

// NEW
viewModelScope.launch {
    val products = DatabaseAdapter.getProducts()
}
```

**⚠️ This is for TESTING ONLY!** See next option for production.

### Option 2: Production-Ready (Recommended)

Create a backend API server:

**Quick Start with Node.js:**
```javascript
// server.js
const express = require('express');
const { Pool } = require('pg');

const app = express();
const pool = new Pool({
    connectionString: process.env.DATABASE_URL
});

app.get('/api/products', async (req, res) => {
    const result = await pool.query('SELECT * FROM products WHERE is_active = true');
    res.json(result.rows);
});

app.post('/api/sales', async (req, res) => {
    const { productId, quantity, price } = req.body;
    await pool.query(
        'INSERT INTO sales (product_firebase_id, quantity, price, order_date) VALUES ($1, $2, $3, NOW())',
        [productId, quantity, price]
    );
    res.json({ success: true });
});

app.listen(3000);
```

**Android with Retrofit:**
```kotlin
interface BaneloApi {
    @GET("api/products")
    suspend fun getProducts(): List<Entity_Products>
}

val retrofit = Retrofit.Builder()
    .baseUrl("http://your-server.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val api = retrofit.create(BaneloApi::class.java)
```

---

## 🐛 Troubleshooting

### "Connection refused" Error

**Problem:** Can't connect to PostgreSQL

**Solutions:**
1. Check PostgreSQL is running:
   ```bash
   sudo systemctl status postgresql
   ```

2. Allow remote connections (edit `postgresql.conf`):
   ```
   listen_addresses = '*'
   ```

3. Allow your IP (edit `pg_hba.conf`):
   ```
   host all all 0.0.0.0/0 md5
   ```

4. Restart PostgreSQL:
   ```bash
   sudo systemctl restart postgresql
   ```

### "Permission denied" Error

**Problem:** Can't create database or tables

**Solution:**
```bash
sudo -u postgres psql
```

Then:
```sql
ALTER USER banelo_user WITH SUPERUSER;
```

### "CSV import failed" Error

**Problem:** CSV file not found

**Solution:**
Make sure you're in the right directory:
```bash
cd migration/05_csv_import
ls *.csv  # Should show all CSV files
```

### "Out of memory" Error

**Problem:** Dataset too large for Python

**Solution:**
Edit transform scripts to generate fewer records:
```python
# In generate_sales.py, change:
generator.generate_from_dataset(limit=1000)  # Instead of 10000
```

---

## ❓ Frequently Asked Questions

### Q: Do I need to keep Firestore?

**A:** During migration, yes (for backup). After successful migration and testing, you can disable it.

### Q: Can I use this for other types of businesses?

**A:** Yes! Just modify the product names and categories in the transformation scripts.

### Q: How much will this cost to run?

**A:**
- Self-hosted (your computer): Free
- DigitalOcean droplet: ~$6/month
- AWS Free tier: Free for 1 year

### Q: Is my data safe?

**A:** Yes, PostgreSQL is enterprise-grade. But **always backup!**
```bash
pg_dump -U banelo_user banelo_db > backup.sql
```

### Q: Can I undo the migration?

**A:** Yes! Your Firestore data is unchanged. You can switch back anytime.

### Q: What if I get stuck?

**A:**
1. Check the error message carefully
2. Look in the Troubleshooting section above
3. Search the error on Google/StackOverflow
4. Ask in programming forums (r/PostgreSQL, r/androiddev)

### Q: How do I add my own real data?

**A:** Two ways:
1. **Manual:** Insert via SQL
   ```sql
   INSERT INTO products (id, name, category, price, quantity, inventory_a, inventory_b, cost_per_unit)
   VALUES (gen_random_uuid(), 'My Product', 'Pastries', 5.99, 100, 80, 20, 2.50);
   ```

2. **CSV Import:** Create your own CSV, then:
   ```sql
   \COPY products(id, name, category, price, quantity) FROM 'my_products.csv' WITH CSV HEADER;
   ```

---

## 🎉 Success Checklist

After completing all steps, you should have:

- ✅ PostgreSQL installed and running
- ✅ Database `banelo_db` created
- ✅ 7 tables with data
- ✅ 500+ products in catalog
- ✅ 10,000+ sales transactions
- ✅ 300+ recipes
- ✅ ML-ready datasets generated
- ✅ Can run SQL queries
- ✅ Android app can connect (testing)

**Congratulations! 🎊 You've successfully migrated to PostgreSQL!**

---

## 📚 Next Steps

1. **Learn SQL:**
   - [PostgreSQL Tutorial](https://www.postgresqltutorial.com/)
   - [SQL in 100 Minutes](https://www.youtube.com/watch?v=zsjvFFKOm3c)

2. **Optimize Performance:**
   - Add more indexes
   - Use EXPLAIN to analyze queries
   - Enable query caching

3. **Build Analytics Dashboard:**
   - Use Metabase (free, open source)
   - Connect to your PostgreSQL
   - Create charts and reports

4. **Secure Your Database:**
   - Change default password
   - Use SSL connections
   - Restrict IP access
   - Regular backups

5. **Deploy to Production:**
   - Choose cloud provider
   - Setup automatic backups
   - Configure monitoring
   - Implement API backend

---

## 🆘 Need More Help?

**Resources:**
- PostgreSQL Documentation: https://www.postgresql.org/docs/
- Stack Overflow: https://stackoverflow.com/questions/tagged/postgresql
- Reddit: r/PostgreSQL, r/androiddev

**This migration guide:**
- GitHub: (Link to your repo)
- Issues: Report problems or ask questions

---

**Good luck with your migration! 🚀**

Remember: Take your time, test thoroughly, and don't delete Firestore data until you're 100% confident everything works!
