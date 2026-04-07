#!/bin/bash
# WARNING: FOR DEMO/DEVELOPMENT USE ONLY — NOT FOR PRODUCTION.
# Security is disabled. Data is unencrypted and unauthenticated.
#
# Starts OpenSearch via Docker Compose for use as a vector store.
# Security plugin is disabled — HTTP only, no authentication for connections.
# An admin password is still required by OpenSearch 2.12+ at startup.
#
# Usage (Linux / macOS):
#   chmod +x start.sh
#   ./start.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# ── Load .env ─────────────────────────────────────────────────────────────────

load_env() {
  [[ -f "$ENV_FILE" ]] || return
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" =~ ^#.*$ || -z "$line" ]] && continue
    key="${line%%=*}"
    value="${line#*=}"
    # Only set if not already exported from environment
    [[ -z "${!key:-}" ]] && export "$key=$value"
  done < "$ENV_FILE"
}

write_env() {
  cat > "$ENV_FILE" <<EOF
OPENSEARCH_PASSWORD=${OPENSEARCH_PASSWORD}
OPENSEARCH_PORT=${OPENSEARCH_PORT}
EOF
  chmod 600 "$ENV_FILE"
}

load_env

# ── Disclaimer ────────────────────────────────────────────────────────────────

echo ""
echo "  This setup is for DEMO/DEVELOPMENT purposes only."
echo "  It is not supported in any production environment."
echo ""
read -rp "  If you are fully aware and agree, type 'ok' to continue: " consent
if [[ "$consent" != "ok" ]]; then
  echo "Aborted."
  exit 0
fi
echo ""

# ── Admin password (required by OpenSearch 2.12+ at startup) ──────────────────

CHANGED=false

if [[ -z "${OPENSEARCH_PASSWORD:-}" ]]; then
  read -rsp "OpenSearch admin password (startup only, not used for connections): " OPENSEARCH_PASSWORD
  echo
  CHANGED=true
fi

# ── Port selection ────────────────────────────────────────────────────────────

is_port_free() {
  local port=$1
  # Try to bind the port; if it fails the port is in use
  if command -v nc &>/dev/null; then
    ! nc -z localhost "$port" 2>/dev/null
  else
    ! (echo >/dev/tcp/localhost/"$port") 2>/dev/null
  fi
}

find_free_port() {
  local port=${1:-19600}
  while ! is_port_free "$port"; do
    echo "Port $port is in use, trying $((port + 1))..."
    port=$((port + 1))
  done
  echo "$port"
}

START_PORT="${OPENSEARCH_PORT:-19600}"
OPENSEARCH_PORT="$(find_free_port "$START_PORT")"
if [[ "$OPENSEARCH_PORT" != "$START_PORT" ]]; then
  echo "Using port $OPENSEARCH_PORT instead."
  CHANGED=true
fi

if [[ "$CHANGED" == "true" ]]; then
  write_env
  echo "Port saved to $ENV_FILE"
fi

# ── Docker check ──────────────────────────────────────────────────────────────

if ! command -v docker &>/dev/null; then
  echo "ERROR: docker is not installed or not on PATH." >&2
  exit 1
fi

# ── Docker Compose ────────────────────────────────────────────────────────────

cd "$SCRIPT_DIR"

# Detect compose command (plugin vs standalone)
if docker compose version &>/dev/null 2>&1; then
  COMPOSE="docker compose"
elif command -v docker-compose &>/dev/null; then
  COMPOSE="docker-compose"
else
  echo "ERROR: neither 'docker compose' plugin nor 'docker-compose' is available." >&2
  exit 1
fi

echo "Starting OpenSearch..."
$COMPOSE up -d

# ── Wait until healthy ────────────────────────────────────────────────────────

echo -n "Waiting for OpenSearch to be ready"
MAX_RETRIES=30
RETRY=0

until docker exec smart-workflow-opensearch \
    curl -fs \
    "http://localhost:9200/_cluster/health" &>/dev/null; do
  if (( RETRY++ >= MAX_RETRIES )); then
    echo
    echo "ERROR: OpenSearch did not become ready after $((MAX_RETRIES * 2)) seconds." >&2
    echo "Check logs: docker logs smart-workflow-opensearch" >&2
    exit 1
  fi
  echo -n "."
  sleep 2
done

echo " ready."
echo

# ── Summary ───────────────────────────────────────────────────────────────────

echo "┌─────────────────────────────────────────────────────────┐"
echo "│  OpenSearch Vector Store  [DEMO/DEV ONLY]               │"
echo "├─────────────────────────────────────────────────────────┤"
printf "│  URL       http://localhost:%-29s│\n" "${OPENSEARCH_PORT}"
echo "├─────────────────────────────────────────────────────────┤"
echo "│  Set these Ivy variables:                               │"
printf "│    AI.RAG.OpenSearch.Url              http://localhost:%-4s│\n" "${OPENSEARCH_PORT}"
echo "│    AI.RAG.OpenSearch.ApiKey             (leave blank)   │"
echo "│    AI.RAG.OpenSearch.UserName           (leave blank)   │"
echo "│    AI.RAG.OpenSearch.Password           (leave blank)   │"
echo "│    AI.RAG.OpenSearch.DefaultCollection  (optional)      │"
echo "└─────────────────────────────────────────────────────────┘"
echo
echo "Stop:   docker compose stop"
echo "Logs:   docker compose logs -f"
echo "Reset:  docker compose down -v   # also deletes index data"
