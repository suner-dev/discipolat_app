#!/usr/bin/env python3
"""
Data generator for performance testing Discipolat/Church OS.
Generates realistic datasets for load testing.
"""

import json
import random
import uuid
from datetime import datetime, timedelta
from faker import Faker
import argparse
import sys

fake = Faker('fr_FR')

# Constants
DEPARTMENT_TYPES = ['AUDIOVISUAL', 'CHOIR', 'LOGISTICS', 'FINANCE', 'PRAYER', 'CHILDREN', 'YOUTH', 'FAMILY', 'EVANGELISM', 'MAINTENANCE', 'BOOKSTORE', 'MEDIA', 'DISCIPLESHIP', 'ACCUEIL', 'PROTOCOLE', 'COORDINATION', 'INTERCESSION', 'SECURITE', 'COMMUNICATION', 'HEALTH']
SPACE_TYPES = ['DEPARTMENT', 'FAMILY', 'SUB_TEAM']
ROLES = ['ADMIN', 'PASTEUR', 'DEPARTMENT_LEADER', 'FAMILY_LEADER', 'MEMBER', 'DISCIPLE_MAKER']
EVENT_TYPES = ['CULTE', 'REUNION', 'FORMATION', 'EVANGELISATION', 'PRIERE', 'EVENEMENT_SPECIAL', 'CAMP', 'RETRAITE']
ASSET_CATEGORIES = ['SON', 'VIDEO', 'LUMIERE', 'INFORMATIQUE', 'MOBILIER', 'VEHICULE', 'INSTRUMENT', 'AUTRE']
CURRENCIES = ['EUR', 'XAF', 'USD']
COUNTRIES = ['FR', 'CM', 'CI', 'SN', 'ML', 'BF', 'US', 'CA', 'BE', 'CH']


def generate_tenant(index):
    """Generate a tenant (church/organization)."""
    country = random.choice(COUNTRIES)
    currency = 'XAF' if country in ['CM', 'CI', 'SN', 'ML', 'BF'] else ('EUR' if country in ['FR', 'BE', 'CH'] else 'USD')

    return {
        'id': str(uuid.uuid4()),
        'name': f"Église {fake.city()} {index}",
        'slug': f"eglise-{fake.slug()}-{index}",
        'legal_name': f"Association {fake.company()}",
        'country': country,
        'city': fake.city(),
        'timezone': fake.timezone(),
        'currency': currency,
        'language': 'fr',
        'status': 'ACTIVE',
        'offline_mode': 'FIELD_OPS',
        'created_at': (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat(),
        'updated_at': datetime.now().isoformat(),
    }


def generate_organization_unit(tenant_id, parent_id=None, level=0, index=0):
    """Generate an organization unit (church, campus, department, etc.)."""
    if level == 0:
        unit_type = 'CHURCH'
        name = f"Église Principale {index}"
    elif level == 1:
        unit_type = random.choice(['CAMPUS', 'MINISTRY'])
        name = f"{unit_type} {fake.city()} {index}"
    elif level == 2:
        unit_type = random.choice(['DEPARTMENT', 'SUB_DEPARTMENT', 'TEAM', 'CELL', 'GROUP', 'FAMILY'])
        name = f"{unit_type} {fake.word().capitalize()} {index}"
    else:
        unit_type = 'SUB_DEPARTMENT'
        name = f"Sous-{fake.word().capitalize()} {index}"

    return {
        'id': str(uuid.uuid4()),
        'tenant_id': tenant_id,
        'parent_id': parent_id,
        'name': name,
        'code': f"{unit_type[:3].upper()}{index:03d}",
        'type': unit_type,
        'description': fake.sentence(),
        'status': 'ACTIVE',
        'icon': fake.word(),
        'color': f"#{random.randint(0, 0xFFFFFF):06x}",
        'sort_order': index,
        'config_source': 'DEFAULT',
        'path': f"/{parent_id}/{uuid.uuid4().hex[:8]}" if parent_id else f"/{uuid.uuid4().hex[:8]}",
        'level': level,
        'created_at': (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat(),
        'updated_at': datetime.now().isoformat(),
    }


def generate_space(tenant_id, org_unit_id, index):
    """Generate a space (department/family/sub-team)."""
    space_type = random.choice(SPACE_TYPES)
    template = random.choice(DEPARTMENT_TYPES) if space_type == 'DEPARTMENT' else 'FAMILY'

    return {
        'id': str(uuid.uuid4()),
        'tenant_id': tenant_id,
        'organization_unit_id': org_unit_id,
        'space_type': space_type,
        'template_code': template,
        'name': f"{space_type} {fake.word().capitalize()} {index}",
        'code': f"{space_type[:3].upper()}{index:03d}",
        'icon': fake.word(),
        'color': f"#{random.randint(0, 0xFFFFFF):06x}",
        'description': fake.paragraph(),
        'status': 'ACTIVE',
        'visible_people_scope': random.choice(['CAMPUS', 'CHURCH']),
        'configuration_json': json.dumps({
            'modules': random.sample(['people', 'events', 'tasks', 'assets', 'finance', 'reports'], k=random.randint(2, 5)),
            'customColors': {'primary': f"#{random.randint(0, 0xFFFFFF):06x}"},
            'dashboardWidgets': ['upcoming_events', 'asset_status']
        }),
        'created_at': (datetime.now() - timedelta(days=random.randint(1, 180))).isoformat(),
        'updated_at': datetime.now().isoformat(),
    }


def generate_person(tenant_id, index):
    """Generate a person."""
    return {
        'id': str(uuid.uuid4()),
        'tenant_id': tenant_id,
        'first_name': fake.first_name(),
        'last_name': fake.last_name(),
        'display_name': None,
        'gender': random.choice(['M', 'F', None]),
        'birth_date': (datetime.now() - timedelta(days=random.randint(365*18, 365*80))).date().isoformat(),
        'phone_normalized': f"+33{fake.msisdn()[:9]}",
        'email_normalized': fake.unique.email(),
        'address': fake.address(),
        'photo_url': None,
        'status': 'ACTIVE',
        'visibility_scope': 'CHURCH',
        'created_at': (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat(),
        'updated_at': datetime.now().isoformat(),
        'deleted_at': None,
    }


def generate_membership(tenant_id, person_id, index):
    """Generate a membership."""
    statuses = ['MEMBRE', 'NOUVEAU', 'VISITEUR', 'ANCIEN']
    return {
        'id': str(uuid.uuid4()),
        'tenant_id': tenant_id,
        'person_id': person_id,
        'membership_status': random.choice(statuses),
        'joined_at': (datetime.now() - timedelta(days=random.randint(1, 1000))).date().isoformat(),
        'left_at': None,
        'source': random.choice(['SELF_REGISTRATION', 'ADMIN_CREATED', 'INVITATION', 'IMPORT']),
        'notes': None,
        'created_at': datetime.now().isoformat(),
        'updated_at': datetime.now().isoformat(),
    }


def generate_space_membership(tenant_id, person_id, space_id, index):
    """Generate a space membership."""
    return {
        'id': str(uuid.uuid4()),
        'tenant_id': tenant_id,
        'person_id': person_id,
        'space_id': space_id,
        'joined_at': (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat(),
        'left_at': None,
        'status': 'ACTIVE',
        'membership_type': random.choice(['MEMBER', 'LEADER', 'CO_LEADER']),
        'responsibility': fake.sentence() if random.random() > 0.5 else None,
        'notes': None,
        'created_at': datetime.now().isoformat(),
        'updated_at': datetime.now().isoformat(),
    }


def generate_event(tenant_id, space_id, index):
    """Generate an event."""
    start = datetime.now() + timedelta(days=random.randint(-30, 90))
    end = start + timedelta(hours=random.randint(1, 4))

    return {
        'id': str(uuid.uuid4()),
        'tenant_id': tenant_id,
        'space_id': space_id,
        'type_evenement': random.choice(EVENT_TYPES),
        'titre': f"{fake.catch_phrase()} {index}",
        'description': fake.paragraph(),
        'lieu': fake.address(),
        'date_debut': start.isoformat(),
        'date_fin': end.isoformat(),
        'limite_places': random.randint(20, 500) if random.random() > 0.5 else None,
        'created_at': datetime.now().isoformat(),
        'updated_at': datetime.now().isoformat(),
    }


def generate_asset(tenant_id, space_id, index):
    """Generate an asset."""
    return {
        'id': str(uuid.uuid4()),
        'tenant_id': tenant_id,
        'space_id': space_id,
        'name': f"{random.choice(ASSET_CATEGORIES)} {fake.word().capitalize()} {index}",
        'serial_number': fake.unique.bothify(text='??-####-??'),
        'category': random.choice(ASSET_CATEGORIES),
        'brand': fake.company(),
        'model': fake.word(),
        'purchase_date': (datetime.now() - timedelta(days=random.randint(30, 1800))).date().isoformat(),
        'purchase_price': round(random.uniform(100, 50000), 2),
        'currency': 'EUR',
        'status': random.choice(['AVAILABLE', 'IN_USE', 'MAINTENANCE', 'RETIRED']),
        'location': fake.word(),
        'condition': random.choice(['NEW', 'GOOD', 'FAIR', 'POOR']),
        'warranty_expiry': (datetime.now() + timedelta(days=random.randint(30, 1000))).date().isoformat() if random.random() > 0.3 else None,
        'notes': fake.sentence(),
        'created_at': datetime.now().isoformat(),
        'updated_at': datetime.now().isoformat(),
    }


def generate_financial_transaction(tenant_id, space_id, person_id, index):
    """Generate a financial transaction."""
    types = ['EXPENSE', 'INCOME', 'DONATION', 'CONTRIBUTION']
    categories = ['SALAIRE', 'MATERIEL', 'LOYER', 'EVENT', 'MISSION', 'AUTRE']

    return {
        'id': str(uuid.uuid4()),
        'tenant_id': tenant_id,
        'space_id': space_id,
        'person_id': person_id,
        'type': random.choice(types),
        'category': random.choice(categories),
        'amount': round(random.uniform(10, 5000), 2),
        'currency': 'EUR',
        'description': fake.sentence(),
        'date': (datetime.now() - timedelta(days=random.randint(1, 365))).date().isoformat(),
        'payment_method': random.choice(['CASH', 'CARD', 'TRANSFER', 'MOBILE_MONEY', 'CHECK']),
        'status': random.choice(['COMPLETED', 'PENDING', 'CANCELLED']),
        'reference': fake.bothify(text='REF-####-??'),
        'created_at': datetime.now().isoformat(),
        'updated_at': datetime.now().isoformat(),
    }


def generate_dress_code(tenant_id, space_id, index):
    """Generate a dress code."""
    groups = ['Hommes', 'Femmes', 'Garçons', 'Filles', 'Lead', 'Chœurs', 'Musiciens', 'Accueil', 'Protocole', 'Enfants']

    return {
        'id': str(uuid.uuid4()),
        'tenant_id': tenant_id,
        'space_id': space_id,
        'event_id': None,
        'service_name': f"Culte du {fake.day_of_week()} {index}",
        'title': f"Tenue {fake.word().capitalize()} {index}",
        'begins_at': (datetime.now() + timedelta(days=random.randint(1, 30))).replace(hour=8, minute=0).isoformat(),
        'ends_at': (datetime.now() + timedelta(days=random.randint(1, 30))).replace(hour=12, minute=0).isoformat(),
        'status': random.choice(['DRAFT', 'PUBLISHED', 'ARCHIVED']),
        'created_by': str(uuid.uuid4()),
        'archived': False,
        'created_at': datetime.now().isoformat(),
        'updated_at': datetime.now().isoformat(),
    }


def generate_dataset(num_tenants=10, persons_per_tenant=1000, spaces_per_tenant=20, events_per_space=50, assets_per_space=20):
    """Generate complete dataset."""
    print(f"Generating dataset: {num_tenants} tenants, {persons_per_tenant} persons/tenant, {spaces_per_tenant} spaces/tenant...")

    dataset = {
        'tenants': [],
        'organization_units': [],
        'spaces': [],
        'persons': [],
        'memberships': [],
        'space_memberships': [],
        'events': [],
        'assets': [],
        'financial_transactions': [],
        'dress_codes': [],
    }

    for t in range(num_tenants):
        tenant = generate_tenant(t)
        dataset['tenants'].append(tenant)
        tenant_id = tenant['id']

        # Organization units (hierarchy)
        church = generate_organization_unit(tenant_id, level=0, index=0)
        dataset['organization_units'].append(church)

        # Campus
        num_campuses = random.randint(1, 3)
        campuses = []
        for c in range(num_campuses):
            campus = generate_organization_unit(tenant_id, church['id'], level=1, index=c)
            dataset['organization_units'].append(campus)
            campuses.append(campus)

        # Departments/Ministries
        for c, campus in enumerate(campuses):
            num_depts = random.randint(3, 8)
            for d in range(num_depts):
                dept = generate_organization_unit(tenant_id, campus['id'], level=2, index=d)
                dataset['organization_units'].append(dept)

                # Spaces for this department
                for s in range(spaces_per_tenant // num_depts):
                    space = generate_space(tenant_id, dept['id'], s)
                    dataset['spaces'].append(space)

        # Persons
        for p in range(persons_per_tenant):
            person = generate_person(tenant_id, p)
            dataset['persons'].append(person)
            dataset['memberships'].append(generate_membership(tenant_id, person['id'], p))

            # Assign to random spaces
            tenant_spaces = [s for s in dataset['spaces'] if s['tenant_id'] == tenant_id]
            if tenant_spaces:
                for _ in range(random.randint(1, 3)):
                    space = random.choice(tenant_spaces)
                    dataset['space_memberships'].append(generate_space_membership(tenant_id, person['id'], space['id'], p))

        # Events per space
        tenant_spaces = [s for s in dataset['spaces'] if s['tenant_id'] == tenant_id]
        for space in tenant_spaces:
            for e in range(events_per_space // len(tenant_spaces) if tenant_spaces else 1):
                dataset['events'].append(generate_event(tenant_id, space['id'], e))

        # Assets per space
        for space in tenant_spaces:
            for a in range(assets_per_space // len(tenant_spaces) if tenant_spaces else 1):
                dataset['assets'].append(generate_asset(tenant_id, space['id'], a))

        # Financial transactions
        tenant_persons = [p for p in dataset['persons'] if p['tenant_id'] == tenant_id]
        for _ in range(100):
            space = random.choice(tenant_spaces) if tenant_spaces else None
            person = random.choice(tenant_persons) if tenant_persons else None
            if space and person:
                dataset['financial_transactions'].append(generate_financial_transaction(tenant_id, space['id'], person['id'], _))

        # Dress codes
        for space in tenant_spaces:
            for d in range(5):
                dataset['dress_codes'].append(generate_dress_code(tenant_id, space['id'], d))

    print(f"Generated:")
    for key, value in dataset.items():
        print(f"  {key}: {len(value)} records")

    return dataset


def save_dataset(dataset, output_dir):
    """Save dataset to JSON files."""
    import os
    os.makedirs(output_dir, exist_ok=True)

    for key, value in dataset.items():
        filepath = os.path.join(output_dir, f"{key}.json")
        with open(filepath, 'w') as f:
            json.dump(value, f, indent=2, default=str)
        print(f"Saved {filepath}")


def main():
    parser = argparse.ArgumentParser(description='Generate performance test data for Discipolat')
    parser.add_argument('--tenants', type=int, default=10, help='Number of tenants')
    parser.add_argument('--persons', type=int, default=1000, help='Persons per tenant')
    parser.add_argument('--spaces', type=int, default=20, help='Spaces per tenant')
    parser.add_argument('--events', type=int, default=50, help='Events per space')
    parser.add_argument('--assets', type=int, default=20, help='Assets per space')
    parser.add_argument('--output', type=str, default='performance-data', help='Output directory')

    args = parser.parse_args()

    dataset = generate_dataset(
        num_tenants=args.tenants,
        persons_per_tenant=args.persons,
        spaces_per_tenant=args.spaces,
        events_per_space=args.events,
        assets_per_space=args.assets,
    )

    save_dataset(dataset, args.output)

    print(f"\nDataset saved to {args.output}/")
    print("Ready for load testing!")


if __name__ == '__main__':
    try:
        import faker
    except ImportError:
        print("Installing faker...")
        import subprocess
        subprocess.check_call([sys.executable, '-m', 'pip', 'install', 'faker'])
        import faker

    main()