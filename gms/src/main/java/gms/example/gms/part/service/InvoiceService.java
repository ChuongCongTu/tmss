package gms.example.gms.part.service;

import gms.example.gms.common.exception.BusinessException;
import gms.example.gms.common.exception.ResourceNotFoundException;
import gms.example.gms.part.dto.CreateInvoiceRequest;
import gms.example.gms.part.dto.InvoiceNoResut;
import gms.example.gms.part.dto.InvoiceResponse;
import gms.example.gms.part.entity.Invoice;
import gms.example.gms.part.entity.RepairOrder;
import gms.example.gms.part.repository.InvoiceRepository;
import gms.example.gms.part.repository.RepairOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final RepairOrderRepository repairOrderRepository;

    public InvoiceResponse createInvoice(UUID orderId, CreateInvoiceRequest request) {
        RepairOrder repairOrder = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("order not found"));

        InvoiceNoResut r = invoiceRepository.nextNo();
        String invoiceNo = String.format("SC-%d-%06d", r.getYear(), r.getLastNo());

        boolean invoiceCheck = invoiceRepository.existsInvoiceByRepairOrderId(orderId);

        if (invoiceCheck) {
            throw new DataIntegrityViolationException("Hóa đơn đã tồn tại với phiếu này.");
        }

        Invoice invoice = new Invoice();
        invoice.setSubtotal(repairOrder.getTotalAmount());
        invoice.setTaxAmount(repairOrder.getTotalAmount().multiply(request.getTaxRate()).setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalAmount(repairOrder.getTotalAmount().add(invoice.getTaxAmount()));
        invoice.setInvoiceNo(invoiceNo);
        invoice.setTaxRate(request.getTaxRate());
        invoice.setIssuedAt(Instant.now());
        invoice.setStatus("UNPAID");
        invoice.setRepairOrder(repairOrder);

        return toResponse(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("invoice not found"));

        return toResponse(invoice);
    }

    public InvoiceResponse changeStatusInvoice(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("invoice not found"));

        if (Objects.equals(invoice.getStatus(), "PAID")) {
            throw new BusinessException("Không thể đổi status khi đang ở trạng thái PAID");
        }
        invoice.setStatus("PAID");

        return toResponse(invoiceRepository.save(invoice));
    }

    private InvoiceResponse toResponse(Invoice i) {
        return InvoiceResponse.builder()
                .id(i.getId())
                .repair_order_id(i.getRepairOrder().getId())
                .invoiceNo(i.getInvoiceNo())
                .subtotal(i.getSubtotal())
                .taxRate(i.getTaxRate())
                .taxAmount(i.getTaxAmount())
                .totalAmount(i.getTotalAmount())
                .status(i.getStatus())
                .issuedAt(i.getIssuedAt())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
