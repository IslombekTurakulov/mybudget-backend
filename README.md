# MyBudget (Мой бюджет)

Комплексное решение для управления личными финансами, состоящее из бэкенд-сервиса и мобильного приложения. Система позволяет пользователям отслеживать расходы, управлять бюджетами и получать аналитику по своим финансам.

## Быстрый старт

### Бэкенд

1. Клонируйте репозиторий:
```bash
git clone https://github.com/IslombekTurakulov/MyBudget-backend.git
cd MyBudget-backend
```

2. Запустите приложение:
```bash
chmod +x run-local-backend.sh
./run-local-backend.sh
```

Скрипт автоматически настроит окружение и запустит приложение.

### Мобильное приложение

1. Клонируйте репозиторий:
```bash
git clone https://github.com/IslombekTurakulov/MyBudget-android.git
cd MyBudget-android
```

2. Откройте проект в Android Studio и запустите приложение

## 🛠 Технологический стек

### Бэкенд
- **Язык**: Kotlin
- **Фреймворк**: Ktor
- **База данных**: PostgreSQL 17
- **Контейнеризация**: Docker & Docker Swarm
- **Прокси**: Traefik
- **CI/CD**: GitHub Actions

## Требования

### Бэкенд
- Docker и Docker Swarm
- PostgreSQL 17
- JDK 17+
- Gradle 8.0+

### Мобильное приложение
- Android Studio
- Android SDK 34+
- Kotlin 1.9+
- Firebase CLI

## 🔧 Настройка окружения

### Бэкенд

1. Создайте файл `.env`:
```env
PG_USER=your_db_user
PG_PASSWORD=your_db_password
PG_DATABASE=your_db_name
JWT_SECRET=your_jwt_secret
FIREBASE_CREDENTIALS=path_to_firebase_credentials.json
```

2. Запустите приложение:
```bash
./run-local-backend.sh
```

Скрипт автоматически:
- Проверяет наличие Docker и Docker Compose
- Создает необходимые Docker сети
- Запускает PostgreSQL в контейнере
- Запускает приложение с помощью Gradle
- Отображает логи в реальном времени

## Развертывание

### Бэкенд

#### Быстрый деплой через GitHub Actions

1. Перейдите в раздел Actions в GitHub репозитории
2. Выберите workflow "Quick Deploy"
3. Нажмите "Run workflow"
4. Введите тег образа (по умолчанию: latest)
5. Нажмите "Run workflow"

Workflow автоматически:
- Проверяет наличие всех необходимых секретов
- Настраивает Docker Swarm
- Создает необходимые сети
- Разворачивает сервисы в правильном порядке:
  1. PostgreSQL
  2. Traefik
  3. Backend приложение
- Проверяет работоспособность всех сервисов
- Собирает и сохраняет логи

#### Ручной деплой

1. Соберите Docker образ:
```bash
docker build -t mybudget-backend .
```

2. Разверните с помощью Docker Swarm:
```bash
docker stack deploy -c docker-compose.yml backend
```

#### Важные моменты при деплое

1. **Сохранение данных**:
   - По умолчанию данные PostgreSQL сохраняются между деплоями
   - Для полного сброса данных установите переменную `FULL_RESET=true`

2. **Мониторинг**:
   - Проверьте статус сервисов: `docker service ls`
   - Просмотрите логи: `docker service logs backend_backend`
   - Проверьте health check: `curl http://51.250.65.154/ping`

3. **Откат изменений**:
   - Docker Swarm автоматически откатывает изменения при проблемах
   - Можно вручную откатить к предыдущей версии: `docker service rollback backend_backend`

4. **Безопасность**:
   - Все секреты хранятся в Docker Secrets
   - Подключения к базе данных шифруются
   - Traefik обеспечивает SSL/TLS терминацию

## API

### Базовый URL
```
http://51.250.65.154
```

### Аутентификация
Все эндпоинты, кроме `/ping`, требуют JWT токен:
```
Authorization: Bearer <token>
```

### Основные эндпоинты

#### Аутентификация
- `POST /auth/register` - Регистрация
- `POST /auth/login` - Вход
- `POST /auth/verify-email` - Подтверждение email
- `POST /auth/reset-password` - Сброс пароля

#### Проекты
- `POST /projects` - Создание
- `GET /projects` - Список
- `GET /projects/{id}` - Детали
- `PUT /projects/{id}` - Обновление
- `DELETE /projects/{id}` - Удаление

#### Транзакции
- `POST /transactions` - Создание
- `GET /transactions` - Список
- `GET /transactions/{id}` - Детали
- `PUT /transactions/{id}` - Обновление
- `DELETE /transactions/{id}` - Удаление

#### Аналитика
- `GET /analytics/overview` - Общая
- `GET /analytics/transactions` - По транзакциям
- `GET /analytics/categories` - По категориям

#### Уведомления
- `GET /notifications` - Список
- `POST /notifications/read` - Отметить прочитанными
- `POST /notifications/settings` - Настройки

#### Настройки
- `GET /settings` - Получить
- `PUT /settings` - Обновить

#### Устройства
- `POST /device-tokens` - Регистрация
- `DELETE /device-tokens/{token}` - Удаление

## Мониторинг

### Установка

1. Разверните стек мониторинга:
```bash
docker stack deploy -c docker-compose.monitoring.yml monitoring
```

2. Доступ к интерфейсам:
   - Grafana: http://51.250.65.154/grafana (логин: admin, пароль: admin)
   - Prometheus: http://51.250.65.154/prometheus

### Что мониторится

1. **Бэкенд приложение**:
   - Количество HTTP запросов
   - Время ответа
   - Использование памяти
   - Количество активных потоков

2. **PostgreSQL**:
   - Количество активных соединений
   - Размер базы данных
   - Количество транзакций
   - Время выполнения запросов

3. **Traefik**:
   - Количество запросов
   - Статусы ответов
   - Время ответа
   - Количество активных соединений

### Дашборды

В Grafana предустановлены следующие дашборды:
- Backend Monitoring - основные метрики бэкенда
- Database Monitoring - метрики PostgreSQL
- Traefik Monitoring - метрики Traefik

### Настройка алертов

1. В Grafana перейдите в раздел Alerting
2. Создайте новые правила алертинга:
   - Высокая нагрузка на CPU (>80%)
   - Большое время ответа (>1s)
   - Ошибки в логах
   - Проблемы с базой данных