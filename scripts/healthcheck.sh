#!/bin/sh

HEALTH_URL="http://localhost:8080/actuator/health"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_URL)

if [ "$RESPONSE" = "200" ]; then
    echo "Health check passed"
    exit 0
else
    echo "Health check failed — HTTP $RESPONSE"
    exit 1
fi