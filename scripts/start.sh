#!/bin/sh

echo "Starting Jully's Craft Backend..."
echo "Profile: $SPRING_PROFILES_ACTIVE"
echo "JVM opts: $JAVA_OPTS"

# Wait for database to be ready
echo "Waiting for database..."
while ! nc -z ${DB_HOST:-localhost} ${DB_PORT:-5432}; do
    sleep 2
done
echo "Database is ready."

# Wait for Redis
echo "Waiting for Redis..."
while ! nc -z ${REDIS_HOST:-localhost} ${REDIS_PORT:-6379}; do
    sleep 2
done
echo "Redis is ready."

# Start app
exec java $JAVA_OPTS -jar /app/app.jar