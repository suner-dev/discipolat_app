#!/bin/bash
if [ "$1" = "daemon" ]; then
  cd /home/jo/discipolat/backend
  export SPRING_PROFILES_ACTIVE=beta
  export SERVER_PORT=8090
  export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/discipolat_beta
  export SPRING_DATASOURCE_USERNAME=discipolat
  export SPRING_DATASOURCE_PASSWORD=discipolat_secret
  export REDIS_URL=redis://localhost:6379
  export JWT_PRIVATE_KEY_PATH=/home/jo/discipolat/keys/private.pem
  export JWT_PUBLIC_KEY_PATH=/home/jo/discipolat/keys/public.pem
  export MAIL_PORT=1026
  exec mvn spring-boot:run -DskipTests >> /home/jo/discipolat/beta-api.log 2>&1
fi
nohup setsid "$0" daemon > /dev/null 2>&1 < /dev/null &
disown
echo "Launched PID: $!"
exit 0
