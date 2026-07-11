CREATE TABLE user_types (
    id   UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX uk_user_types_nome ON user_types (nome);

INSERT INTO user_types (id, nome) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Dono de Restaurante'),
    ('00000000-0000-0000-0000-000000000002', 'Cliente');

ALTER TABLE users ADD COLUMN user_type_id UUID;

UPDATE users SET user_type_id = '00000000-0000-0000-0000-000000000002'
    WHERE user_type_id IS NULL;

ALTER TABLE users ALTER COLUMN user_type_id SET NOT NULL;

ALTER TABLE users ADD CONSTRAINT fk_users_user_type
    FOREIGN KEY (user_type_id) REFERENCES user_types (id);
