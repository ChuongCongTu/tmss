package gms.example.gms.part.dto;

import gms.example.gms.customer.dto.VehicleResponse;
import gms.example.gms.customer.entity.Vehicle;
import gms.example.gms.part.entity.RepairOrderItem;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class RepairOrderResponse {
    private UUID id;

    private VehicleResponse vehicle;

    private List<RepairOrderItemResponse> items = new ArrayList<>();

    private String status;

    private BigDecimal laborCost;

    private BigDecimal totalAmount;

    private Instant createdAt;

    private Instant updatedAt;
}
