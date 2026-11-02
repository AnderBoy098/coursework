# Перевірка Servlet окремо від JavaFX (етап 3)

Сервер: `http://localhost:8080/event-manager-server`. У Windows PowerShell використовуйте `curl.exe`
(а не `curl`), у командному рядку або Git Bash — `curl`. Параметри передаються як HTTP-форма.

| № | Запит | Очікуваний результат |
|---|-------|----------------------|
| 1 | `curl.exe http://localhost:8080/event-manager-server/tasks` | 200, JSON-масив завдань за спаданням показника `priorityIndex` |
| 2 | `curl.exe http://localhost:8080/event-manager-server/persons` | 200, JSON-масив відповідальних осіб |
| 3 | `curl.exe -X POST http://localhost:8080/event-manager-server/resources -d "action=add" -d "name=Тестовий ресурс" -d "resourceType=Обладнання" -d "totalQuantity=4" -d "unit=шт."` | 200, у списку з'явився новий ресурс (перевірити в phpMyAdmin) |
| 4 | `curl.exe -X POST http://localhost:8080/event-manager-server/resources -d "action=update" -d "id=ID" -d "name=Змінений" -d "resourceType=Обладнання" -d "totalQuantity=5" -d "unit=шт."` | 200, запис змінено |
| 5 | `curl.exe -X POST http://localhost:8080/event-manager-server/resources -d "action=delete" -d "id=ID"` | 200, запис видалено |
| 6 | `curl.exe -X POST http://localhost:8080/event-manager-server/tasks -d "action=add" -d "eventId=1" -d "title=Перевірка" -d "deadline=2026-11-20 18:00" -d "criticality=4"` | 200, нове завдання зі станом `CREATED` та обчисленим `priorityIndex` |
| 7 | `curl.exe -X POST http://localhost:8080/event-manager-server/tasks -d "action=transition" -d "id=ID" -d "state=DONE"` | **400**, `{"error":"Перехід CREATED → DONE заборонений …"}` |
| 8 | `curl.exe -X POST http://localhost:8080/event-manager-server/tasks -d "action=assign" -d "id=ID" -d "personId=1"` | 200, стан `ASSIGNED` |
| 9 | `curl.exe -X POST http://localhost:8080/event-manager-server/tasks -d "action=add" -d "eventId=1" -d "title=" -d "deadline=2026-11-20 18:00" -d "criticality=4"` | **400**, `{"error":"Поле «Назва завдання» є обов'язковим"}` |
| 10 | `curl.exe -X POST http://localhost:8080/event-manager-server/tasks -d "action=add" -d "eventId=1" -d "title=X" -d "deadline=вчора" -d "criticality=4"` | **400**, помилка формату дати |
| 11 | `curl.exe -X POST http://localhost:8080/event-manager-server/tasks -d "action=zzz"` | **400**, «Невідома операція» |
| 12 | `curl.exe "http://localhost:8080/event-manager-server/task-links?id=1"` | 200, `{"dependsOn":[…],"resources":[…]}` |

Після кожного 400-запиту перевірте, що сервер продовжує відповідати (запит № 1 знову повертає 200) —
помилковий запит не призводить до аварійного завершення сервера. Журнал: `logs/server.log`
у робочому каталозі Tomcat та консоль Tomcat (`catalina.out` / вікно запуску).
