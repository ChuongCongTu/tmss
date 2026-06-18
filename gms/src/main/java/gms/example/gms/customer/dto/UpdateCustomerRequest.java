package gms.example.gms.customer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCustomerRequest {
    private String fullName;
    private String address;
    private String phone;
}
