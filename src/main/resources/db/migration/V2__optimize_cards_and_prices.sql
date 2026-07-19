-- 1. Suppression des anciennes tables pour repartir sur une structure propre
DROP TABLE IF EXISTS price;
DROP TABLE IF EXISTS card;

-- 2. Recréation de la table 'card' simplifiée (sans nom FR)
CREATE TABLE card (
    id UUID PRIMARY KEY,
    scryfall_id UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    set_code VARCHAR(10) NOT NULL,
    collector_number VARCHAR(20) NOT NULL,
    rarity VARCHAR(50) NOT NULL,
    image_uri VARCHAR(512)
);

-- 3. Recréation de la table 'price' fusionnée (USD/EUR & Finitions)
CREATE TABLE price (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL REFERENCES card(id) ON DELETE CASCADE,
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

-- Index pour accélérer les recherches fréquentes
CREATE INDEX idx_card_name ON card(name);
CREATE INDEX idx_price_card_id ON price(card_id);