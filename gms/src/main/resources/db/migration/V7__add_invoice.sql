CREATE TABLE invoices (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   repair_order_id UUID NOT NULL UNIQUE REFERENCES repair_orders(id),
   invoice_no  VARCHAR(30) not null unique,
   subtotal NUMERIC(12,2),
   tax_rate NUMERIC,
   tax_amount NUMERIC(12,2),
   total_amount NUMERIC(12,2),
   status VARCHAR(20),
   issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE invoice_counters (
   year        INTEGER     NOT NULL,
   last_no     BIGINT      NOT NULL DEFAULT 0,
   updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
   CONSTRAINT pk_invoice_counter PRIMARY KEY (year)
);