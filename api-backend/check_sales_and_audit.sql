-- ============================================================================
-- QUERY: Check Recent Sales and Audit Logs
-- Run this in pgAdmin to see what's happening
-- ============================================================================

-- 1. Recent Sales (Last 24 hours)
SELECT
    product_name,
    category,
    quantity,
    price,
    price * quantity as total,
    order_date,
    payment_mode,
    cashier_username
FROM sales
WHERE order_date > NOW() - INTERVAL '24 hours'
ORDER BY order_date DESC
LIMIT 20;

-- 2. All Sales Audit Logs
SELECT
    action,
    description,
    status,
    date_time,
    username
FROM audit_logs
WHERE action = 'SALE_TRANSACTION'
ORDER BY date_time DESC
LIMIT 20;

-- 3. All Recent Audit Logs (any action)
SELECT
    action,
    description,
    status,
    date_time,
    username
FROM audit_logs
ORDER BY date_time DESC
LIMIT 50;

-- 4. Ingredient Stock Status
SELECT
    name,
    quantity,
    inventory_a,
    inventory_b,
    (inventory_a + inventory_b) as calculated_total,
    CASE
        WHEN (inventory_a + inventory_b) = quantity THEN '✅ Match'
        ELSE '⚠️ Mismatch'
    END as status
FROM products
WHERE category = 'Ingredients'
ORDER BY name;

-- 5. Recipe-Based Products (should all be 0 stock)
SELECT
    name,
    category,
    quantity,
    inventory_a,
    inventory_b,
    CASE
        WHEN quantity = 0 AND inventory_a = 0 AND inventory_b = 0 THEN '✅ Correct'
        ELSE '❌ Has stock (should be 0)'
    END as status
FROM products
WHERE category IN ('Beverages', 'Pastries')
ORDER BY category, name;

-- 6. Total Sales Count
SELECT
    COUNT(*) as total_sales,
    SUM(quantity * price) as total_revenue,
    COUNT(DISTINCT DATE(order_date)) as days_with_sales
FROM sales;
