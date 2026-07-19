-- 1. On supprime les anciennes tables pour rebâtir proprement la relation de clés primaires/étrangères
DROP TABLE IF EXISTS price;
DROP TABLE IF EXISTS card;

-- 2. Recréation de la table 'card' avec 'scryfall_id' en tant que PRIMARY KEY VARCHAR
CREATE TABLE card (
    scryfall_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    set_code VARCHAR(10) NOT NULL,
    collector_number VARCHAR(20) NOT NULL,
    rarity VARCHAR(50) NOT NULL,
    image_uri VARCHAR(512),

    -- Métriques financières conservées
    current_price NUMERIC(10, 2),
    max_price NUMERIC(10, 2),
    max_price_date DATE,
    min_price NUMERIC(10, 2),
    min_price_date DATE,
    price_change_7d NUMERIC(5, 2) DEFAULT 0.00,
    price_change_30d NUMERIC(5, 2) DEFAULT 0.00,
    volatility_score NUMERIC(10, 4) DEFAULT 0.0000,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Recréation de la table 'price' avec une clé étrangère VARCHAR(36) pointant vers 'card'
CREATE TABLE price (
    id UUID PRIMARY KEY,
    card_id VARCHAR(36) NOT NULL REFERENCES card(scryfall_id) ON DELETE CASCADE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Prix Normaux
    price_eur DECIMAL(10, 2),
    price_usd DECIMAL(10, 2),
    -- Prix Foil
    price_foil_eur DECIMAL(10, 2),
    price_foil_usd DECIMAL(10, 2),
    -- Prix Etched
    price_etched_eur DECIMAL(10, 2),
    price_etched_usd DECIMAL(10, 2)
);

-- Index pour les performances
CREATE INDEX idx_card_name ON card(name);
CREATE INDEX idx_price_card_id ON price(card_id);
CREATE INDEX idx_card_price_change_7d ON card(price_change_7d);
CREATE INDEX idx_card_price_change_30d ON card(price_change_30d);