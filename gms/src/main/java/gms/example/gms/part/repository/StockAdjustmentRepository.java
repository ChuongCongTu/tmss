package gms.example.gms.part.repository;

import gms.example.gms.part.entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, UUID> {
}
