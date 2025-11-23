"""
Generate waste log entries
Creates realistic waste tracking data for inventory management
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
    get_waste_reasons, progress_bar
)


class WasteLogGenerator:
    """Generate waste log entries"""

    def __init__(self, products_csv: str, users_csv: str):
        self.products_csv = products_csv
        self.users_csv = users_csv
        self.waste_logs = []
        self.products_df = None
        self.users_df = None

    def load_products(self) -> pd.DataFrame:
        """Load product catalog"""
        print(f"📂 Loading products from: {self.products_csv}")
        df = pd.read_csv(self.products_csv)

        # Only Pastries and Beverages have waste (not ingredients)
        df = df[df['category'].isin(['Pastries', 'Beverages'])]
        print(f"✅ Loaded {len(df)} perishable products")

        self.products_df = df
        return df

    def load_users(self) -> pd.DataFrame:
        """Load users"""
        print(f"📂 Loading users from: {self.users_csv}")
        df = pd.read_csv(self.users_csv)
        print(f"✅ Loaded {len(df)} users")
        self.users_df = df
        return df

    def generate_waste_logs(self, num_logs: int = 500) -> List[Dict]:
        """Generate waste log entries"""
        print(f"\n🗑️  Generating {num_logs} waste log entries...")

        self.load_products()
        self.load_users()

        if len(self.products_df) == 0:
            print("❌ No products available!")
            return []

        waste_logs = []
        users = self.users_df['username'].tolist() if self.users_df is not None else ['admin']
        reasons = get_waste_reasons()

        # Date range (last 6 months)
        end_date = datetime.now()
        start_date = end_date - timedelta(days=180)

        for i in range(num_logs):
            # Select random product
            product = self.products_df.sample(1).iloc[0]

            # Generate waste details
            # Most waste is small quantities (1-5 items)
            # Occasionally larger waste (10-20 items)
            if random.random() < 0.9:
                quantity = random.randint(1, 5)
            else:
                quantity = random.randint(10, 20)

            waste_date = random_date(start_date, end_date)
            reason = random.choice(reasons)

            # Calculate cost impact
            cost_impact = quantity * product['cost_per_unit']

            waste_log = {
                'id': generate_uuid(),
                'firebase_id': generate_firebase_id(),
                'product_firebase_id': product['id'],
                'product_name': product['name'],
                'category': product['category'],
                'quantity': quantity,
                'reason': reason,
                'waste_date': waste_date,
                'recorded_by': random.choice(users),
                'cost_impact': round(cost_impact, 2),
                'created_at': waste_date
            }

            waste_logs.append(waste_log)

            if (i + 1) % 100 == 0:
                progress_bar(i + 1, num_logs, prefix='Generating')

        progress_bar(num_logs, num_logs, prefix='Generating')

        self.waste_logs = waste_logs
        print(f"\n✅ Generated {len(waste_logs)} waste log entries")
        return waste_logs

    def save_to_csv(self, output_path: str):
        """Save waste logs to CSV"""
        df = pd.DataFrame(self.waste_logs)

        # Format timestamps
        df['waste_date'] = pd.to_datetime(df['waste_date']).dt.strftime('%Y-%m-%d %H:%M:%S')
        df['created_at'] = pd.to_datetime(df['created_at']).dt.strftime('%Y-%m-%d %H:%M:%S')

        df.to_csv(output_path, index=False)
        print(f"💾 Saved waste logs to: {output_path}")

        # Summary
        print(f"\n📊 Waste Log Summary:")
        print(f"  Total waste entries: {len(df):,}")
        print(f"  Total items wasted: {df['quantity'].sum():,}")
        print(f"  Total cost impact: ${df['cost_impact'].sum():,.2f}")
        print(f"  Date range: {df['waste_date'].min()} to {df['waste_date'].max()}")
        print(f"\n  Top waste reasons:")
        print(df['reason'].value_counts().head())
        print(f"\n  Most wasted products:")
        waste_by_product = df.groupby('product_name').agg({
            'quantity': 'sum',
            'cost_impact': 'sum'
        }).sort_values('cost_impact', ascending=False).head()
        print(waste_by_product)


def main():
    """Main execution"""
    print("=" * 70)
    print("🗑️  WASTE LOG GENERATOR")
    print("=" * 70)

    products_csv = '../05_csv_import/products.csv'
    users_csv = '../05_csv_import/users.csv'

    if not os.path.exists(products_csv):
        print("❌ Products CSV not found! Run generate_products.py first.")
        return

    if not os.path.exists(users_csv):
        print("❌ Users CSV not found! Run generate_users.py first.")
        return

    generator = WasteLogGenerator(products_csv, users_csv)
    waste_logs = generator.generate_waste_logs(num_logs=500)

    # Save to CSV
    output_dir = '../05_csv_import'
    generator.save_to_csv(f'{output_dir}/waste_logs.csv')

    print("\n✅ Waste log generation complete!")


if __name__ == '__main__':
    main()
