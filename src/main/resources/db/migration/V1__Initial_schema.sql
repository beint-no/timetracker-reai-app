CREATE TABLE employees
(
    id         INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE time_entries
(
    id                 INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id         INT NOT NULL,
    start_time         TIMESTAMP    NOT NULL,
    end_time           TIMESTAMP,
    start_time_millis  BIGINT,
    end_time_millis    BIGINT,
    total_hours        NUMERIC(10, 3),
    total_milliseconds BIGINT,
    employee_id        INT          NOT NULL,
    billable           BOOLEAN   DEFAULT TRUE,
    synced             BOOLEAN   DEFAULT FALSE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    entry_date         DATE      DEFAULT CURRENT_DATE
);