CREATE TABLE parts (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   part_no VARCHAR(20) NOT NULL UNIQUE,
   part_name VARCHAR(255),
   price NUMERIC(15,2),
   quantity INTEGER NOT NULL CHECK (quantity > 0),
   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
