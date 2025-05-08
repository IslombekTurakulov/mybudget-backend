#!/bin/bash
set -e

# Запуск Postgres в контейнере (docker или podman)
CONTAINER_NAME="mybudget-postgres-local"
POSTGRES_PORT=5433
POSTGRES_USER="iuturakulov"
POSTGRES_PASSWORD="p123"
POSTGRES_DB="postgres"

# Определяем команду контейнера
if command -v podman &> /dev/null; then
  CONTAINER_CMD="podman"
else
  CONTAINER_CMD="docker"
fi

# Проверяем, не запущен ли уже контейнер
if ! ($CONTAINER_CMD ps -a --format '{{.Names}}' | grep -q "^$CONTAINER_NAME$"); then
  echo "Запускаю контейнер с Postgres..."
  $CONTAINER_CMD run -d \
    --name $CONTAINER_NAME \
    -e POSTGRES_USER=$POSTGRES_USER \
    -e POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
    -e POSTGRES_DB=$POSTGRES_DB \
    -p $POSTGRES_PORT:5432 \
    postgres:17-alpine
else
  echo "Контейнер $CONTAINER_NAME уже существует. Запускаю его..."
  $CONTAINER_CMD start $CONTAINER_NAME
fi

# Ждем, пока база поднимется
echo "Ожидание запуска Postgres..."
until $CONTAINER_CMD exec $CONTAINER_NAME pg_isready -U $POSTGRES_USER -d $POSTGRES_DB > /dev/null 2>&1; do
  sleep 1
done
echo "Postgres готов!"

# Экспорт переменных окружения для backend (только для локального запуска!)
export DATABASE_URL="jdbc:postgresql://localhost:$POSTGRES_PORT/$POSTGRES_DB"
export PG_DATABASE=$POSTGRES_DB
export PG_HOST=localhost
export PG_PASSWORD=$POSTGRES_PASSWORD
export PG_PORT=$POSTGRES_PORT
export PG_USER=$POSTGRES_USER
export SERVER_PORT=8080

# ВАЖНО: Эти переменные только для локального запуска! В продакшене используйте database:5432
echo "Экспортированы переменные окружения для backend:"
echo "DATABASE_URL=$DATABASE_URL"
echo "PG_USER=$PG_USER"
echo "PG_PASSWORD=$PG_PASSWORD"
echo "PG_DATABASE=$PG_DATABASE"
echo "PG_PORT=$PG_PORT"
echo "PG_HOST=$PG_HOST"
echo "SERVER_PORT=$SERVER_PORT"

# Запуск backend
# ./gradlew run
# или если у вас fat-jar:
# java -jar build/libs/MyBudget-backend-1.0.0.jar

echo "Теперь запустите backend командой:"
echo "./gradlew run" 