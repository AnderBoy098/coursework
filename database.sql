-- =====================================================================
-- Курсова робота: «Інформаційна система організації подій і контролю завдань»
-- Варіант 23. СУБД: MySQL 8 / MariaDB (XAMPP). Кодування: UTF-8.
-- Скрипт повністю відтворює базу даних з нуля (виконується в phpMyAdmin).
-- =====================================================================

DROP DATABASE IF EXISTS event_manager;
CREATE DATABASE event_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE event_manager;

-- ---------- Відповідальна особа ----------
CREATE TABLE persons (
    id         INT          NOT NULL AUTO_INCREMENT,
    full_name  VARCHAR(150) NOT NULL,
    email      VARCHAR(120) NOT NULL,
    phone      VARCHAR(30)  NULL,
    job_title  VARCHAR(100) NULL,
    CONSTRAINT pk_persons PRIMARY KEY (id),
    CONSTRAINT uq_persons_email UNIQUE (email)
) ENGINE = InnoDB;

-- ---------- Подія ----------
CREATE TABLE events (
    id            INT          NOT NULL AUTO_INCREMENT,
    title         VARCHAR(200) NOT NULL,
    description   TEXT         NULL,
    location      VARCHAR(200) NULL,
    start_date    DATETIME     NOT NULL,
    end_date      DATETIME     NOT NULL,
    organizer_id  INT          NOT NULL,
    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id)
        REFERENCES persons (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_events_dates CHECK (end_date >= start_date)
) ENGINE = InnoDB;

-- ---------- Завдання (об'єкт життєвого циклу) ----------
CREATE TABLE tasks (
    id              INT           NOT NULL AUTO_INCREMENT,
    event_id        INT           NOT NULL,
    title           VARCHAR(200)  NOT NULL,
    description     TEXT          NULL,
    deadline        DATETIME      NOT NULL,
    criticality     TINYINT       NOT NULL DEFAULT 3,
    status          ENUM('CREATED','ASSIGNED','IN_PROGRESS','DONE') NOT NULL DEFAULT 'CREATED',
    assignee_id     INT           NULL,
    priority_index  DECIMAL(5,2)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT fk_tasks_event FOREIGN KEY (event_id)
        REFERENCES events (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id)
        REFERENCES persons (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------- Ресурс ----------
CREATE TABLE resources (
    id              INT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(150) NOT NULL,
    resource_type   VARCHAR(50)  NOT NULL,
    total_quantity  INT          NOT NULL DEFAULT 1,
    unit            VARCHAR(20)  NOT NULL DEFAULT 'шт.',
    CONSTRAINT pk_resources PRIMARY KEY (id),
    CONSTRAINT chk_resources_qty CHECK (total_quantity >= 0)
) ENGINE = InnoDB;

-- ---------- Зв'язок «завдання — ресурс» (M:N) ----------
CREATE TABLE task_resources (
    task_id          INT NOT NULL,
    resource_id      INT NOT NULL,
    quantity_needed  INT NOT NULL DEFAULT 1,
    CONSTRAINT pk_task_resources PRIMARY KEY (task_id, resource_id),
    CONSTRAINT fk_tr_task FOREIGN KEY (task_id)
        REFERENCES tasks (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tr_resource FOREIGN KEY (resource_id)
        REFERENCES resources (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------- Залежності між завданнями (M:N, для фактора F3) ----------
-- Запис (task_id, depends_on_id) означає: task_id залежить від depends_on_id.
CREATE TABLE task_dependencies (
    task_id        INT NOT NULL,
    depends_on_id  INT NOT NULL,
    CONSTRAINT pk_task_dependencies PRIMARY KEY (task_id, depends_on_id),
    CONSTRAINT fk_td_task FOREIGN KEY (task_id)
        REFERENCES tasks (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_td_depends FOREIGN KEY (depends_on_id)
        REFERENCES tasks (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =====================================================================
-- Початкові демонстраційні дані
-- =====================================================================
INSERT INTO persons (full_name, email, phone, job_title) VALUES
 ('Скляр Андрій Олександрович', 'sklyar@example.com',  '+380501112233', 'Організатор'),
 ('Іваненко Марія Петрівна',    'ivanenko@example.com', '+380502223344', 'Координатор'),
 ('Коваленко Дмитро Сергійович','kovalenko@example.com','+380503334455', 'Технічний спеціаліст'),
 ('Мельник Олена Василівна',    'melnyk@example.com',   '+380504445566', 'Менеджер із логістики');

INSERT INTO events (title, description, location, start_date, end_date, organizer_id) VALUES
 ('Студентська наукова конференція', 'Щорічна конференція кафедри комп''ютерних технологій', 'Полтава, головний корпус, ауд. 101', '2026-11-20 10:00:00', '2026-11-20 17:00:00', 1),
 ('День відкритих дверей',           'Захід для абітурієнтів та їхніх батьків',               'Полтава, актова зала',               '2026-12-05 11:00:00', '2026-12-05 15:00:00', 2);

INSERT INTO resources (name, resource_type, total_quantity, unit) VALUES
 ('Проєктор',            'Обладнання', 3,   'шт.'),
 ('Мікрофон бездротовий','Обладнання', 6,   'шт.'),
 ('Лекційна аудиторія',  'Приміщення', 2,   'шт.'),
 ('Роздатковий матеріал','Витратні',   500, 'шт.'),
 ('Кейтеринг (кава-брейк)','Послуги',  1,   'компл.');

INSERT INTO tasks (event_id, title, description, deadline, criticality, status, assignee_id) VALUES
 (1, 'Забронювати аудиторію',           'Узгодити з деканатом аудиторію на дату конференції', '2026-10-30 18:00:00', 5, 'IN_PROGRESS', 4),
 (1, 'Підготувати програму конференції','Сформувати розклад доповідей',                       '2026-11-10 18:00:00', 4, 'ASSIGNED',    2),
 (1, 'Налаштувати відеообладнання',     'Перевірити проєктор і мікрофони',                    '2026-11-19 16:00:00', 3, 'CREATED',     NULL),
 (1, 'Надіслати запрошення учасникам',  'Розіслати електронні запрошення',                    '2026-11-05 12:00:00', 4, 'DONE',        2),
 (2, 'Замовити кейтеринг',              'Кава-брейк для гостей',                              '2026-11-25 12:00:00', 2, 'CREATED',     NULL),
 (2, 'Підготувати роздатковий матеріал','Надрукувати буклети',                                '2026-12-01 15:00:00', 3, 'ASSIGNED',    4),
 (2, 'Розробити сценарій заходу',       'Скласти хронологію Дня відкритих дверей',            '2026-11-20 18:00:00', 5, 'IN_PROGRESS', 2);

INSERT INTO task_resources (task_id, resource_id, quantity_needed) VALUES
 (1, 3, 1), (3, 1, 1), (3, 2, 2), (5, 5, 1), (6, 4, 300);

-- 3 залежить від 1; 2 залежить від 1; 3 залежить від 2; 6 залежить від 7
INSERT INTO task_dependencies (task_id, depends_on_id) VALUES
 (2, 1), (3, 1), (3, 2), (6, 7);
