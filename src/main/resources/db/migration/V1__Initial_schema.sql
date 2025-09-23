CREATE TABLE employees
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    department VARCHAR(255),
    tenant_id  BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE time_entries
(
    id           BIGSERIAL PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    start_time   TIMESTAMP    NOT NULL,
    end_time     TIMESTAMP,
    description  TEXT,
    employee_id  BIGINT       NOT NULL,
    billable     BOOLEAN   DEFAULT TRUE,
    synced       BOOLEAN   DEFAULT FALSE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE
);
