# Інформаційна система організації подій і контролю завдань (варіант 23)

Курсова робота з дисципліни «Кросплатформене програмування». Студент: Скляр А. О., гр. 202-TH.

## Структура
- `server/` — серверна частина (Maven, WAR для Tomcat 9, Servlet API 4.0.1, Jackson, JDBC/MySQL)
- `client/` — JavaFX-клієнт (Maven, Java 21, HTTP-клієнт + ObjectMapper)
- `database.sql` — створення БД `event_manager` з нуля + демонстраційні дані
- `client/sample-data/` — CSV-файли для імпорту (`tasks.csv`, `resources.csv`, `tasks_with_errors.csv`), шляхи відносні
- `docs/diagrams/` — схеми diagrams.net (`.drawio`) та зображення

## Запуск (етап 1)
1. Запустити MySQL у XAMPP, у phpMyAdmin виконати `database.sql`.
2. За потреби створити `server/db.local.properties` (не потрапляє в git) і перевизначити `db.url`, `db.user`, `db.password`.
3. Перевірка підключення:
   ```
   cd server
   mvn compile exec:java
   ```
4. Клієнт: `cd client && mvn javafx:run`.

## Етап II: DAO, Service, валідація, показник, життєвий цикл
Пакети `server/src/main/java/ua/pntu/events/server`: `model`, `db`, `dao`, `service`, `util`, `demo`.

- DAO (JDBC, PreparedStatement): `PersonDAO`, `EventDAO`, `ResourceDAO`, `TaskDAO`, `TaskDependencyDAO`, `TaskResourceDAO`
- Service: `PersonService`, `EventService`, `ResourceService`, `TaskService`, `PriorityCalculator`
- Життєвий цикл завдання: `CREATED → ASSIGNED → IN_PROGRESS → DONE` (`TaskService.isAllowedTransition`)
- Показник: `I = 0,40·F1 + 0,35·F2 + 0,25·F3` (`PriorityCalculator`)
- Журнал: `server/logs/server.log` (java.util.logging, не потрапляє в git)

Демонстрація роботи Service з MySQL (після виконання `database.sql`):
```
cd server
mvn compile exec:java -Dexec.mainClass=ua.pntu.events.server.demo.ServerDemo
```

## Етап III: HTTP/JSON, Servlet, JavaFX-клієнт
Маршрут: `JavaFX → HttpEventManagerClient → HTTP/JSON → Servlet → Service → DAO → JDBC → MySQL`.

| URL | GET | POST (`action`) |
|-----|-----|-----------------|
| `/persons` | список | `add`, `update`, `delete` |
| `/events` | список | `add`, `update`, `delete` |
| `/resources` | список | `add`, `update`, `delete` |
| `/tasks` | список за показником I (`?eventId=` — лише завдання події) | `add`, `update`, `delete`, `assign`, `transition`, `refresh` |
| `/task-links` | залежності й ресурси завдання (`?id=`) | `addDependency`, `removeDependency`, `addResource`, `removeResource` |

Помилки: HTTP 400 (некоректні дані, заборонений перехід) або 500, тіло `{"error": "..."}`.

### Запуск
1. MySQL (XAMPP) запущено, `database.sql` виконано.
2. Сервер: `cd server && mvn clean package` → `server/target/event-manager-server.war` скопіювати в `webapps` Tomcat 9.x, запустити Tomcat.
   Пароль до БД, якщо він не порожній: `CATALINA_OPTS=-Ddb.password=ваш_пароль` (також `-Ddb.url`, `-Ddb.user`).
3. Перевірка сервера: `http://localhost:8080/event-manager-server/tasks` у браузері; приклади запитів — `docs/api-tests.md`.
4. Клієнт: `cd client && mvn javafx:run` (інша адреса сервера: `-Dserver.url=http://host:port/event-manager-server`).
5. CSV: вкладки «Завдання» і «Ресурси» → «Імпортувати CSV» (`client/sample-data`).
