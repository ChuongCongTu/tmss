package gms.example.gms.part.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
public class InvoiceResponse {
    private UUID id;

    private UUID repair_order_id;

    private String invoiceNo;

    private String status;

    private BigDecimal subtotal;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private Instant issuedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
