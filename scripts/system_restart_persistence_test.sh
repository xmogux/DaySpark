#!/usr/bin/env bash
set -euo pipefail
# Real two-process restart against a persistent, isolated H2 file DB.
ROOT="$(pwd)"
WORK="$ROOT/target/persistence-restart"
mkdir -p "$WORK/uploads"
export TZ=Asia/Seoul
export SPRING_DATASOURCE_URL="jdbc:h2:file:$WORK/daydb"
export SPRING_DATASOURCE_USERNAME=sa
export SPRING_DATASOURCE_PASSWORD=""
export SPRING_JPA_HIBERNATE_DDL_AUTO=update
export APP_SEED_DEMO_DATA=false
export APP_UPLOAD_DIR="$WORK/uploads"
export SERVER_PORT=8091
export SERVER_ADDRESS=127.0.0.1
PORT_URL=http://127.0.0.1:8091
JAR="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)"
[ -n "$JAR" ] || { echo 'FAIL: executable Spring Boot JAR missing'; exit 1; }
PID=""
finish() { if [ -n "$PID" ]; then kill "$PID" 2>/dev/null || true; wait "$PID" 2>/dev/null || true; fi; }
trap finish EXIT
start_server() {
  local log="$1"
  java -jar "$JAR" > "$WORK/$log" 2>&1 &
  PID=$!
  for n in $(seq 1 75); do
    if curl --max-time 3 -fsS "$PORT_URL/challenges" >/dev/null; then return 0; fi
    if ! kill -0 "$PID" 2>/dev/null; then break; fi
    sleep 1
  done
  cat "$WORK/$log"
  echo 'FAIL: server did not start'
  exit 1
}
start_server 'before-restart.log'
TITLE="PERSISTENCE_RESTART_$(date +%s)_$$"
DATE="$(TZ=Asia/Seoul date '+%Y-%m-%d')"
curl --max-time 15 -fsS -L \
  --data-urlencode "title=$TITLE" \
  --data-urlencode "description=restart test" \
  --data-urlencode 'category=STUDY' \
  --data-urlencode 'minutes=1' \
  --data-urlencode 'repeatType=DAILY' \
  --data-urlencode "startDate=$DATE" \
  --data-urlencode 'targetValue=1' \
  --data-urlencode 'targetUnit=회' \
  "$PORT_URL/challenges/save" > "$WORK/create-response.html"
curl --max-time 10 -fsS "$PORT_URL/challenges" > "$WORK/before.html"
grep -F "$TITLE" "$WORK/before.html" >/dev/null || { echo 'FAIL: pre-restart record absent'; exit 1; }
echo '[PASS] PERSIST-01 record created and visible before restart'
kill "$PID"
wait "$PID" 2>/dev/null || true
PID=""
# Confirm the port is released before starting a new JVM, rather than hitting the old process.
for i in $(seq 1 30); do
  if ! curl --max-time 2 -fsS "$PORT_URL/challenges" >/dev/null 2>&1; then break; fi
  sleep 1
done
start_server 'after-restart.log'
curl --max-time 10 -fsS "$PORT_URL/challenges" > "$WORK/after.html"
grep -F "$TITLE" "$WORK/after.html" >/dev/null || { echo 'FAIL: record lost after JVM restart'; exit 1; }
echo '[PASS] PERSIST-02 persistent H2 data survives full JVM restart'
printf '{"cases":[{"id":"PERSIST-01","status":"PASS"},{"id":"PERSIST-02","status":"PASS"}],"status":"PASS"}\n' > "$WORK/results.json"
