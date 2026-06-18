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
public class PartResponse {
    private UUID id;

    private String partNo;

    private String partName;

    private BigDecimal price;

    private Integer quantity;

    private Instant createdAt;

    private Instant updatedAt;
}
