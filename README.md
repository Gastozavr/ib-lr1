# Secure API Lab 1

Учебный REST API на Java 21 и Spring Boot с JWT-аутентификацией, BCrypt, H2, защитой от SQL-инъекций и экранированием пользовательских данных.

## Запуск

Требования: JDK 21, `openssl` и `curl`.

```bash
export APP_JWT_SECRET_BASE64="$(openssl rand -base64 32)"
./mvnw spring-boot:run
```

В другом терминале:

```bash
./scripts/local-demo.sh
```

База H2 хранится в каталоге `data/`, который исключен из Git. Секрет JWT не хранится в репозитории и передается через переменную окружения.

## API

| Метод и путь | Доступ | Назначение |
|---|---|---|
| `POST /auth/register` | публичный | Регистрация; пароль от 12 до 72 символов хэшируется BCrypt |
| `POST /auth/login` | публичный | Проверка учетных данных и выдача JWT на 15 минут |
| `GET /api/data?q=...` | Bearer JWT | Получение всех записей или поиск по заголовку |
| `POST /api/data` | Bearer JWT | Создание новой записи |

Пример регистрации:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"StrongPassword1!","displayName":"Student"}'
```

Пример входа:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"StrongPassword1!"}'
```

В защищенных запросах передайте токен из ответа:

```bash
curl http://localhost:8080/api/data \
  -H 'Authorization: Bearer YOUR_TOKEN'
```

## Реализованные меры защиты

### SQL-инъекции

Доступ к H2 выполняется через Spring Data JPA. Методы репозиториев передают параметры отдельно от SQL, поэтому пользовательский ввод не конкатенируется с запросами. Интеграционный тест дополнительно проверяет, что строка вида `' OR '1'='1` не обходит аутентификацию.

### XSS

Поля `displayName`, `title` и `content` перед сохранением экранируются через `HtmlUtils.htmlEscape`. Ответы имеют JSON-формат, а заголовок Content Security Policy запрещает загрузку контента (`default-src 'none'`). Ограничения длины снижают риск злоупотребления входными данными.

### Аутентификация

Пароли хэшируются адаптивным алгоритмом BCrypt с cost factor 12. После успешного входа сервер выдает подписанный HMAC-SHA JWT со сроком жизни 15 минут и проверяет подпись, issuer и срок действия в `JwtAuthenticationFilter`. Сервер не создает HTTP-сессию. Секрет подписи длиной не менее 256 бит поступает только из `APP_JWT_SECRET_BASE64`.

### Дополнительные меры

- Bean Validation ограничивает формат и длину полей.
- Ошибки API не содержат stack trace.
- H2 Console отключена.
- Для ответов включены CSP и запрет встраивания во frame.
- JWT-фильтр отклоняет поврежденные, подделанные и просроченные токены.

## Тесты и security-сканирование

Быстрые локальные проверки:

```bash
./mvnw clean test
./mvnw verify -Psecurity
```

Профиль `security` запускает:

- SpotBugs — SAST-анализ байткода;
- OWASP Dependency-Check — SCA-анализ зависимостей с отчетами HTML, JSON и SARIF.

Первый запуск Dependency-Check может занять 20 минут и более из-за загрузки базы NVD. Для запуска текущей версии сканера добавьте секрет репозитория `NVD_API_KEY`; порядок описан в `GITHUB_STEPS.md`.

## CI/CD

Workflow `.github/workflows/ci.yml` запускается при каждом push и pull request в `main`, выполняет тесты, SpotBugs и OWASP Dependency-Check, затем сохраняет отчеты как artifact `security-reports`.

Перед первым push создайте в репозитории Actions secret `NVD_API_KEY`. Значение передается сканеру через имя переменной окружения и не записывается в команды, файлы или логи проекта.

После публикации репозитория добавьте в этот README:

1. ссылку на публичный репозиторий;
2. скриншот успешного запуска Actions;
3. скриншоты отчетов SpotBugs и OWASP Dependency-Check;
4. ссылку на последний успешный запуск pipeline.

Подробные действия для GitHub находятся в `GITHUB_STEPS.md`.
