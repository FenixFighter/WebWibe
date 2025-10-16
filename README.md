# WebWibe - AI Banking Support System

Производственная система поддержки клиентов банка с интеграцией ИИ, построенная на Spring Boot и PostgreSQL.

## 🚀 Быстрый запуск

### Требования

- **Docker и Docker Compose** - для контейнеризации
- **Git** - для клонирования репозитория
- **Java 21** - для локальной разработки
- **Gradle 8.x** - для сборки проекта
- **Минимум 4GB RAM** - для работы системы
- **Порты 8081 и 5432** должны быть свободны

### Настройка Gradle

```bash
# Проверка версии Java
java -version

# Проверка версии Gradle
./gradlew --version

# Если Gradle не установлен, используйте wrapper
chmod +x gradlew
./gradlew --version
```

### Полная настройка и запуск

1. **Запуск системы с базой данных**

```bash
# Запуск всех сервисов (приложение + PostgreSQL + база знаний)
docker-compose up -d

# Проверка статуса
docker-compose ps

# Просмотр логов
docker-compose logs -f ww2-app
```

2. **Проверка работоспособности**

```bash
# Проверка здоровья приложения
curl http://localhost:8081/actuator/health

# Проверка базы данных
docker-compose exec postgres psql -U postgres -d postgres -c "SELECT COUNT(*) FROM knowledge_vectors;"
```

### Доступ к приложению

- **Чат с клиентами**: http://localhost:8081/chat.html
- **Панель поддержки**: http://localhost:8081/support.html
- **API**: http://localhost:8081/api
- **Health Check**: http://localhost:8081/actuator/health

## 📊 База знаний

Система автоматически загружает базу знаний из файла `smart_support_vtb_belarus_faq_final.csv` при первом запуске.

### Структура базы знаний

- **Категории**: продукты, кредиты, карты, вклады
- **Подкатегории**: потребительские, ипотечные, депозитные
- **Вопросы**: 996+ готовых вопросов и ответов
- **Векторный поиск**: интеллектуальный поиск похожих вопросов

### Проверка загрузки базы знаний

```bash
# Подключение к базе данных
docker-compose exec postgres psql -U postgres -d postgres

# Проверка количества записей
SELECT COUNT(*) FROM knowledge_vectors;

# Просмотр категорий
SELECT DISTINCT category FROM knowledge_vectors LIMIT 10;

# Выход из psql
\q
```

## 🔧 API Использование

### Обработка вопроса

```bash
curl -X POST http://localhost:8081/api/question \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Как получить карту Форсаж?",
    "category": "карты"
  }'
```

### Доступные категории

- `продукты` - Банковские продукты
- `кредиты` - Кредитные продукты
- `карты` - Банковские карты
- `вклады` - Депозитные продукты
- `техническая поддержка` - Технические вопросы

## 🏗️ Архитектура

### Компоненты системы

- **Frontend**: HTML5, CSS3, JavaScript, WebSocket
- **Backend**: Spring Boot, WebFlux, WebSocket (STOMP)
- **База данных**: PostgreSQL 16 с расширением pgvector
- **ИИ интеграция**: Внешний API для генерации ответов
- **Векторный поиск**: Интеллектуальный поиск по базе знаний

### Технологический стек

- **Java 21** - Основной язык разработки
- **Spring Boot 3.x** - Фреймворк приложения
- **PostgreSQL 16** - База данных
- **pgvector** - Векторные операции
- **Docker** - Контейнеризация
- **WebSocket** - Реальное время
- **Gradle** - Сборка проекта

## 🛠️ Разработка

### Настройка Gradle

#### Установка Gradle

```bash
# Вариант 1: Использование Gradle Wrapper (рекомендуется)
./gradlew --version

# Вариант 2: Установка Gradle глобально
# macOS
brew install gradle

# Ubuntu/Debian
sudo apt install gradle

# Windows
# Скачайте с https://gradle.org/install/
```

#### Конфигурация Gradle

```bash
# Проверка конфигурации
./gradlew properties

# Очистка кэша
./gradlew clean

# Просмотр задач
./gradlew tasks
```

#### Зависимости проекта

```bash
# Просмотр зависимостей
./gradlew dependencies

# Обновление зависимостей
./gradlew dependencyUpdates

# Проверка уязвимостей
./gradlew dependencyCheckAnalyze
```

### Сборка проекта

```bash
# Сборка JAR файла
./gradlew bootJar

# Сборка с тестами
./gradlew build

# Сборка без тестов
./gradlew build -x test
```

### Локальная разработка

```bash
# Запуск только базы данных
docker-compose up -d postgres

# Запуск приложения локально
./gradlew bootRun
```

## 📈 Мониторинг

### Health Checks

- **Application**: http://localhost:8081/actuator/health
- **Database**: Автоматическая проверка подключения
- **AI Service**: Проверка доступности внешнего API

### Логирование

- **Уровень**: INFO для продакшена
- **Формат**: JSON для структурированных логов
- **Ротация**: Автоматическая ротация логов

## 🔒 Безопасность

### Конфигурация

- **База данных**: Изолированная сеть Docker
- **API**: Валидация входных данных
- **WebSocket**: Аутентификация через сессии

### Переменные окружения

```bash
# База данных
POSTGRES_DB=postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Приложение
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8081
```

## 🚨 Устранение неполадок

### Проблемы с запуском

```bash
# Проверка портов
netstat -tulpn | grep :8081
netstat -tulpn | grep :5432

# Очистка Docker
docker system prune -a
docker-compose down -v
```

### Проблемы с базой данных

```bash
# Проверка подключения
docker-compose exec postgres pg_isready

# Сброс базы данных
docker-compose down -v
docker-compose up -d
```

### Проблемы с ИИ

- Проверьте доступность внешнего API
- Убедитесь в корректности API ключей
- Проверьте логи приложения

## 📞 Поддержка

При возникновении проблем:

1. Проверьте логи: `docker-compose logs -f`
2. Проверьте статус: `docker-compose ps`
3. Перезапустите систему: `docker-compose restart`
4. При критических ошибках: `docker-compose down -v && docker-compose up -d`

---

**Версия**: 1.0.0  
**Автор**: WebWibe Team  
**Лицензия**: Proprietary
