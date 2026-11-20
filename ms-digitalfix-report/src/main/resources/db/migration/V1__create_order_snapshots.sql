CREATE TABLE order_snapshots (
    order_id BIGINT PRIMARY KEY,
    client_name VARCHAR(120),
    technician_name VARCHAR(120),
    status VARCHAR(30) NOT NULL,
    last_event_id VARCHAR(64),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);