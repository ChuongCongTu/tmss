package gms.example.gms.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVehicleRequest {
    @NotBlank
    private String plateNo;

    private String brand;

    private String color;

    private String model;

    private Integer year;
}
