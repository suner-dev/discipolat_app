#!/bin/bash
# Double-fork daemon launcher
if [ "$1" = "daemon" ]; then
  cd /home/jo/discipolat/backend
  export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/discipolat
  export SPRING_DATASOURCE_USERNAME=discipolat
  export SPRING_DATASOURCE_PASSWORD=discipolat_secret
  exec mvn spring-boot:run -DskipTests >> /home/jo/discipolat/backend-run.log 2>&1
fi
# Parent: double-fork to daemonize
nohup setsid "$0" daemon > /dev/null 2>&1 < /dev/null &
disown
echo "Launched PID: $!"
exit 0
