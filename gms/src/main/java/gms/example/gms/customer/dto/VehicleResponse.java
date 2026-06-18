package gms.example.gms.customer.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
public class VehicleResponse {
    private UUID id;
    private String plateNo;

    private String brand;

    private String color;

    private String model;

    private Integer year;

    private UUID customerId;

    private Instant createdAt;

    private Instant updatedAt;
}
