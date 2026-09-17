package vn.vinamik.erp_backend.production.output;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_stock_lot_contract;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class production_output_service_tests {
    @Test
    void defective_only_output_does_not_create_lot_or_receipt() {
        production_output_repository output_repository = mock(production_output_repository.class);
        inventory_stock_lot_contract lot_contract = mock(inventory_stock_lot_contract.class);
        inventory_receipt_contract receipt_contract = mock(inventory_receipt_contract.class);
        audit_event_writer audit_writer = mock(audit_event_writer.class);
        production_output_service service = new production_output_service(
                output_repository, lot_contract, receipt_contract, audit_writer);

        production_output_repository.order_snapshot order =
                new production_output_repository.order_snapshot(
                        9L, "order_001", 7L, new BigDecimal("10.000000"), "in_progress");
        production_output_response draft = output(
                3L, "draft", BigDecimal.ZERO.setScale(6), new BigDecimal("1.000000"), null, null);
        production_output_response received = output(
                3L, "received", BigDecimal.ZERO.setScale(6), new BigDecimal("1.000000"), null, null);
        when(output_repository.load_order(9L, true)).thenReturn(order);
        when(output_repository.find_by_id_for_update(9L, 3L)).thenReturn(draft);
        when(output_repository.mark_received(9L, 3L, null, null)).thenReturn(1);
        when(output_repository.find_by_id(3L)).thenReturn(received);

        production_output_response result = service.post(
                9L, 3L, new authenticated_user(1L, "tester", List.of()), "test-correlation");

        assertEquals("received", result.status());
        verify(lot_contract, never()).ensure_active_lot(
                7L, "lot_bad", LocalDate.of(2026, 1, 1), null, 1L);
        verify(receipt_contract, never()).create_and_post(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString());
        verify(output_repository).mark_received(9L, 3L, null, null);
    }

    private production_output_response output(long output_id, String status,
                                              BigDecimal good_quantity, BigDecimal defective_quantity,
                                              Long lot_id, Long receipt_id) {
        return new production_output_response(
                output_id, 9L, "order_001", 7L, 4L, 5L, "lot_bad",
                LocalDate.of(2026, 1, 1), null, good_quantity, defective_quantity,
                lot_id, receipt_id, status, "key_001", Instant.parse("2026-01-01T00:00:00Z"),
                null, "defective output");
    }
}