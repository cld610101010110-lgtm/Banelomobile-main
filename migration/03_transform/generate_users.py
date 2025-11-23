"""
Generate user accounts (staff and managers)
Creates seed data for the users table
"""

import pandas as pd
from datetime import datetime, timedelta
import random
from typing import List, Dict
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils import generate_uuid, generate_firebase_id, random_date


class UserGenerator:
    """Generate user accounts for the system"""

    FIRST_NAMES = [
        "Maria", "Juan", "Ana", "Pedro", "Sofia", "Miguel",
        "Isabella", "Carlos", "Gabriela", "Luis", "Carmen",
        "Diego", "Elena", "Fernando", "Rosa", "Antonio"
    ]

    LAST_NAMES = [
        "Santos", "Reyes", "Cruz", "Ramos", "Garcia",
        "Mendoza", "Torres", "Flores", "Rivera", "Gonzales",
        "Bautista", "Dela Cruz", "Martinez", "Fernandez"
    ]

    MIDDLE_NAMES = [
        "M.", "S.", "R.", "C.", "A.", "P.", "L.", "G.", "D.", "F."
    ]

    def __init__(self):
        self.users = []

    def generate_users(self, num_staff: int = 10, num_managers: int = 3) -> List[Dict]:
        """Generate user accounts"""
        print(f"\n👥 Generating {num_staff} staff and {num_managers} managers...")

        users = []

        # Create admin user first
        admin = {
            'id': generate_uuid(),
            'firebase_id': generate_firebase_id(),
            'fname': 'Admin',
            'lname': 'User',
            'mname': 'A.',
            'username': 'admin',
            'auth_email': 'admin@banelo.com',
            'role': 'manager',
            'status': 'active',
            'joined_date': datetime.now() - timedelta(days=730),  # 2 years ago
            'created_at': datetime.now(),
            'updated_at': datetime.now()
        }
        users.append(admin)

        # Generate managers
        for i in range(num_managers):
            fname = random.choice(self.FIRST_NAMES)
            lname = random.choice(self.LAST_NAMES)
            mname = random.choice(self.MIDDLE_NAMES)
            username = f"{fname.lower()}.{lname.lower().replace(' ', '')}"

            manager = {
                'id': generate_uuid(),
                'firebase_id': generate_firebase_id(),
                'fname': fname,
                'lname': lname,
                'mname': mname,
                'username': username,
                'auth_email': f"{username}@banelo.com",
                'role': 'manager',
                'status': 'active',
                'joined_date': random_date(
                    datetime.now() - timedelta(days=365*2),
                    datetime.now() - timedelta(days=180)
                ),
                'created_at': datetime.now(),
                'updated_at': datetime.now()
            }
            users.append(manager)

        # Generate staff
        for i in range(num_staff):
            fname = random.choice(self.FIRST_NAMES)
            lname = random.choice(self.LAST_NAMES)
            mname = random.choice(self.MIDDLE_NAMES)
            username = f"{fname.lower()}.{lname.lower().replace(' ', '')}{i+1}"

            # Some staff might be inactive
            status = 'active' if random.random() > 0.1 else 'inactive'

            staff = {
                'id': generate_uuid(),
                'firebase_id': generate_firebase_id(),
                'fname': fname,
                'lname': lname,
                'mname': mname,
                'username': username,
                'auth_email': f"{username}@banelo.com",
                'role': 'staff',
                'status': status,
                'joined_date': random_date(
                    datetime.now() - timedelta(days=365),
                    datetime.now() - timedelta(days=30)
                ),
                'created_at': datetime.now(),
                'updated_at': datetime.now()
            }
            users.append(staff)

        self.users = users
        print(f"✅ Generated {len(users)} users")
        return users

    def save_to_csv(self, output_path: str):
        """Save users to CSV"""
        df = pd.DataFrame(self.users)

        # Format timestamps
        df['joined_date'] = pd.to_datetime(df['joined_date']).dt.strftime('%Y-%m-%d %H:%M:%S')
        df['created_at'] = pd.to_datetime(df['created_at']).dt.strftime('%Y-%m-%d %H:%M:%S')
        df['updated_at'] = pd.to_datetime(df['updated_at']).dt.strftime('%Y-%m-%d %H:%M:%S')

        df.to_csv(output_path, index=False)
        print(f"💾 Saved users to: {output_path}")

        # Summary
        print(f"\n📊 User Summary:")
        print(f"  Total users: {len(df)}")
        print(f"\n  By role:")
        print(df['role'].value_counts())
        print(f"\n  By status:")
        print(df['status'].value_counts())

    def generate_sql_insert(self, output_path: str):
        """Generate SQL INSERT statements"""
        df = pd.DataFrame(self.users)

        with open(output_path, 'w') as f:
            f.write("-- User seed data\n")
            f.write("-- Generated by generate_users.py\n\n")

            for _, user in df.iterrows():
                joined = pd.to_datetime(user['joined_date']).strftime('%Y-%m-%d %H:%M:%S')

                sql = f"""INSERT INTO users (id, firebase_id, fname, lname, mname, username, auth_email, role, status, joined_date)
VALUES ('{user['id']}', '{user['firebase_id']}', '{user['fname']}', '{user['lname']}', '{user['mname']}',
        '{user['username']}', '{user['auth_email']}', '{user['role']}', '{user['status']}', '{joined}');\n\n"""
                f.write(sql)

        print(f"💾 Saved SQL to: {output_path}")


def main():
    """Main execution"""
    print("=" * 70)
    print("👤 USER GENERATOR")
    print("=" * 70)

    generator = UserGenerator()
    users = generator.generate_users(num_staff=15, num_managers=4)

    # Save to CSV
    csv_dir = '../05_csv_import'
    os.makedirs(csv_dir, exist_ok=True)
    generator.save_to_csv(f'{csv_dir}/users.csv')

    # Also save to seed data directory
    seed_dir = '../04_seed_data'
    os.makedirs(seed_dir, exist_ok=True)
    generator.save_to_csv(f'{seed_dir}/users_seed.csv')
    generator.generate_sql_insert(f'{seed_dir}/seed_users.sql')

    print("\n✅ User generation complete!")


if __name__ == '__main__':
    main()
