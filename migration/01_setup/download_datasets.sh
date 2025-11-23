#!/bin/bash

##############################################################################
# Download Kaggle Datasets for Banelo Pastry Shop Migration
# This script downloads bakery and recipe datasets from Kaggle
##############################################################################

set -e  # Exit on error

echo "🔽 Downloading Kaggle Datasets for PostgreSQL Migration..."
echo "================================================================"

# Create datasets directory
mkdir -p ../datasets
cd ../datasets

# Check if Kaggle API is configured
if [ ! -f ~/.kaggle/kaggle.json ]; then
    echo "❌ ERROR: Kaggle API credentials not found!"
    echo ""
    echo "Please follow these steps:"
    echo "1. Go to https://www.kaggle.com/settings"
    echo "2. Scroll to 'API' section"
    echo "3. Click 'Create New Token'"
    echo "4. Save kaggle.json to ~/.kaggle/kaggle.json"
    echo "5. Run: chmod 600 ~/.kaggle/kaggle.json"
    echo ""
    exit 1
fi

echo "✅ Kaggle API credentials found"
echo ""

# Dataset 1: Bakery Sales (Primary dataset for products and sales)
echo "📊 Dataset 1: French Bakery Daily Sales"
echo "  - URL: https://www.kaggle.com/datasets/matthieugimbert/french-bakery-daily-sales"
echo "  - Size: ~234K transactions"
echo "  - Use: Product catalog, sales data, POS transactions"
echo ""

if [ -f "Bakery_sales.csv" ]; then
    echo "  ⏭️  Already downloaded, skipping..."
else
    echo "  Downloading..."
    kaggle datasets download -d matthieugimbert/french-bakery-daily-sales
    unzip -o french-bakery-daily-sales.zip
    rm french-bakery-daily-sales.zip
    echo "  ✅ Downloaded!"
fi
echo ""

# Dataset 2: Food.com Recipes (for recipe and ingredient data)
echo "📊 Dataset 2: Food.com Recipes and Interactions"
echo "  - URL: https://www.kaggle.com/datasets/shuyangli94/food-com-recipes-and-interactions"
echo "  - Size: ~230K recipes, 1M+ ratings"
echo "  - Use: Recipes, ingredients, nutrition data"
echo ""

if [ -f "RAW_recipes.csv" ]; then
    echo "  ⏭️  Already downloaded, skipping..."
else
    echo "  Downloading..."
    kaggle datasets download -d shuyangli94/food-com-recipes-and-interactions
    unzip -o food-com-recipes-and-interactions.zip
    rm food-com-recipes-and-interactions.zip
    echo "  ✅ Downloaded!"
fi
echo ""

# Alternative Dataset 3: Coffee and Bakery Sales (backup/supplementary)
echo "📊 Dataset 3: Coffee Shop Sales (Supplementary)"
echo "  - URL: https://www.kaggle.com/datasets/ylchang/coffee-shop-sample-data-1113"
echo "  - Use: Additional product ideas, pricing reference"
echo ""

if [ -d "coffee-shop-sample-data-1113" ]; then
    echo "  ⏭️  Already downloaded, skipping..."
else
    echo "  Downloading..."
    kaggle datasets download -d ylchang/coffee-shop-sample-data-1113 || {
        echo "  ⚠️  Optional dataset not available, continuing..."
    }
    if [ -f "coffee-shop-sample-data-1113.zip" ]; then
        unzip -o coffee-shop-sample-data-1113.zip -d coffee-shop-sample-data-1113
        rm coffee-shop-sample-data-1113.zip
        echo "  ✅ Downloaded!"
    fi
fi
echo ""

# Display downloaded files
echo "================================================================"
echo "✅ Download Complete! Files in $(pwd):"
echo ""
ls -lh *.csv 2>/dev/null || echo "CSV files will be extracted by Python scripts"
echo ""
echo "Next steps:"
echo "1. Run: cd ../01_setup && bash setup_postgres.sh"
echo "2. Then run transformation scripts in 03_transform/"
echo ""
