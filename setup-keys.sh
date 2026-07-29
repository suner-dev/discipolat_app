#!/bin/bash
# Generate RSA keys for JWT signing
set -e

KEYS_DIR="keys"
mkdir -p "$KEYS_DIR"

# Generate private key
openssl genpkey -algorithm RSA -out "$KEYS_DIR/private.pem" -pkeyopt rsa_keygen_bits:2048

# Extract public key
openssl pkey -in "$KEYS_DIR/private.pem" -pubout -out "$KEYS_DIR/public.pem"

# Generate .env file with base64 encoded keys
PRIVATE_B64=$(base64 -w0 < "$KEYS_DIR/private.pem")
PUBLIC_B64=$(base64 -w0 < "$KEYS_DIR/public.pem")

cat > .env << EOF
# Database
POSTGRES_DB=discipolat
POSTGRES_USER=discipolat
POSTGRES_PASSWORD=discipolat_secret

# JWT Keys (Base64-encoded RSA keys)
JWT_PRIVATE_KEY=${PRIVATE_B64}
JWT_PUBLIC_KEY=${PUBLIC_B64}

# App
SPRING_PROFILES_ACTIVE=docker
EOF

echo "✅ RSA keys generated in $KEYS_DIR/"
echo "✅ .env file created"
chmod +x setup-keys.sh
