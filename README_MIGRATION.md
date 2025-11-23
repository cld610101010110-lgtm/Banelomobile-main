# 🔄 Firestore to PostgreSQL Migration

Complete migration solution for **Banelo Pastry Shop Management System** from Firebase Firestore to PostgreSQL.

---

## 📋 Overview

This migration package provides:

- ✅ **PostgreSQL schema** matching your Firestore structure
- ✅ **Real bakery/recipe data** from Kaggle datasets
- ✅ **Transformation scripts** to convert datasets → PostgreSQL
- ✅ **500+ products**, 10,000+ sales, 300+ recipes
- ✅ **ML-ready datasets** for analytics
- ✅ **Android adapter** for minimal code changes
- ✅ **Complete documentation** (beginner-friendly!)

---

## 🎯 Quick Start

### For Beginners (Never done this before?)

👉 **Start here:** [`migration/BEGINNERS_GUIDE.md`](migration/BEGINNERS_GUIDE.md)

Step-by-step guide assuming **zero prior knowledge**.

### For Developers (Know what you're doing?)

👉 **Quick start:** [`migration/QUICK_REFERENCE.md`](migration/QUICK_REFERENCE.md)

One-page cheat sheet with all commands.

### For Complete Details

👉 **Full guide:** [`migration/README_MIGRATION_GUIDE.md`](migration/README_MIGRATION_GUIDE.md)

Comprehensive documentation with architecture details.

---

## 🚀 5-Minute Setup

```bash
# 1. Setup PostgreSQL
cd migration/01_setup
bash setup_postgres.sh

# 2. Transform data (using sample data)
cd ../03_transform
python3 transform_dataset.py

# 3. Create schema & import
cd ../02_schema
psql -U banelo_user -d banelo_db -f schema.sql

cd ../05_csv_import
psql -U banelo_user -d banelo_db -f import_all.sql

# Done! Test it:
psql -U banelo_user -d banelo_db -c "SELECT COUNT(*) FROM products;"
```

---

## 📊 What You Get

After migration:

| Component | Count | Source |
|-----------|-------|--------|
| **Products** | 500+ | Kaggle Bakery Sales dataset |
| **Recipes** | 300+ | Food.com Recipes dataset |
| **Ingredients** | 2,000+ | Derived from recipes |
| **Sales Transactions** | 10,000+ | Generated from patterns |
| **Waste Logs** | 500+ | Realistic waste data |
| **Users** | 15-20 | Sample staff/managers |

**Plus:**
- Complete PostgreSQL schema with indexes
- 4 ML-ready datasets (sales analysis, waste prediction, inventory forecast, recommendations)
- Android Kotlin adapters
- SQL views for common queries

---

## 📁 Project Structure

```
migration/
├── README_MIGRATION_GUIDE.md      # 📘 Complete guide
├── BEGINNERS_GUIDE.md             # 🎓 Beginner-friendly tutorial
├── QUICK_REFERENCE.md             # ⚡ One-page cheat sheet
│
├── 01_setup/                      # 🛠️ Environment setup
│   ├── requirements.txt           # Python dependencies
│   ├── download_datasets.sh       # Download Kaggle data
│   └── setup_postgres.sh          # PostgreSQL setup
│
├── 02_schema/                     # 🗂️ Database schema
│   └── schema.sql                 # Full PostgreSQL schema
│
├── 03_transform/                  # 🔄 Data transformation
│   ├── transform_dataset.py       # Master orchestrator
│   ├── generate_products.py       # Product catalog
│   ├── generate_recipes.py        # Recipes + ingredients
│   ├── generate_sales.py          # Sales transactions
│   ├── generate_users.py          # User accounts
│   ├── generate_waste_logs.py     # Waste tracking
│   └── utils.py                   # Helper functions
│
├── 04_seed_data/                  # 🌱 Seed data (generated)
│   └── seed_users.sql
│
├── 05_csv_import/                 # 📥 CSV import (generated)
│   ├── import_all.sql             # Bulk import script
│   ├── products.csv
│   ├── recipes.csv
│   ├── recipe_ingredients.csv
│   ├── sales.csv
│   ├── waste_logs.csv
│   └── users.csv
│
├── 06_ml_datasets/                # 🤖 ML-ready datasets
│   ├── generate_ml_datasets.py
│   ├── sales_analysis.csv         # (generated)
│   ├── waste_prediction.csv       # (generated)
│   ├── inventory_forecast.csv     # (generated)
│   └── product_recommendations.csv # (generated)
│
└── 07_android_adapter/            # 📱 Android integration
    ├── PostgresConnection.kt      # PostgreSQL client
    ├── DatabaseAdapter.kt         # Firestore-like API
    └── migration_checklist.md     # Android migration guide
```

---

## 🎓 Documentation Guide

| Document | Audience | Purpose |
|----------|----------|---------|
| **BEGINNERS_GUIDE.md** | Never done migration | Complete tutorial from scratch |
| **README_MIGRATION_GUIDE.md** | General | Full documentation with details |
| **QUICK_REFERENCE.md** | Developers | Fast lookup, all commands |
| **migration_checklist.md** | Android devs | Update Android app |

---

## 🛠️ Prerequisites

| Requirement | Version | Install |
|-------------|---------|---------|
| **PostgreSQL** | 12+ | [postgresql.org](https://www.postgresql.org/download/) |
| **Python** | 3.8+ | [python.org](https://www.python.org/downloads/) |
| **pip** | Latest | `apt install python3-pip` |
| **Kaggle Account** | - | [kaggle.com](https://www.kaggle.com/) (optional) |

---

## 🎯 Migration Strategies

### Strategy 1: Test First (Recommended)

1. ✅ Setup PostgreSQL locally
2. ✅ Import sample data from Kaggle
3. ✅ Test queries and performance
4. ✅ Update Android app (dev build)
5. ✅ Verify everything works
6. ⚡ Deploy to production

### Strategy 2: Gradual Migration

1. ✅ Keep both Firestore and PostgreSQL
2. ✅ Dual-write to both databases
3. ✅ Read from PostgreSQL, fallback to Firestore
4. ✅ Gradually migrate users
5. ⚡ Disable Firestore when ready

### Strategy 3: One-Time Migration

1. ✅ Export Firestore data
2. ✅ Transform to PostgreSQL format
3. ✅ Import to PostgreSQL
4. ✅ Switch Android app
5. ⚡ Go live

---

## 📱 Android App Changes

**Minimal changes needed!**

### Option A: Direct PostgreSQL (Testing)

Add dependency:
```kotlin
implementation("org.postgresql:postgresql:42.6.0")
```

Replace Firestore:
```kotlin
// OLD
firestore.collection("products").get()

// NEW
viewModelScope.launch {
    val products = DatabaseAdapter.getProducts()
}
```

### Option B: REST API (Production)

Create backend:
```javascript
// Node.js example
app.get('/api/products', async (req, res) => {
    const products = await pool.query('SELECT * FROM products');
    res.json(products.rows);
});
```

Android with Retrofit:
```kotlin
interface BaneloApi {
    @GET("api/products")
    suspend fun getProducts(): List<Entity_Products>
}
```

See [`07_android_adapter/migration_checklist.md`](migration/07_android_adapter/migration_checklist.md) for details.

---

## 📊 Database Schema

### Tables

- **users** - Staff and manager accounts
- **products** - Product catalog (Pastries, Beverages, Ingredients)
- **recipes** - Recipe definitions
- **recipe_ingredients** - Recipe components
- **sales** - POS transactions
- **waste_logs** - Waste tracking
- **audit_logs** - Activity audit trail

### Views (Pre-built Queries)

- **v_product_inventory** - Current stock levels
- **v_sales_by_product** - Sales summary
- **v_waste_by_product** - Waste analysis
- **v_recipes_with_ingredients** - Complete recipes

---

## 🎨 Customization

### Change Generated Data

Edit `migration/03_transform/utils.py`:

```python
# Add your products
PASTRIES = [
    "Your Custom Pastry",
    "Another Product",
    # ...
]

# Adjust prices
PRICE_RANGES = {
    "Pastries": (3.00, 10.00),  # min, max
}
```

### Change Record Counts

Edit individual generator scripts:

```python
# generate_products.py
generator.generate_from_dataset(limit=1000)  # Change from 500

# generate_sales.py
generator.generate_from_dataset(limit=50000)  # Change from 10000
```

---

## 🔐 Security Best Practices

- ✅ Change default passwords
- ✅ Use environment variables for credentials
- ✅ Enable SSL for PostgreSQL
- ✅ Restrict IP access (`pg_hba.conf`)
- ✅ Regular automated backups
- ✅ Use read-only users for analytics
- ✅ Never commit credentials to Git

---

## 📈 Performance Tips

```sql
-- Analyze query performance
EXPLAIN ANALYZE SELECT * FROM products WHERE category = 'Pastries';

-- Update statistics
ANALYZE;

-- Rebuild indexes
REINDEX DATABASE banelo_db;

-- Monitor slow queries
SELECT query, mean_exec_time FROM pg_stat_statements ORDER BY mean_exec_time DESC;
```

---

## 🐛 Common Issues

| Problem | Solution |
|---------|----------|
| Can't connect to PostgreSQL | Check service is running: `sudo systemctl status postgresql` |
| Permission denied | Grant user permissions: `ALTER USER banelo_user WITH SUPERUSER;` |
| CSV import fails | Check you're in `05_csv_import/` directory |
| Python module not found | Install requirements: `pip3 install -r 01_setup/requirements.txt` |

See [BEGINNERS_GUIDE.md](migration/BEGINNERS_GUIDE.md#troubleshooting) for more.

---

## 📚 Resources

- **PostgreSQL Docs:** https://www.postgresql.org/docs/
- **SQL Tutorial:** https://www.postgresqltutorial.com/
- **Python Pandas:** https://pandas.pydata.org/docs/
- **Android JDBC:** https://github.com/pgjdbc/pgjdbc

---

## 🤝 Contributing

Found an issue? Want to improve the migration scripts?

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

---

## 📝 License

This migration package is provided as-is for the Banelo project.

---

## 🎉 Success Metrics

After migration, you should have:

- ✅ PostgreSQL database with 13,000+ records
- ✅ Android app successfully connecting
- ✅ All CRUD operations working
- ✅ Analytics queries running fast (< 100ms)
- ✅ Automated backups configured
- ✅ Monitoring in place

---

## 📞 Support

**Need help?**

1. Check the documentation guides
2. Search existing issues
3. Ask on Stack Overflow (tag: `postgresql`, `android`)
4. Create an issue in this repository

---

**Ready to migrate? Start with [BEGINNERS_GUIDE.md](migration/BEGINNERS_GUIDE.md)!**

🚀 Happy migrating!
