package vn.vinamik.erp_backend.inventory.stock_item;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.inventory.master_data.inventory_location_lookup_response;
import vn.vinamik.erp_backend.inventory.master_data.inventory_supplier_lookup_response;
import vn.vinamik.erp_backend.inventory.master_data.inventory_warehouse_lookup_response;

import java.util.List;

@Repository
public class inventory_master_lookup_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public inventory_master_lookup_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public List<stock_unit_response> find_active_units() {
        return jpa_query_executor.query(
                """
                SELECT unit_of_measure_id, unit_code, unit_name, decimal_places
                FROM inventory.unit_of_measure
                WHERE status = 'active'
                ORDER BY unit_code
                """,
                (result_set, row_number) -> new stock_unit_response(
                        result_set.getLong("unit_of_measure_id"), result_set.getString("unit_code"),
                        result_set.getString("unit_name"), result_set.getShort("decimal_places")));
    }

    public List<stock_category_response> find_active_categories() {
        return jpa_query_executor.query(
                """
                SELECT item_category_id, category_code, category_name
                FROM inventory.item_category
                WHERE status = 'active'
                ORDER BY category_code
                """,
                (result_set, row_number) -> new stock_category_response(
                        result_set.getLong("item_category_id"), result_set.getString("category_code"),
                        result_set.getString("category_name")));
    }
    public List<inventory_supplier_lookup_response> find_active_suppliers() {
        return jpa_query_executor.query(
                "SELECT supplier_id, supplier_code, supplier_name FROM inventory.supplier "
                        + "WHERE status = 'active' ORDER BY supplier_code",
                (result_set, row_number) -> new inventory_supplier_lookup_response(
                        result_set.getLong("supplier_id"), result_set.getString("supplier_code"),
                        result_set.getString("supplier_name")));
    }

    public List<inventory_warehouse_lookup_response> find_active_warehouses() {
        return jpa_query_executor.query(
                "SELECT warehouse_id, warehouse_code, warehouse_name FROM inventory.warehouse "
                        + "WHERE status = 'active' ORDER BY warehouse_code",
                (result_set, row_number) -> new inventory_warehouse_lookup_response(
                        result_set.getLong("warehouse_id"), result_set.getString("warehouse_code"),
                        result_set.getString("warehouse_name")));
    }

    public List<inventory_location_lookup_response> find_active_locations(long warehouse_id) {
        return jpa_query_executor.query(
                "SELECT warehouse_location_id, warehouse_id, location_code, location_name "
                        + "FROM inventory.warehouse_location WHERE warehouse_id = ? AND status = 'active' "
                        + "ORDER BY location_code",
                (result_set, row_number) -> new inventory_location_lookup_response(
                        result_set.getLong("warehouse_location_id"), result_set.getLong("warehouse_id"),
                        result_set.getString("location_code"), result_set.getString("location_name")), warehouse_id);
    }
}
