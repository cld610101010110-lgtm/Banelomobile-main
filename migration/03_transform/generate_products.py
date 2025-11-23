"""
Generate product catalog from Kaggle datasets
Transforms external dataset into PostgreSQL products table
"""

import pandas as pd
import random
from datetime import datetime
from typing import List, Dict
import sys
import os

# Add parent directory to path for imports
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils import (
    generate_uuid, generate_firebase_id, PastryNameGenerator,
    PriceGenerator, generate_realistic_inventory, clean_text,
    safe_float, progress_bar
)


class ProductGenerator:
    """Generate product catalog from Kaggle bakery sales data"""

    def __init__(self, dataset_path: str = None):
        self.dataset_path = dataset_path or '../datasets/Bakery_sales.csv'
        self.products = []

    def load_bakery_dataset(self) -> pd.DataFrame:
        """Load the French bakery sales dataset"""
        print(f"📂 Loading dataset: {self.dataset_path}")

        try:
            df = pd.read_csv(self.dataset_path)
            print(f"✅ Loaded {len(df)} rows")
            return df
        except FileNotFoundError:
            print(f"⚠️  Dataset not found. Generating sample products instead.")
            return None

    def extract_unique_products(self, df: pd.DataFrame) -> List[str]:
        """Extract unique product names from dataset"""
        if df is None:
            return []

        # The bakery dataset has an 'article' column with product names
        if 'article' in df.columns:
            unique_products = df['article'].unique().tolist()
            print(f"📊 Found {len(unique_products)} unique products in dataset")
            return unique_products

        return []

    def categorize_product(self, product_name: str) -> str:
        """Categorize product based on name"""
        product_lower = product_name.lower()

        # Beverages
        beverage_keywords = ['coffee', 'tea', 'juice', 'water', 'soda', 'milk']
        if any(keyword in product_lower for keyword in beverage_keywords):
            return 'Beverages'

        # Ingredients (less common in sales data, but possible)
        ingredient_keywords = ['flour', 'sugar', 'butter', 'egg']
        if any(keyword in product_lower for keyword in ingredient_keywords):
            return 'Ingredients'

        # Default to Pastries
        return 'Pastries'

    def generate_from_dataset(self, limit: int = 500) -> List[Dict]:
        """Generate products from the bakery dataset"""
        print("\n🏭 Generating products from Kaggle dataset...")

        df = self.load_bakery_dataset()

        if df is not None:
            unique_products = self.extract_unique_products(df)
            products_to_process = unique_products[:limit] if unique_products else []
        else:
            products_to_process = []

        # If dataset doesn't have enough products, supplement with generated ones
        if len(products_to_process) < limit:
            needed = limit - len(products_to_process)
            print(f"📝 Generating {needed} additional products...")
            products_to_process.extend(self._generate_sample_products(needed))

        products = []
        for idx, product_name in enumerate(products_to_process, 1):
            category = self.categorize_product(product_name)
            price = PriceGenerator.get_price(category)
            inventory = generate_realistic_inventory()

            product = {
                'id': generate_uuid(),
                'firebase_id': generate_firebase_id(),
                'name': clean_text(product_name),
                'category': category,
                'price': price,
                'quantity': inventory['quantity'],
                'inventory_a': inventory['inventory_a'],
                'inventory_b': inventory['inventory_b'],
                'cost_per_unit': PriceGenerator.get_cost_per_unit(price),
                'image_uri': f'https://res.cloudinary.com/banelo/products/{generate_firebase_id()}.jpg',
                'description': f'Delicious {product_name.lower()}',
                'sku': f'SKU-{idx:05d}',
                'is_active': True,
                'created_at': datetime.now(),
                'updated_at': datetime.now()
            }

            products.append(product)
            progress_bar(idx, len(products_to_process), prefix='Generating')

        self.products = products
        print(f"\n✅ Generated {len(products)} products")
        return products

    def _generate_sample_products(self, count: int) -> List[str]:
        """Generate sample product names"""
        names = []

        # Generate balanced mix of categories
        pastries_count = int(count * 0.6)
        beverages_count = int(count * 0.3)
        ingredients_count = count - pastries_count - beverages_count

        for _ in range(pastries_count):
            names.append(PastryNameGenerator.get_random_pastry())

        for _ in range(beverages_count):
            names.append(PastryNameGenerator.get_random_beverage())

        for _ in range(ingredients_count):
            names.append(PastryNameGenerator.get_random_ingredient())

        # Remove duplicates
        return list(set(names))

    def to_dataframe(self) -> pd.DataFrame:
        """Convert products to DataFrame"""
        return pd.DataFrame(self.products)

    def save_to_csv(self, output_path: str):
        """Save products to CSV for bulk import"""
        df = self.to_dataframe()

        # Format timestamps
        df['created_at'] = df['created_at'].dt.strftime('%Y-%m-%d %H:%M:%S')
        df['updated_at'] = df['updated_at'].dt.strftime('%Y-%m-%d %H:%M:%S')

        df.to_csv(output_path, index=False)
        print(f"💾 Saved products to: {output_path}")

        # Print summary
        print("\n📊 Product Summary:")
        print(df['category'].value_counts())
        print(f"\n💰 Price range: ${df['price'].min():.2f} - ${df['price'].max():.2f}")
        print(f"📦 Total inventory value: ${(df['quantity'] * df['cost_per_unit']).sum():,.2f}")


def main():
    """Main execution"""
    print("=" * 70)
    print("🛍️  PRODUCT CATALOG GENERATOR")
    print("=" * 70)

    generator = ProductGenerator()
    products = generator.generate_from_dataset(limit=500)

    # Save to CSV
    output_dir = '../05_csv_import'
    os.makedirs(output_dir, exist_ok=True)
    generator.save_to_csv(f'{output_dir}/products.csv')

    print("\n✅ Product generation complete!")


if __name__ == '__main__':
    main()
