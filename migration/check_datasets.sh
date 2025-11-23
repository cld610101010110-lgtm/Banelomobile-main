#!/bin/bash
##############################################################################
# Check if Kaggle datasets have been downloaded
##############################################################################

echo "🔍 Checking for Kaggle datasets..."
echo ""

DATASET_DIR="migration/datasets"

if [ ! -d "$DATASET_DIR" ]; then
    echo "❌ Datasets directory not found: $DATASET_DIR"
    echo ""
    echo "To download datasets:"
    echo "  cd migration/01_setup"
    echo "  bash download_datasets.sh"
    echo ""
    echo "Or run without datasets (will use sample data):"
    echo "  cd migration/03_transform"
    echo "  python3 transform_dataset.py"
    exit 1
fi

echo "📁 Datasets directory: $DATASET_DIR"
echo ""

# Check for key files
FILES=(
    "Bakery_sales.csv"
    "RAW_recipes.csv"
    "RAW_interactions.csv"
)

FOUND=0
for file in "${FILES[@]}"; do
    if [ -f "$DATASET_DIR/$file" ]; then
        SIZE=$(du -h "$DATASET_DIR/$file" | cut -f1)
        echo "✅ $file ($SIZE)"
        FOUND=$((FOUND + 1))
    else
        echo "❌ $file (not found)"
    fi
done

echo ""
if [ $FOUND -gt 0 ]; then
    echo "✅ Found $FOUND dataset file(s)"
    echo "   Ready to transform data!"
    echo "   Run: cd migration/03_transform && python3 transform_dataset.py"
else
    echo "⚠️  No datasets found"
    echo ""
    echo "Options:"
    echo "  1. Download from Kaggle:"
    echo "     - Setup Kaggle API (see BEGINNERS_GUIDE.md)"
    echo "     - Run: cd migration/01_setup && bash download_datasets.sh"
    echo ""
    echo "  2. Use sample data (no download needed):"
    echo "     - Run: cd migration/03_transform && python3 transform_dataset.py"
    echo "     - Scripts will auto-generate realistic sample data"
fi
