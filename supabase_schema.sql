-- Copy toàn bộ nội dung file này và chạy trên tab SQL Editor của Supabase

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    role VARCHAR(50),
    balance DOUBLE PRECISION DEFAULT 0.0,
    store_name VARCHAR(255),
    rating DOUBLE PRECISION DEFAULT 0.0
);

CREATE TABLE IF NOT EXISTS items (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    condition_item VARCHAR(50),
    seller_id VARCHAR(255) REFERENCES users(id) ON DELETE CASCADE,
    starting_price DOUBLE PRECISION NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    artist_name VARCHAR(255),
    material VARCHAR(255),
    creation_year INT,
    brand VARCHAR(255),
    model VARCHAR(255),
    year INT,
    km INT,
    warranty_months INT
);

CREATE TABLE IF NOT EXISTS auctions (
    id VARCHAR(255) PRIMARY KEY,
    item_id VARCHAR(255) REFERENCES items(id) ON DELETE CASCADE,
    current_winner_id VARCHAR(255) REFERENCES users(id) ON DELETE SET NULL,
    current_price DOUBLE PRECISION NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS bid_transactions (
    id VARCHAR(255) PRIMARY KEY,
    bidder_id VARCHAR(255) REFERENCES users(id) ON DELETE CASCADE,
    auction_id VARCHAR(255) REFERENCES auctions(id) ON DELETE CASCADE,
    bid_amount DOUBLE PRECISION NOT NULL,
    bid_time TIMESTAMP NOT NULL,
    is_auto_bid BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS auto_bids (
    user_id VARCHAR(255) REFERENCES users(id) ON DELETE CASCADE,
    auction_id VARCHAR(255) REFERENCES auctions(id) ON DELETE CASCADE,
    max_bid DOUBLE PRECISION NOT NULL,
    increment DOUBLE PRECISION NOT NULL,
    registered_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, auction_id)
);

-- ==========================================
-- THÊM DỮ LIỆU MẪU (SEED DATA)
-- ==========================================

-- 1. Tài khoản (Admin & Member)
INSERT INTO users (id, username, password, email, full_name, phone, address, is_active, role, balance, store_name, rating) 
VALUES ('USR-ADMIN-001', 'admin', '123456', 'admin@auction.com', 'Administrator', '0123456789', 'Hanoi', true, 'ADMIN', 10000000.0, 'Admin Store', 5.0) ON CONFLICT(id) DO NOTHING;

INSERT INTO users (id, username, password, email, full_name, phone, address, is_active, role, balance, store_name, rating) 
VALUES ('USR-MEM-001', 'member', '123456', 'member@auction.com', 'Member 1', '0987654321', 'HCM', true, 'MEMBER', 5000000.0, NULL, 5.0) ON CONFLICT(id) DO NOTHING;

-- 2. Sản phẩm (Items)
INSERT INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) 
VALUES ('ITEM-ART-001', 'Tranh Đông Hồ', 'Tranh dân gian Đông Hồ chính gốc', 'Mới', 'USR-ADMIN-001', 500000.0, 'ART', 'Nghệ nhân Đông Hồ', 'Giấy điệp', 2023, NULL, NULL, NULL, NULL, NULL) ON CONFLICT(id) DO NOTHING;
INSERT INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) 
VALUES ('ITEM-ELE-001', 'Tủ lạnh Samsung', 'Tủ lạnh Samsung Inverter 2 cánh', 'Mới', 'USR-ADMIN-001', 4000000.0, 'ELECTRONICS', NULL, NULL, NULL, 'Samsung', NULL, NULL, NULL, 24) ON CONFLICT(id) DO NOTHING;
INSERT INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) 
VALUES ('ITEM-VEH-001', 'VF3', 'Xe ô tô điện VinFast VF3 siêu tiết kiệm', 'Mới', 'USR-ADMIN-001', 240000000.0, 'VEHICLE', NULL, NULL, NULL, 'VinFast', 'VF3', 2024, 0, NULL) ON CONFLICT(id) DO NOTHING;
INSERT INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) 
VALUES ('ITEM-ELE-010', 'MacBook Pro M2 2023', 'Sản phẩm mới 100%, nguyên seal.', 'Mới', 'USR-ADMIN-001', 30000000.0, 'ELECTRONICS', NULL, NULL, NULL, 'Apple', NULL, NULL, NULL, 12) ON CONFLICT(id) DO NOTHING;
INSERT INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) 
VALUES ('ITEM-ART-010', 'Tranh Sơn Mài Hồ Gươm', 'Tranh sơn mài thủ công.', 'Mới', 'USR-ADMIN-001', 5000000.0, 'ART', 'Nguyễn Văn Tài', 'Sơn mài', 2022, NULL, NULL, NULL, NULL, NULL) ON CONFLICT(id) DO NOTHING;
INSERT INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) 
VALUES ('ITEM-VEH-010', 'Honda Air Blade 2022', 'Xe nguyên bản.', 'Như mới', 'USR-ADMIN-001', 28000000.0, 'VEHICLE', NULL, NULL, NULL, 'Honda', 'Air Blade 125', 2022, 12000, NULL) ON CONFLICT(id) DO NOTHING;
INSERT INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) 
VALUES ('ITEM-ELE-011', 'iPhone 15 Pro Max 256GB', 'Máy như mới 99%.', 'Như mới', 'USR-ADMIN-001', 22000000.0, 'ELECTRONICS', NULL, NULL, NULL, 'Apple', NULL, NULL, NULL, 9) ON CONFLICT(id) DO NOTHING;

-- 3. Phiên đấu giá
INSERT INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) 
VALUES ('AUC-001', 'ITEM-ART-001', NULL, 8700000.0, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', 'FINISHED') ON CONFLICT(id) DO NOTHING;
INSERT INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) 
VALUES ('AUC-002', 'ITEM-ELE-001', NULL, 35500000.0, NOW() - INTERVAL '1 day', NOW() + INTERVAL '1 day', 'RUNNING') ON CONFLICT(id) DO NOTHING;
INSERT INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) 
VALUES ('AUC-003', 'ITEM-VEH-001', NULL, 33000000.0, NOW() - INTERVAL '1 day', NOW() + INTERVAL '2 days', 'RUNNING') ON CONFLICT(id) DO NOTHING;
INSERT INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) 
VALUES ('AUC-010', 'ITEM-ELE-010', NULL, 30000000.0, NOW(), NOW() + INTERVAL '7 days', 'RUNNING') ON CONFLICT(id) DO NOTHING;
INSERT INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) 
VALUES ('AUC-011', 'ITEM-ART-010', NULL, 5000000.0, NOW() + INTERVAL '1 day', NOW() + INTERVAL '8 days', 'OPEN') ON CONFLICT(id) DO NOTHING;
INSERT INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) 
VALUES ('AUC-012', 'ITEM-VEH-010', 'USR-MEM-001', 32000000.0, NOW() - INTERVAL '10 days', NOW() - INTERVAL '1 day', 'FINISHED') ON CONFLICT(id) DO NOTHING;
INSERT INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) 
VALUES ('AUC-013', 'ITEM-ELE-011', 'USR-MEM-001', 25000000.0, NOW() - INTERVAL '15 days', NOW() - INTERVAL '5 days', 'PAID') ON CONFLICT(id) DO NOTHING;

-- 4. Bids mẫu
INSERT INTO bid_transactions (id, bidder_id, auction_id, bid_amount, bid_time, is_auto_bid) 
VALUES ('BID-001', 'USR-MEM-001', 'AUC-012', 32000000.0, NOW() - INTERVAL '2 days', FALSE) ON CONFLICT(id) DO NOTHING;
INSERT INTO bid_transactions (id, bidder_id, auction_id, bid_amount, bid_time, is_auto_bid) 
VALUES ('BID-002', 'USR-MEM-001', 'AUC-013', 25000000.0, NOW() - INTERVAL '6 days', FALSE) ON CONFLICT(id) DO NOTHING;
