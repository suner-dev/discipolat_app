#!/bin/bash
cd /home/jo/discipolat/backend
exec env SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/discipolat SPRING_DATASOURCE_USERNAME=discipolat SPRING_DATASOURCE_PASSWORD=discipolat_secret mvn spring-boot:run -DskipTests > /home/jo/discipolat/backend-run.log 2>&1
