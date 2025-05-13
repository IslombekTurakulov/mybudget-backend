#!/bin/bash
set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Функция для вывода сообщений
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Функция для проверки наличия команды
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 не установлен. Пожалуйста, установите $1 и попробуйте снова."
        exit 1
    fi
}

# Функция для проверки порта
check_port() {
    if lsof -i :$1 > /dev/null 2>&1; then
        log_error "Порт $1 уже используется. Пожалуйста, освободите порт или измените конфигурацию."
        exit 1
    fi
}

# Проверка необходимых команд
log_info "Проверка необходимых команд..."
if command -v podman &> /dev/null; then
    CONTAINER_CMD="podman"
    log_info "Используется Podman"
elif command -v docker &> /dev/null; then
    CONTAINER_CMD="docker"
    log_info "Используется Docker"
else
    log_error "Не найден ни podman, ни docker. Пожалуйста, установите один из них и попробуйте снова."
    exit 1
fi

if [ -x "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
    log_info "Используется Gradle Wrapper"
elif command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
    log_info "Используется системный Gradle"
else
    log_error "Не найден gradle или gradlew. Пожалуйста, установите Gradle или используйте wrapper."
    exit 1
fi

# Запуск Postgres в контейнере (docker или podman)
CONTAINER_NAME="mybudget-postgres-local"
POSTGRES_PORT=5433
POSTGRES_USER="iuturakulov"
POSTGRES_PASSWORD="p123"
POSTGRES_DB="postgres"
SERVER_PORT=8080

# Проверка портов
log_info "Проверка портов..."
check_port $POSTGRES_PORT
check_port $SERVER_PORT

# Проверяем, не запущен ли уже контейнер
if ! ($CONTAINER_CMD ps -a --format '{{.Names}}' | grep -q "^$CONTAINER_NAME$"); then
    log_info "Запускаю контейнер с Postgres..."
    $CONTAINER_CMD run -d \
        --name $CONTAINER_NAME \
        -e POSTGRES_USER=$POSTGRES_USER \
        -e POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
        -e POSTGRES_DB=$POSTGRES_DB \
        -p $POSTGRES_PORT:5432 \
        postgres:17-alpine
else
    log_info "Контейнер $CONTAINER_NAME уже существует. Запускаю его..."
    $CONTAINER_CMD start $CONTAINER_NAME
fi

# Проверяем, что контейнер запустился
if ! $CONTAINER_CMD ps | grep -q $CONTAINER_NAME; then
    log_error "Не удалось запустить контейнер $CONTAINER_NAME"
    log_info "Проверка логов контейнера:"
    $CONTAINER_CMD logs $CONTAINER_NAME
    exit 1
fi

# Ждем, пока база поднимется
log_info "Ожидание запуска Postgres..."
MAX_RETRIES=30
RETRY_COUNT=0
until $CONTAINER_CMD exec $CONTAINER_NAME pg_isready -U $POSTGRES_USER -d $POSTGRES_DB > /dev/null 2>&1; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
        log_error "Postgres не запустился в течение $MAX_RETRIES секунд"
        log_info "Проверка логов контейнера:"
        $CONTAINER_CMD logs $CONTAINER_NAME
        exit 1
    fi
    sleep 1
done
log_info "Postgres готов!"

# Проверка подключения к базе данных
log_info "Проверка подключения к базе данных..."
if ! $CONTAINER_CMD exec $CONTAINER_NAME psql -U $POSTGRES_USER -d $POSTGRES_DB -c "SELECT 1" > /dev/null 2>&1; then
    log_error "Не удалось подключиться к базе данных"
    exit 1
fi

# Экспорт переменных окружения для backend
log_info "Настройка переменных окружения..."
export DATABASE_URL="jdbc:postgresql://localhost:$POSTGRES_PORT/$POSTGRES_DB"
export PG_DATABASE=$POSTGRES_DB
export PG_HOST=localhost
export PG_PASSWORD=$POSTGRES_PASSWORD
export PG_PORT=$POSTGRES_PORT
export PG_USER=$POSTGRES_USER
export SERVER_PORT=$SERVER_PORT

# Проверка переменных окружения
log_info "Проверка переменных окружения..."
for var in DATABASE_URL PG_DATABASE PG_HOST PG_PASSWORD PG_PORT PG_USER SERVER_PORT; do
    if [ -z "${!var}" ]; then
        log_error "Переменная $var не установлена"
        exit 1
    fi
done

# Вывод настроек
log_info "Настройки окружения:"
echo "DATABASE_URL=$DATABASE_URL"
echo "PG_USER=$PG_USER"
echo "PG_PASSWORD=$PG_PASSWORD"
echo "PG_DATABASE=$PG_DATABASE"
echo "PG_PORT=$PG_PORT"
echo "PG_HOST=$PG_HOST"
echo "SERVER_PORT=$SERVER_PORT"

# Проверка Gradle
log_info "Проверка Gradle..."
if ! $GRADLE_CMD --version > /dev/null 2>&1; then
    log_error "Gradle не работает корректно"
    exit 1
fi

# Проверка сборки проекта
log_info "Проверка сборки проекта..."
if ! $GRADLE_CMD build -x test > /dev/null 2>&1; then
    log_error "Ошибка при сборке проекта"
    log_info "Проверка логов сборки:"
    $GRADLE_CMD build -x test --stacktrace
    exit 1
fi

log_info "Все проверки пройдены успешно!"

# Проверяем, был ли передан параметр --no-run
if [[ "$1" != "--no-run" ]]; then
    log_info "Запускаю backend..."
    $GRADLE_CMD run
else
    log_info "Backend не запущен (параметр --no-run)."
    log_info "Для запуска backend выполните:"
    echo "$GRADLE_CMD run"
fi 