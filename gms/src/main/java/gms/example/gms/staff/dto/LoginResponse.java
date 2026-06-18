package gms.example.gms.staff.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
public class LoginResponse {
    private UUID id;

    private String username;

    private String token;

    private String role;

    private Boolean enabled = true;

    private String fullName;

    private Instant createdAt;

    private Instant updatedAt;
}
