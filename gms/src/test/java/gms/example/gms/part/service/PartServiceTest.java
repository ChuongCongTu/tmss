package gms.example.gms.part.service;

import gms.example.gms.common.exception.BusinessException;
import gms.example.gms.common.exception.ResourceNotFoundException;
import gms.example.gms.part.dto.AdjustPartStockRequest;
import gms.example.gms.part.dto.PartResponse;
import gms.example.gms.part.entity.Part;
import gms.example.gms.part.entity.StockAdjustment;
import gms.example.gms.part.repository.PartRepository;
import gms.example.gms.part.repository.RepairOrderRepository;
import gms.example.gms.part.repository.StockAdjustmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PartService#adjustPartStock}.
 *
 * Pure Mockito — repositories are mocked, so no Spring context and no DB are needed.
 * The focus is the business logic, not the persistence: that stock is decreased
 * atomically (insufficient stock => no ledger row) and that the ledger is written
 * on success.
 */
@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @Mock
    private PartRepository partRepository;

    // Not exercised by adjustPartStock, but required by the constructor.
    @Mock
    private RepairOrderRepository repairOrderRepository;

    @Mock
    private StockAdjustmentRepository stockAdjustmentRepository;

    @InjectMocks
    private PartService partService;

    private UUID partId;
    private Part part;

    @BeforeEach
    void setUp() {
        partId = UUID.randomUUID();
        part = Part.builder()
                .id(partId)
                .partNo("P-001")
                .partName("Brake pad")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .build();
    }

    @Test
    @DisplayName("Decrease succeeds when stock is sufficient -> quantity updated, ledger written")
    void adjustPartStock_decreaseWithSufficientStock_writesLedgerAndReturnsUpdatedQuantity() {
        AdjustPartStockRequest request = AdjustPartStockRequest.builder()
                .delta(-3)
                .reason("Used on repair order")
                .build();

        // The service re-reads the part after the atomic UPDATE, so the second
        // findById reflects the decremented quantity (10 - 3 = 7).
        Part partAfter = Part.builder()
                .id(partId)
                .partNo("P-001")
                .partName("Brake pad")
                .price(new BigDecimal("100.00"))
                .quantity(7)
                .build();
        when(partRepository.findById(partId))
                .thenReturn(Optional.of(part))
                .thenReturn(Optional.of(partAfter));
        when(partRepository.decreaseStock(partId, 3)).thenReturn(1);

        PartResponse response = partService.adjustPartStock(partId, request);

        // Stock was decreased by the absolute delta, atomically.
        verify(partRepository).decreaseStock(partId, 3);
        verify(partRepository, never()).increaseStock(eq(partId), org.mockito.ArgumentMatchers.anyInt());

        // Ledger row written exactly once, capturing the fact at the time it happened.
        ArgumentCaptor<StockAdjustment> captor = ArgumentCaptor.forClass(StockAdjustment.class);
        verify(stockAdjustmentRepository, times(1)).save(captor.capture());
        StockAdjustment saved = captor.getValue();
        assertThat(saved.getDelta()).isEqualTo(-3);
        assertThat(saved.getReason()).isEqualTo("Used on repair order");
        assertThat(saved.getPart()).isSameAs(part);

        // Response carries the re-read (post-adjustment) quantity.
        assertThat(response.getQuantity()).isEqualTo(7);
        assertThat(response.getId()).isEqualTo(partId);
    }

    @Test
    @DisplayName("Increase always succeeds -> quantity updated, ledger written")
    void adjustPartStock_increase_writesLedgerAndReturnsUpdatedQuantity() {
        AdjustPartStockRequest request = AdjustPartStockRequest.builder()
                .delta(5)
                .reason("Restock")
                .build();

        Part partAfter = Part.builder()
                .id(partId)
                .partNo("P-001")
                .partName("Brake pad")
                .price(new BigDecimal("100.00"))
                .quantity(15)
                .build();
        when(partRepository.findById(partId))
                .thenReturn(Optional.of(part))
                .thenReturn(Optional.of(partAfter));

        PartResponse response = partService.adjustPartStock(partId, request);

        verify(partRepository).increaseStock(partId, 5);
        verify(partRepository, never()).decreaseStock(eq(partId), org.mockito.ArgumentMatchers.anyInt());
        verify(stockAdjustmentRepository, times(1)).save(org.mockito.ArgumentMatchers.any(StockAdjustment.class));
        assertThat(response.getQuantity()).isEqualTo(15);
    }

    @Test
    @DisplayName("Decrease beyond available stock (rows affected = 0) -> BusinessException, no ledger row")
    void adjustPartStock_decreaseBeyondStock_throwsBusinessExceptionAndWritesNoLedger() {
        AdjustPartStockRequest request = AdjustPartStockRequest.builder()
                .delta(-100)
                .reason("Oversell attempt")
                .build();

        when(partRepository.findById(partId)).thenReturn(Optional.of(part));
        // The atomic guarded UPDATE matched no rows => not enough stock.
        when(partRepository.decreaseStock(partId, 100)).thenReturn(0);

        assertThatThrownBy(() -> partService.adjustPartStock(partId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tồn kho");

        // The whole point: no ledger row when the stock change did not happen.
        verify(stockAdjustmentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Part does not exist -> ResourceNotFoundException, no stock change, no ledger row")
    void adjustPartStock_partNotFound_throwsResourceNotFoundException() {
        AdjustPartStockRequest request = AdjustPartStockRequest.builder()
                .delta(-1)
                .reason("Whatever")
                .build();

        when(partRepository.findById(partId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> partService.adjustPartStock(partId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(partRepository, never()).decreaseStock(eq(partId), org.mockito.ArgumentMatchers.anyInt());
        verify(partRepository, never()).increaseStock(eq(partId), org.mockito.ArgumentMatchers.anyInt());
        verify(stockAdjustmentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
