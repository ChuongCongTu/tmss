package gms.example.gms.part.repository;

import gms.example.gms.customer.entity.Customer;
import gms.example.gms.part.entity.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PartRepository extends JpaRepository<Part, UUID> {
    boolean existsByPartNo(String partNo);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Part p SET p.quantity = p.quantity - :qty WHERE p.id = :id AND p.quantity >= :qty")
    int decreaseStock(@Param("id") UUID id, @Param("qty") Integer qty);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Part p SET p.quantity = p.quantity + :qty WHERE p.id = :id")
    int increaseStock(@Param("id") UUID id, @Param("qty") Integer qty);

    @Query("""
       SELECT c FROM Part c
       WHERE (:partNo IS NULL OR LOWER(c.partNo) LIKE LOWER(CONCAT('%', CAST(:partNo AS string), '%')))
       """)
    Page<Part> findAll(@Param("partNo") String partNo,
                                    Pageable pageable);
}
