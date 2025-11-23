"""
Utility functions for data transformation
"""

import random
import string
from datetime import datetime, timedelta
from typing import List, Dict
import uuid


def generate_uuid() -> str:
    """Generate a UUID string"""
    return str(uuid.uuid4())


def generate_firebase_id() -> str:
    """Generate a Firebase-style document ID (20 chars)"""
    chars = string.ascii_letters + string.digits
    return ''.join(random.choices(chars, k=20))


def random_date(start_date: datetime, end_date: datetime) -> datetime:
    """Generate a random datetime between start and end dates"""
    delta = end_date - start_date
    random_days = random.randint(0, delta.days)
    random_seconds = random.randint(0, 86400)
    return start_date + timedelta(days=random_days, seconds=random_seconds)


def clean_text(text: str) -> str:
    """Clean and normalize text"""
    if not text:
        return ""
    return str(text).strip().replace('\n', ' ').replace('\r', '')


def safe_float(value, default=0.0) -> float:
    """Safely convert to float"""
    try:
        return float(value)
    except (ValueError, TypeError):
        return default


def safe_int(value, default=0) -> int:
    """Safely convert to integer"""
    try:
        return int(value)
    except (ValueError, TypeError):
        return default


class PastryNameGenerator:
    """Generate realistic pastry/bakery product names"""

    PASTRIES = [
        "Croissant", "Chocolate Croissant", "Almond Croissant",
        "Baguette", "Sourdough Bread", "Ciabatta", "Focaccia",
        "Chocolate Cake", "Vanilla Cake", "Red Velvet Cake",
        "Cheesecake", "Tiramisu", "Eclair", "Macaron",
        "Donut", "Glazed Donut", "Chocolate Donut",
        "Cinnamon Roll", "Danish Pastry", "Apple Turnover",
        "Blueberry Muffin", "Chocolate Chip Muffin", "Banana Bread",
        "Brownie", "Cookie", "Chocolate Chip Cookie",
        "Cupcake", "Scone", "Bagel", "Pretzel"
    ]

    BEVERAGES = [
        "Espresso", "Americano", "Cappuccino", "Latte",
        "Mocha", "Macchiato", "Flat White",
        "Iced Coffee", "Iced Latte", "Iced Mocha",
        "Hot Chocolate", "Tea", "Green Tea", "Chai Latte",
        "Orange Juice", "Apple Juice", "Lemonade",
        "Smoothie", "Milkshake", "Frappe"
    ]

    INGREDIENTS = [
        "Flour", "Sugar", "Butter", "Eggs", "Milk",
        "Chocolate", "Cocoa Powder", "Vanilla Extract",
        "Baking Powder", "Baking Soda", "Salt", "Yeast",
        "Cream Cheese", "Heavy Cream", "Almonds", "Walnuts",
        "Cinnamon", "Nutmeg", "Honey", "Maple Syrup",
        "Coffee Beans", "Tea Leaves", "Fruit Preserves"
    ]

    @classmethod
    def get_random_pastry(cls) -> str:
        return random.choice(cls.PASTRIES)

    @classmethod
    def get_random_beverage(cls) -> str:
        return random.choice(cls.BEVERAGES)

    @classmethod
    def get_random_ingredient(cls) -> str:
        return random.choice(cls.INGREDIENTS)

    @classmethod
    def get_random_product(cls, category: str = None) -> str:
        """Get random product by category"""
        if category == "Pastries":
            return cls.get_random_pastry()
        elif category == "Beverages":
            return cls.get_random_beverage()
        elif category == "Ingredients":
            return cls.get_random_ingredient()
        else:
            # Random category
            cat = random.choice(["Pastries", "Beverages", "Ingredients"])
            return cls.get_random_product(cat)


class PriceGenerator:
    """Generate realistic prices for bakery products"""

    PRICE_RANGES = {
        "Pastries": (2.50, 8.50),
        "Beverages": (2.00, 6.50),
        "Ingredients": (1.00, 15.00)
    }

    @classmethod
    def get_price(cls, category: str) -> float:
        """Get a realistic price for category"""
        min_price, max_price = cls.PRICE_RANGES.get(category, (2.00, 10.00))
        price = random.uniform(min_price, max_price)
        return round(price, 2)

    @classmethod
    def get_cost_per_unit(cls, price: float) -> float:
        """Get cost per unit (typically 30-50% of price)"""
        margin = random.uniform(0.3, 0.5)
        cost = price * margin
        return round(cost, 2)


def normalize_category(category: str) -> str:
    """Normalize category names to standard format"""
    category_map = {
        'pastry': 'Pastries',
        'pastries': 'Pastries',
        'bread': 'Pastries',
        'cake': 'Pastries',
        'beverage': 'Beverages',
        'beverages': 'Beverages',
        'drink': 'Beverages',
        'coffee': 'Beverages',
        'ingredient': 'Ingredients',
        'ingredients': 'Ingredients',
    }

    return category_map.get(category.lower(), 'Pastries')


def generate_realistic_inventory() -> Dict[str, int]:
    """Generate realistic inventory levels"""
    inventory_a = random.randint(50, 500)  # Warehouse
    inventory_b = random.randint(10, 100)   # Display
    total = inventory_a + inventory_b

    return {
        'inventory_a': inventory_a,
        'inventory_b': inventory_b,
        'quantity': total
    }


def format_timestamp(dt: datetime) -> str:
    """Format datetime to PostgreSQL timestamp format"""
    return dt.strftime('%Y-%m-%d %H:%M:%S')


def get_payment_mode() -> str:
    """Get random payment mode with realistic distribution"""
    modes = ['Cash'] * 60 + ['GCash'] * 30 + ['Card'] * 10
    return random.choice(modes)


def generate_gcash_reference() -> str:
    """Generate a GCash-style reference ID"""
    timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
    random_suffix = ''.join(random.choices(string.digits, k=6))
    return f"GCASH{timestamp}{random_suffix}"


def get_waste_reasons() -> List[str]:
    """Get common waste reasons"""
    return [
        "End of day waste",
        "Expired product",
        "Damaged during handling",
        "Customer return",
        "Quality issue",
        "Overproduction",
        "Burnt/Overcooked",
        "Display sample"
    ]


def get_audit_actions() -> List[str]:
    """Get common audit log actions"""
    return [
        "LOGIN",
        "LOGOUT",
        "SALE_TRANSACTION",
        "INVENTORY_UPDATE",
        "PRODUCT_ADD",
        "PRODUCT_UPDATE",
        "PRODUCT_DELETE",
        "WASTE_LOG",
        "INVENTORY_TRANSFER",
        "RECIPE_CREATE",
        "RECIPE_UPDATE",
        "USER_CREATE",
        "USER_UPDATE",
        "REPORT_GENERATE"
    ]


def progress_bar(current: int, total: int, prefix: str = '', length: int = 50):
    """Display a progress bar"""
    percent = 100 * (current / float(total))
    filled = int(length * current // total)
    bar = '█' * filled + '-' * (length - filled)
    print(f'\r{prefix} |{bar}| {percent:.1f}% ({current}/{total})', end='')
    if current == total:
        print()
