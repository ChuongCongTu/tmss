CREATE TABLE repair_orders (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   vehicle_id UUID NOT NULL REFERENCES vehicles(id),
   status VARCHAR(20),
   labor_cost NUMERIC(12,2),
   total_amount NUMERIC(12,2),
   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_repair_orders_vehicle_id ON repair_orders (vehicle_id);

CREATE TABLE repair_order_items (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   repair_order_id UUID NOT NULL REFERENCES repair_orders(id),
   part_id UUID NOT NULL REFERENCES parts(id),
   unit_price NUMERIC(12,2),
   quantity INTEGER NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_repair_order_items_repair_order_id ON repair_order_items (repair_order_id);

