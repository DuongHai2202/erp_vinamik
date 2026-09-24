package vn.vinamik.erp_backend.production.output;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_result;
import vn.vinamik.erp_backend.inventory.api.inventory_stock_lot_snapshot;
import vn.vinamik.erp_backend.inventory.api.inventory_stock_lot_contract;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    @Test
    void creates_good_output_with_pending_receipt_status() {
        production_output_repository output_repository = mock(production_output_repository.class);
        audit_event_writer audit_writer = mock(audit_event_writer.class);
        var order = new production_output_repository.order_snapshot(
                9L, "order_001", 7L, new BigDecimal("10.000000"), "in_progress");
        var request = valid_request();
        var created = output(3L, "pending_receipt", new BigDecimal("8.000000"),
                new BigDecimal("2.000000"), null, null);
        when(output_repository.load_order(9L, true)).thenReturn(order);
        when(output_repository.find_by_idempotency(9L, "key_001")).thenReturn(null);
        when(output_repository.total_output_quantity(9L)).thenReturn(BigDecimal.ZERO.setScale(6));
        when(output_repository.find_by_order_lot(9L, "lot_good")).thenReturn(null);
        when(output_repository.idempotency_key_exists("key_001")).thenReturn(false);
        when(output_repository.insert(anyLong(), anyLong(), anyString(), any(), any(), any(), any(),
                anyLong(), anyLong(), anyString(), anyString(), anyLong(), any())).thenReturn(3L);
        when(output_repository.find_by_id(3L)).thenReturn(created);

        production_output_service service = new production_output_service(
                output_repository, mock(inventory_stock_lot_contract.class),
                mock(inventory_receipt_contract.class), audit_writer);

        production_output_response result = service.create(
                9L, request, new authenticated_user(7L, "production_test", List.of()), "corr-001");

        assertEquals("pending_receipt", result.status());
        verify(output_repository).insert(9L, 7L, "lot_good",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                new BigDecimal("8.000000"), new BigDecimal("2.000000"),
                4L, 5L, "pending_receipt", "key_001", 7L, "production output");
    }

    @Test
    void create_returns_existing_output_for_same_idempotency_key() {
        production_output_repository output_repository = mock(production_output_repository.class);
        var existing = output(3L, "pending_receipt", new BigDecimal("8.000000"),
                new BigDecimal("2.000000"), null, null);
        when(output_repository.load_order(9L, true)).thenReturn(
                new production_output_repository.order_snapshot(
                        9L, "order_001", 7L, new BigDecimal("10.000000"), "in_progress"));
        when(output_repository.find_by_idempotency(9L, "key_001")).thenReturn(existing);

        production_output_service service = new production_output_service(
                output_repository, mock(inventory_stock_lot_contract.class),
                mock(inventory_receipt_contract.class), mock(audit_event_writer.class));

        production_output_response result = service.create(
                9L, valid_request(), new authenticated_user(7L, "production_test", List.of()), "corr-001");

        assertEquals(existing, result);
        verify(output_repository, never()).insert(
                anyLong(), anyLong(), anyString(), any(), any(), any(), any(),
                anyLong(), anyLong(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    void posting_good_output_creates_lot_and_receipt_once() {
        production_output_repository output_repository = mock(production_output_repository.class);
        inventory_stock_lot_contract lot_contract = mock(inventory_stock_lot_contract.class);
        inventory_receipt_contract receipt_contract = mock(inventory_receipt_contract.class);
        audit_event_writer audit_writer = mock(audit_event_writer.class);
        var actor = new authenticated_user(7L, "production_test", List.of());
        var order = new production_output_repository.order_snapshot(
                9L, "order_001", 7L, new BigDecimal("10.000000"), "in_progress");
        var pending = good_output("pending_receipt", null, null);
        var posted = good_output("received", 55L, 88L);
        var lot = new inventory_stock_lot_snapshot(
                55L, 7L, "lot_good", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1), "active");
        when(output_repository.load_order(9L, true)).thenReturn(order);
        when(output_repository.find_by_id_for_update(9L, 3L)).thenReturn(pending, posted);
        when(lot_contract.ensure_active_lot(
                7L, "lot_good", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1), 7L)).thenReturn(lot);
        when(receipt_contract.create_and_post(any(), anyLong(), anyString()))
                .thenReturn(new inventory_receipt_result(88L, "prod_receipt_3", List.of()));
        when(output_repository.mark_received(9L, 3L, 55L, 88L)).thenReturn(1);
        when(output_repository.find_by_id(3L)).thenReturn(posted);

        production_output_service service = new production_output_service(
                output_repository, lot_contract, receipt_contract, audit_writer);

        production_output_response result = service.post(9L, 3L, actor, "corr-001");
        production_output_response retried = service.post(9L, 3L, actor, "corr-002");

        assertEquals("received", result.status());
        assertEquals("received", retried.status());
        verify(lot_contract).ensure_active_lot(
                7L, "lot_good", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1), 7L);
        verify(receipt_contract).create_and_post(any(), eq(7L), eq("corr-001"));
        verify(output_repository).mark_received(9L, 3L, 55L, 88L);
    }

    private production_output_response good_output(String status, Long lot_id, Long receipt_id) {
        return new production_output_response(
                3L, 9L, "order_001", 7L, 4L, 5L, "lot_good",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                new BigDecimal("8.000000"), new BigDecimal("2.000000"),
                lot_id, receipt_id, status, "key_001", Instant.parse("2026-01-01T00:00:00Z"),
                null, "production output");
    }

    private production_output_request valid_request() {
        return new production_output_request(
                4L, 5L, "lot_good", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1), new BigDecimal("8.000000"),
                new BigDecimal("2.000000"), "key_001", "production output");
    }
}