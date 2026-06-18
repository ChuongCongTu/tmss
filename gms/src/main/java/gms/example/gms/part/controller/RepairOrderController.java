package gms.example.gms.part.controller;

import gms.example.gms.common.ApiResponse;
import gms.example.gms.part.dto.CreateRepairOrderRequest;
import gms.example.gms.part.dto.RepairOrderResponse;
import gms.example.gms.part.service.RepairOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RepairOrderController {

    private final RepairOrderService repairOrderService;

    @PostMapping("/api/repair-orders")
    ResponseEntity<ApiResponse<RepairOrderResponse>> createRepairOrder(@RequestBody @Valid CreateRepairOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(repairOrderService.createRepairOrder(request)));
    }

    @GetMapping("/api/repair-orders/{id}")
    ResponseEntity<ApiResponse<RepairOrderResponse>> getRepairOrder(@PathVariable UUID uuid) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(repairOrderService.getRepairOrder(uuid)));
    }

    @GetMapping("/api/{vehicleId}/repair-orders")
    ResponseEntity<ApiResponse<List<RepairOrderResponse>>> getRepairOrderByVehicle(@PathVariable UUID vehicleId) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(repairOrderService.getRepairOrderByVehicle(vehicleId)));
    }
}
