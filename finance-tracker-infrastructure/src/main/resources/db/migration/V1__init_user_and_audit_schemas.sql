-- V1__init_user_and_audit_schemas.sql

CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100),
    payload TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes for performance tuning
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_audit_events_aggregate_id ON audit_events(aggregate_id);
CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp);
