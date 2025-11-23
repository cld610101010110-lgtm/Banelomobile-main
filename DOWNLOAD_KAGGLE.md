# 📥 How to Download Kaggle Datasets

## Step-by-Step Guide for Complete Beginners

### Step 1: Create Kaggle Account (5 minutes)

1. Go to **https://www.kaggle.com/**
2. Click **"Register"** (top right)
3. Sign up with Google or email
4. Verify your email

### Step 2: Get API Token (2 minutes)

1. Login to Kaggle
2. Click your **profile picture** (top right corner)
3. Click **"Settings"** from dropdown
4. Scroll down to **"API"** section
5. Click **"Create New API Token"** button
6. A file called `kaggle.json` will download

**The file looks like this:**
```json
{
  "username": "your_username",
  "key": "abc123xyz789..."
}
```

### Step 3: Install Kaggle API Token (3 minutes)

#### On Linux/Mac:
```bash
# Create kaggle directory
mkdir -p ~/.kaggle

# Move the downloaded file
mv ~/Downloads/kaggle.json ~/.kaggle/

# Set permissions (IMPORTANT!)
chmod 600 ~/.kaggle/kaggle.json

# Verify it's there
cat ~/.kaggle/kaggle.json
```

#### On Windows:
```cmd
# Create directory
mkdir %HOMEPATH%\.kaggle

# Move file (adjust path if needed)
move %HOMEPATH%\Downloads\kaggle.json %HOMEPATH%\.kaggle\

# Verify
type %HOMEPATH%\.kaggle\kaggle.json
```

### Step 4: Install Kaggle Python Package (1 minute)

```bash
pip3 install kaggle
```

### Step 5: Download the Datasets (10 minutes)

```bash
cd migration/01_setup
bash download_datasets.sh
```

**This downloads:**
- ✅ French Bakery Daily Sales (~20 MB)
- ✅ Food.com Recipes and Interactions (~500 MB)
- ✅ Coffee Shop Sales (~5 MB)

**Progress will look like:**
```
🔽 Downloading Kaggle Datasets for PostgreSQL Migration...
================================================================

📊 Dataset 1: French Bakery Daily Sales
  Downloading...
  ✅ Downloaded!

📊 Dataset 2: Food.com Recipes and Interactions
  Downloading...
  ✅ Downloaded!

✅ Download Complete!
```

### Step 6: Verify Download

```bash
cd ..
bash check_datasets.sh
```

**Should show:**
```
🔍 Checking for Kaggle datasets...

📁 Datasets directory: migration/datasets

✅ Bakery_sales.csv (20M)
✅ RAW_recipes.csv (500M)
✅ RAW_interactions.csv (100M)

✅ Found 3 dataset file(s)
   Ready to transform data!
```

### Step 7: Run Transformation with Real Data

```bash
cd migration/03_transform
python3 transform_dataset.py
```

Now it will use the **real Food.com recipes** instead of generated data!

---

## 🔍 What's in the Food.com Dataset?

**File:** `RAW_recipes.csv`

**Contents:**
- 230,000+ real recipes
- Recipe names (e.g., "Best Chocolate Cake", "Classic Croissant")
- Ingredient lists
- Cooking instructions
- Prep time, cook time
- Nutrition info

**Sample from dataset:**
```csv
name,ingredients,steps,minutes,n_steps
"chocolate cake","['flour', 'sugar', 'cocoa powder', 'eggs', 'butter', 'vanilla']","Mix dry ingredients, add wet ingredients, bake at 350F",45,8
"french croissant","['flour', 'butter', 'yeast', 'milk', 'sugar', 'salt']","Make dough, fold butter, rest, shape, proof, bake",180,12
"blueberry muffin","['flour', 'sugar', 'eggs', 'milk', 'blueberries', 'baking powder']","Mix ingredients, fold in berries, bake at 375F",30,5
```

**VS Sample Data (what you get without Kaggle):**
```python
# Auto-generated from predefined lists
PASTRIES = [
    "Croissant", "Chocolate Croissant", "Almond Croissant",
    "Baguette", "Sourdough Bread", "Ciabatta",
    "Chocolate Cake", "Vanilla Cake", "Red Velvet Cake",
    # ... etc
]
```

---

## 📊 Comparison: Real Data vs Sample Data

| Feature | With Food.com | Without (Sample) |
|---------|--------------|------------------|
| **Product Names** | Real recipes from dataset | Predefined realistic names |
| **Recipe Instructions** | Actual cooking steps | Generic placeholders |
| **Ingredients** | Real ingredient lists | Random from common ingredients |
| **Variety** | 230,000+ unique recipes | ~50 predefined products |
| **Download Size** | ~500 MB | None |
| **Setup Time** | 15 minutes | 2 minutes |
| **For Testing** | Overkill | Perfect |
| **For Production** | Better | Good enough |

---

## ⚠️ Common Issues

### "Permission denied" when downloading
**Solution:**
```bash
chmod 600 ~/.kaggle/kaggle.json
```

### "Unauthorized" or "403 Forbidden"
**Solution:**
- Make sure you accepted the dataset terms on Kaggle website
- Visit each dataset page and click "Download" once (accept terms)
  - https://www.kaggle.com/datasets/matthieugimbert/french-bakery-daily-sales
  - https://www.kaggle.com/datasets/shuyangli94/food-com-recipes-and-interactions

### Download is very slow
**Solution:**
- The Food.com dataset is 500MB, it takes time
- You can skip it and use sample data instead

### Can't find kaggle.json file
**Solution:**
- Check your Downloads folder
- On browser, go to Kaggle Settings → API → Create New Token again

---

## 💡 My Recommendation

**For your first test:** **Skip Kaggle, use sample data**
- Run `bash QUICKSTART.sh` (already created for you)
- Test the migration
- See if everything works
- Takes 5 minutes

**If you like it and want more variety:** Download Kaggle datasets
- Follow this guide
- Re-run the transformation
- Get 230,000 real recipes

---

## 🎯 Quick Decision Guide

**Choose Sample Data if:**
- ✅ You want to test quickly
- ✅ You don't have Kaggle account
- ✅ You're okay with 500 predefined products
- ✅ Internet is slow

**Choose Kaggle Data if:**
- ✅ You want maximum variety
- ✅ You want real recipe instructions
- ✅ You're deploying to production
- ✅ You have time to download 500MB

---

**Questions? Just ask!**
