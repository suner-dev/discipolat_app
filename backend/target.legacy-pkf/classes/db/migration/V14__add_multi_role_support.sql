-- V14: Multi-Role Support
-- Adds user_roles join table and active_role column

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Migrate existing single role to user_roles
INSERT INTO user_roles (user_id, role)
SELECT id, 'CHEF_DE_FAMILLE' FROM users WHERE est_chef_de_famille = true;

INSERT INTO user_roles (user_id, role)
SELECT id, role FROM users;

-- Add active_role column (defaults to the existing role)
ALTER TABLE users ADD COLUMN active_role VARCHAR(50);

UPDATE users SET active_role = role;

-- Add index for role lookups
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role);
