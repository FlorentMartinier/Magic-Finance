-- Create cards table (Catalog + Daily Aggregated Statistics)
CREATE TABLE cards (
    scryfall_id VARCHAR(36) PRIMARY KEY,
    name_en VARCHAR(255) NOT NULL,
    name_fr VARCHAR(255),
    image_url VARCHAR(512),
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

-- Index to speed up bilingual text search
CREATE INDEX idx_cards_names ON cards(name_en, name_fr);

-- Create price history table (Optimized for time-series)
CREATE TABLE price_history (
    price_id BIGSERIAL PRIMARY KEY,
    card_id VARCHAR(36) NOT NULL,
    is_foil BOOLEAN NOT NULL DEFAULT FALSE,
    price_date DATE NOT NULL,
    price_eur NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Referential integrity constraint
    CONSTRAINT fk_price_card FOREIGN KEY (card_id) REFERENCES cards(scryfall_id) ON DELETE CASCADE,
    -- Uniqueness constraint: only one price record per card, per type (foil/normal) per day
    CONSTRAINT uq_card_foil_date UNIQUE (card_id, is_foil, price_date)
);

-- Index to optimize statistics calculations (Min, Max, StdDev) over the last 30 days
CREATE INDEX idx_price_history_calc ON price_history(card_id, is_foil, price_date);