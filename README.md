# Інформаційна система організації подій і контролю завдань (варіант 23)

Курсова робота з дисципліни «Кросплатформене програмування». Студент: Скляр А. О., гр. 202-TH.

## Структура
- `server/` — серверна частина (Maven, Java 21, JDBC/MySQL; далі Servlet API, Service, DAO)
- `client/` — JavaFX-клієнт (Maven, Java 21)
- `database.sql` — створення БД `event_manager` з нуля + демонстраційні дані
- `data/` — CSV-файли для імпорту (`tasks.csv`, `resources.csv`), шляхи відносні
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
