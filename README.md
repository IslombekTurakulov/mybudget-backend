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

## Решение проблем

### Порт 5433 уже используется

Если при запуске скрипта вы видите ошибку:
```
[ERROR] Порт 5433 уже используется. Пожалуйста, освободите порт или измените конфигурацию.
```

Это означает, что контейнер с PostgreSQL уже запущен. Выполните следующие шаги:

1. Проверить запущенные контейнеры:
```bash
podman ps
```

2. Остановить существующий контейнер:
```bash
podman stop mybudget-postgres-local
```

3. Удалить контейнер:
```bash
podman rm mybudget-postgres-local
```

4. Запустить скрипт заново:
```bash
./run-local-backend.sh
```

После этих действий скрипт должен успешно запустить новый контейнер с PostgreSQL и настроить окружение.

## Развертывание

### Бэкенд

1. Соберите Docker образ:
```bash
docker build -t mybudget-backend .
```

2. Разверните с помощью Docker Swarm:
```bash
docker stack deploy -c docker-compose.yml backend
```

## API

### Базовый Production URL
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

### Бэкенд
- Health checks через Traefik
- Логирование через Docker
- Метрики производительности

### Мобильное приложение
- Firebase Analytics
- Firebase Crashlytics
- Firebase Performance Monitoring

## Безопасность

### Бэкенд
- JWT аутентификация
- Шифрование паролей
- Rate limiting

### Мобильное приложение
- Firebase Authentication
- Безопасное хранение данных
- Шифрование локальных данных
- Защита от реверс-инжиниринга


