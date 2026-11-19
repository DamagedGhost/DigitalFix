CREATE TABLE audit_records (
    id BIGSERIAL PRIMARY KEY,
    audit_id VARCHAR(64) NOT NULL UNIQUE,
    event_timestamp VARCHAR(40),
    source_service VARCHAR(80),
    flow VARCHAR(40),
    outcome VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    order_id BIGINT,
    original_event_id VARCHAR(64) NOT NULL,
    original_type VARCHAR(80),
    trace_id VARCHAR(64),
    correlation_id VARCHAR(80),
    received_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_order ON audit_records(order_id);