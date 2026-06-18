package gms.example.gms.staff.controller;

import gms.example.gms.common.ApiResponse;
import gms.example.gms.staff.dto.LoginRequest;
import gms.example.gms.staff.dto.LoginResponse;
import gms.example.gms.staff.dto.RegisterRequest;
import gms.example.gms.staff.dto.StaffResponse;
import gms.example.gms.staff.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class StaffController {
    private final StaffService staffService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(staffService.login(request)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<StaffResponse>> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(staffService.register(request)));
    }
}
