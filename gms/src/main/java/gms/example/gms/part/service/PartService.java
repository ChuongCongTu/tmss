package gms.example.gms.part.service;

import gms.example.gms.common.PageResponse;
import gms.example.gms.common.exception.BusinessException;
import gms.example.gms.common.exception.ResourceNotFoundException;
import gms.example.gms.customer.entity.Customer;
import gms.example.gms.part.dto.AdjustPartStockRequest;
import gms.example.gms.part.dto.CreatePartRequest;
import gms.example.gms.part.dto.PartResponse;
import gms.example.gms.part.dto.UpdatePartRequest;
import gms.example.gms.part.entity.Part;
import gms.example.gms.part.entity.StockAdjustment;
import gms.example.gms.part.repository.PartRepository;
import gms.example.gms.part.repository.RepairOrderRepository;
import gms.example.gms.part.repository.StockAdjustmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartService {
    private final PartRepository partRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;

    public PartResponse createPart(CreatePartRequest request) {
        if (partRepository.existsByPartNo(request.getPartNo())) {
            throw new BusinessException("Mã phụ tùng đã tồn tại.");
        }

        Part part = new Part();
        part.setPartNo(request.getPartNo());
        part.setPartName(request.getPartName());
        part.setPrice(request.getPrice());
        part.setQuantity(request.getQuantity());

        return toResponse(partRepository.save(part));
    }

    @Transactional(readOnly = true)
    public PartResponse findPartById(UUID id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("part not found"));

        return toResponse(part);
    }

    @Transactional(readOnly = true)
    public PageResponse<PartResponse> search(String partNo, Pageable pageable) {
        Page<Part> parts = partRepository.findAll(partNo, pageable);

        return PageResponse.from(parts.map(this::toResponse));
    }

    @Transactional
    public PartResponse update(UUID id, UpdatePartRequest request) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("part not found"));

        if (request.getPartNo() != null) {
            part.setPartNo(request.getPartNo());
        }

        if (request.getPartName() != null) {
            part.setPartName((request.getPartName()));
        }

        if (request.getPrice() != null) {
            part.setPrice(request.getPrice());
        }

        return toResponse(part);
    }

    @Transactional
    public PartResponse adjustPartStock(UUID id, AdjustPartStockRequest request) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("part not found"));

        if (request.getDelta() < 0) {
            if (partRepository.decreaseStock(id, Math.abs(request.getDelta())) == 0) {
                throw new BusinessException("Lỗi không đủ tồn kho.");
            }
        } else {
            partRepository.increaseStock(id, Math.abs(request.getDelta()));
        }

        StockAdjustment stockAdjustment = new StockAdjustment();
        stockAdjustment.setDelta(request.getDelta());
        stockAdjustment.setReason(request.getReason());
        stockAdjustment.setPart(part);

        Part partUpdate = partRepository.findById(id).orElseThrow();

        stockAdjustmentRepository.save(stockAdjustment);
        return toResponse(partUpdate);
    }

    private PartResponse toResponse(Part part) {
        return PartResponse.builder()
                .id(part.getId())
                .partNo(part.getPartNo())
                .price(part.getPrice())
                .partName(part.getPartName())
                .quantity(part.getQuantity())
                .createdAt(part.getCreatedAt())
                .updatedAt(part.getUpdatedAt())
                .build();
    }
}
