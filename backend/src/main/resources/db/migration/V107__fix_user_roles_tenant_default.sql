-- Fix user_roles: set a default tenant_id so @ElementCollection inserts don't fail
ALTER TABLE user_roles ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-0000-0000-000000000001';
