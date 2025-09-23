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
    FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE
);

CREATE INDEX idx_employees_tenant_id ON employees(tenant_id);
CREATE INDEX idx_employees_email_tenant ON employees(email, tenant_id);
CREATE INDEX idx_time_entries_tenant_id ON time_entries(tenant_id);
CREATE INDEX idx_time_entries_employee_tenant ON time_entries(employee_id, tenant_id);
CREATE INDEX idx_time_entries_synced_tenant ON time_entries(synced, tenant_id);
CREATE INDEX idx_time_entries_start_time ON time_entries(start_time);
ALTER TABLE employees DROP CONSTRAINT IF EXISTS employees_email_key;
ALTER TABLE employees ADD CONSTRAINT employees_email_tenant_unique UNIQUE (email, tenant_id);


