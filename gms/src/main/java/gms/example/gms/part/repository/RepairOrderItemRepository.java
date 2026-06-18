package gms.example.gms.part.repository;

import gms.example.gms.part.entity.RepairOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RepairOrderItemRepository extends JpaRepository<RepairOrderItem, UUID> {
}
