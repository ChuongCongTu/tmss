package gms.example.gms.customer.repository;

import gms.example.gms.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findById(UUID uuid);

    @Query("""
       SELECT c FROM Customer c
       WHERE (:fullName IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', CAST(:fullName AS string), '%')))
         AND (:phone IS NULL OR c.phone LIKE CONCAT('%', CAST(:phone AS string), '%'))
       """)
    Page<Customer> findAllCustomers(@Param("fullName") String fullName,
                                    @Param("phone") String phone,
                                    Pageable pageable);
}
