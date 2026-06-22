package gms.example.gms.customer.repository;

import gms.example.gms.customer.entity.Customer;
import gms.example.gms.customer.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    boolean existsByPlateNo(String plateNo);

    List<Vehicle> findAllByCustomer(Customer customer);

    boolean existsByCustomer(Customer customer);
}
