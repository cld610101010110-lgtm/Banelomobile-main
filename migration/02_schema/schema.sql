-- ============================================================================
-- PostgreSQL Schema for Banelo Pastry Shop Management System
-- Migrated from Firebase Firestore
--
-- This schema recreates the Firestore structure with:
-- - Same field names (snake_case for PostgreSQL conventions)
-- - UUID primary keys (like Firebase document IDs)
-- - Proper foreign key relationships
-- - Indexes for performance
-- ============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- USERS TABLE
-- Manages staff and manager accounts
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_id VARCHAR(255) UNIQUE,           -- Original Firestore doc ID
    fname VARCHAR(100) NOT NULL,               -- First name
    lname VARCHAR(100) NOT NULL,               -- Last name
    mname VARCHAR(100),                        -- Middle name
    username VARCHAR(100) NOT NULL UNIQUE,     -- Login username
    auth_email VARCHAR(255),                   -- Firebase Auth email
    role VARCHAR(50) DEFAULT 'Staff',          -- manager, staff
    status VARCHAR(50) DEFAULT 'active',       -- active, inactive
    joined_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT check_role CHECK (role IN ('manager', 'staff', 'Manager', 'Staff')),
    CONSTRAINT check_status CHECK (status IN ('active', 'inactive'))
);

-- ============================================================================
-- PRODUCTS TABLE
-- Product catalog: Beverages, Pastries, and Ingredients
-- ============================================================================

CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_id VARCHAR(255) UNIQUE,           -- Original Firestore doc ID
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,            -- Beverages, Pastries, Ingredients
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    quantity INTEGER DEFAULT 0,                -- Computed total (inventoryA + inventoryB)
    inventory_a INTEGER DEFAULT 0,             -- Warehouse/Main inventory
    inventory_b INTEGER DEFAULT 0,             -- Display/Expendable inventory
    cost_per_unit DECIMAL(10,2) DEFAULT 0.00,  -- Cost for waste calculation
    image_uri TEXT,                            -- Cloudinary URL
    description TEXT,                          -- Product description
    sku VARCHAR(100),                          -- Stock keeping unit
    is_active BOOLEAN DEFAULT true,            -- Product availability
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT check_category CHECK (category IN ('Beverages', 'Pastries', 'Ingredients')),
    CONSTRAINT check_price_positive CHECK (price >= 0),
    CONSTRAINT check_inventory_positive CHECK (inventory_a >= 0 AND inventory_b >= 0)
);

-- ============================================================================
-- RECIPES TABLE
-- Recipe definitions for products (mainly pastries)
-- ============================================================================

CREATE TABLE IF NOT EXISTS recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_id VARCHAR(255) UNIQUE,           -- Original Firestore doc ID
    recipe_id INTEGER,                         -- Local Room database ID
    product_firebase_id UUID REFERENCES products(id) ON DELETE CASCADE,
    product_name VARCHAR(255),
    instructions TEXT,                         -- Cooking instructions
    prep_time_minutes INTEGER,                 -- Preparation time
    cook_time_minutes INTEGER,                 -- Cooking time
    servings INTEGER DEFAULT 1,                -- Number of servings
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- RECIPE_INGREDIENTS TABLE
-- Ingredients needed for each recipe
-- ============================================================================

CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_id VARCHAR(255) UNIQUE,           -- Original Firestore doc ID
    recipe_firebase_id UUID REFERENCES recipes(id) ON DELETE CASCADE,
    recipe_id INTEGER,                         -- Local Room database ID (FK to recipes)
    ingredient_firebase_id UUID REFERENCES products(id),
    ingredient_name VARCHAR(255) NOT NULL,
    quantity_needed DECIMAL(10,3) NOT NULL,    -- Amount needed
    unit VARCHAR(50) DEFAULT 'g',              -- g, kg, ml, L, pcs, etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT check_quantity_positive CHECK (quantity_needed > 0)
);

-- ============================================================================
-- SALES TABLE
-- Point-of-sale transactions
-- ============================================================================

CREATE TABLE IF NOT EXISTS sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_id VARCHAR(255) UNIQUE,           -- Original Firestore doc ID
    order_id INTEGER,                          -- Local Room database ID
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    quantity INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) GENERATED ALWAYS AS (quantity * price) STORED,
    order_date TIMESTAMP NOT NULL,
    product_firebase_id UUID REFERENCES products(id),
    payment_mode VARCHAR(50) DEFAULT 'Cash',   -- Cash, GCash, Card
    gcash_reference_id VARCHAR(255),           -- GCash transaction reference
    cashier_username VARCHAR(100),             -- Who processed the sale
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT check_quantity_positive CHECK (quantity > 0),
    CONSTRAINT check_price_positive CHECK (price >= 0),
    CONSTRAINT check_payment_mode CHECK (payment_mode IN ('Cash', 'GCash', 'Card', 'cash', 'gcash', 'card'))
);

-- ============================================================================
-- WASTE_LOGS TABLE
-- Track product waste for inventory management
-- ============================================================================

CREATE TABLE IF NOT EXISTS waste_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_id VARCHAR(255) UNIQUE,           -- Original Firestore doc ID
    product_firebase_id UUID REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    quantity INTEGER NOT NULL,
    reason VARCHAR(255) DEFAULT 'End of day waste',
    waste_date TIMESTAMP NOT NULL,
    recorded_by VARCHAR(100),                  -- Username who recorded
    cost_impact DECIMAL(10,2),                 -- Financial impact
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT check_waste_quantity_positive CHECK (quantity > 0)
);

-- ============================================================================
-- AUDIT_LOGS TABLE
-- Activity tracking and compliance
-- ============================================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_id VARCHAR(255) UNIQUE,           -- Original Firestore doc ID
    username VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,              -- LOGIN, SALE_TRANSACTION, etc.
    description TEXT,
    date_time TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'Success',      -- Success, Failed
    ip_address VARCHAR(45),                    -- IPv4 or IPv6
    user_agent TEXT,
    metadata JSONB,                            -- Additional flexible data
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT check_status CHECK (status IN ('Success', 'Failed', 'success', 'failed'))
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- Users
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- Products
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_is_active ON products(is_active);

-- Recipes
CREATE INDEX idx_recipes_product_id ON recipes(product_firebase_id);
CREATE INDEX idx_recipes_product_name ON recipes(product_name);

-- Recipe Ingredients
CREATE INDEX idx_recipe_ingredients_recipe_id ON recipe_ingredients(recipe_firebase_id);
CREATE INDEX idx_recipe_ingredients_ingredient_id ON recipe_ingredients(ingredient_firebase_id);

-- Sales
CREATE INDEX idx_sales_order_date ON sales(order_date);
CREATE INDEX idx_sales_product_id ON sales(product_firebase_id);
CREATE INDEX idx_sales_category ON sales(category);
CREATE INDEX idx_sales_payment_mode ON sales(payment_mode);
CREATE INDEX idx_sales_cashier ON sales(cashier_username);

-- Waste Logs
CREATE INDEX idx_waste_logs_waste_date ON waste_logs(waste_date);
CREATE INDEX idx_waste_logs_product_id ON waste_logs(product_firebase_id);
CREATE INDEX idx_waste_logs_recorded_by ON waste_logs(recorded_by);

-- Audit Logs
CREATE INDEX idx_audit_logs_username ON audit_logs(username);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_date_time ON audit_logs(date_time);
CREATE INDEX idx_audit_logs_status ON audit_logs(status);

-- ============================================================================
-- TRIGGERS FOR AUTO-UPDATE TIMESTAMPS
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to tables with updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_recipes_updated_at BEFORE UPDATE ON recipes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VIEWS FOR COMMON QUERIES
-- ============================================================================

-- Product inventory summary
CREATE OR REPLACE VIEW v_product_inventory AS
SELECT
    id,
    name,
    category,
    price,
    inventory_a,
    inventory_b,
    (inventory_a + inventory_b) as total_quantity,
    cost_per_unit,
    (inventory_a + inventory_b) * cost_per_unit as total_inventory_value,
    is_active
FROM products
ORDER BY category, name;

-- Sales summary by product
CREATE OR REPLACE VIEW v_sales_by_product AS
SELECT
    p.id as product_id,
    p.name as product_name,
    p.category,
    COUNT(s.id) as transaction_count,
    SUM(s.quantity) as total_quantity_sold,
    SUM(s.total_amount) as total_revenue
FROM products p
LEFT JOIN sales s ON p.id = s.product_firebase_id
GROUP BY p.id, p.name, p.category
ORDER BY total_revenue DESC NULLS LAST;

-- Waste summary by product
CREATE OR REPLACE VIEW v_waste_by_product AS
SELECT
    p.id as product_id,
    p.name as product_name,
    p.category,
    COUNT(w.id) as waste_log_count,
    SUM(w.quantity) as total_quantity_wasted,
    SUM(w.cost_impact) as total_cost_impact
FROM products p
LEFT JOIN waste_logs w ON p.id = w.product_firebase_id
GROUP BY p.id, p.name, p.category
ORDER BY total_cost_impact DESC NULLS LAST;

-- Recipe with ingredients details
CREATE OR REPLACE VIEW v_recipes_with_ingredients AS
SELECT
    r.id as recipe_id,
    r.product_name,
    ri.ingredient_name,
    ri.quantity_needed,
    ri.unit,
    p.cost_per_unit as ingredient_cost_per_unit,
    (ri.quantity_needed * p.cost_per_unit) as ingredient_total_cost
FROM recipes r
JOIN recipe_ingredients ri ON r.id = ri.recipe_firebase_id
LEFT JOIN products p ON ri.ingredient_firebase_id = p.id
ORDER BY r.product_name, ri.ingredient_name;

-- ============================================================================
-- GRANTS (if using different users)
-- ============================================================================

-- Grant permissions to banelo_user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO banelo_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO banelo_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO banelo_user;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Uncomment to verify schema creation
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;
-- SELECT * FROM information_schema.columns WHERE table_schema = 'public' ORDER BY table_name, ordinal_position;

-- ============================================================================
-- SCHEMA CREATION COMPLETE
-- ============================================================================

\echo '✅ Schema created successfully!'
\echo '📊 Tables: users, products, recipes, recipe_ingredients, sales, waste_logs, audit_logs'
\echo '🔍 Views: v_product_inventory, v_sales_by_product, v_waste_by_product, v_recipes_with_ingredients'
\echo ''
\echo 'Next: Import data using scripts in 05_csv_import/'
