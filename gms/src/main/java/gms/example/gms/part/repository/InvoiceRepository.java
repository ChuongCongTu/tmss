package gms.example.gms.part.repository;

import gms.example.gms.part.dto.InvoiceNoResut;
import gms.example.gms.part.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    boolean existsInvoiceByRepairOrderId(UUID repairOrderId);

    @Query(value = """
    INSERT INTO invoice_counters (year, last_no)
    values (EXTRACT(YEAR FROM now())::int, 1)
    on conflict (year)
    DO UPDATE SET last_no = invoice_counters.last_no + 1,
                                   updated_at = now()
    RETURNING year, last_no
       """, nativeQuery = true)
    InvoiceNoResut nextNo();
}
