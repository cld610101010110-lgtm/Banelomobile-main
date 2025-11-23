"""
FIXED VERSION - Generate recipes and recipe ingredients
This creates proper ingredient-based recipes for your POS system

Place this file in: migration/03_transform/
Then run: python FIXED_generate_recipes.py
"""

import pandas as pd
import random
from datetime import datetime
from typing import List, Dict, Tuple
import sys
import os
import uuid

# Generate UUID
def generate_uuid():
    return str(uuid.uuid4())

def generate_firebase_id():
    import string
    chars = string.ascii_letters + string.digits
    return ''.join(random.choices(chars, k=20))

# Recipe templates with realistic ingredients
RECIPE_TEMPLATES = {
    "Chocolate Cake": [
        ("Flour", 500, "g"),
        ("Sugar", 300, "g"),
        ("Butter", 200, "g"),
        ("Eggs", 4, "pcs"),
        ("Cocoa Powder", 100, "g")
    ],
    "Croissant": [
        ("Flour", 400, "g"),
        ("Butter", 250, "g"),
        ("Milk", 200, "ml"),
        ("Sugar", 50, "g"),
        ("Eggs", 2, "pcs")
    ],
    "Baguette": [
        ("Flour", 600, "g"),
        ("Water", 400, "ml"),
        ("Salt", 10, "g"),
        ("Yeast", 5, "g")
    ],
    "Chocolate Chip Cookie": [
        ("Flour", 300, "g"),
        ("Butter", 150, "g"),
        ("Sugar", 200, "g"),
        ("Eggs", 2, "pcs"),
        ("Chocolate", 150, "g")
    ],
    "Vanilla Cake": [
        ("Flour", 450, "g"),
        ("Sugar", 350, "g"),
        ("Butter", 200, "g"),
        ("Eggs", 4, "pcs"),
        ("Vanilla Extract", 10, "ml"),
        ("Milk", 150, "ml")
    ],
    "Blueberry Muffin": [
        ("Flour", 350, "g"),
        ("Sugar", 150, "g"),
        ("Eggs", 2, "pcs"),
        ("Milk", 200, "ml"),
        ("Butter", 100, "g")
    ],
    "Donut": [
        ("Flour", 400, "g"),
        ("Sugar", 150, "g"),
        ("Milk", 200, "ml"),
        ("Eggs", 2, "pcs"),
        ("Butter", 50, "g")
    ]
}

def get_recipe_for_product(product_name):
    """Get or generate recipe for a product"""
    # Try exact match
    if product_name in RECIPE_TEMPLATES:
        return RECIPE_TEMPLATES[product_name]

    # Try partial match
    for recipe_name, ingredients in RECIPE_TEMPLATES.items():
        if recipe_name.lower() in product_name.lower() or product_name.lower() in recipe_name.lower():
            return ingredients

    # Default generic recipe
    return [
        ("Flour", 300, "g"),
        ("Sugar", 150, "g"),
        ("Butter", 100, "g"),
        ("Eggs", 2, "pcs")
    ]

def main():
    print("=" * 70)
    print("👨‍🍳 FIXED RECIPE GENERATOR")
    print("=" * 70)
    print()

    # Load products
    products_csv = 'migration/05_csv_import/products.csv'
    if not os.path.exists(products_csv):
        print(f"❌ Products file not found: {products_csv}")
        print("Run generate_products.py first!")
        return

    print(f"📂 Loading products from: {products_csv}")
    all_products = pd.read_csv(products_csv)

    # Separate final products (Pastries/Beverages) and ingredients
    final_products = all_products[all_products['category'].isin(['Pastries', 'Beverages'])].copy()
    ingredients = all_products[all_products['category'] == 'Ingredients'].copy()

    print(f"✅ Found {len(final_products)} final products")
    print(f"✅ Found {len(ingredients)} ingredients")
    print()

    # Generate recipes
    recipes = []
    recipe_ingredients = []

    print("🍰 Generating recipes...")

    for idx, product in final_products.iterrows():
        recipe_uuid = generate_uuid()
        recipe_fb_id = generate_firebase_id()

        # Create recipe
        recipe = {
            'id': recipe_uuid,
            'firebase_id': recipe_fb_id,
            'recipe_id': idx + 1,
            'product_firebase_id': product['id'],
            'product_name': product['name'],
            'instructions': f'Recipe instructions for {product["name"]}. Mix ingredients and bake.',
            'prep_time_minutes': random.randint(15, 45),
            'cook_time_minutes': random.randint(20, 60),
            'servings': random.randint(4, 12),
            'created_at': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'updated_at': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }
        recipes.append(recipe)

        # Get recipe template
        recipe_template = get_recipe_for_product(product['name'])

        # Create ingredients
        for ing_name, quantity, unit in recipe_template:
            # Try to match ingredient from products
            matched_ing = ingredients[ingredients['name'].str.contains(ing_name, case=False, na=False)]

            if len(matched_ing) > 0:
                ing_product = matched_ing.iloc[0]
            elif len(ingredients) > 0:
                # Random ingredient if no match
                ing_product = ingredients.sample(1).iloc[0]
            else:
                # Skip if no ingredients available
                continue

            recipe_ingredient = {
                'id': generate_uuid(),
                'firebase_id': generate_firebase_id(),
                'recipe_firebase_id': recipe_uuid,
                'recipe_id': idx + 1,
                'ingredient_firebase_id': ing_product['id'],
                'ingredient_name': ing_product['name'],
                'quantity_needed': quantity,
                'unit': unit,
                'created_at': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            }
            recipe_ingredients.append(recipe_ingredient)

        if (idx + 1) % 10 == 0:
            print(f"Generated {idx + 1} recipes...")

    print(f"✅ Generated {len(recipes)} recipes")
    print(f"✅ Generated {len(recipe_ingredients)} ingredient mappings")
    print()

    # Save to CSV
    output_dir = 'migration/05_csv_import'
    os.makedirs(output_dir, exist_ok=True)

    recipes_df = pd.DataFrame(recipes)
    recipes_df.to_csv(f'{output_dir}/recipes.csv', index=False)
    print(f"💾 Saved: {output_dir}/recipes.csv")

    ingredients_df = pd.DataFrame(recipe_ingredients)
    ingredients_df.to_csv(f'{output_dir}/recipe_ingredients.csv', index=False)
    print(f"💾 Saved: {output_dir}/recipe_ingredients.csv")

    # Summary
    print()
    print("📊 Recipe Summary:")
    print(f"  Total recipes: {len(recipes)}")
    print(f"  Total ingredient mappings: {len(recipe_ingredients)}")
    print(f"  Avg ingredients per recipe: {len(recipe_ingredients)/len(recipes):.1f}")
    print()
    print("✅ Recipe generation complete!")
    print()
    print("Next: Run python FIXED_generate_sales.py")

if __name__ == '__main__':
    main()
