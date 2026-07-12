CREATE TABLE menu_items (
    id                          UUID PRIMARY KEY,
    nome                        VARCHAR(150) NOT NULL,
    descricao                   VARCHAR(500),
    preco                       NUMERIC(10,2) NOT NULL,
    disponivel_somente_no_local BOOLEAN NOT NULL,
    foto_path                   VARCHAR(500),
    restaurant_id               UUID NOT NULL,
    data_criacao                TIMESTAMP NOT NULL,
    data_ultima_alteracao       TIMESTAMP NOT NULL,
    CONSTRAINT fk_menu_items_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
);

-- Every list/lookup in this module filters by restaurant_id (nested routes
-- are always scoped to one restaurant) - a plain FK constraint alone
-- doesn't index the referencing column in Postgres.
CREATE INDEX idx_menu_items_restaurant_id ON menu_items (restaurant_id);
