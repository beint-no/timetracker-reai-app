CREATE TABLE employees
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    tenant_id  BIGINT       NOT NULL,
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
    tenant_id    BIGINT       NOT NULL,
    billable     BOOLEAN   DEFAULT TRUE,
    synced       BOOLEAN   DEFAULT FALSE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    entry_date DATE DEFAULT CURRENT_DATE
);

CREATE TABLE tenant_oauth_configs (
                                      tenant_id BIGINT PRIMARY KEY,
                                      client_id VARCHAR(255) NOT NULL,
                                      client_secret VARCHAR(255) NOT NULL,
                                      scopes VARCHAR(500) DEFAULT NULL,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

