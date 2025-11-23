"""
Generate sales transactions from Kaggle bakery sales dataset
Transforms sales data into PostgreSQL sales table
"""

import pandas as pd
import random
from datetime import datetime, timedelta
from typing import List, Dict
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils import (
    generate_uuid, generate_firebase_id, random_date,
    get_payment_mode, generate_gcash_reference, progress_bar,
    format_timestamp
)


class SalesGenerator:
    """Generate sales transactions from bakery dataset"""

    def __init__(self, products_csv: str, users_csv: str, dataset_path: str = None):
        self.products_csv = products_csv
        self.users_csv = users_csv
        self.dataset_path = dataset_path or '../datasets/Bakery_sales.csv'
        self.sales = []
        self.products_df = None
        self.users_df = None

    def load_products(self) -> pd.DataFrame:
        """Load product catalog"""
        print(f"📂 Loading products from: {self.products_csv}")
        df = pd.read_csv(self.products_csv)
        print(f"✅ Loaded {len(df)} products")
        self.products_df = df
        return df

    def load_users(self) -> pd.DataFrame:
        """Load users for cashier assignment"""
        print(f"📂 Loading users from: {self.users_csv}")
        df = pd.read_csv(self.users_csv)
        print(f"✅ Loaded {len(df)} users")
        self.users_df = df
        return df

    def load_sales_dataset(self) -> pd.DataFrame:
        """Load bakery sales dataset"""
        print(f"📂 Loading sales dataset: {self.dataset_path}")

        try:
            df = pd.read_csv(self.dataset_path)
            print(f"✅ Loaded {len(df)} sales transactions")
            return df
        except FileNotFoundError:
            print("⚠️  Sales dataset not found. Will generate sample sales.")
            return None

    def match_product(self, product_name: str) -> Dict:
        """Match dataset product name to our product catalog"""
        # Try exact match first
        match = self.products_df[
            self.products_df['name'].str.lower() == product_name.lower()
        ]

        if len(match) > 0:
            return match.iloc[0].to_dict()

        # Try partial match
        match = self.products_df[
            self.products_df['name'].str.contains(product_name, case=False, na=False)
        ]

        if len(match) > 0:
            return match.iloc[0].to_dict()

        # Return random product
        return self.products_df.sample(1).iloc[0].to_dict()

    def generate_from_dataset(self, limit: int = 10000) -> List[Dict]:
        """Generate sales from bakery dataset"""
        print("\n💰 Generating sales transactions...")

        # Load dependencies
        self.load_products()
        self.load_users()

        sales_df = self.load_sales_dataset()

        if sales_df is None or len(sales_df) == 0:
            print("📝 Generating sample sales...")
            return self._generate_sample_sales(limit)

        # Take a sample
        sample_size = min(limit, len(sales_df))
        sales_sample = sales_df.sample(sample_size)

        sales = []
        cashiers = self.users_df['username'].tolist()

        # Date range for sales (last 12 months)
        end_date = datetime.now()
        start_date = end_date - timedelta(days=365)

        for idx, (_, sale_row) in enumerate(sales_sample.iterrows(), 1):
            # Get product info
            product_name = sale_row.get('article', 'Unknown Product')
            product = self.match_product(str(product_name))

            # Generate sale details
            quantity = random.randint(1, 5)
            payment_mode = get_payment_mode()
            sale_date = random_date(start_date, end_date)

            sale = {
                'id': generate_uuid(),
                'firebase_id': generate_firebase_id(),
                'order_id': idx,
                'product_name': product['name'],
                'category': product['category'],
                'quantity': quantity,
                'price': product['price'],
                'order_date': sale_date,
                'product_firebase_id': product['id'],
                'payment_mode': payment_mode,
                'gcash_reference_id': generate_gcash_reference() if payment_mode == 'GCash' else '',
                'cashier_username': random.choice(cashiers) if cashiers else 'admin',
                'created_at': sale_date
            }

            sales.append(sale)
            progress_bar(idx, sample_size, prefix='Generating')

        self.sales = sales
        print(f"\n✅ Generated {len(sales)} sales transactions")
        return sales

    def _generate_sample_sales(self, limit: int) -> List[Dict]:
        """Generate sample sales when dataset unavailable"""
        sales = []
        cashiers = self.users_df['username'].tolist() if self.users_df is not None else ['admin']

        end_date = datetime.now()
        start_date = end_date - timedelta(days=365)

        for idx in range(limit):
            product = self.products_df.sample(1).iloc[0]
            quantity = random.randint(1, 5)
            payment_mode = get_payment_mode()
            sale_date = random_date(start_date, end_date)

            sale = {
                'id': generate_uuid(),
                'firebase_id': generate_firebase_id(),
                'order_id': idx + 1,
                'product_name': product['name'],
                'category': product['category'],
                'quantity': quantity,
                'price': product['price'],
                'order_date': sale_date,
                'product_firebase_id': product['id'],
                'payment_mode': payment_mode,
                'gcash_reference_id': generate_gcash_reference() if payment_mode == 'GCash' else '',
                'cashier_username': random.choice(cashiers),
                'created_at': sale_date
            }
            sales.append(sale)

            if (idx + 1) % 1000 == 0:
                progress_bar(idx + 1, limit, prefix='Generating')

        progress_bar(limit, limit, prefix='Generating')
        return sales

    def save_to_csv(self, output_path: str):
        """Save sales to CSV"""
        df = pd.DataFrame(self.sales)

        # Format timestamps
        if len(df) > 0:
            if 'order_date' in df.columns:
                df['order_date'] = pd.to_datetime(df['order_date']).dt.strftime('%Y-%m-%d %H:%M:%S')
            if 'created_at' in df.columns:
                df['created_at'] = pd.to_datetime(df['created_at']).dt.strftime('%Y-%m-%d %H:%M:%S')

        df.to_csv(output_path, index=False)
        print(f"💾 Saved sales to: {output_path}")

        # Summary
        print(f"\n📊 Sales Summary:")
        print(f"  Total transactions: {len(df):,}")
        print(f"  Total revenue: ${(df['quantity'] * df['price']).sum():,.2f}")
        print(f"  Date range: {df['order_date'].min()} to {df['order_date'].max()}")
        print(f"\n  Payment modes:")
        print(df['payment_mode'].value_counts())
        print(f"\n  Top 5 products:")
        print(df['product_name'].value_counts().head())


def main():
    """Main execution"""
    print("=" * 70)
    print("💳 SALES TRANSACTION GENERATOR")
    print("=" * 70)

    products_csv = '../05_csv_import/products.csv'
    users_csv = '../04_seed_data/users_seed.csv'

    if not os.path.exists(products_csv):
        print("❌ Products CSV not found! Run generate_products.py first.")
        return

    # Create sample users CSV if not exists
    if not os.path.exists(users_csv):
        print("⚠️  Users CSV not found. Creating sample users...")
        os.makedirs(os.path.dirname(users_csv), exist_ok=True)
        sample_users = pd.DataFrame([
            {'username': 'admin'},
            {'username': 'staff1'},
            {'username': 'staff2'}
        ])
        sample_users.to_csv(users_csv, index=False)

    generator = SalesGenerator(products_csv, users_csv)
    sales = generator.generate_from_dataset(limit=10000)

    # Save to CSV
    output_dir = '../05_csv_import'
    generator.save_to_csv(f'{output_dir}/sales.csv')

    print("\n✅ Sales generation complete!")


if __name__ == '__main__':
    main()
