package gms.example.gms.staff.repository;

import gms.example.gms.staff.entity.Staff;
import gms.example.gms.staff.enums.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {
    Optional<Staff> findByUsername(String name);
    boolean existsByUsername(String name);
    boolean existsByRole(StaffRole role);
}
