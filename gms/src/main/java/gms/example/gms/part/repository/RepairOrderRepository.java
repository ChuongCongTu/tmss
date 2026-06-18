package gms.example.gms.part.repository;

import gms.example.gms.customer.entity.Vehicle;
import gms.example.gms.part.entity.RepairOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RepairOrderRepository extends JpaRepository<RepairOrder, UUID> {
    List<RepairOrder> findAllByVehicle(Vehicle vehicle);
}
