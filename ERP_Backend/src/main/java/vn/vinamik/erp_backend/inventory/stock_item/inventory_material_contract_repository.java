package vn.vinamik.erp_backend.inventory.stock_item;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.inventory.api.inventory_material_snapshot;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;

import java.util.List;

@Repository
public class inventory_material_contract_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public inventory_material_contract_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public List<inventory_material_snapshot> find_active_stock_item(long stock_item_id) {
        return jpa_query_executor.query(
                """
                SELECT stock_item.stock_item_id, stock_item.item_code, stock_item.item_name,
                       stock_item.item_type, unit_of_measure.unit_code, stock_item.status
                FROM inventory.stock_item AS stock_item
                JOIN inventory.unit_of_measure AS unit_of_measure
                  ON unit_of_measure.unit_of_measure_id = stock_item.base_unit_of_measure_id
                WHERE stock_item.stock_item_id = ? AND stock_item.status = 'active'
                """,
                (result_set, row_number) -> new inventory_material_snapshot(
                        result_set.getLong("stock_item_id"), result_set.getString("item_code"),
                        result_set.getString("item_name"), result_set.getString("item_type"),
                        result_set.getString("unit_code"), result_set.getString("status")), stock_item_id);
    }
}
