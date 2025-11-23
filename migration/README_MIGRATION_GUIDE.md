# 🔄 Firestore to PostgreSQL Migration Guide

## 📋 Overview

This guide will help you migrate your **Banelo Pastry Shop Management System** from Firebase Firestore to PostgreSQL using a real-world dataset from Kaggle.

### What You'll Get
- ✅ PostgreSQL schema matching your current Firestore structure
- ✅ Real bakery/food data from Kaggle
- ✅ Transformation scripts to convert dataset → PostgreSQL
- ✅ Seed data for testing
- ✅ CSV files for bulk import
- ✅ ML-ready datasets for analytics
- ✅ Step-by-step instructions (no prior knowledge needed!)

---

## 🎯 Your Current Data Structure

Your app manages a pastry shop with these entities:

| Entity | Purpose | Records |
|--------|---------|---------|
| **Products** | Beverages, Pastries, Ingredients | Catalog items |
| **Recipes** | Recipe formulas | Product recipes |
| **Recipe Ingredients** | Ingredient mappings | Recipe components |
| **Users** | Staff & Managers | User accounts |
| **Sales** | POS transactions | Sales records |
| **Waste Logs** | Waste tracking | Waste entries |
| **Audit Logs** | Activity tracking | Audit trail |

---

## 📊 Recommended Dataset

**Dataset**: [Food.com Recipes and Interactions](https://www.kaggle.com/datasets/shuyangli94/food-com-recipes-and-interactions)
- **Size**: 230K+ recipes, 1M+ interactions
- **Format**: CSV files
- **Content**: Recipe names, ingredients, nutrition, ratings
- **Perfect for**: Bakery products, ingredients, recipes

**Alternative Dataset**: [Bakery Sales Dataset](https://www.kaggle.com/datasets/matthieugimbert/french-bakery-daily-sales)
- **Size**: 234K+ transactions
- **Format**: CSV
- **Content**: Bakery products, sales transactions, dates
- **Perfect for**: Sales data, product catalog

We'll use **BOTH** datasets:
- Food.com → Products, Recipes, Ingredients
- Bakery Sales → Sales transactions, product names

---

## 🗂️ Migration Files Structure

```
migration/
├── README_MIGRATION_GUIDE.md          # This guide
├── 01_setup/
│   ├── requirements.txt               # Python dependencies
│   ├── download_datasets.sh           # Download Kaggle datasets
│   └── setup_postgres.sh              # PostgreSQL setup
├── 02_schema/
│   ├── schema.sql                     # Full PostgreSQL schema
│   ├── indexes.sql                    # Performance indexes
│   └── views.sql                      # Useful views
├── 03_transform/
│   ├── transform_dataset.py           # Main transformation script
│   ├── generate_products.py           # Product catalog generator
│   ├── generate_recipes.py            # Recipe generator
│   ├── generate_sales.py              # Sales data generator
│   ├── generate_users.py              # User seed data
│   └── utils.py                       # Helper functions
├── 04_seed_data/
│   ├── seed_users.sql                 # Initial users
│   ├── seed_categories.sql            # Product categories
│   └── seed_master_data.sql           # Master data
├── 05_csv_import/
│   ├── products.csv                   # Product catalog
│   ├── recipes.csv                    # Recipes
│   ├── recipe_ingredients.csv         # Ingredients
│   ├── sales.csv                      # Sales transactions
│   ├── waste_logs.csv                 # Waste tracking
│   └── import_all.sql                 # Bulk import script
├── 06_ml_datasets/
│   ├── sales_analysis.csv             # Sales analytics
│   ├── waste_prediction.csv           # Waste prediction
│   ├── inventory_forecast.csv         # Inventory forecasting
│   └── product_recommendations.csv    # Product recommendations
└── 07_android_adapter/
    ├── PostgresConnection.kt          # Kotlin PostgreSQL client
    ├── DatabaseAdapter.kt             # Firestore → Postgres adapter
    └── migration_checklist.md         # App changes needed
```

---

## 🚀 Quick Start (5 Steps)

### Step 1: Install Prerequisites

```bash
# Install PostgreSQL (Ubuntu/Debian)
sudo apt update
sudo apt install postgresql postgresql-contrib

# Install Python 3.8+
sudo apt install python3 python3-pip

# Install Kaggle CLI
pip3 install kaggle pandas numpy sqlalchemy psycopg2-binary
```

### Step 2: Download Datasets

```bash
# Setup Kaggle API credentials
mkdir -p ~/.kaggle
# Download your kaggle.json from https://www.kaggle.com/settings
# Place it in ~/.kaggle/kaggle.json
chmod 600 ~/.kaggle/kaggle.json

# Run download script
cd migration/01_setup
bash download_datasets.sh
```

### Step 3: Setup PostgreSQL

```bash
cd migration/01_setup
bash setup_postgres.sh
```

This creates:
- Database: `banelo_db`
- User: `banelo_user`
- Password: `banelo_password_2024`

### Step 4: Transform Dataset & Import

```bash
# Transform Kaggle data → PostgreSQL format
cd migration/03_transform
python3 transform_dataset.py

# Import to PostgreSQL
cd migration/02_schema
psql -U banelo_user -d banelo_db -f schema.sql

cd ../05_csv_import
psql -U banelo_user -d banelo_db -f import_all.sql
```

### Step 5: Verify Data

```bash
psql -U banelo_user -d banelo_db

-- Check record counts
SELECT 'products' as table_name, COUNT(*) FROM products
UNION ALL
SELECT 'recipes', COUNT(*) FROM recipes
UNION ALL
SELECT 'sales', COUNT(*) FROM sales;
```

---

## 📱 Android App Changes

### What Needs to Change?

1. **Add PostgreSQL dependency** to `build.gradle.kts`
2. **Replace Firestore calls** with PostgreSQL queries
3. **Update repositories** to use PostgreSQL adapter
4. **Keep Room database** for offline support (hybrid approach)

### Migration Strategy

**Option A: Complete Replacement** (Recommended for new deployments)
- Remove Firebase dependencies
- Use PostgreSQL as primary database
- Keep Room for offline caching

**Option B: Gradual Migration** (Recommended for existing users)
- Keep both Firestore and PostgreSQL
- Dual-write to both databases
- Gradually migrate users to PostgreSQL

---

## 🔧 PostgreSQL Schema Design

### Key Design Principles

1. **Same field names** as Firestore (easy migration)
2. **UUID primary keys** (like Firebase document IDs)
3. **Proper foreign keys** (referential integrity)
4. **Indexes** on frequently queried fields
5. **JSONB** for flexible fields (Firebase compatibility)

### Schema Highlights

```sql
-- Products table (matches Firestore structure)
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_id VARCHAR(255) UNIQUE,  -- For migration tracking
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    quantity INTEGER DEFAULT 0,
    inventory_a INTEGER DEFAULT 0,    -- Warehouse inventory
    inventory_b INTEGER DEFAULT 0,    -- Display inventory
    cost_per_unit DECIMAL(10,2) DEFAULT 0.0,
    image_uri TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Recipes table
CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_id VARCHAR(255) UNIQUE,
    product_firebase_id UUID REFERENCES products(id),
    product_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sales table
CREATE TABLE sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_id VARCHAR(255) UNIQUE,
    product_name VARCHAR(255),
    category VARCHAR(100),
    quantity INTEGER,
    price DECIMAL(10,2),
    order_date TIMESTAMP,
    product_firebase_id UUID REFERENCES products(id),
    payment_mode VARCHAR(50) DEFAULT 'Cash',
    gcash_reference_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 📈 ML-Ready Datasets

We'll generate these CSV files for machine learning:

### 1. Sales Analysis (`sales_analysis.csv`)
```csv
date,product_id,product_name,category,quantity_sold,revenue,day_of_week,month,year
2024-01-01,uuid-123,Chocolate Cake,Pastries,15,750.00,Monday,1,2024
```

### 2. Waste Prediction (`waste_prediction.csv`)
```csv
date,product_id,product_name,category,quantity_wasted,waste_cost,reason,day_of_week
2024-01-01,uuid-123,Croissant,Pastries,5,25.00,End of day waste,Monday
```

### 3. Inventory Forecast (`inventory_forecast.csv`)
```csv
product_id,product_name,current_stock,avg_daily_sales,reorder_point,lead_time_days
uuid-123,Baguette,50,30,40,2
```

### 4. Product Recommendations (`product_recommendations.csv`)
```csv
product_a_id,product_b_id,co_purchase_count,confidence_score
uuid-123,uuid-456,150,0.85
```

---

## 🎓 Detailed Step-by-Step Guide

### Understanding PostgreSQL vs Firestore

| Aspect | Firestore | PostgreSQL |
|--------|-----------|------------|
| **Type** | NoSQL (Document DB) | SQL (Relational DB) |
| **Structure** | Collections → Documents | Tables → Rows |
| **Schema** | Schema-less | Schema required |
| **Queries** | Limited (no joins) | Full SQL (joins, aggregations) |
| **IDs** | Auto-generated strings | UUIDs or integers |
| **Relationships** | Manual (document refs) | Foreign keys (automatic) |
| **Offline** | Built-in | Requires client library |
| **Cost** | Per read/write | Fixed (hosting) |

### Why Migrate to PostgreSQL?

1. **Cost** - Firestore charges per operation, PostgreSQL is fixed cost
2. **Queries** - Full SQL support (complex analytics)
3. **Relationships** - Proper foreign keys and joins
4. **Data Integrity** - ACID transactions
5. **Reporting** - Better for business intelligence tools
6. **ML** - Easier to export data for machine learning

---

## 🔐 Security & Best Practices

### Database Security

```sql
-- Create read-only user for analytics
CREATE USER analytics_user WITH PASSWORD 'analytics_pass';
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics_user;

-- Create app user with limited permissions
CREATE USER app_user WITH PASSWORD 'app_pass';
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO app_user;
```

### Connection Security

- ✅ Use SSL/TLS for connections
- ✅ Store credentials in environment variables
- ✅ Use connection pooling (PgBouncer)
- ✅ Limit database user permissions

---

## 📞 Need Help?

### Common Issues

**Issue**: "psql: FATAL: Peer authentication failed"
**Solution**: Edit `/etc/postgresql/*/main/pg_hba.conf`, change `peer` to `md5`

**Issue**: "Permission denied for table"
**Solution**: Grant proper permissions: `GRANT ALL ON table_name TO user_name;`

**Issue**: "CSV import fails with encoding error"
**Solution**: Convert CSV to UTF-8: `iconv -f ISO-8859-1 -t UTF-8 input.csv > output.csv`

### Next Steps

1. Follow the Quick Start guide above
2. Review generated schema in `02_schema/schema.sql`
3. Run transformation scripts in `03_transform/`
4. Import data using scripts in `05_csv_import/`
5. Update Android app with PostgreSQL adapter

---

## 📊 Success Metrics

After migration, you should have:

- ✅ PostgreSQL database with ~500+ products
- ✅ ~1,000+ recipes with ingredients
- ✅ ~10,000+ sales transactions
- ✅ ~100+ waste log entries
- ✅ Sample users (admin, staff)
- ✅ ML-ready CSV datasets
- ✅ Working Android app with PostgreSQL

---

**Let's begin! 🚀**

Next: Create the setup scripts and transformation code.
