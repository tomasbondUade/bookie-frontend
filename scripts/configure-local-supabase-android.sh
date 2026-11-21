#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! -f "$1/supabase/config.toml" ]]; then
  echo "Uso: bash scripts/configure-local-supabase-android.sh /ruta/a/bookie-backend" >&2
  exit 1
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "Se necesita jq para leer el estado de Supabase (brew install jq)." >&2
  exit 1
fi

BACKEND_DIR="$1"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CONFIG_FILE="$ROOT_DIR/androidApp/src/debug/res/values/bookie_supabase_local.xml"

# Do not echo the JSON: supabase status includes secrets not needed by this script.
status_json="$(cd "$BACKEND_DIR" && npx supabase status -o json)"
api_url="$(printf '%s' "$status_json" | jq -er '.API_URL')"
anon_key="$(printf '%s' "$status_json" | jq -er '.ANON_KEY')"

case "$api_url" in
  http://127.0.0.1:*|http://localhost:*) ;;
  *) echo "La API URL no es un Supabase local de loopback." >&2; exit 1 ;;
esac
if [[ -z "$anon_key" || "$anon_key" == "null" ]]; then
  echo "Supabase no devolvió una clave pública ANON_KEY." >&2
  exit 1
fi

# ANON_KEY (public JWT) and loopback URL contain no XML special characters.
cat > "$CONFIG_FILE" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="bookie_supabase_url" translatable="false">$api_url</string>
    <string name="bookie_supabase_publishable_key" translatable="false">$anon_key</string>
</resources>
EOF

echo "Configuración local escrita en androidApp/src/debug/res/values/bookie_supabase_local.xml"
echo "El archivo está ignorado por Git. No copies ni compartas las claves del comando status."
