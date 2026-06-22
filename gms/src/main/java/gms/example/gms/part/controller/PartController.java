package gms.example.gms.part.controller;

import gms.example.gms.common.ApiResponse;
import gms.example.gms.common.PageResponse;
import gms.example.gms.customer.dto.CustomerResponse;
import gms.example.gms.customer.dto.UpdateCustomerRequest;
import gms.example.gms.part.dto.AdjustPartStockRequest;
import gms.example.gms.part.dto.CreatePartRequest;
import gms.example.gms.part.dto.PartResponse;
import gms.example.gms.part.dto.UpdatePartRequest;
import gms.example.gms.part.service.PartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;

    @PostMapping
    public ResponseEntity<ApiResponse<PartResponse>> createPart(@RequestBody @Valid CreatePartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(partService.createPart(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PartResponse>> findPartById(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(partService.findPartById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PartResponse>>> search(String partNo, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(partService.search(partNo, pageable)));
    }

    @PatchMapping("/{id}")
    ResponseEntity<ApiResponse<PartResponse>> update(@PathVariable UUID id, @RequestBody @Valid UpdatePartRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(partService.update(id, request)));
    }

    // /api/parts/{id}/stock-adjustments
    @PostMapping("/{id}/stock-adjustments")
    public ResponseEntity<ApiResponse<PartResponse>> adjustPartStock(@PathVariable UUID id, @RequestBody @Valid AdjustPartStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(partService.adjustPartStock(id, request)));
    }

}
