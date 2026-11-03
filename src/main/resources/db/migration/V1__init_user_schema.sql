CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(30) NULL,
    avatar_url VARCHAR(500) NULL,
    date_of_birth DATE NULL,
    gender VARCHAR(30) NULL,
    bio VARCHAR(500) NULL,
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'en',
    preferred_currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    marketing_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_profiles_uuid (uuid),
    INDEX idx_user_profiles_email (email),
    INDEX idx_user_profiles_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_profile_roles (
    user_profile_id BIGINT NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_profile_id, role_name),
    CONSTRAINT fk_user_profile_roles_profile FOREIGN KEY (user_profile_id) REFERENCES user_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_profile_id BIGINT NOT NULL,
    label VARCHAR(50) NULL,
    recipient_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    address_type VARCHAR(20) NOT NULL DEFAULT 'BOTH',
    street_line1 VARCHAR(255) NOT NULL,
    street_line2 VARCHAR(255) NULL,
    city VARCHAR(100) NOT NULL,
    state_province VARCHAR(100) NOT NULL,
    postal_code VARCHAR(30) NOT NULL,
    country_code VARCHAR(10) NOT NULL DEFAULT 'US',
    is_default_shipping BOOLEAN NOT NULL DEFAULT FALSE,
    is_default_billing BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_instructions VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_addresses_uuid (uuid),
    INDEX idx_addresses_user (user_profile_id),
    CONSTRAINT fk_user_addresses_profile FOREIGN KEY (user_profile_id) REFERENCES user_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS seller_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_profile_id BIGINT NOT NULL UNIQUE,
    store_name VARCHAR(150) NOT NULL,
    store_slug VARCHAR(150) NOT NULL UNIQUE,
    store_description TEXT NULL,
    store_logo_url VARCHAR(500) NULL,
    store_banner_url VARCHAR(500) NULL,
    business_name VARCHAR(200) NULL,
    business_registration_number VARCHAR(100) NULL,
    tax_id VARCHAR(100) NULL,
    support_email VARCHAR(150) NULL,
    support_phone VARCHAR(30) NULL,
    return_policy TEXT NULL,
    shipping_policy TEXT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_seller_uuid (uuid),
    INDEX idx_seller_slug (store_slug),
    INDEX idx_seller_status (verification_status),
    CONSTRAINT fk_seller_profiles_user FOREIGN KEY (user_profile_id) REFERENCES user_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

