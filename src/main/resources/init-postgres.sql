CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'TEST_ENGINEER', 'DEVELOPER', 'PROJECT_MANAGER'))
);

CREATE TABLE IF NOT EXISTS defects (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by INT NOT NULL,
    assigned_to INT NULL,
    resolution_notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_defects_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_defects_status CHECK (status IN ('NEW', 'ASSIGNED', 'FIXED', 'PENDING', 'RE-OPEN')),
    CONSTRAINT fk_defect_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_defect_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id)
);

ALTER SEQUENCE defects_id_seq RESTART WITH 1001;

INSERT INTO users (id, full_name, email, password, role)
VALUES (1, 'System Admin', 'admin@dts.com', 'admin123', 'ADMIN')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, full_name, email, password, role)
VALUES (2, 'QA Tester', 'tester@dts.com', 'test123', 'TEST_ENGINEER')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, full_name, email, password, role)
VALUES (3, 'Software Developer', 'dev@dts.com', 'dev123', 'DEVELOPER')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, full_name, email, password, role)
VALUES (4, 'Project Manager', 'pm@dts.com', 'pm123', 'PROJECT_MANAGER')
ON CONFLICT (id) DO NOTHING;

INSERT INTO defects (title, description, severity, status, created_by, assigned_to, resolution_notes)
SELECT 'Login button not working', 'Clicking login button does nothing on Chrome 123.', 'HIGH', 'NEW', 2, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM defects);
