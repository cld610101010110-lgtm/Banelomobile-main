"""
FIXED VERSION - Generate sales transactions
This creates realistic POS sales data

Place this file in: migration/03_transform/
Then run: python FIXED_generate_sales.py
"""

import pandas as pd
import random
from datetime import datetime, timedelta
from typing import List, Dict
import sys
import os
import uuid

def generate_uuid():
    return str(uuid.uuid4())

def generate_firebase_id():
    import string
    chars = string.ascii_letters + string.digits
    return ''.join(random.choices(chars, k=20))

def generate_gcash_reference():
    """Generate GCash-style reference"""
    timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
    random_suffix = ''.join(random.choices('0123456789', k=6))
    return f"GCASH{timestamp}{random_suffix}"

def get_payment_mode():
    """Get random payment mode with realistic distribution"""
    modes = ['Cash'] * 60 + ['GCash'] * 30 + ['Card'] * 10
    return random.choice(modes)

def main():
    print("=" * 70)
    print("💳 FIXED SALES TRANSACTION GENERATOR")
    print("=" * 70)
    print()

    # Load products
    products_csv = 'migration/05_csv_import/products.csv'
    users_csv = 'migration/05_csv_import/users.csv'

    if not os.path.exists(products_csv):
        print(f"❌ Products file not found: {products_csv}")
        return

    print(f"📂 Loading products from: {products_csv}")
    all_products = pd.read_csv(products_csv)

    # Only final products (Pastries, Beverages) can be sold
    sellable_products = all_products[all_products['category'].isin(['Pastries', 'Beverages'])].copy()
    print(f"✅ Found {len(sellable_products)} sellable products")

    # Load users
    if os.path.exists(users_csv):
        users = pd.read_csv(users_csv)
        cashiers = users['username'].tolist()
        print(f"✅ Found {len(cashiers)} users")
    else:
        cashiers = ['admin', 'staff1', 'staff2']
        print("⚠️  Using default cashiers")

    print()

    # Generate sales
    num_sales = 10000
    print(f"💰 Generating {num_sales:,} sales transactions...")
    print()

    sales = []

    # Date range (last 12 months)
    end_date = datetime.now()
    start_date = end_date - timedelta(days=365)

    for i in range(num_sales):
        # Random product
        product = sellable_products.sample(1).iloc[0]

        # Random sale details
        quantity = random.randint(1, 5)
        payment_mode = get_payment_mode()

        # Random date
        days_offset = random.randint(0, 365)
        seconds_offset = random.randint(0, 86400)
        sale_date = start_date + timedelta(days=days_offset, seconds=seconds_offset)

        # Create sale
        sale = {
            'id': generate_uuid(),
            'firebase_id': generate_firebase_id(),
            'order_id': i + 1,
            'product_name': product['name'],
            'category': product['category'],
            'quantity': quantity,
            'price': product['price'],
            'order_date': sale_date.strftime('%Y-%m-%d %H:%M:%S'),
            'product_firebase_id': product['id'],
            'payment_mode': payment_mode,
            'gcash_reference_id': generate_gcash_reference() if payment_mode == 'GCash' else '',
            'cashier_username': random.choice(cashiers),
            'created_at': sale_date.strftime('%Y-%m-%d %H:%M:%S')
        }

        sales.append(sale)

        # Progress
        if (i + 1) % 1000 == 0:
            print(f"Generated {i + 1:,} sales...")

    print(f"✅ Generated {len(sales):,} sales")
    print()

    # Save to CSV
    output_dir = 'migration/05_csv_import'
    os.makedirs(output_dir, exist_ok=True)

    sales_df = pd.DataFrame(sales)
    sales_df.to_csv(f'{output_dir}/sales.csv', index=False)
    print(f"💾 Saved: {output_dir}/sales.csv")

    # Summary
    total_revenue = (sales_df['quantity'] * sales_df['price']).sum()
    print()
    print("📊 Sales Summary:")
    print(f"  Total transactions: {len(sales_df):,}")
    print(f"  Total revenue: ${total_revenue:,.2f}")
    print(f"  Date range: {sales_df['order_date'].min()} to {sales_df['order_date'].max()}")
    print()
    print("  Payment modes:")
    print(sales_df['payment_mode'].value_counts())
    print()
    print("  Top 5 products sold:")
    print(sales_df['product_name'].value_counts().head())
    print()
    print("✅ Sales generation complete!")
    print()
    print("Next: Import to PostgreSQL")

if __name__ == '__main__':
    main()
