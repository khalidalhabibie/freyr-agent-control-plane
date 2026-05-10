CREATE TABLE farm_fields (
    id UUID PRIMARY KEY,
    farmer_id UUID NOT NULL REFERENCES farmers (id),
    area_name VARCHAR(255) NOT NULL,
    area_size NUMERIC(12, 2) NOT NULL,
    crop_stage VARCHAR(50) NOT NULL,
    water_status VARCHAR(50) NOT NULL,
    pest_reported BOOLEAN NOT NULL,
    last_visit_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_farm_fields_farmer_id ON farm_fields (farmer_id);
