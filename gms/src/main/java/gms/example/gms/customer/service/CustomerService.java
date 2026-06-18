package gms.example.gms.customer.service;

import gms.example.gms.common.PageResponse;
import gms.example.gms.common.exception.BusinessException;
import gms.example.gms.common.exception.ResourceNotFoundException;
import gms.example.gms.customer.dto.CreateCustomerRequest;
import gms.example.gms.customer.dto.CreateVehicleRequest;
import gms.example.gms.customer.dto.CustomerResponse;
import gms.example.gms.customer.dto.VehicleResponse;
import gms.example.gms.customer.entity.Customer;
import gms.example.gms.customer.entity.Vehicle;
import gms.example.gms.customer.repository.CustomerRepository;
import gms.example.gms.customer.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;

    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setAddress(request.getAddress());
        customer.setPhone(request.getPhone());
        customerRepository.save(customer);
        return toCustomerResponse(customer);
    }

    public CustomerResponse findCustomerById(UUID uuid) {
        Customer customer = customerRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("customer not found"));
        return toCustomerResponse(customer);
    }

    public PageResponse<CustomerResponse> search(String fullName, String phone, Pageable pageable) {
        Page<Customer> customers = customerRepository.findAllCustomers(fullName, phone, pageable);
        return PageResponse.from(customers.map(this::toCustomerResponse));
    }

    public VehicleResponse addVehicleForCustomer(UUID id, CreateVehicleRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("customer not found"));

        Vehicle vehicle = new Vehicle();
        if (vehicleRepository.existsByPlateNo(request.getPlateNo())) {
            throw new BusinessException("Biển số đã tồn tại");
        }
        vehicle.setCustomer(customer);
        vehicle.setColor(request.getColor());
        vehicle.setModel(request.getModel());
        vehicle.setBrand(request.getBrand());
        vehicle.setYear(request.getYear());
        vehicle.setPlateNo(request.getPlateNo());

        return toVehicleResponse(vehicleRepository.save(vehicle));
    }

    public List<VehicleResponse> findAllVehicleByCustomerId(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("customer not found"));

        List<Vehicle> vehicles = vehicleRepository.findAllByCustomer(customer);

        return vehicles.stream().map(this::toVehicleResponse).toList();
    }

    private CustomerResponse toCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .fullName(customer.getFullName())
                .address(customer.getAddress())
                .phone(customer.getPhone())
                .updatedAt(customer.getUpdatedAt())
                .createdAt(customer.getCreatedAt())
                .id(customer.getId())
                .build();
    }

    private VehicleResponse toVehicleResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .brand(vehicle.getBrand())
                .color(vehicle.getColor())
                .year(vehicle.getYear())
                .model(vehicle.getModel())
                .updatedAt(vehicle.getUpdatedAt())
                .createdAt(vehicle.getCreatedAt())
                .customerId(vehicle.getCustomer().getId())
                .id(vehicle.getId())
                .plateNo(vehicle.getPlateNo())
                .build();
    }
}
