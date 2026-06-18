CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    plate_no VARCHAR(20) NOT NULL UNIQUE,
    brand VARCHAR(255),
    model VARCHAR(255),
    color VARCHAR(255),
    year INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vehicles_customer_id ON vehicles (customer_id);