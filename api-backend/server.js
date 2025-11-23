const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');
const bodyParser = require('body-parser');

const app = express();
const PORT = 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// PostgreSQL connection pool
const pool = new Pool({
    host: 'localhost',
    port: 5432,
    database: 'banelo_db',
    user: 'postgres',
    password: 'admin123',
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 2000,
});

// Test database connection
pool.connect((err, client, release) => {
    if (err) {
        console.error('❌ Error connecting to PostgreSQL:', err.stack);
    } else {
        console.log('✅ Connected to PostgreSQL database: banelo_db');
        release();
    }
});

// ============================================================================
// USERS / ACCOUNTS CRUD
// ============================================================================

// Get all users
app.get('/api/users', async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT * FROM users WHERE status = $1 ORDER BY fname',
            ['active']
        );
        res.json({ success: true, data: result.rows });
    } catch (error) {
        console.error('Error fetching users:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Get user by username (for login)
app.post('/api/users/login', async (req, res) => {
    const { username } = req.body;

    try {
        const result = await pool.query(
            'SELECT * FROM users WHERE username = $1 AND status = $2',
            [username, 'active']
        );

        if (result.rows.length > 0) {
            res.json({ success: true, data: result.rows[0] });
        } else {
            res.status(404).json({ success: false, error: 'User not found' });
        }
    } catch (error) {
        console.error('Error during login:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Create new user
app.post('/api/users', async (req, res) => {
    const { fname, lname, mname, username, auth_email, role } = req.body;

    try {
        const result = await pool.query(
            `INSERT INTO users (fname, lname, mname, username, auth_email, role, status)
             VALUES ($1, $2, $3, $4, $5, $6, 'active')
             RETURNING *`,
            [fname, lname, mname, username, auth_email, role]
        );

        res.json({ success: true, data: result.rows[0] });
    } catch (error) {
        console.error('Error creating user:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Update user
app.put('/api/users/:firebaseId', async (req, res) => {
    const { firebaseId } = req.params;
    const { fname, lname, mname, username, auth_email, role, status } = req.body;

    try {
        const result = await pool.query(
            `UPDATE users
             SET fname = $1, lname = $2, mname = $3, username = $4,
                 auth_email = $5, role = $6, status = $7, updated_at = CURRENT_TIMESTAMP
             WHERE firebase_id = $8
             RETURNING *`,
            [fname, lname, mname, username, auth_email, role, status, firebaseId]
        );

        res.json({ success: true, data: result.rows[0] });
    } catch (error) {
        console.error('Error updating user:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Delete user (soft delete)
app.delete('/api/users/:firebaseId', async (req, res) => {
    const { firebaseId } = req.params;

    try {
        await pool.query(
            `UPDATE users SET status = 'inactive' WHERE firebase_id = $1`,
            [firebaseId]
        );

        res.json({ success: true, message: 'User deactivated' });
    } catch (error) {
        console.error('Error deleting user:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// ============================================================================
// PRODUCTS / INVENTORY CRUD
// ============================================================================

// Get all products
app.get('/api/products', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT id, firebase_id, name, category, price, quantity,
                    inventory_a, inventory_b, cost_per_unit, image_uri, description, sku
             FROM products
             WHERE is_active = true
             ORDER BY name`
        );
        res.json({ success: true, data: result.rows });
    } catch (error) {
        console.error('Error fetching products:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Get products by category
app.get('/api/products/category/:category', async (req, res) => {
    const { category } = req.params;

    try {
        const result = await pool.query(
            `SELECT * FROM products
             WHERE category = $1 AND is_active = true
             ORDER BY name`,
            [category]
        );
        res.json({ success: true, data: result.rows });
    } catch (error) {
        console.error('Error fetching products by category:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Get single product
app.get('/api/products/:firebaseId', async (req, res) => {
    const { firebaseId } = req.params;

    try {
        const result = await pool.query(
            'SELECT * FROM products WHERE firebase_id = $1',
            [firebaseId]
        );

        if (result.rows.length > 0) {
            res.json({ success: true, data: result.rows[0] });
        } else {
            res.status(404).json({ success: false, error: 'Product not found' });
        }
    } catch (error) {
        console.error('Error fetching product:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Create new product
app.post('/api/products', async (req, res) => {
    const { name, category, price, quantity, inventory_a, inventory_b, cost_per_unit, image_uri, description, sku } = req.body;

    try {
        const result = await pool.query(
            `INSERT INTO products (name, category, price, quantity, inventory_a, inventory_b,
                                  cost_per_unit, image_uri, description, sku, is_active)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, true)
             RETURNING *`,
            [name, category, price, quantity, inventory_a, inventory_b, cost_per_unit, image_uri, description, sku]
        );

        res.json({ success: true, data: result.rows[0] });
    } catch (error) {
        console.error('Error creating product:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Update product
app.put('/api/products/:firebaseId', async (req, res) => {
    const { firebaseId } = req.params;
    const { name, category, price, quantity, inventory_a, inventory_b, cost_per_unit, image_uri, description } = req.body;

    try {
        const result = await pool.query(
            `UPDATE products
             SET name = $1, category = $2, price = $3, quantity = $4,
                 inventory_a = $5, inventory_b = $6, cost_per_unit = $7,
                 image_uri = $8, description = $9, updated_at = CURRENT_TIMESTAMP
             WHERE firebase_id = $10
             RETURNING *`,
            [name, category, price, quantity, inventory_a, inventory_b, cost_per_unit, image_uri, description, firebaseId]
        );

        res.json({ success: true, data: result.rows[0] });
    } catch (error) {
        console.error('Error updating product:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Delete product (soft delete)
app.delete('/api/products/:firebaseId', async (req, res) => {
    const { firebaseId } = req.params;

    try {
        await pool.query(
            'UPDATE products SET is_active = false WHERE firebase_id = $1',
            [firebaseId]
        );

        res.json({ success: true, message: 'Product deactivated' });
    } catch (error) {
        console.error('Error deleting product:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Transfer inventory A → B
app.post('/api/products/transfer', async (req, res) => {
    const { firebaseId, quantity } = req.body;

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        // Get current inventory
        const productResult = await client.query(
            'SELECT inventory_a, inventory_b FROM products WHERE firebase_id = $1',
            [firebaseId]
        );

        if (productResult.rows.length === 0) {
            throw new Error('Product not found');
        }

        const product = productResult.rows[0];

        if (product.inventory_a < quantity) {
            throw new Error(`Insufficient stock in Inventory A. Available: ${product.inventory_a}`);
        }

        // Transfer
        await client.query(
            `UPDATE products
             SET inventory_a = inventory_a - $1,
                 inventory_b = inventory_b + $1,
                 updated_at = CURRENT_TIMESTAMP
             WHERE firebase_id = $2`,
            [quantity, firebaseId]
        );

        await client.query('COMMIT');
        res.json({ success: true, message: `Transferred ${quantity} units from warehouse to display` });

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Error transferring inventory:', error);
        res.status(500).json({ success: false, error: error.message });
    } finally {
        client.release();
    }
});

// ============================================================================
// SALES - WITH INGREDIENT-BASED DEDUCTION
// ============================================================================

// Get all sales
app.get('/api/sales', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT s.*, p.name as product_name
             FROM sales s
             LEFT JOIN products p ON s.product_firebase_id = p.id
             ORDER BY s.order_date DESC
             LIMIT 1000`
        );
        res.json({ success: true, data: result.rows });
    } catch (error) {
        console.error('Error fetching sales:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Get sales by date range
app.get('/api/sales/range', async (req, res) => {
    const { startDate, endDate } = req.query;

    try {
        const result = await pool.query(
            `SELECT * FROM sales
             WHERE order_date BETWEEN $1 AND $2
             ORDER BY order_date DESC`,
            [startDate, endDate]
        );
        res.json({ success: true, data: result.rows });
    } catch (error) {
        console.error('Error fetching sales by range:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Process sale with ingredient-based deduction
app.post('/api/sales/process', async (req, res) => {
    const { productFirebaseId, quantity, productName, category, price, paymentMode, gcashReferenceId, cashierUsername } = req.body;

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        console.log('━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('💰 Processing sale with ingredient deduction');
        console.log(`Product: ${productName}, Quantity: ${quantity}`);

        // Step 1: Insert sale record
        const saleResult = await client.query(
            `INSERT INTO sales (product_name, category, quantity, price, order_date,
                               product_firebase_id, payment_mode, gcash_reference_id, cashier_username)
             VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP,
                    (SELECT id FROM products WHERE firebase_id = $5 LIMIT 1),
                    $6, $7, $8)
             RETURNING *`,
            [productName, category, quantity, price, productFirebaseId, paymentMode, gcashReferenceId, cashierUsername]
        );

        console.log('✅ Sale recorded');

        // Step 2: Get recipe ingredients
        const ingredientsResult = await client.query(
            `SELECT ri.quantity_needed, ri.unit, ri.ingredient_name, p.firebase_id, p.name,
                    p.inventory_a, p.inventory_b
             FROM recipes r
             JOIN recipe_ingredients ri ON r.id = ri.recipe_firebase_id
             JOIN products p ON ri.ingredient_firebase_id = p.id
             WHERE r.product_firebase_id = (SELECT id FROM products WHERE firebase_id = $1 LIMIT 1)`,
            [productFirebaseId]
        );

        const ingredients = ingredientsResult.rows;
        console.log(`📋 Found ${ingredients.length} ingredients in recipe`);

        if (ingredients.length === 0) {
            console.log('⚠️ No recipe found - no ingredients to deduct');
        }

        // Step 3: Deduct each ingredient
        for (const ingredient of ingredients) {
            const totalNeeded = ingredient.quantity_needed * quantity;

            console.log(`📦 Deducting: ${ingredient.name} - ${totalNeeded} ${ingredient.unit}`);
            console.log(`   Before - A: ${ingredient.inventory_a}, B: ${ingredient.inventory_b}`);

            let inventoryA = ingredient.inventory_a;
            let inventoryB = ingredient.inventory_b;
            let remaining = Math.floor(totalNeeded);

            // Deduct from B first
            if (inventoryB > 0) {
                const deductFromB = Math.min(remaining, inventoryB);
                inventoryB -= deductFromB;
                remaining -= deductFromB;
                console.log(`   Deducted ${deductFromB} from Inventory B`);
            }

            // Then deduct from A
            if (remaining > 0 && inventoryA > 0) {
                const deductFromA = Math.min(remaining, inventoryA);
                inventoryA -= deductFromA;
                remaining -= deductFromA;
                console.log(`   Deducted ${deductFromA} from Inventory A`);
            }

            const newQuantity = inventoryA + inventoryB;
            console.log(`   After - A: ${inventoryA}, B: ${inventoryB}, Total: ${newQuantity}`);

            if (remaining > 0) {
                console.log(`⚠️ Warning: Insufficient stock for ${ingredient.name}! Short by: ${remaining}`);
            }

            // Update inventory
            await client.query(
                `UPDATE products
                 SET inventory_a = $1, inventory_b = $2, quantity = $3, updated_at = CURRENT_TIMESTAMP
                 WHERE firebase_id = $4`,
                [inventoryA, inventoryB, newQuantity, ingredient.firebase_id]
            );
        }

        await client.query('COMMIT');
        console.log('✅ Transaction committed successfully');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━');

        res.json({
            success: true,
            message: 'Sale processed - ingredients deducted',
            sale: saleResult.rows[0],
            ingredientsDeducted: ingredients.length
        });

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('❌ Error processing sale:', error);
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━');
        res.status(500).json({ success: false, error: error.message });
    } finally {
        client.release();
    }
});

// ============================================================================
// RECIPES CRUD
// ============================================================================

// Get all recipes with ingredients
app.get('/api/recipes', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT r.*, p.name as product_name,
                    json_agg(
                        json_build_object(
                            'ingredient_name', ri.ingredient_name,
                            'quantity_needed', ri.quantity_needed,
                            'unit', ri.unit
                        )
                    ) as ingredients
             FROM recipes r
             LEFT JOIN products p ON r.product_firebase_id = p.id
             LEFT JOIN recipe_ingredients ri ON r.id = ri.recipe_firebase_id
             GROUP BY r.id, p.name
             ORDER BY r.product_name`
        );
        res.json({ success: true, data: result.rows });
    } catch (error) {
        console.error('Error fetching recipes:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Get recipe by product
app.get('/api/recipes/product/:firebaseId', async (req, res) => {
    const { firebaseId } = req.params;

    try {
        const result = await pool.query(
            `SELECT r.*,
                    json_agg(
                        json_build_object(
                            'ingredient_name', ri.ingredient_name,
                            'quantity_needed', ri.quantity_needed,
                            'unit', ri.unit
                        )
                    ) as ingredients
             FROM recipes r
             LEFT JOIN recipe_ingredients ri ON r.id = ri.recipe_firebase_id
             WHERE r.product_firebase_id = (SELECT id FROM products WHERE firebase_id = $1 LIMIT 1)
             GROUP BY r.id`,
            [firebaseId]
        );

        if (result.rows.length > 0) {
            res.json({ success: true, data: result.rows[0] });
        } else {
            res.status(404).json({ success: false, error: 'Recipe not found' });
        }
    } catch (error) {
        console.error('Error fetching recipe:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Create recipe with ingredients
app.post('/api/recipes', async (req, res) => {
    const { productFirebaseId, productName, instructions, prep_time_minutes, cook_time_minutes, servings, ingredients } = req.body;

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        // Insert recipe
        const recipeResult = await client.query(
            `INSERT INTO recipes (product_firebase_id, product_name, instructions,
                                 prep_time_minutes, cook_time_minutes, servings)
             VALUES ((SELECT id FROM products WHERE firebase_id = $1 LIMIT 1), $2, $3, $4, $5, $6)
             RETURNING *`,
            [productFirebaseId, productName, instructions, prep_time_minutes, cook_time_minutes, servings]
        );

        const recipeId = recipeResult.rows[0].id;

        // Insert ingredients
        for (const ingredient of ingredients) {
            await client.query(
                `INSERT INTO recipe_ingredients (recipe_firebase_id, ingredient_firebase_id,
                                                ingredient_name, quantity_needed, unit)
                 VALUES ($1, (SELECT id FROM products WHERE firebase_id = $2 LIMIT 1), $3, $4, $5)`,
                [recipeId, ingredient.ingredientFirebaseId, ingredient.ingredientName,
                 ingredient.quantityNeeded, ingredient.unit]
            );
        }

        await client.query('COMMIT');
        res.json({ success: true, data: recipeResult.rows[0] });

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Error creating recipe:', error);
        res.status(500).json({ success: false, error: error.message });
    } finally {
        client.release();
    }
});

// ============================================================================
// AUDIT TRAIL / LOGS
// ============================================================================

// Get audit logs
app.get('/api/audit', async (req, res) => {
    const { limit = 100 } = req.query;

    try {
        const result = await pool.query(
            `SELECT * FROM audit_logs
             ORDER BY created_at DESC
             LIMIT $1`,
            [limit]
        );
        res.json({ success: true, data: result.rows });
    } catch (error) {
        console.error('Error fetching audit logs:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Create audit log
app.post('/api/audit', async (req, res) => {
    const { action, table_name, record_id, user_id, changes } = req.body;

    try {
        await pool.query(
            `INSERT INTO audit_logs (action, table_name, record_id, user_id, changes)
             VALUES ($1, $2, $3, $4, $5)`,
            [action, table_name, record_id, user_id, JSON.stringify(changes)]
        );

        res.json({ success: true, message: 'Audit log created' });
    } catch (error) {
        console.error('Error creating audit log:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// ============================================================================
// WASTE LOGS
// ============================================================================

// Get waste logs
app.get('/api/waste', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT w.*, p.name as product_name
             FROM waste_logs w
             LEFT JOIN products p ON w.product_firebase_id = p.id
             ORDER BY w.waste_date DESC
             LIMIT 500`
        );
        res.json({ success: true, data: result.rows });
    } catch (error) {
        console.error('Error fetching waste logs:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Create waste log
app.post('/api/waste', async (req, res) => {
    const { productFirebaseId, productName, category, quantity, reason, recordedBy } = req.body;

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        // Get product cost
        const productResult = await pool.query(
            'SELECT cost_per_unit FROM products WHERE firebase_id = $1',
            [productFirebaseId]
        );

        const costImpact = productResult.rows.length > 0
            ? productResult.rows[0].cost_per_unit * quantity
            : 0;

        // Insert waste log
        await client.query(
            `INSERT INTO waste_logs (product_firebase_id, product_name, category, quantity,
                                    reason, waste_date, recorded_by, cost_impact)
             VALUES ((SELECT id FROM products WHERE firebase_id = $1 LIMIT 1), $2, $3, $4, $5, CURRENT_TIMESTAMP, $6, $7)`,
            [productFirebaseId, productName, category, quantity, reason, recordedBy, costImpact]
        );

        // Deduct from inventory
        await client.query(
            `UPDATE products
             SET quantity = quantity - $1,
                 inventory_b = GREATEST(0, inventory_b - $1),
                 inventory_a = GREATEST(0, inventory_a - GREATEST(0, $1 - inventory_b))
             WHERE firebase_id = $2`,
            [quantity, productFirebaseId]
        );

        await client.query('COMMIT');
        res.json({ success: true, message: 'Waste logged and inventory updated' });

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Error creating waste log:', error);
        res.status(500).json({ success: false, error: error.message });
    } finally {
        client.release();
    }
});

// ============================================================================
// DASHBOARD / REPORTS
// ============================================================================

// Get sales summary
app.get('/api/reports/sales-summary', async (req, res) => {
    const { startDate, endDate } = req.query;

    try {
        const result = await pool.query(
            `SELECT
                COUNT(*) as total_transactions,
                SUM(quantity * price) as total_revenue,
                AVG(quantity * price) as average_sale,
                payment_mode,
                COUNT(*) as transaction_count
             FROM sales
             WHERE order_date BETWEEN $1 AND $2
             GROUP BY payment_mode`,
            [startDate, endDate]
        );

        res.json({ success: true, data: result.rows });
    } catch (error) {
        console.error('Error fetching sales summary:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Get top products
app.get('/api/reports/top-products', async (req, res) => {
    const { limit = 10 } = req.query;

    try {
        const result = await pool.query(
            `SELECT product_name, category,
                    SUM(quantity) as units_sold,
                    SUM(quantity * price) as revenue
             FROM sales
             GROUP BY product_name, category
             ORDER BY revenue DESC
             LIMIT $1`,
            [limit]
        );

        res.json({ success: true, data: result.rows });
    } catch (error) {
        console.error('Error fetching top products:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Get low stock alert
app.get('/api/reports/low-stock', async (req, res) => {
    const { threshold = 10 } = req.query;

    try {
        const result = await pool.query(
            `SELECT name, category, quantity, inventory_a, inventory_b
             FROM products
             WHERE quantity < $1 AND is_active = true
             ORDER BY quantity ASC`,
            [threshold]
        );

        res.json({ success: true, data: result.rows });
    } catch (error) {
        console.error('Error fetching low stock:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// ============================================================================
// SERVER START
// ============================================================================

app.listen(PORT, () => {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🚀 Banelo POS API Server');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`✅ Server running on: http://localhost:${PORT}`);
    console.log(`📡 API endpoint: http://localhost:${PORT}/api`);
    console.log(`📊 Database: PostgreSQL (banelo_db)`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('Available endpoints:');
    console.log('  Users:     GET/POST/PUT/DELETE /api/users');
    console.log('  Products:  GET/POST/PUT/DELETE /api/products');
    console.log('  Sales:     GET/POST /api/sales');
    console.log('  Recipes:   GET/POST /api/recipes');
    console.log('  Audit:     GET/POST /api/audit');
    console.log('  Waste:     GET/POST /api/waste');
    console.log('  Reports:   GET /api/reports/*');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
});

// Graceful shutdown
process.on('SIGTERM', () => {
    console.log('⚠️  SIGTERM received, closing database pool...');
    pool.end(() => {
        console.log('✅ Database pool closed');
        process.exit(0);
    });
});
