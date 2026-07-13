CREATE TABLE restaurants (
    id                    UUID PRIMARY KEY,
    nome                  VARCHAR(150) NOT NULL,
    endereco              VARCHAR(255) NOT NULL,
    tipo_cozinha          VARCHAR(30) NOT NULL,
    horario_abertura      TIME NOT NULL,
    horario_fechamento    TIME NOT NULL,
    owner_id              UUID NOT NULL,
    data_criacao          TIMESTAMP NOT NULL,
    data_ultima_alteracao TIMESTAMP NOT NULL,
    CONSTRAINT fk_restaurants_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);
