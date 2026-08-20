#!/bin/bash
cd /home/jo/discipolat
exec java -jar backend/target/discipolat-backend-1.0.0.jar \
  --spring.profiles.active=beta \
  --spring.datasource.url=jdbc:postgresql://localhost:5433/discipolat \
  --spring.datasource.username=discipolat \
  --spring.datasource.password=discipolat_secret \
  --app.beta-testing.seed-demo-accounts=true \
  --app.beta-testing.demo-accounts-enabled=true \
  --app.environment=beta \
  --app.jwt.private-key-path=keys/private.pem \
  --app.jwt.public-key-path=keys/public.pem
