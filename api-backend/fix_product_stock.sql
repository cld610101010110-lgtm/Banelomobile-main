-- ============================================================================
-- FIX: Set Beverages and Pastries to 0 stock (they are recipe-based only)
-- ============================================================================

-- Reset all Beverages and Pastries to 0 stock
UPDATE products
SET quantity = 0,
    inventory_a = 0,
    inventory_b = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE category IN ('Beverages', 'Pastries');

-- Verify the fix
SELECT name, category, quantity, inventory_a, inventory_b
FROM products
WHERE category IN ('Beverages', 'Pastries')
ORDER BY category, name;

-- Show ingredients status
SELECT name, category, quantity, inventory_a, inventory_b
FROM products
WHERE category = 'Ingredients'
ORDER BY name;
