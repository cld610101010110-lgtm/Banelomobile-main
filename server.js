const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');
const bodyParser = require('body-parser');
const bcrypt = require('bcrypt');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

console.log("Using DATABASE_URL:", !!process.env.DATABASE_URL)
// PostgreSQL connection pool
// Render provides DATABASE_URL automatically, fallback to individual params for local dev
const pool = new Pool(
    process.env.DATABASE_URL
    ? {
        connectionString: process.env.DATABASE_URL,
        ssl: { require: true, rejectUnauthorized: false }
    }
    : {
        host: process.env.DB_HOST || 'localhost',
        port: parseInt(process.env.DB_PORT || '5432'),
        database: process.env.DB_NAME || 'banelo_db',
        user: process.env.DB_USER || 'postgres',
        password: process.env.DB_PASSWORD || 'admin123',
        max: parseInt(process.env.DB_MAX_CONNECTIONS || '20'),
        idleTimeoutMillis: parseInt(process.env.DB_IDLE_TIMEOUT || '30000'),
        connectionTimeoutMillis: parseInt(process.env.DB_CONNECTION_TIMEOUT || '2000'),
    }
);

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

// Get user by username (for login) - WITH PASSWORD VERIFICATION
app.post('/api/users/login', async (req, res) => {
    const { username, password } = req.body;

    try {
        // Validate input
        if (!username || !password) {
            return res.status(400).json({
                success: false,
                error: 'Username and password are required'
            });
        }

        // Get user from database
        const result = await pool.query(
            'SELECT * FROM users WHERE username = $1 AND status = $2',
            [username, 'active']
        );

        if (result.rows.length === 0) {
            return res.status(401).json({
                success: false,
                error: 'Invalid username or password'
            });
        }

        const user = result.rows[0];

        // Verify password
        const passwordMatch = await bcrypt.compare(password, user.password_hash);

        if (!passwordMatch) {
            console.log(`❌ Failed login attempt for user: ${username}`);
            return res.status(401).json({
                success: false,
                error: 'Invalid username or password'
            });
        }

        console.log(`✅ Successful login: ${username} (${user.role})`);

        // Don't send password_hash to client
        delete user.password_hash;

        res.json({ success: true, data: user });

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
        // ✅ FIX: Beverages and Pastries are recipe-based, they should NEVER have stock
        const isRecipeBased = ['Beverages', 'Pastries'].includes(category);
        const finalQuantity = isRecipeBased ? 0 : quantity;
        const finalInventoryA = isRecipeBased ? 0 : inventory_a;
        const finalInventoryB = isRecipeBased ? 0 : inventory_b;

        const result = await pool.query(
            `INSERT INTO products (name, category, price, quantity, inventory_a, inventory_b,
                                  cost_per_unit, image_uri, description, sku, is_active)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, true)
             RETURNING *`,
            [name, category, price, finalQuantity, finalInventoryA, finalInventoryB, cost_per_unit, image_uri, description, sku]
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
        // ✅ FIX: Beverages and Pastries are recipe-based, they should NEVER have stock
        const isRecipeBased = ['Beverages', 'Pastries'].includes(category);
        const isIngredient = category === 'Ingredients';

        let finalQuantity, finalInventoryA, finalInventoryB;

        if (isRecipeBased) {
            // Recipe-based products: always 0 stock
            finalQuantity = 0;
            finalInventoryA = 0;
            finalInventoryB = 0;
        } else if (isIngredient) {
            // ✅ FIX: For ingredients, auto-calculate inventory_a if not properly set
            // If inventory_a + inventory_b doesn't match quantity, put all in inventory_a
            if ((inventory_a + inventory_b) !== quantity && inventory_a === 0) {
                finalInventoryA = quantity;
                finalInventoryB = inventory_b;
                finalQuantity = quantity;
            } else {
                finalInventoryA = inventory_a;
                finalInventoryB = inventory_b;
                finalQuantity = inventory_a + inventory_b; // Ensure consistency
            }
        } else {
            // Other products: use as-is
            finalQuantity = quantity;
            finalInventoryA = inventory_a;
            finalInventoryB = inventory_b;
        }

        const result = await pool.query(
            `UPDATE products
             SET name = $1, category = $2, price = $3, quantity = $4,
                 inventory_a = $5, inventory_b = $6, cost_per_unit = $7,
                 image_uri = $8, description = $9, updated_at = CURRENT_TIMESTAMP
             WHERE firebase_id = $10
             RETURNING *`,
            [name, category, price, finalQuantity, finalInventoryA, finalInventoryB, cost_per_unit, image_uri, description, firebaseId]
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

// Get all sales (limited to last month for performance)
app.get('/api/sales', async (req, res) => {
    try {
        // Calculate date from 1 month ago
        const oneMonthAgo = new Date();
        oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1);

        const result = await pool.query(
            `SELECT s.*, p.name as product_name
             FROM sales s
             LEFT JOIN products p ON s.product_firebase_id = p.id
             WHERE s.order_date >= $1
             ORDER BY s.order_date DESC
             LIMIT 1000`,
            [oneMonthAgo]
        );
        res.json({
            success: true,
            data: result.rows,
            meta: {
                from_date: oneMonthAgo.toISOString(),
                record_count: result.rows.length
            }
        });
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

        // Step 3: Validate ingredient stock BEFORE deducting
        for (const ingredient of ingredients) {
            const totalNeeded = Math.ceil(ingredient.quantity_needed * quantity);
            const availableStock = ingredient.inventory_a + ingredient.inventory_b;

            if (availableStock < totalNeeded) {
                throw new Error(`Insufficient stock for ${ingredient.name}. Need: ${totalNeeded}, Available: ${availableStock}`);
            }
        }

        console.log('✅ All ingredients have sufficient stock');

        // Step 4: Deduct each ingredient
        for (const ingredient of ingredients) {
            const totalNeeded = ingredient.quantity_needed * quantity;

            console.log(`📦 Deducting: ${ingredient.name} - ${totalNeeded} ${ingredient.unit}`);
            console.log(`   Before - A: ${ingredient.inventory_a}, B: ${ingredient.inventory_b}`);

            let inventoryA = ingredient.inventory_a;
            let inventoryB = ingredient.inventory_b;
            let remaining = Math.ceil(totalNeeded); // ✅ FIX: Use Math.ceil to round up, preventing 0 deductions

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

        // ✅ FIX: Return 'data' field to match ApiResponse<T> structure
        res.json({
            success: true,
            data: {
                sale: saleResult.rows[0],
                ingredientsDeducted: ingredients.length
            }
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
            `SELECT r.id, r.firebase_id, p.firebase_id as product_firebase_id, r.product_name,
                    r.instructions, r.prep_time_minutes, r.cook_time_minutes,
                    r.servings, r.created_at, r.updated_at, p.name as product_name,
                    COALESCE(
                        json_agg(
                            json_build_object(
                                'firebaseId', ri.firebase_id,
                                'ingredientFirebaseId', ing_prod.firebase_id,
                                'ingredientName', ri.ingredient_name,
                                'quantityNeeded', ri.quantity_needed,
                                'unit', ri.unit
                            )
                        ) FILTER (WHERE ri.ingredient_name IS NOT NULL),
                        '[]'::json
                    ) as ingredients
             FROM recipes r
             LEFT JOIN products p ON r.product_firebase_id = p.id
             LEFT JOIN recipe_ingredients ri ON r.id = ri.recipe_firebase_id
             LEFT JOIN products ing_prod ON ri.ingredient_firebase_id = ing_prod.id
             GROUP BY r.id, r.firebase_id, p.firebase_id, r.product_name,
                      r.instructions, r.prep_time_minutes, r.cook_time_minutes,
                      r.servings, r.created_at, r.updated_at, p.name
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
            `SELECT r.id, r.firebase_id, p.firebase_id as product_firebase_id, r.product_name,
                    r.instructions, r.prep_time_minutes, r.cook_time_minutes,
                    r.servings, r.created_at, r.updated_at,
                    COALESCE(
                        json_agg(
                            json_build_object(
                                'firebaseId', ri.firebase_id,
                                'ingredientFirebaseId', ing_prod.firebase_id,
                                'ingredientName', ri.ingredient_name,
                                'quantityNeeded', ri.quantity_needed,
                                'unit', ri.unit
                            )
                        ) FILTER (WHERE ri.ingredient_name IS NOT NULL),
                        '[]'::json
                    ) as ingredients
             FROM recipes r
             LEFT JOIN products p ON r.product_firebase_id = p.id
             LEFT JOIN recipe_ingredients ri ON r.id = ri.recipe_firebase_id
             LEFT JOIN products ing_prod ON ri.ingredient_firebase_id = ing_prod.id
             WHERE p.firebase_id = $1
             GROUP BY r.id, r.firebase_id, p.firebase_id, r.product_name,
                      r.instructions, r.prep_time_minutes, r.cook_time_minutes,
                      r.servings, r.created_at, r.updated_at`,
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
// UPDATE RECIPE - COMPLETE CORRECTED VERSION
// ============================================================================
app.put('/api/recipes/:recipeId', async (req, res) => {
    const { recipeId } = req.params;
    const { productFirebaseId, productName, productNumber, ingredients } = req.body;

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        // Get the numeric recipe ID first
        const recipe = await pool.query(
            'SELECT * FROM recipes WHERE id = $1 OR firebase_id = $1',  // ✅ CORRECT
            [recipeId]
        );

        if (recipeResult.rows.length === 0) {
            throw new Error('Recipe not found');
        }

        const numericRecipeId = recipeResult.rows[0].id;

        // Update recipe
        await client.query(
            `UPDATE recipes
             SET product_firebase_id = (SELECT id FROM products WHERE firebase_id = $1 LIMIT 1),
                 product_name = $2,
                 updated_at = CURRENT_TIMESTAMP
             WHERE firebase_id = $3`,
            [productFirebaseId, productName, recipeId]
        );

        // Delete old ingredients
        await client.query(
            'DELETE FROM recipe_ingredients WHERE recipe_id = $1',
            [numericRecipeId]
        );

        // Insert new ingredients
        for (const ingredient of ingredients) {
            await client.query(
                `INSERT INTO recipe_ingredients (recipe_id, ingredient_firebase_id, 
                                                 ingredient_name, quantity_needed, unit)
                 VALUES ($1, (SELECT id FROM products WHERE firebase_id = $2 LIMIT 1), $3, $4, $5)`,
                [
                    numericRecipeId,
                    ingredient.ingredientFirebaseId,
                    ingredient.ingredientName,
                    ingredient.quantityNeeded,
                    ingredient.unit || 'g'
                ]
            );
        }

        await client.query('COMMIT');
        res.json({ success: true, message: `Recipe for ${productName} updated successfully `});

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Error updating recipe:', error);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        client.release();
    }
});

// ============================================================================
// DELETE RECIPE - COMPLETE CORRECTED VERSION
// ============================================================================
app.delete('/api/recipes/:recipeId', async (req, res) => {
    const { recipeId } = req.params;

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        // Get recipe info
        const recipe = await pool.query(
        'SELECT * FROM recipes WHERE id = $1 OR firebase_id = $1',  // ✅ CORRECT
        [recipeId]
        );

        if (recipeResult.rows.length === 0) {
            throw new Error('Recipe not found');
        }

        const numericRecipeId = recipeResult.rows[0].id;
        const productName = recipeResult.rows[0].product_name;

        // Delete ingredients first
        await client.query(
            'DELETE FROM recipe_ingredients WHERE recipe_id = $1',
            [numericRecipeId]
        );

        // Delete recipe
        await client.query(
            'DELETE FROM recipes WHERE firebase_id = $1',
            [recipeId]
        );

        await client.query('COMMIT');
        res.json({ success: true, message: `Recipe for ${productName} deleted successfully `});

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Error deleting recipe:', error);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        client.release();
    }
});

// ============================================================================
// INVENTORY TRANSFER (A → B) - COMPLETE CORRECTED VERSION
// ============================================================================
app.post('/api/inventory/transfer', async (req, res) => {
    const { product_id, quantity } = req.body;

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        // Get current inventory using firebase_id
        const productResult = await client.query(
            'SELECT inventory_a, inventory_b, name FROM products WHERE firebase_id = $1',
            [product_id]
        );

        if (productResult.rows.length === 0) {
            throw new Error('Product not found');
        }

        const product = productResult.rows[0];

        if (product.inventory_a < quantity) {
            throw new Error(`Insufficient stock in Inventory A. Available: ${product.inventory_a}`);
        }

        // Transfer from A to B
        await client.query(
            `UPDATE products
             SET inventory_a = inventory_a - $1,
                 inventory_b = inventory_b + $1,
                 quantity = inventory_b + $1,
                 updated_at = CURRENT_TIMESTAMP
             WHERE firebase_id = $2`,
            [quantity, product_id]
        );

        await client.query('COMMIT');
        res.json({ success: true, message: `Transferred ${quantity} units from warehouse to display `});

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Error transferring inventory:', error);
        res.status(500).json({ success: false, message: error.message });
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
    const { username, action, description, dateTime, status, metadata } = req.body;

    try {
        await pool.query(
            `INSERT INTO audit_logs (username, action, description, date_time, status, metadata)
             VALUES ($1, $2, $3, $4, $5, $6)`,
            [username, action, description, dateTime || new Date().toISOString(), status || 'Success', metadata ? JSON.stringify(metadata) : null]
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

// Get top products (last month data for performance)
app.get('/api/reports/top-products', async (req, res) => {
    const { limit = 10 } = req.query;

    try {
        // Calculate date from 1 month ago
        const oneMonthAgo = new Date();
        oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1);

        const result = await pool.query(
            `SELECT product_name, category,
                    SUM(quantity) as units_sold,
                    SUM(quantity * price) as revenue
             FROM sales
             WHERE order_date >= $1
             GROUP BY product_name, category
             ORDER BY revenue DESC
             LIMIT $2`,
            [oneMonthAgo, limit]
        );

        res.json({
            success: true,
            data: result.rows,
            meta: {
                from_date: oneMonthAgo.toISOString(),
                period: 'last_month'
            }
        });
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

app.listen(PORT, '0.0.0.0', () => {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🚀 Banelo POS API Server');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`✅ Server running on: http://0.0.0.0:${PORT}`);
    console.log(`📡 API endpoint: http://0.0.0.0:${PORT}/api`);
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
