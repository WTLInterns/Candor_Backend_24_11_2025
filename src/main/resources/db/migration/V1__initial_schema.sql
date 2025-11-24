CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role ENUM('ADMIN','AGENT') NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE leads (
    id VARCHAR(36) PRIMARY KEY,
    company_name VARCHAR(255),
    contact_name VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255),
    address VARCHAR(500),
    latitude DOUBLE,
    longitude DOUBLE,
    status ENUM('NEW','CONTACTED','QUALIFIED','PROPOSAL','CLOSED_WON','CLOSED_LOST'),
    priority ENUM('LOW','MEDIUM','HIGH'),
    assigned_agent_id VARCHAR(36),
    source VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_lead_assigned_agent FOREIGN KEY (assigned_agent_id) REFERENCES users(id)
);

CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_assigned_agent ON leads(assigned_agent_id);

CREATE TABLE agent_profiles (
    id VARCHAR(36) PRIMARY KEY,
    photo_url VARCHAR(500),
    territory VARCHAR(255),
    last_seen TIMESTAMP NULL,
    status ENUM('ACTIVE','INACTIVE','ONLINE','OFFLINE'),
    CONSTRAINT fk_agent_profiles_user FOREIGN KEY (id) REFERENCES users(id)
);

CREATE TABLE interactions (
    id VARCHAR(36) PRIMARY KEY,
    lead_id VARCHAR(36) NOT NULL,
    agent_id VARCHAR(36) NOT NULL,
    type ENUM('CALL','VISIT','EMAIL','NOTE') NOT NULL,
    notes TINYTEXT,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_interactions_lead FOREIGN KEY (lead_id) REFERENCES leads(id),
    CONSTRAINT fk_interactions_agent FOREIGN KEY (agent_id) REFERENCES users(id)
);

CREATE TABLE locations (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    accuracy FLOAT,
    status VARCHAR(50),
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT fk_locations_agent FOREIGN KEY (agent_id) REFERENCES users(id)
);

CREATE TABLE import_jobs (
    id VARCHAR(36) PRIMARY KEY,
    uploaded_by VARCHAR(36) NOT NULL,
    file_url VARCHAR(500),
    total_rows INT,
    success_count INT,
    fail_count INT,
    status ENUM('PENDING','PROCESSING','COMPLETED','FAILED') NOT NULL,
    error_file_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_import_jobs_user FOREIGN KEY (uploaded_by) REFERENCES users(id)
);
