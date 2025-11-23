#!/usr/bin/env python3
"""
Master Transformation Script
Orchestrates all data generation scripts in the correct order
"""

import os
import sys
import subprocess
from pathlib import Path


def run_script(script_name: str, description: str):
    """Run a Python script and handle errors"""
    print("\n" + "=" * 70)
    print(f"Running: {description}")
    print("=" * 70)

    result = subprocess.run(
        [sys.executable, script_name],
        cwd=os.path.dirname(os.path.abspath(__file__))
    )

    if result.returncode != 0:
        print(f"\n❌ Error running {script_name}")
        print(f"Exit code: {result.returncode}")
        sys.exit(1)

    print(f"\n✅ {description} completed successfully!")


def main():
    """Main transformation orchestrator"""
    print("""
╔════════════════════════════════════════════════════════════════╗
║                                                                ║
║     BANELO FIRESTORE → POSTGRESQL MIGRATION                    ║
║     Dataset Transformation Pipeline                            ║
║                                                                ║
╚════════════════════════════════════════════════════════════════╝
    """)

    # Ensure we're in the right directory
    script_dir = Path(__file__).parent.absolute()
    os.chdir(script_dir)

    # Check if datasets exist
    dataset_dir = Path('../datasets')
    if not dataset_dir.exists():
        print("\n⚠️  WARNING: Datasets directory not found!")
        print("Please run: cd ../01_setup && bash download_datasets.sh")
        print("\nContinuing with sample data generation...\n")

    # Create output directories
    os.makedirs('../05_csv_import', exist_ok=True)
    os.makedirs('../04_seed_data', exist_ok=True)
    os.makedirs('../06_ml_datasets', exist_ok=True)

    # Transformation pipeline
    scripts = [
        ('generate_users.py', '👥 User Generation'),
        ('generate_products.py', '🛍️  Product Catalog Generation'),
        ('generate_recipes.py', '👨‍🍳 Recipe & Ingredients Generation'),
        ('generate_sales.py', '💳 Sales Transaction Generation'),
        ('generate_waste_logs.py', '🗑️  Waste Log Generation'),
    ]

    # Run each script in sequence
    for script, description in scripts:
        script_path = Path(script)
        if not script_path.exists():
            print(f"❌ Script not found: {script}")
            sys.exit(1)

        run_script(script, description)

    # Final summary
    print("\n" + "=" * 70)
    print("🎉 TRANSFORMATION COMPLETE!")
    print("=" * 70)

    # Count generated files
    csv_dir = Path('../05_csv_import')
    csv_files = list(csv_dir.glob('*.csv'))

    print(f"\n📁 Generated {len(csv_files)} CSV files in {csv_dir}:")
    for csv_file in sorted(csv_files):
        size = csv_file.stat().st_size / 1024  # KB
        print(f"  • {csv_file.name:<30} ({size:,.1f} KB)")

    print("\n" + "=" * 70)
    print("Next Steps:")
    print("=" * 70)
    print("\n1. Create PostgreSQL database:")
    print("   cd ../01_setup")
    print("   bash setup_postgres.sh")
    print("\n2. Create schema:")
    print("   cd ../02_schema")
    print("   psql -U banelo_user -d banelo_db -f schema.sql")
    print("\n3. Import CSV data:")
    print("   cd ../05_csv_import")
    print("   psql -U banelo_user -d banelo_db -f import_all.sql")
    print("\n4. Verify data:")
    print("   psql -U banelo_user -d banelo_db")
    print("   SELECT * FROM v_product_inventory LIMIT 10;")
    print("\n" + "=" * 70)
    print()


if __name__ == '__main__':
    main()
