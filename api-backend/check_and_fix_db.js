const { Pool } = require('pg');

const pool = new Pool({
    host: 'localhost',
    port: 5432,
    database: 'banelo_db',
    user: 'postgres',
    password: 'admin123',
});

async function checkAndFixDatabase() {
    try {
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🔍 CHECKING DATABASE STATUS');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

        // 1. Check ingredients
        console.log('\n📦 INGREDIENTS:');
        const ingredients = await pool.query(`
            SELECT name, category, quantity, inventory_a, inventory_b, firebase_id
            FROM products
            WHERE category = 'Ingredients'
            ORDER BY name
        `);

        if (ingredients.rows.length === 0) {
            console.log('❌ NO INGREDIENTS FOUND IN DATABASE!');
            console.log('   Please add ingredients through the mobile app first.');
        } else {
            ingredients.rows.forEach(ing => {
                const total = ing.inventory_a + ing.inventory_b;
                const status = total === 0 ? '❌ NO STOCK' : total === ing.quantity ? '✅' : '⚠️  MISMATCH';
                console.log(`${status} ${ing.name.padEnd(20)} Qty:${ing.quantity.toString().padStart(5)} | A:${ing.inventory_a.toString().padStart(5)} | B:${ing.inventory_b.toString().padStart(5)}`);
            });
        }

        // 2. Check beverages and pastries (should have 0 direct stock)
        console.log('\n🍰 BEVERAGES & PASTRIES:');
        const products = await pool.query(`
            SELECT name, category, quantity, inventory_a, inventory_b
            FROM products
            WHERE category IN ('Beverages', 'Pastries')
            ORDER BY category, name
        `);

        if (products.rows.length === 0) {
            console.log('❌ NO PRODUCTS FOUND!');
        } else {
            products.rows.forEach(prod => {
                const hasStock = prod.quantity > 0;
                const status = hasStock ? '⚠️  HAS STOCK (should be 0)' : '✅ 0 stock (correct)';
                console.log(`${status} ${prod.name.padEnd(25)} (${prod.category})`);
            });
        }

        // 3. Check recipes
        console.log('\n📋 RECIPES:');
        const recipes = await pool.query(`
            SELECT r.product_name, COUNT(ri.id) as ingredient_count
            FROM recipes r
            LEFT JOIN recipe_ingredients ri ON r.id = ri.recipe_firebase_id
            GROUP BY r.id, r.product_name
            ORDER BY r.product_name
        `);

        if (recipes.rows.length === 0) {
            console.log('❌ NO RECIPES FOUND!');
        } else {
            recipes.rows.forEach(recipe => {
                const status = recipe.ingredient_count > 0 ? '✅' : '❌';
                console.log(`${status} ${recipe.product_name.padEnd(25)} - ${recipe.ingredient_count} ingredients`);
            });
        }

        // 4. Suggest fixes
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('💡 RECOMMENDATIONS:');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

        const zeroStockIngredients = ingredients.rows.filter(i => i.quantity === 0);
        if (zeroStockIngredients.length > 0) {
            console.log('\n⚠️  Ingredients with 0 stock:');
            zeroStockIngredients.forEach(ing => {
                console.log(`   - ${ing.name}: Add stock through the mobile app`);
            });
        }

        const productsWithStock = products.rows.filter(p => p.quantity > 0);
        if (productsWithStock.length > 0) {
            console.log('\n⚠️  Products that should have 0 stock:');
            console.log('   Run this SQL to fix:');
            console.log('   UPDATE products SET quantity = 0, inventory_a = 0, inventory_b = 0');
            console.log('   WHERE category IN (\'Beverages\', \'Pastries\');');
        }

        console.log('\n✅ Check complete!');
        pool.end();

    } catch (err) {
        console.error('❌ Error:', err.message);
        console.log('\n💡 Make sure PostgreSQL is running!');
        pool.end();
    }
}

checkAndFixDatabase();
