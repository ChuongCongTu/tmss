package gms.example.gms.staff.dto;

import gms.example.gms.staff.enums.StaffRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
public class StaffResponse {
    private UUID id;

    private String username;

    private StaffRole role;

    private Boolean enabled = true;

    private String fullName;

    private Instant createdAt;

    private Instant updatedAt;
}
