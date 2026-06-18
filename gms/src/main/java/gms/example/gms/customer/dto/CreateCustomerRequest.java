package gms.example.gms.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class CreateCustomerRequest {
    @NotBlank
    private String fullName;
    private String address;
    private String phone;
}
