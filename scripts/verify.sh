#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p out
javac -d out src/Main.java
java -cp out Main --smoke

PORT="${PORT:-8080}"
java -cp out Main > /tmp/fello-verify.log 2>&1 &
APP_PID=$!
trap 'kill "$APP_PID" 2>/dev/null || true' EXIT
sleep 1

base="http://127.0.0.1:${PORT}"
health="$(curl -fsS "$base/api/health")"
echo "$health" | rg '"ok":true' >/dev/null
for tenant in northstar acme lumen; do
  curl -fsS "$base/api/summary?tenant=$tenant" | rg "\"tenant\":\"$tenant\"" >/dev/null
done
curl -fsS "$base/api/accounts/acct-0001?tenant=northstar" | rg '"modelVersion":"risk-logreg-v1"' >/dev/null
curl -fsS -X POST "$base/api/agent/run" | rg '"ok":true' >/dev/null
echo "LIVE_API_OK port=$PORT"

