-- Seed Default User Profiles matching ecomm-auth-service default users
INSERT INTO user_profiles (
    id, uuid, email, first_name, last_name, phone_number,
    avatar_url, date_of_birth, gender, bio,
    preferred_language, preferred_currency, marketing_opt_in, status, created_at, updated_at
) VALUES
    (1, 'c0000000-0000-0000-0000-000000000001', 'customer@ecomm.com', 'Alice', 'Customer', '+1000000001',
     'https://cdn.ecomm.com/avatars/alice.png', '1994-06-15', 'FEMALE', 'Avid online shopper and tech enthusiast.',
     'en', 'USD', TRUE, 'ACTIVE', NOW(), NOW()),
    (2, 'a0000000-0000-0000-0000-000000000002', 'admin@ecomm.com', 'Admin', 'Root', '+1000000002',
     'https://cdn.ecomm.com/avatars/admin.png', '1988-01-10', 'PREFER_NOT_TO_SAY', 'Platform Administrator.',
     'en', 'USD', FALSE, 'ACTIVE', NOW(), NOW()),
    (3, 's0000000-0000-0000-0000-000000000003', 'seller@ecomm.com', 'Bob', 'Merchant', '+1000000003',
     'https://cdn.ecomm.com/avatars/bob.png', '1985-11-22', 'MALE', 'Founder of Bob Tech & Gadgets Official Store.',
     'en', 'USD', TRUE, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE email=email;

-- Seed Roles for Default Profiles
INSERT INTO user_profile_roles (user_profile_id, role_name) VALUES
    (1, 'ROLE_CUSTOMER'),
    (2, 'ROLE_ADMIN'),
    (2, 'ROLE_CUSTOMER'),
    (3, 'ROLE_SELLER')
ON DUPLICATE KEY UPDATE role_name=role_name;

-- Seed Customer Shipping & Billing Addresses for customer@ecomm.com
INSERT INTO user_addresses (
    id, uuid, user_profile_id, label, recipient_name, phone_number, address_type,
    street_line1, street_line2, city, state_province, postal_code, country_code,
    is_default_shipping, is_default_billing, delivery_instructions, created_at, updated_at
) VALUES
    (1, 'addr-0000-0000-0000-000000000001', 1, 'Home', 'Alice Customer', '+1000000001', 'BOTH',
     '742 Evergreen Terrace', 'Apt 2B', 'Springfield', 'IL', '62704', 'US',
     TRUE, TRUE, 'Leave package at the front porch near the planter.', NOW(), NOW()),
    (2, 'addr-0000-0000-0000-000000000002', 1, 'Office', 'Alice Customer', '+1000000001', 'SHIPPING',
     '350 Fifth Avenue', 'Suite 4200', 'New York', 'NY', '10118', 'US',
     FALSE, FALSE, 'Deliver to 42nd floor reception desk during business hours.', NOW(), NOW())
ON DUPLICATE KEY UPDATE uuid=uuid;

-- Seed Seller / Merchant Profile for seller@ecomm.com
INSERT INTO seller_profiles (
    id, uuid, user_profile_id, store_name, store_slug, store_description,
    store_logo_url, store_banner_url, business_name, business_registration_number,
    tax_id, support_email, support_phone, return_policy, shipping_policy,
    verification_status, verified_at, created_at, updated_at
) VALUES
    (1, 'sell-0000-0000-0000-000000000001', 3, 'Bob Tech & Gadgets', 'bob-tech-gadgets',
     'Official authorized retailer of consumer electronics, smart home devices, and premium accessories.',
     'https://cdn.ecomm.com/stores/bob-tech/logo.png',
     'https://cdn.ecomm.com/stores/bob-tech/banner.png',
     'Bob Merchant Electronics LLC', 'REG-US-9948271', 'EIN-84-2910482',
     'support@bobtech.ecomm.com', '+18005550199',
     '30-day hassle-free returns on all unopened and defective electronics.',
     'Free 2-day expedited shipping on orders over $50; ships within 24 hours.',
     'VERIFIED', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE uuid=uuid;

