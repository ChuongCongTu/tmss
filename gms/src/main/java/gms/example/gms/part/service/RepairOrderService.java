package gms.example.gms.part.service;

import gms.example.gms.common.exception.BusinessException;
import gms.example.gms.common.exception.ResourceNotFoundException;
import gms.example.gms.customer.dto.VehicleResponse;
import gms.example.gms.customer.entity.Vehicle;
import gms.example.gms.customer.repository.VehicleRepository;
import gms.example.gms.part.dto.CreateRepairOrderItemRequest;
import gms.example.gms.part.dto.CreateRepairOrderRequest;
import gms.example.gms.part.dto.RepairOrderItemResponse;
import gms.example.gms.part.dto.RepairOrderResponse;
import gms.example.gms.part.entity.Part;
import gms.example.gms.part.entity.RepairOrder;
import gms.example.gms.part.entity.RepairOrderItem;
import gms.example.gms.part.repository.PartRepository;
import gms.example.gms.part.repository.RepairOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepairOrderService extends RuntimeException {

    private final RepairOrderRepository repairOrderRepository;
    private final VehicleRepository vehicleRepository;
    private final PartRepository partRepository;

    @Transactional
    public RepairOrderResponse createRepairOrder(CreateRepairOrderRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        RepairOrder order = new RepairOrder();
        order.setVehicle(vehicle);
        order.setStatus(request.getStatus());
        order.setLaborCost(request.getLaborCost());
        BigDecimal total = BigDecimal.ZERO;
        for (CreateRepairOrderItemRequest item : request.getItems()) {
            Part part = partRepository.findById(item.getPartId())
                    .orElseThrow(() -> new ResourceNotFoundException("Part not found"));

            if (partRepository.decreaseStock(part.getId(), Math.abs(item.getQuantity())) == 0) {
                throw new BusinessException("Lỗi không đủ tồn kho.");
            }

            RepairOrderItem repairOrderItem = new RepairOrderItem();
            repairOrderItem.setUnitPrice(part.getPrice());
            repairOrderItem.setPart(part);
            repairOrderItem.setQuantity(item.getQuantity());

            order.addItem(repairOrderItem);
            total = total.add(part.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal total_amount = total.add(request.getLaborCost());
        order.setTotalAmount(total_amount);


        return toResponse(repairOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public RepairOrderResponse getRepairOrder(UUID uuid) {
        RepairOrder repairOrder = repairOrderRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("RepairOrder not found"));

        return toResponse(repairOrder);
    }

    @Transactional(readOnly = true)
    public List<RepairOrderResponse> getRepairOrderByVehicle(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        List<RepairOrder> repairOrder = repairOrderRepository.findAllByVehicle(vehicle);

        return repairOrder.stream().map(this::toResponse).toList();
    }

    private RepairOrderResponse toResponse(RepairOrder repairOrder) {
        Vehicle v = repairOrder.getVehicle();
        VehicleResponse vehicleResponse = VehicleResponse.builder()
                .id(v.getId())
                .plateNo(v.getPlateNo())
                .brand(v.getBrand())
                .model(v.getModel())
                .year(v.getYear())
                .customerId(v.getCustomer().getId())
                .updatedAt(v.getUpdatedAt())
                .createdAt(v.getCreatedAt())
                .build();

        List<RepairOrderItemResponse> repairOrderItemResponses = repairOrder.getItems().stream()
                .map(it -> RepairOrderItemResponse.builder()
                        .id(it.getId())
                        .partId(it.getPart().getId())
                        .partNo(it.getPart().getPartNo())     // load lazy part
                        .unitPrice(it.getUnitPrice())
                        .quantity(it.getQuantity())
                        .repairOrderId(it.getRepairOrder().getId())
                        .build()).toList();

        return RepairOrderResponse.builder()
                .vehicle(vehicleResponse)
                .status(repairOrder.getStatus())
                .items(repairOrderItemResponses)
                .totalAmount(repairOrder.getTotalAmount())
                .laborCost(repairOrder.getLaborCost())
                .id(repairOrder.getId())
                .createdAt(repairOrder.getCreatedAt())
                .updatedAt(repairOrder.getUpdatedAt())
                .build();
    }
}
