ALTER TABLE user_types ADD COLUMN can_own_restaurant BOOLEAN NOT NULL DEFAULT FALSE;

-- Cliente (00000000-0000-0000-0000-000000000002) is already FALSE via the
-- column default above - only Dono de Restaurante needs an explicit flip.
UPDATE user_types SET can_own_restaurant = TRUE
    WHERE id = '00000000-0000-0000-0000-000000000001';
