package gms.example.gms.customer.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
public class CustomerResponse {
    private UUID id;
    private String fullName;
    private String address;
    private String phone;
    private Instant createdAt;
    private Instant updatedAt;
}
