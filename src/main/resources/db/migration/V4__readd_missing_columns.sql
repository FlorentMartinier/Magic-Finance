-- Ajout des champs de prix courants et historiques de la carte
ALTER TABLE card
    ADD COLUMN current_price NUMERIC(10, 2),
    ADD COLUMN max_price NUMERIC(10, 2),
    ADD COLUMN max_price_date DATE,
    ADD COLUMN min_price NUMERIC(10, 2),
    ADD COLUMN min_price_date DATE;

-- Ajout des indicateurs de tendances et de volatilité
ALTER TABLE card
    ADD COLUMN price_change_7d NUMERIC(5, 2) DEFAULT 0.00,
    ADD COLUMN price_change_30d NUMERIC(5, 2) DEFAULT 0.00,
    ADD COLUMN volatility_score NUMERIC(10, 4) DEFAULT 0.0000;

-- Ajout des métadonnées temporelles
ALTER TABLE card
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Index pour optimiser les requêtes de recherche des cartes "en mouvement" (les plus fortes baisses/hausses)
CREATE INDEX idx_card_price_change_7d ON card(price_change_7d);
CREATE INDEX idx_card_price_change_30d ON card(price_change_30d);