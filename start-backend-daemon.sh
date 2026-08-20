#!/bin/bash
# Daemon script for backend — starts Java in a subshell with setsid
PROJECT_DIR="/home/jo/discipolat"
LOGFILE="/tmp/discipolat-backend.log"
PIDFILE="/tmp/discipolat-backend.pid"

# Kill existing
if [ -f "$PIDFILE" ]; then
  OLD_PID=$(cat "$PIDFILE")
  kill "$OLD_PID" 2>/dev/null
  sleep 1
fi

# Start in a truly detached subprocess
setsid bash -c "
  exec java -jar $PROJECT_DIR/backend/target/discipolat-backend-1.0.0.jar \
    --spring.profiles.active=beta \
    --spring.datasource.url=jdbc:postgresql://localhost:5433/discipolat \
    --spring.datasource.username=discipolat \
    --spring.datasource.password=discipolat_secret \
    --app.beta-testing.seed-demo-accounts=true \
    --app.beta-testing.demo-accounts-enabled=true \
    --app.environment=beta \
    --app.jwt.private-key-path=$PROJECT_DIR/keys/private.pem \
    --app.jwt.public-key-path=$PROJECT_DIR/keys/public.pem \
    > $LOGFILE 2>&1
" < /dev/null &

echo $! > "$PIDFILE"
echo "Backend started: PID=$(cat "$PIDFILE")"
