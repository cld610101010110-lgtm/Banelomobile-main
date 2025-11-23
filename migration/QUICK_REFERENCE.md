# ⚡ Quick Reference Card

One-page cheat sheet for the Firestore → PostgreSQL migration.

---

## 🚀 Quick Start (TL;DR)

```bash
# 1. Install dependencies
cd migration/01_setup
pip3 install -r requirements.txt

# 2. Download datasets
bash download_datasets.sh

# 3. Setup PostgreSQL
bash setup_postgres.sh

# 4. Transform data
cd ../03_transform
python3 transform_dataset.py

# 5. Create schema
cd ../02_schema
psql -U banelo_user -d banelo_db -f schema.sql

# 6. Import data
cd ../05_csv_import
psql -U banelo_user -d banelo_db -f import_all.sql

# 7. Generate ML datasets
cd ../06_ml_datasets
python3 generate_ml_datasets.py

# Done! 🎉
```

---

## 📊 Database Connection

```bash
# Connect to PostgreSQL
psql -U banelo_user -d banelo_db

# Connection string for apps
postgresql://banelo_user:banelo_password_2024@localhost:5432/banelo_db
```

---

## 📁 File Structure

```
migration/
├── README_MIGRATION_GUIDE.md      # Main guide
├── BEGINNERS_GUIDE.md             # For newcomers
├── QUICK_REFERENCE.md             # This file
├── 01_setup/
│   ├── requirements.txt           # Python deps
│   ├── download_datasets.sh       # Get Kaggle data
│   └── setup_postgres.sh          # DB setup
├── 02_schema/
│   └── schema.sql                 # Database structure
├── 03_transform/
│   ├── transform_dataset.py       # Main script
│   ├── generate_products.py       # Product generator
│   ├── generate_recipes.py        # Recipe generator
│   ├── generate_sales.py          # Sales generator
│   ├── generate_users.py          # User generator
│   ├── generate_waste_logs.py     # Waste log generator
│   └── utils.py                   # Helper functions
├── 04_seed_data/
│   └── [Generated seed files]
├── 05_csv_import/
│   ├── import_all.sql             # Bulk import
│   └── [Generated CSV files]
├── 06_ml_datasets/
│   ├── generate_ml_datasets.py
│   └── [Generated ML CSVs]
└── 07_android_adapter/
    ├── PostgresConnection.kt      # DB connection
    ├── DatabaseAdapter.kt         # Firestore-like API
    └── migration_checklist.md     # Android guide
```

---

## 🗂️ Database Tables

| Table | Description | Key Fields |
|-------|-------------|------------|
| **users** | Staff accounts | `username`, `role`, `status` |
| **products** | Product catalog | `name`, `category`, `price`, `inventory_a`, `inventory_b` |
| **recipes** | Recipe definitions | `product_name`, `instructions` |
| **recipe_ingredients** | Recipe components | `ingredient_name`, `quantity_needed`, `unit` |
| **sales** | POS transactions | `product_name`, `quantity`, `price`, `payment_mode` |
| **waste_logs** | Waste tracking | `product_name`, `quantity`, `reason`, `cost_impact` |
| **audit_logs** | Activity trail | `username`, `action`, `description` |

---

## 🔍 Useful SQL Queries

```sql
-- Product inventory
SELECT * FROM v_product_inventory;

-- Sales summary
SELECT * FROM v_sales_by_product ORDER BY total_revenue DESC;

-- Waste analysis
SELECT * FROM v_waste_by_product ORDER BY total_cost_impact DESC;

-- Today's sales
SELECT * FROM sales WHERE DATE(order_date) = CURRENT_DATE;

-- Low stock products
SELECT name, category, quantity FROM products WHERE quantity < 50;

-- Top 10 sellers
SELECT product_name, SUM(quantity) as sold, SUM(quantity * price) as revenue
FROM sales GROUP BY product_name ORDER BY revenue DESC LIMIT 10;

-- Monthly revenue
SELECT DATE_TRUNC('month', order_date) as month, SUM(quantity * price) as revenue
FROM sales GROUP BY month ORDER BY month;

-- Waste by category
SELECT category, SUM(cost_impact) as total_waste
FROM waste_logs GROUP BY category ORDER BY total_waste DESC;
```

---

## 🛠️ Common Commands

### PostgreSQL Management

```bash
# Start PostgreSQL
sudo systemctl start postgresql

# Stop PostgreSQL
sudo systemctl stop postgresql

# Restart PostgreSQL
sudo systemctl restart postgresql

# Check status
sudo systemctl status postgresql

# List databases
psql -U banelo_user -d postgres -c "\l"

# Backup database
pg_dump -U banelo_user banelo_db > backup_$(date +%Y%m%d).sql

# Restore database
psql -U banelo_user banelo_db < backup.sql

# Drop and recreate database
psql -U postgres -c "DROP DATABASE banelo_db;"
psql -U postgres -c "CREATE DATABASE banelo_db OWNER banelo_user;"
```

### Data Management

```bash
# Re-import CSV data
cd migration/05_csv_import
psql -U banelo_user -d banelo_db -c "TRUNCATE users, products, recipes, recipe_ingredients, sales, waste_logs CASCADE;"
psql -U banelo_user -d banelo_db -f import_all.sql

# Regenerate all data
cd migration/03_transform
python3 transform_dataset.py
cd ../05_csv_import
psql -U banelo_user -d banelo_db -f import_all.sql
```

---

## 🔧 Customization

### Change number of generated records

Edit `migration/03_transform/transform_dataset.py`:

```python
# Find these lines and change the limits
('generate_users.py', '👥 User Generation'),           # Edit file: num_staff=15
('generate_products.py', '🛍️  Product Generation'),    # Edit file: limit=500
('generate_recipes.py', '👨‍🍳 Recipe Generation'),       # Edit file: limit=300
('generate_sales.py', '💳 Sales Generation'),          # Edit file: limit=10000
('generate_waste_logs.py', '🗑️  Waste Generation'),   # Edit file: num_logs=500
```

### Add custom products

Edit `migration/03_transform/utils.py`:

```python
class PastryNameGenerator:
    PASTRIES = [
        "Your Custom Product",
        "Another Product",
        # ... add more
    ]
```

### Change price ranges

Edit `migration/03_transform/utils.py`:

```python
class PriceGenerator:
    PRICE_RANGES = {
        "Pastries": (2.50, 8.50),      # min, max
        "Beverages": (2.00, 6.50),
        "Ingredients": (1.00, 15.00)
    }
```

---

## 📱 Android Migration

### Minimal changes needed

**Step 1:** Add dependency to `app/build.gradle.kts`
```kotlin
implementation("org.postgresql:postgresql:42.6.0")
```

**Step 2:** Copy adapter files
```
07_android_adapter/PostgresConnection.kt → app/src/main/java/.../database/
07_android_adapter/DatabaseAdapter.kt → app/src/main/java/.../database/
```

**Step 3:** Update server IP
```kotlin
// In PostgresConnection.kt
private const val DB_HOST = "192.168.1.100"  // Your PostgreSQL server
```

**Step 4:** Replace Firestore calls
```kotlin
// OLD
firestore.collection("products").get()

// NEW
viewModelScope.launch {
    val products = DatabaseAdapter.getProducts()
}
```

---

## 🐛 Troubleshooting

| Error | Solution |
|-------|----------|
| Connection refused | Check PostgreSQL is running: `sudo systemctl status postgresql` |
| Permission denied | Grant permissions: `ALTER USER banelo_user WITH SUPERUSER;` |
| CSV not found | Run from correct directory: `cd migration/05_csv_import` |
| Python module not found | Install: `pip3 install -r migration/01_setup/requirements.txt` |
| Kaggle download fails | Check `~/.kaggle/kaggle.json` exists and has correct permissions |

---

## 📊 Expected Data Counts

After full migration:

```
users:              15-20 records
products:           500 records
recipes:            300 records
recipe_ingredients: ~2,000 records
sales:              10,000 records
waste_logs:         500 records
```

---

## ⚡ Performance Tips

```sql
-- Analyze tables for better query planning
ANALYZE;

-- Rebuild indexes
REINDEX DATABASE banelo_db;

-- Check table sizes
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_tables WHERE schemaname = 'public';

-- Find slow queries (enable in postgresql.conf)
SELECT query, mean_exec_time, calls FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;
```

---

## 🔐 Security Checklist

- [ ] Change default password
- [ ] Use environment variables for credentials
- [ ] Enable SSL connections
- [ ] Restrict IP access in `pg_hba.conf`
- [ ] Regular backups
- [ ] Use read-only user for analytics
- [ ] Never commit credentials to Git

---

## 📚 Resources

- **PostgreSQL Docs:** https://www.postgresql.org/docs/
- **SQL Tutorial:** https://www.postgresqltutorial.com/
- **Kaggle Datasets:** https://www.kaggle.com/datasets
- **Android JDBC:** https://developer.android.com/

---

## 🆘 Getting Help

1. Check `BEGINNERS_GUIDE.md` for detailed explanations
2. Read `README_MIGRATION_GUIDE.md` for full documentation
3. Search error messages on Stack Overflow
4. Check PostgreSQL logs: `sudo tail -f /var/log/postgresql/postgresql-15-main.log`

---

**Last Updated:** 2024
**Version:** 1.0
