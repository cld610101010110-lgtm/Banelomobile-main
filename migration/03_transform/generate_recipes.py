"""
Generate recipes and recipe ingredients from Food.com dataset
Transforms recipe data into PostgreSQL recipes and recipe_ingredients tables
"""

import pandas as pd
import ast
import random
from datetime import datetime
from typing import List, Dict, Tuple
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils import (
    generate_uuid, generate_firebase_id, clean_text,
    safe_float, safe_int, progress_bar
)


class RecipeGenerator:
    """Generate recipes from Food.com dataset"""

    def __init__(self, products_csv: str, dataset_path: str = None):
        self.products_csv = products_csv
        self.dataset_path = dataset_path or '../datasets/RAW_recipes.csv'
        self.recipes = []
        self.recipe_ingredients = []
        self.products_df = None

    def load_products(self) -> pd.DataFrame:
        """Load generated products to link recipes"""
        print(f"📂 Loading products from: {self.products_csv}")
        df = pd.read_csv(self.products_csv)

        # Filter only pastries (recipes are for food items)
        pastries = df[df['category'] == 'Pastries'].copy()
        print(f"✅ Loaded {len(pastries)} pastry products")

        self.products_df = pastries
        return pastries

    def load_recipe_dataset(self) -> pd.DataFrame:
        """Load Food.com recipes dataset"""
        print(f"📂 Loading recipe dataset: {self.dataset_path}")

        try:
            df = pd.read_csv(self.dataset_path)
            print(f"✅ Loaded {len(df)} recipes from dataset")

            # Filter to bakery-related recipes
            bakery_keywords = ['cake', 'bread', 'cookie', 'pastry', 'muffin',
                              'scone', 'biscuit', 'tart', 'pie', 'croissant',
                              'danish', 'donut', 'brownie']

            mask = df['name'].str.lower().str.contains('|'.join(bakery_keywords), na=False)
            bakery_recipes = df[mask].copy()

            print(f"🥐 Filtered to {len(bakery_recipes)} bakery-related recipes")
            return bakery_recipes

        except FileNotFoundError:
            print(f"⚠️  Recipe dataset not found. Will generate sample recipes.")
            return None

    def parse_ingredients(self, ingredients_str: str) -> List[str]:
        """Parse ingredient list from string"""
        try:
            # The dataset stores ingredients as string representation of list
            ingredients = ast.literal_eval(ingredients_str)
            return [clean_text(ing) for ing in ingredients]
        except:
            return []

    def match_ingredient_to_product(self, ingredient_name: str) -> Tuple[str, str, str]:
        """Match ingredient name to a product in our catalog"""
        # First, try to find in Ingredients category
        ingredients_df = pd.read_csv(self.products_csv)
        ingredients_df = ingredients_df[ingredients_df['category'] == 'Ingredients']

        # Simple keyword matching
        ingredient_lower = ingredient_name.lower()
        for _, product in ingredients_df.iterrows():
            product_name_lower = product['name'].lower()
            if product_name_lower in ingredient_lower or ingredient_lower in product_name_lower:
                return product['id'], product['firebase_id'], product['name']

        # If no match, return a random ingredient
        if len(ingredients_df) > 0:
            random_ing = ingredients_df.sample(1).iloc[0]
            return random_ing['id'], random_ing['firebase_id'], random_ing['name']

        # Fallback
        return generate_uuid(), generate_firebase_id(), ingredient_name

    def estimate_quantity(self, ingredient_name: str) -> Tuple[float, str]:
        """Estimate realistic quantity and unit for ingredient"""
        ingredient_lower = ingredient_name.lower()

        # Liquids (ml, L)
        if any(word in ingredient_lower for word in ['milk', 'water', 'juice', 'oil', 'cream']):
            return round(random.uniform(50, 500), 2), 'ml'

        # Powders/solids (g, kg)
        if any(word in ingredient_lower for word in ['flour', 'sugar', 'salt', 'powder']):
            return round(random.uniform(100, 1000), 2), 'g'

        # Eggs (pcs)
        if 'egg' in ingredient_lower:
            return random.randint(1, 6), 'pcs'

        # Butter, cheese (g)
        if any(word in ingredient_lower for word in ['butter', 'cheese', 'chocolate']):
            return round(random.uniform(50, 300), 2), 'g'

        # Default
        return round(random.uniform(50, 500), 2), 'g'

    def generate_from_dataset(self, limit: int = 300) -> Tuple[List[Dict], List[Dict]]:
        """Generate recipes from dataset"""
        print("\n🍰 Generating recipes from Food.com dataset...")

        # Load products (pastries)
        pastries = self.load_products()
        if len(pastries) == 0:
            print("❌ No pastry products found!")
            return [], []

        # Load recipe dataset
        recipe_df = self.load_recipe_dataset()

        if recipe_df is None or len(recipe_df) == 0:
            print("⚠️  Generating sample recipes instead...")
            return self._generate_sample_recipes(pastries, limit)

        # Take a sample of recipes
        sample_size = min(limit, len(recipe_df))
        recipe_sample = recipe_df.sample(sample_size)

        recipes = []
        recipe_ingredients = []

        for idx, (_, recipe_row) in enumerate(recipe_sample.iterrows(), 1):
            # Match recipe to a pastry product
            if idx <= len(pastries):
                product = pastries.iloc[idx - 1]
            else:
                product = pastries.sample(1).iloc[0]

            recipe_uuid = generate_uuid()
            recipe_firebase_id = generate_firebase_id()

            # Create recipe
            recipe = {
                'id': recipe_uuid,
                'firebase_id': recipe_firebase_id,
                'recipe_id': idx,
                'product_firebase_id': product['id'],
                'product_name': product['name'],
                'instructions': clean_text(recipe_row.get('steps', '')),
                'prep_time_minutes': safe_int(recipe_row.get('minutes', 0)),
                'cook_time_minutes': random.randint(15, 60),
                'servings': safe_int(recipe_row.get('n_steps', 4)),
                'created_at': datetime.now(),
                'updated_at': datetime.now()
            }
            recipes.append(recipe)

            # Parse and create ingredients
            ingredients_str = recipe_row.get('ingredients', '[]')
            ingredients_list = self.parse_ingredients(ingredients_str)

            for ing_name in ingredients_list[:10]:  # Limit to 10 ingredients
                ing_uuid, ing_firebase_id, ing_product_name = self.match_ingredient_to_product(ing_name)
                quantity, unit = self.estimate_quantity(ing_name)

                recipe_ingredient = {
                    'id': generate_uuid(),
                    'firebase_id': generate_firebase_id(),
                    'recipe_firebase_id': recipe_uuid,
                    'recipe_id': idx,
                    'ingredient_firebase_id': ing_uuid,
                    'ingredient_name': ing_product_name,
                    'quantity_needed': quantity,
                    'unit': unit,
                    'created_at': datetime.now()
                }
                recipe_ingredients.append(recipe_ingredient)

            progress_bar(idx, sample_size, prefix='Generating')

        self.recipes = recipes
        self.recipe_ingredients = recipe_ingredients

        print(f"\n✅ Generated {len(recipes)} recipes with {len(recipe_ingredients)} ingredients")
        return recipes, recipe_ingredients

    def _generate_sample_recipes(self, pastries: pd.DataFrame, limit: int) -> Tuple[List[Dict], List[Dict]]:
        """Generate sample recipes when dataset is unavailable"""
        print("📝 Generating sample recipes...")

        recipes = []
        recipe_ingredients = []

        # Load ingredients
        all_products = pd.read_csv(self.products_csv)
        ingredients = all_products[all_products['category'] == 'Ingredients']

        for idx in range(min(limit, len(pastries))):
            product = pastries.iloc[idx]
            recipe_uuid = generate_uuid()

            recipe = {
                'id': recipe_uuid,
                'firebase_id': generate_firebase_id(),
                'recipe_id': idx + 1,
                'product_firebase_id': product['id'],
                'product_name': product['name'],
                'instructions': f'Recipe instructions for {product["name"]}',
                'prep_time_minutes': random.randint(10, 30),
                'cook_time_minutes': random.randint(15, 60),
                'servings': random.randint(4, 12),
                'created_at': datetime.now(),
                'updated_at': datetime.now()
            }
            recipes.append(recipe)

            # Add 3-8 random ingredients
            num_ingredients = random.randint(3, 8)
            for _ in range(num_ingredients):
                if len(ingredients) > 0:
                    ingredient = ingredients.sample(1).iloc[0]
                    quantity, unit = self.estimate_quantity(ingredient['name'])

                    recipe_ingredient = {
                        'id': generate_uuid(),
                        'firebase_id': generate_firebase_id(),
                        'recipe_firebase_id': recipe_uuid,
                        'recipe_id': idx + 1,
                        'ingredient_firebase_id': ingredient['id'],
                        'ingredient_name': ingredient['name'],
                        'quantity_needed': quantity,
                        'unit': unit,
                        'created_at': datetime.now()
                    }
                    recipe_ingredients.append(recipe_ingredient)

        return recipes, recipe_ingredients

    def save_to_csv(self, recipes_output: str, ingredients_output: str):
        """Save recipes and ingredients to CSV"""
        # Recipes
        recipes_df = pd.DataFrame(self.recipes)
        if len(recipes_df) > 0 and 'created_at' in recipes_df.columns:
            recipes_df['created_at'] = pd.to_datetime(recipes_df['created_at']).dt.strftime('%Y-%m-%d %H:%M:%S')
            recipes_df['updated_at'] = pd.to_datetime(recipes_df['updated_at']).dt.strftime('%Y-%m-%d %H:%M:%S')
        recipes_df.to_csv(recipes_output, index=False)
        print(f"💾 Saved recipes to: {recipes_output}")

        # Recipe Ingredients
        ingredients_df = pd.DataFrame(self.recipe_ingredients)
        if len(ingredients_df) > 0 and 'created_at' in ingredients_df.columns:
            ingredients_df['created_at'] = pd.to_datetime(ingredients_df['created_at']).dt.strftime('%Y-%m-%d %H:%M:%S')
        ingredients_df.to_csv(ingredients_output, index=False)
        print(f"💾 Saved ingredients to: {ingredients_output}")

        # Summary
        print(f"\n📊 Recipe Summary:")
        print(f"  Total recipes: {len(self.recipes)}")
        print(f"  Total ingredients: {len(self.recipe_ingredients)}")
        print(f"  Avg ingredients per recipe: {len(self.recipe_ingredients)/len(self.recipes):.1f}")


def main():
    """Main execution"""
    print("=" * 70)
    print("👨‍🍳 RECIPE GENERATOR")
    print("=" * 70)

    products_csv = '../05_csv_import/products.csv'

    if not os.path.exists(products_csv):
        print("❌ Products CSV not found! Run generate_products.py first.")
        return

    generator = RecipeGenerator(products_csv)
    recipes, ingredients = generator.generate_from_dataset(limit=300)

    # Save to CSV
    output_dir = '../05_csv_import'
    generator.save_to_csv(
        f'{output_dir}/recipes.csv',
        f'{output_dir}/recipe_ingredients.csv'
    )

    print("\n✅ Recipe generation complete!")


if __name__ == '__main__':
    main()
