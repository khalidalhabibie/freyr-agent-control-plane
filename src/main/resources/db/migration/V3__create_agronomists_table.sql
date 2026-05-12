CREATE TABLE agronomists (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    assigned_district VARCHAR(255) NOT NULL,
    max_daily_visit INTEGER NOT NULL,
    availability_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
