package gms.example.gms.staff.dto;

import gms.example.gms.staff.enums.StaffRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegisterRequest {
    private String username;

    private StaffRole role;

    private Boolean enabled = true;

    private String fullName;

    private String password;
}
