package gms.example.gms.part.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class CreateRepairOrderRequest {

    private UUID vehicleId;

    private List<CreateRepairOrderItemRequest> items = new ArrayList<>();

    private String status;

    private BigDecimal laborCost;
}
