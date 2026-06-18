package gms.example.gms.part.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class AdjustPartStockRequest {
    private String reason;

    private Integer delta;
}
