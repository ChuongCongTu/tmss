package gms.example.gms.part.dto;

import gms.example.gms.part.entity.Part;
import gms.example.gms.part.entity.RepairOrder;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class RepairOrderItemResponse {
    private UUID id;

    private UUID repairOrderId;

    private UUID partId;

    private String partNo;

    private BigDecimal unitPrice;

    private Integer quantity;
}
