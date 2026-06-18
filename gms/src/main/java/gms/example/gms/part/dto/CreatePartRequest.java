package gms.example.gms.part.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CreatePartRequest {
    private String partNo;

    private String partName;

    private BigDecimal price;

    private Integer quantity;
}
