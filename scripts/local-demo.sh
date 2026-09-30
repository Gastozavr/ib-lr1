#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
USERNAME="demo$(date +%s)"

echo "1. Protected endpoint without a token must return 401"
curl -i -sS "$BASE_URL/api/data"

echo
echo "2. Register a user; displayName contains an XSS payload"
curl -i -sS -X POST "$BASE_URL/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"StrongPassword1!\",\"displayName\":\"<script>alert(1)</script>\"}"

echo
echo "3. Log in and extract the JWT"
LOGIN_RESPONSE="$(curl -sS -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"StrongPassword1!\"}")"
TOKEN="$(printf '%s' "$LOGIN_RESPONSE" | sed -E 's/.*"token":"([^"]+)".*/\1/')"
printf '%s\n' "$LOGIN_RESPONSE"

echo
echo "4. Create sanitized data with the JWT"
curl -i -sS -X POST "$BASE_URL/api/data" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"<b>Security note</b>","content":"<img src=x onerror=alert(1)>"}'

echo
echo "5. Read protected data with the JWT"
curl -i -sS "$BASE_URL/api/data" -H "Authorization: Bearer $TOKEN"
