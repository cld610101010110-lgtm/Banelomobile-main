#!/usr/bin/env python3
"""
Generate ML-ready datasets from PostgreSQL data
Creates datasets for:
  1. Sales analysis and forecasting
  2. Waste prediction
  3. Inventory optimization
  4. Product recommendations
"""

import pandas as pd
import os
from datetime import datetime


class MLDatasetGenerator:
    """Generate machine learning datasets"""

    def __init__(self, csv_dir: str = '../05_csv_import'):
        self.csv_dir = csv_dir
        self.sales_df = None
        self.products_df = None
        self.waste_df = None

    def load_data(self):
        """Load CSV data"""
        print("📂 Loading data from CSV files...")

        self.sales_df = pd.read_csv(f'{self.csv_dir}/sales.csv')
        self.products_df = pd.read_csv(f'{self.csv_dir}/products.csv')
        self.waste_df = pd.read_csv(f'{self.csv_dir}/waste_logs.csv')

        # Convert dates
        self.sales_df['order_date'] = pd.to_datetime(self.sales_df['order_date'])
        self.waste_df['waste_date'] = pd.to_datetime(self.waste_df['waste_date'])

        print(f"✅ Loaded {len(self.sales_df)} sales, {len(self.products_df)} products, {len(self.waste_df)} waste logs")

    def generate_sales_analysis(self) -> pd.DataFrame:
        """Generate sales analysis dataset"""
        print("\n📊 Generating sales analysis dataset...")

        df = self.sales_df.copy()

        # Add date features
        df['date'] = df['order_date'].dt.date
        df['day_of_week'] = df['order_date'].dt.day_name()
        df['day_of_week_num'] = df['order_date'].dt.dayofweek
        df['month'] = df['order_date'].dt.month
        df['month_name'] = df['order_date'].dt.month_name()
        df['year'] = df['order_date'].dt.year
        df['hour'] = df['order_date'].dt.hour
        df['is_weekend'] = df['day_of_week_num'].isin([5, 6])

        # Calculate revenue
        df['revenue'] = df['quantity'] * df['price']

        # Aggregate by date and product
        agg_df = df.groupby(['date', 'product_firebase_id', 'product_name', 'category']).agg({
            'quantity': 'sum',
            'revenue': 'sum',
            'order_id': 'count',
            'day_of_week': 'first',
            'day_of_week_num': 'first',
            'month': 'first',
            'month_name': 'first',
            'year': 'first',
            'is_weekend': 'first'
        }).reset_index()

        agg_df.rename(columns={'order_id': 'transaction_count'}, inplace=True)

        print(f"✅ Generated {len(agg_df)} sales analysis records")
        return agg_df

    def generate_waste_prediction(self) -> pd.DataFrame:
        """Generate waste prediction dataset"""
        print("\n🗑️  Generating waste prediction dataset...")

        df = self.waste_df.copy()

        # Add date features
        df['date'] = df['waste_date'].dt.date
        df['day_of_week'] = df['waste_date'].dt.day_name()
        df['day_of_week_num'] = df['waste_date'].dt.dayofweek
        df['month'] = df['waste_date'].dt.month
        df['year'] = df['waste_date'].dt.year
        df['is_weekend'] = df['day_of_week_num'].isin([5, 6])

        # Merge with product data
        products = self.products_df[['id', 'price', 'category']].copy()
        products.rename(columns={'id': 'product_firebase_id'}, inplace=True)

        df = df.merge(products, on='product_firebase_id', how='left', suffixes=('', '_prod'))

        # Select relevant columns
        result = df[[
            'date', 'product_firebase_id', 'product_name', 'category',
            'quantity', 'cost_impact', 'reason', 'day_of_week',
            'day_of_week_num', 'month', 'year', 'is_weekend'
        ]]

        print(f"✅ Generated {len(result)} waste prediction records")
        return result

    def generate_inventory_forecast(self) -> pd.DataFrame:
        """Generate inventory forecasting dataset"""
        print("\n📦 Generating inventory forecast dataset...")

        # Calculate sales velocity
        sales = self.sales_df.copy()
        sales['date'] = pd.to_datetime(sales['order_date']).dt.date

        # Aggregate daily sales per product
        daily_sales = sales.groupby(['product_firebase_id', 'product_name', 'date']).agg({
            'quantity': 'sum'
        }).reset_index()

        # Calculate average daily sales
        avg_daily_sales = daily_sales.groupby(['product_firebase_id', 'product_name']).agg({
            'quantity': ['mean', 'std', 'min', 'max']
        }).reset_index()

        avg_daily_sales.columns = ['product_id', 'product_name', 'avg_daily_sales',
                                     'std_daily_sales', 'min_daily_sales', 'max_daily_sales']

        # Merge with current inventory
        products = self.products_df[['id', 'name', 'category', 'quantity',
                                      'inventory_a', 'inventory_b', 'price']].copy()
        products.rename(columns={'id': 'product_id', 'name': 'product_name_prod'}, inplace=True)

        forecast = products.merge(avg_daily_sales, on='product_id', how='left')

        # Fill NaN (products with no sales) with 0
        forecast['avg_daily_sales'].fillna(0, inplace=True)
        forecast['std_daily_sales'].fillna(0, inplace=True)
        forecast['min_daily_sales'].fillna(0, inplace=True)
        forecast['max_daily_sales'].fillna(0, inplace=True)

        # Calculate reorder point (avg_daily_sales * lead_time + safety_stock)
        lead_time_days = 2  # Assume 2-day lead time
        safety_factor = 1.5  # 1.5x std deviation for safety stock

        forecast['lead_time_days'] = lead_time_days
        forecast['safety_stock'] = (forecast['std_daily_sales'] * safety_factor).round()
        forecast['reorder_point'] = (
            forecast['avg_daily_sales'] * lead_time_days + forecast['safety_stock']
        ).round()

        # Days until stockout (if no new inventory)
        forecast['days_until_stockout'] = (
            forecast['quantity'] / forecast['avg_daily_sales'].replace(0, 1)
        ).round()

        # Select columns
        result = forecast[[
            'product_id', 'product_name', 'category', 'quantity',
            'inventory_a', 'inventory_b', 'price',
            'avg_daily_sales', 'std_daily_sales', 'min_daily_sales', 'max_daily_sales',
            'reorder_point', 'safety_stock', 'lead_time_days', 'days_until_stockout'
        ]]

        print(f"✅ Generated {len(result)} inventory forecast records")
        return result

    def generate_product_recommendations(self) -> pd.DataFrame:
        """Generate product recommendation dataset (co-purchase analysis)"""
        print("\n🔗 Generating product recommendations dataset...")

        # Group sales by transaction (same order_date and cashier)
        sales = self.sales_df.copy()
        sales['transaction_key'] = (
            sales['order_date'].astype(str) + '_' +
            sales['cashier_username'].astype(str)
        )

        # Find product pairs in same transaction
        transactions = sales.groupby('transaction_key')['product_firebase_id'].apply(list).reset_index()

        # Create product pairs
        pairs = []
        for _, row in transactions.iterrows():
            products = row['product_firebase_id']
            if len(products) >= 2:
                # Create all pairs
                for i in range(len(products)):
                    for j in range(i + 1, len(products)):
                        pairs.append({
                            'product_a_id': products[i],
                            'product_b_id': products[j]
                        })

        if not pairs:
            print("⚠️  No product pairs found (single-item transactions)")
            return pd.DataFrame(columns=['product_a_id', 'product_a_name', 'product_b_id',
                                        'product_b_name', 'co_purchase_count', 'confidence_score'])

        pairs_df = pd.DataFrame(pairs)

        # Count co-purchases
        co_purchases = pairs_df.groupby(['product_a_id', 'product_b_id']).size().reset_index(name='co_purchase_count')

        # Add product names
        product_names = self.products_df[['id', 'name']].copy()
        product_names.columns = ['product_a_id', 'product_a_name']
        co_purchases = co_purchases.merge(product_names, on='product_a_id', how='left')

        product_names.columns = ['product_b_id', 'product_b_name']
        co_purchases = co_purchases.merge(product_names, on='product_b_id', how='left')

        # Calculate confidence score (normalized by max co-purchases)
        max_count = co_purchases['co_purchase_count'].max()
        co_purchases['confidence_score'] = (co_purchases['co_purchase_count'] / max_count).round(2)

        # Sort by co-purchase count
        result = co_purchases.sort_values('co_purchase_count', ascending=False)

        print(f"✅ Generated {len(result)} product recommendation pairs")
        return result

    def save_datasets(self, output_dir: str = '.'):
        """Generate and save all ML datasets"""
        print("\n" + "=" * 70)
        print("🤖 GENERATING ML-READY DATASETS")
        print("=" * 70)

        os.makedirs(output_dir, exist_ok=True)

        # Generate datasets
        sales_analysis = self.generate_sales_analysis()
        waste_prediction = self.generate_waste_prediction()
        inventory_forecast = self.generate_inventory_forecast()
        product_recommendations = self.generate_product_recommendations()

        # Save to CSV
        sales_analysis.to_csv(f'{output_dir}/sales_analysis.csv', index=False)
        print(f"💾 Saved: {output_dir}/sales_analysis.csv")

        waste_prediction.to_csv(f'{output_dir}/waste_prediction.csv', index=False)
        print(f"💾 Saved: {output_dir}/waste_prediction.csv")

        inventory_forecast.to_csv(f'{output_dir}/inventory_forecast.csv', index=False)
        print(f"💾 Saved: {output_dir}/inventory_forecast.csv")

        product_recommendations.to_csv(f'{output_dir}/product_recommendations.csv', index=False)
        print(f"💾 Saved: {output_dir}/product_recommendations.csv")

        # Summary
        print("\n" + "=" * 70)
        print("📊 ML DATASET SUMMARY")
        print("=" * 70)
        print(f"1. Sales Analysis:            {len(sales_analysis):,} records")
        print(f"2. Waste Prediction:          {len(waste_prediction):,} records")
        print(f"3. Inventory Forecast:        {len(inventory_forecast):,} records")
        print(f"4. Product Recommendations:   {len(product_recommendations):,} pairs")
        print("\n✅ All ML datasets generated successfully!")


def main():
    """Main execution"""
    generator = MLDatasetGenerator()
    generator.load_data()
    generator.save_datasets()


if __name__ == '__main__':
    main()
