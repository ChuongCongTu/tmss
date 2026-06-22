package gms.example.gms.customer.controller;

import gms.example.gms.common.ApiResponse;
import gms.example.gms.common.PageResponse;
import gms.example.gms.customer.dto.*;
import gms.example.gms.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@RequestBody @Valid CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(customerService.createCustomer(request)));
    }

    @PatchMapping("/{id}")
    ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(@PathVariable UUID id, @RequestBody @Valid UpdateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(customerService.updateCustomer(id, request)));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<String>> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully"));
    }

    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<CustomerResponse>> findCustomerById(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(customerService.findCustomerById(id)));
    }

    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> search(String fullName, String phone, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(customerService.search(fullName, phone, pageable)));
    }

    @PostMapping("/{id}/vehicles")
    ResponseEntity<ApiResponse<VehicleResponse>> addVehicleForCustomer(@PathVariable UUID id, @RequestBody @Valid CreateVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(customerService.addVehicleForCustomer(id, request)));
    }

    @GetMapping("/{id}/vehicles")
    ResponseEntity<ApiResponse<List<VehicleResponse>>> findAllVehicleByCustomerId(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(customerService.findAllVehicleByCustomerId(id)));
    }
}
