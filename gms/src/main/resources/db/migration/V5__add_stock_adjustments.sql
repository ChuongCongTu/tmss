CREATE TABLE stock_adjustments (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   part_id UUID NOT NULL REFERENCES parts(id),
   delta INTEGER,
   reason VARCHAR(255),
   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
