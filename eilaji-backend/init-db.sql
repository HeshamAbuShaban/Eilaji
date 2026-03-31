-- Enable PostGIS extension for geospatial queries (pharmacy location search)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enum types for type safety
CREATE TYPE user_role AS ENUM ('PATIENT', 'PHARMACIST', 'DOCTOR', 'ADMIN');
CREATE TYPE prescription_status AS ENUM ('PENDING', 'SENT_TO_PHARMACY', 'RECEIVED_QUOTE', 'ACCEPTED', 'REJECTED', 'COMPLETED', 'CANCELLED');
CREATE TYPE message_type AS ENUM ('TEXT', 'IMAGE', 'MEDICINE_INFO');

-- Users table (patients, pharmacists, doctors, admins)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    fcm_token VARCHAR(500),
    role user_role NOT NULL DEFAULT 'PATIENT',
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Pharmacies table
CREATE TABLE IF NOT EXISTS pharmacies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    address TEXT NOT NULL,
    city VARCHAR(100),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    phone VARCHAR(20) NOT NULL,
    license_number VARCHAR(100),
    is_open BOOLEAN DEFAULT TRUE,
    opening_hours JSONB, -- {monday: {open: "09:00", close: "21:00"}, ...}
    rating_avg DECIMAL(3,2) DEFAULT 0.0 CHECK (rating_avg >= 0 AND rating_avg <= 5),
    total_ratings INTEGER DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create index for geospatial queries (find nearby pharmacies)
CREATE INDEX IF NOT EXISTS idx_pharmacies_location ON pharmacies(latitude, longitude);

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name_ar VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    icon_url VARCHAR(500),
    parent_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Subcategories table
CREATE TABLE IF NOT EXISTS subcategories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id UUID REFERENCES categories(id) ON DELETE CASCADE,
    name_ar VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    icon_url VARCHAR(500),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Medicines table
CREATE TABLE IF NOT EXISTS medicines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title_ar VARCHAR(255) NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    description_ar TEXT,
    description_en TEXT,
    image_url VARCHAR(500),
    price DECIMAL(10,2),
    subcategory_id UUID REFERENCES subcategories(id) ON DELETE SET NULL,
    manufacturer VARCHAR(255),
    requires_prescription BOOLEAN DEFAULT FALSE,
    alternatives_ids UUID[] DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Prescriptions table
CREATE TABLE IF NOT EXISTS prescriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    notes TEXT,
    status prescription_status NOT NULL DEFAULT 'PENDING',
    selected_pharmacy_id UUID REFERENCES pharmacies(id) ON DELETE SET NULL,
    quoted_price DECIMAL(10,2),
    pharmacist_notes TEXT,
    sent_to_eilaji_plus BOOLEAN DEFAULT FALSE,
    eilaji_plus_prescription_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for fast prescription lookups by patient
CREATE INDEX IF NOT EXISTS idx_prescriptions_patient ON prescriptions(patient_user_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_status ON prescriptions(status);

-- Pharmacy-Medicine Inventory (which pharmacy has which medicine)
CREATE TABLE IF NOT EXISTS pharmacy_medicines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pharmacy_id UUID REFERENCES pharmacies(id) ON DELETE CASCADE,
    medicine_id UUID REFERENCES medicines(id) ON DELETE CASCADE,
    price DECIMAL(10,2),
    stock_quantity INTEGER DEFAULT 0,
    is_available BOOLEAN DEFAULT TRUE,
    UNIQUE(pharmacy_id, medicine_id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for pharmacy inventory lookups
CREATE INDEX IF NOT EXISTS idx_pharmacy_medicines_pharmacy ON pharmacy_medicines(pharmacy_id);
CREATE INDEX IF NOT EXISTS idx_pharmacy_medicines_medicine ON pharmacy_medicines(medicine_id);

-- Chats table (between patients and pharmacies)
CREATE TABLE IF NOT EXISTS chats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    pharmacy_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    prescription_id UUID REFERENCES prescriptions(id) ON DELETE SET NULL,
    last_message_text TEXT,
    last_message_image_url VARCHAR(500),
    last_message_sender_id UUID REFERENCES users(id),
    last_message_at TIMESTAMP WITH TIME ZONE,
    unread_count_patient INTEGER DEFAULT 0,
    unread_count_pharmacy INTEGER DEFAULT 0,
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(patient_user_id, pharmacy_user_id, prescription_id)
);

-- Index for chat lookups
CREATE INDEX IF NOT EXISTS idx_chats_patient ON chats(patient_user_id);
CREATE INDEX IF NOT EXISTS idx_chats_pharmacy ON chats(pharmacy_user_id);

-- Messages table
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    sender_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    message_text TEXT,
    message_image_url VARCHAR(500),
    medicine_name VARCHAR(255),
    message_type message_type NOT NULL DEFAULT 'TEXT',
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for message ordering and lookups
CREATE INDEX IF NOT EXISTS idx_messages_chat ON messages(chat_id);
CREATE INDEX IF NOT EXISTS idx_messages_created ON messages(created_at DESC);

-- Favorites table (users can favorite medicines and pharmacies)
CREATE TABLE IF NOT EXISTS favorites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    medicine_id UUID REFERENCES medicines(id) ON DELETE CASCADE,
    pharmacy_id UUID REFERENCES pharmacies(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, medicine_id, pharmacy_id),
    CHECK (medicine_id IS NOT NULL OR pharmacy_id IS NOT NULL)
);

-- Index for favorite lookups
CREATE INDEX IF NOT EXISTS idx_favorites_user ON favorites(user_id);

-- Ratings table (users can rate pharmacies)
CREATE TABLE IF NOT EXISTS ratings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    pharmacy_id UUID REFERENCES pharmacies(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, pharmacy_id)
);

-- Index for rating lookups
CREATE INDEX IF NOT EXISTS idx_ratings_pharmacy ON ratings(pharmacy_id);

-- Medication Reminders table
CREATE TABLE IF NOT EXISTS medication_reminders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    medicine_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100),
    frequency VARCHAR(50) NOT NULL, -- DAILY, WEEKLY, CUSTOM
    schedule_time TIME NOT NULL,
    custom_days INTEGER[] DEFAULT '{}', -- For weekly: [1,3,5] = Mon,Wed,Fri
    notes TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for reminder lookups
CREATE INDEX IF NOT EXISTS idx_reminders_user ON medication_reminders(user_id);
CREATE INDEX IF NOT EXISTS idx_reminders_active ON medication_reminders(is_active) WHERE is_active = TRUE;

-- Audit Logs table (for compliance and debugging)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for audit log lookups
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs(created_at DESC);

-- Insert default admin user (password: admin123 - CHANGE IN PRODUCTION!)
-- Password hash generated with BCrypt cost factor 10
INSERT INTO users (email, password_hash, full_name, role, is_verified)
VALUES (
    'admin@eilaji.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Eilaji Admin',
    'ADMIN',
    TRUE
) ON CONFLICT (email) DO NOTHING;

-- Insert sample categories (bilingual)
INSERT INTO categories (name_ar, name_en, icon_url, display_order) VALUES
('مسكنات الألم', 'Pain Relievers', '/icons/pain.svg', 1),
('المضادات الحيوية', 'Antibiotics', '/icons/antibiotics.svg', 2),
('الفيتامينات والمكملات', 'Vitamins & Supplements', '/icons/vitamins.svg', 3),
('العناية بالبشرة', 'Skin Care', '/icons/skin.svg', 4),
('أدوية البرد والإنفلونزا', 'Cold & Flu', '/icons/flu.svg', 5),
('الهضم والجهاز الهضمي', 'Digestive Health', '/icons/digestive.svg', 6)
ON CONFLICT DO NOTHING;

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for auto-updating updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pharmacies_updated_at BEFORE UPDATE ON pharmacies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_medicines_updated_at BEFORE UPDATE ON medicines
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_prescriptions_updated_at BEFORE UPDATE ON prescriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pharmacy_medicines_updated_at BEFORE UPDATE ON pharmacy_medicines
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chats_updated_at BEFORE UPDATE ON chats
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_medication_reminders_updated_at BEFORE UPDATE ON medication_reminders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE users IS 'Stores all user accounts (patients, pharmacists, doctors, admins)';
COMMENT ON TABLE pharmacies IS 'Pharmacy locations with geospatial data';
COMMENT ON TABLE medicines IS 'Medicine catalog with bilingual support';
COMMENT ON TABLE prescriptions IS 'Prescription images and status tracking';
COMMENT ON TABLE chats IS 'Real-time chat conversations between patients and pharmacies';
COMMENT ON TABLE messages IS 'Individual messages within chat conversations';
COMMENT ON TABLE medication_reminders IS 'User medication reminder schedules';
COMMENT ON TABLE audit_logs IS 'Compliance and security audit trail';
