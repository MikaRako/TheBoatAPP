#!/bin/sh
set -e

# ── 1. Process realm template ─────────────────────────────────────
mkdir -p /opt/keycloak/data/import
sed \
  -e "s|__KC_BOATUSER_PASSWORD__|${KC_BOATUSER_PASSWORD}|g" \
  -e "s|__KC_BOATADMIN_PASSWORD__|${KC_BOATADMIN_PASSWORD}|g" \
  /tmp/realm-export.json.template \
  > /opt/keycloak/data/import/realm-export.json

# ── 2. Start Keycloak in background ──────────────────────────────
/opt/keycloak/bin/kc.sh start-dev --import-realm &
KC_PID=$!

# ── 3. Wait until admin API is reachable ─────────────────────────
echo "[boat-theme] Waiting for Keycloak admin API..."
until /opt/keycloak/bin/kcadm.sh config credentials \
    --server http://localhost:8080 \
    --realm master \
    --user "${KEYCLOAK_ADMIN}" \
    --password "${KEYCLOAK_ADMIN_PASSWORD}" 2>/dev/null; do
  sleep 3
done

# ── 4. Force the login theme on the realm ────────────────────────
/opt/keycloak/bin/kcadm.sh update realms/boat-realm \
  -s loginTheme=boat-theme
echo "[boat-theme] boat-theme applied to boat-realm."

# ── 5. Keep container alive with the Keycloak process ────────────
wait $KC_PID
