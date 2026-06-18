package gms.example.gms.part.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "invoice_counters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceCounter {
    @Id
    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "last_no", nullable = false)
    private Long lastNo = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
