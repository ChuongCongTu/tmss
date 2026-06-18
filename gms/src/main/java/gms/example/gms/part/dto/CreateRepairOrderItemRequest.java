package gms.example.gms.part.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class CreateRepairOrderItemRequest {
    private UUID partId;

    private Integer quantity;
}
