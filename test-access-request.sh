#!/bin/bash

echo "🚀 Starting Access Request POC Test"

# 1. Crear access request
echo -e "\n📝 Creating access request..."
ACCESS_REQUEST_ID=$(curl -s -X POST http://localhost:8080/api/v1/access-requests \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "john.doe",
    "accessType": "DATABASE_ACCESS",
    "justification": "Need access for testing"
  }' | jq -r '.id')

echo "✅ Created: $ACCESS_REQUEST_ID"

# 2. Esperar un poco
echo -e "\n⏳ Waiting 5 seconds..."
sleep 5

# 3. Consultar estado
echo -e "\n📊 Checking status..."
curl -s http://localhost:8080/api/v1/access-requests/$ACCESS_REQUEST_ID | jq '.status'

# 4. Esperar a que falle (si max-retries=1, fallará rápido)
echo -e "\n⏳ Waiting for job to fail (40 seconds)..."
sleep 40

# 5. Verificar que falló
echo -e "\n📊 Checking if FAILED..."
STATUS=$(curl -s http://localhost:8080/api/v1/access-requests/$ACCESS_REQUEST_ID | jq -r '.status')
echo "Status: $STATUS"

# 6. Hacer retry
echo -e "\n🔄 Retrying access request..."
curl -s -X POST http://localhost:8080/api/v1/access-requests/$ACCESS_REQUEST_ID/retry | jq

# 7. Hacer un segundo retry inmediatamente (para probar concurrencia)
echo -e "\n🔄 Second retry (should skip if already processing)..."
curl -s -X POST http://localhost:8080/api/v1/access-requests/$ACCESS_REQUEST_ID/retry | jq

# 8. Ver dashboard
echo -e "\n🌐 Opening JobRunr dashboard..."
"$BROWSER" http://localhost:8000

echo -e "\n✅ Test completed!"
