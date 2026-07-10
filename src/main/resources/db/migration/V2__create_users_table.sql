CREATE TABLE users (
    id                    UUID PRIMARY KEY,
    nome                  VARCHAR(150) NOT NULL,
    email                 VARCHAR(255) NOT NULL,
    login                 VARCHAR(100) NOT NULL,
    senha_hash            VARCHAR(255) NOT NULL,
    endereco              VARCHAR(255),
    data_criacao          TIMESTAMP NOT NULL,
    data_ultima_alteracao TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_users_email ON users (email);
CREATE UNIQUE INDEX uk_users_login ON users (login);
