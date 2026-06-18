package gms.example.gms.part.controller;

import gms.example.gms.common.ApiResponse;
import gms.example.gms.part.dto.CreateInvoiceRequest;
import gms.example.gms.part.dto.InvoiceResponse;
import gms.example.gms.part.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/api/repair-orders/{orderid}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(@PathVariable UUID orderid, @RequestBody @Valid CreateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(invoiceService.createInvoice(orderid, request)));
    }

    @GetMapping("/api/invoices/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(invoiceService.getInvoiceById(id)));
    }

    @PostMapping("/api/invoices/{id}/payment")
    public ResponseEntity<ApiResponse<InvoiceResponse>> changeStatusInvoice(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(invoiceService.changeStatusInvoice(id)));
    }
}
