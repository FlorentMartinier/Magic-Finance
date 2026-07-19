-- Rendre les colonnes de statistiques financières de la table card explicitement nullables
ALTER TABLE card
    ALTER COLUMN current_price DROP NOT NULL,
    ALTER COLUMN max_price DROP NOT NULL,
    ALTER COLUMN max_price_date DROP NOT NULL,
    ALTER COLUMN min_price DROP NOT NULL,
    ALTER COLUMN min_price_date DROP NOT NULL;