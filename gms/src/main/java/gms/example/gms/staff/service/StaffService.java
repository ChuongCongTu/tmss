package gms.example.gms.staff.service;

import gms.example.gms.common.exception.BusinessException;
import gms.example.gms.common.exception.UnauthorizedException;
import gms.example.gms.common.security.JwtUtil;
import gms.example.gms.staff.dto.LoginRequest;
import gms.example.gms.staff.dto.LoginResponse;
import gms.example.gms.staff.dto.RegisterRequest;
import gms.example.gms.staff.dto.StaffResponse;
import gms.example.gms.staff.entity.Staff;
import gms.example.gms.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffService {
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        Staff staff = staffRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Login không thành công"));

        if (!passwordEncoder.matches(request.getPassword(), staff.getPasswordHash())) {
            throw new UnauthorizedException("Login không thành công");
        }

        String token = jwtUtil.generateToken(staff);

        return LoginResponse.builder()
                .id(staff.getId())
                .token(token)
                .username(request.getUsername())
                .fullName(staff.getFullName())
                .enabled(staff.getEnabled())
                .role(staff.getRole().name())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }

    public StaffResponse register(RegisterRequest request) {
        if (staffRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username is exists");
        }

        Staff staff = new Staff();
        staff.setRole(request.getRole());
        staff.setFullName(request.getFullName());
        staff.setUsername(request.getUsername());
        staff.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        staffRepository.save(staff);

        return StaffResponse.builder()
                .id(staff.getId())
                .username(request.getUsername())
                .fullName(staff.getFullName())
                .enabled(staff.getEnabled())
                .role(staff.getRole())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }
}
