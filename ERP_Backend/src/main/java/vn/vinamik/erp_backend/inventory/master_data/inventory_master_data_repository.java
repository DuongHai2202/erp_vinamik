package vn.vinamik.erp_backend.inventory.master_data;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.pagination_guard;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.util.List;

@Repository
public class inventory_master_data_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public inventory_master_data_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count_units(String search, String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.unit_of_measure "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(unit_code) LIKE '%' || ? || '%' "
                        + "OR lower(unit_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?)",
                Long.class, search, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<inventory_unit_response> search_units(String search, String status, int page, int page_size) {
        return jpa_query_executor.query(
                "SELECT unit_of_measure_id, unit_code, unit_name, decimal_places, status "
                        + "FROM inventory.unit_of_measure "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(unit_code) LIKE '%' || ? || '%' "
                        + "OR lower(unit_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?) "
                        + "ORDER BY unit_code LIMIT ? OFFSET ?",
                this::map_unit, search, search, search, status, status,
                page_size, pagination_guard.offset(page, page_size));
    }

    public inventory_unit_response find_unit(long unit_of_measure_id) {
        return jpa_query_executor.queryForObject(
                "SELECT unit_of_measure_id, unit_code, unit_name, decimal_places, status "
                        + "FROM inventory.unit_of_measure WHERE unit_of_measure_id = ?",
                this::map_unit, unit_of_measure_id);
    }

    public boolean unit_code_exists(String unit_code, Long unit_of_measure_id) {
        Long total = unit_of_measure_id == null
                ? jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.unit_of_measure WHERE lower(unit_code) = ?",
                Long.class, unit_code)
                : jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.unit_of_measure WHERE lower(unit_code) = ? "
                        + "AND unit_of_measure_id <> ?",
                Long.class, unit_code, unit_of_measure_id);
        return total != null && total > 0;
    }

    public Long insert_unit(String code, String name, short decimal_places, String status, long actor_user_id) {
        return jpa_query_executor.queryForObject(
                "INSERT INTO inventory.unit_of_measure "
                        + "(unit_code, unit_name, decimal_places, status, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING unit_of_measure_id",
                Long.class, code, name, decimal_places, status, actor_user_id, actor_user_id);
    }

    public int update_unit(long unit_of_measure_id, String code, String name, short decimal_places,
                           String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.unit_of_measure SET unit_code = ?, unit_name = ?, decimal_places = ?, status = ?, "
                        + "updated_at = now(), updated_by_user_id = ? WHERE unit_of_measure_id = ?",
                code, name, decimal_places, status, actor_user_id, unit_of_measure_id);
    }

    public int deactivate_unit(long unit_of_measure_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.unit_of_measure SET status = 'inactive', updated_at = now(), "
                        + "updated_by_user_id = ? WHERE unit_of_measure_id = ?",
                actor_user_id, unit_of_measure_id);
    }

    public long count_categories(String search, String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.item_category "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(category_code) LIKE '%' || ? || '%' "
                        + "OR lower(category_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?)",
                Long.class, search, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<inventory_category_response> search_categories(String search, String status, int page, int page_size) {
        return jpa_query_executor.query(
                "SELECT item_category_id, category_code, category_name, status "
                        + "FROM inventory.item_category "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(category_code) LIKE '%' || ? || '%' "
                        + "OR lower(category_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?) "
                        + "ORDER BY category_code LIMIT ? OFFSET ?",
                this::map_category, search, search, search, status, status,
                page_size, pagination_guard.offset(page, page_size));
    }

    public inventory_category_response find_category(long item_category_id) {
        return jpa_query_executor.queryForObject(
                "SELECT item_category_id, category_code, category_name, status "
                        + "FROM inventory.item_category WHERE item_category_id = ?",
                this::map_category, item_category_id);
    }

    public boolean category_code_exists(String category_code, Long item_category_id) {
        Long total = item_category_id == null
                ? jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.item_category WHERE lower(category_code) = ?",
                Long.class, category_code)
                : jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.item_category WHERE lower(category_code) = ? "
                        + "AND item_category_id <> ?",
                Long.class, category_code, item_category_id);
        return total != null && total > 0;
    }

    public Long insert_category(String code, String name, String status, long actor_user_id) {
        return jpa_query_executor.queryForObject(
                "INSERT INTO inventory.item_category "
                        + "(category_code, category_name, status, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING item_category_id",
                Long.class, code, name, status, actor_user_id, actor_user_id);
    }

    public int update_category(long item_category_id, String code, String name, String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.item_category SET category_code = ?, category_name = ?, status = ?, "
                        + "updated_at = now(), updated_by_user_id = ? WHERE item_category_id = ?",
                code, name, status, actor_user_id, item_category_id);
    }

    public int deactivate_category(long item_category_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.item_category SET status = 'inactive', updated_at = now(), "
                        + "updated_by_user_id = ? WHERE item_category_id = ?",
                actor_user_id, item_category_id);
    }

    public long count_suppliers(String search, String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.supplier "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(supplier_code) LIKE '%' || ? || '%' "
                        + "OR lower(supplier_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?)",
                Long.class, search, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<inventory_supplier_response> search_suppliers(String search, String status, int page, int page_size) {
        return jpa_query_executor.query(
                "SELECT supplier_id, supplier_code, supplier_name, phone_number, email, address, status "
                        + "FROM inventory.supplier "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(supplier_code) LIKE '%' || ? || '%' "
                        + "OR lower(supplier_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?) "
                        + "ORDER BY supplier_code LIMIT ? OFFSET ?",
                this::map_supplier, search, search, search, status, status,
                page_size, pagination_guard.offset(page, page_size));
    }

    public inventory_supplier_response find_supplier(long supplier_id) {
        return jpa_query_executor.queryForObject(
                "SELECT supplier_id, supplier_code, supplier_name, phone_number, email, address, status "
                        + "FROM inventory.supplier WHERE supplier_id = ?",
                this::map_supplier, supplier_id);
    }

    public boolean supplier_code_exists(String supplier_code, Long supplier_id) {
        Long total = supplier_id == null
                ? jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.supplier WHERE lower(supplier_code) = ?",
                Long.class, supplier_code)
                : jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.supplier WHERE lower(supplier_code) = ? AND supplier_id <> ?",
                Long.class, supplier_code, supplier_id);
        return total != null && total > 0;
    }

    public Long insert_supplier(String code, String name, String phone, String email, String address,
                                String status, long actor_user_id) {
        return jpa_query_executor.queryForObject(
                "INSERT INTO inventory.supplier "
                        + "(supplier_code, supplier_name, phone_number, email, address, status, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING supplier_id",
                Long.class, code, name, phone, email, address, status, actor_user_id, actor_user_id);
    }

    public int update_supplier(long supplier_id, String code, String name, String phone, String email,
                               String address, String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.supplier SET supplier_code = ?, supplier_name = ?, phone_number = ?, email = ?, "
                        + "address = ?, status = ?, updated_at = now(), updated_by_user_id = ? WHERE supplier_id = ?",
                code, name, phone, email, address, status, actor_user_id, supplier_id);
    }

    public int deactivate_supplier(long supplier_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.supplier SET status = 'inactive', updated_at = now(), "
                        + "updated_by_user_id = ? WHERE supplier_id = ?",
                actor_user_id, supplier_id);
    }

    public long count_warehouses(String search, String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.warehouse "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(warehouse_code) LIKE '%' || ? || '%' "
                        + "OR lower(warehouse_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?)",
                Long.class, search, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<inventory_warehouse_response> search_warehouses(String search, String status, int page, int page_size) {
        return jpa_query_executor.query(
                "SELECT warehouse_id, warehouse_code, warehouse_name, address, status "
                        + "FROM inventory.warehouse "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(warehouse_code) LIKE '%' || ? || '%' "
                        + "OR lower(warehouse_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?) "
                        + "ORDER BY warehouse_code LIMIT ? OFFSET ?",
                this::map_warehouse, search, search, search, status, status,
                page_size, pagination_guard.offset(page, page_size));
    }

    public inventory_warehouse_response find_warehouse(long warehouse_id) {
        return jpa_query_executor.queryForObject(
                "SELECT warehouse_id, warehouse_code, warehouse_name, address, status "
                        + "FROM inventory.warehouse WHERE warehouse_id = ?",
                this::map_warehouse, warehouse_id);
    }

    public boolean warehouse_code_exists(String warehouse_code, Long warehouse_id) {
        Long total = warehouse_id == null
                ? jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.warehouse WHERE lower(warehouse_code) = ?",
                Long.class, warehouse_code)
                : jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.warehouse WHERE lower(warehouse_code) = ? AND warehouse_id <> ?",
                Long.class, warehouse_code, warehouse_id);
        return total != null && total > 0;
    }

    public boolean active_warehouse(long warehouse_id) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.warehouse WHERE warehouse_id = ? AND status = 'active'",
                Long.class, warehouse_id);
        return total != null && total > 0;
    }

    public Long insert_warehouse(String code, String name, String address, String status, long actor_user_id) {
        return jpa_query_executor.queryForObject(
                "INSERT INTO inventory.warehouse "
                        + "(warehouse_code, warehouse_name, address, status, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING warehouse_id",
                Long.class, code, name, address, status, actor_user_id, actor_user_id);
    }

    public int update_warehouse(long warehouse_id, String code, String name, String address,
                                String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.warehouse SET warehouse_code = ?, warehouse_name = ?, address = ?, status = ?, "
                        + "updated_at = now(), updated_by_user_id = ? WHERE warehouse_id = ?",
                code, name, address, status, actor_user_id, warehouse_id);
    }

    public int deactivate_warehouse(long warehouse_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.warehouse SET status = 'inactive', updated_at = now(), "
                        + "updated_by_user_id = ? WHERE warehouse_id = ?",
                actor_user_id, warehouse_id);
    }

    public long count_locations(long warehouse_id, String search, String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.warehouse_location "
                        + "WHERE warehouse_id = ? "
                        + "AND (CAST(? AS text) IS NULL OR lower(location_code) LIKE '%' || ? || '%' "
                        + "OR lower(location_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?)",
                Long.class, warehouse_id, search, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<inventory_location_response> search_locations(long warehouse_id, String search, String status,
                                                              int page, int page_size) {
        return jpa_query_executor.query(
                "SELECT warehouse_location_id, warehouse_id, location_code, location_name, status "
                        + "FROM inventory.warehouse_location WHERE warehouse_id = ? "
                        + "AND (CAST(? AS text) IS NULL OR lower(location_code) LIKE '%' || ? || '%' "
                        + "OR lower(location_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?) "
                        + "ORDER BY location_code LIMIT ? OFFSET ?",
                this::map_location, warehouse_id, search, search, search, status, status,
                page_size, pagination_guard.offset(page, page_size));
    }

    public inventory_location_response find_location(long warehouse_location_id) {
        return jpa_query_executor.queryForObject(
                "SELECT warehouse_location_id, warehouse_id, location_code, location_name, status "
                        + "FROM inventory.warehouse_location WHERE warehouse_location_id = ?",
                this::map_location, warehouse_location_id);
    }

    public boolean location_code_exists(long warehouse_id, String location_code, Long location_id) {
        Long total = location_id == null
                ? jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.warehouse_location "
                        + "WHERE warehouse_id = ? AND lower(location_code) = ?",
                Long.class, warehouse_id, location_code)
                : jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.warehouse_location "
                        + "WHERE warehouse_id = ? AND lower(location_code) = ? AND warehouse_location_id <> ?",
                Long.class, warehouse_id, location_code, location_id);
        return total != null && total > 0;
    }

    public Long insert_location(long warehouse_id, String code, String name, String status, long actor_user_id) {
        return jpa_query_executor.queryForObject(
                "INSERT INTO inventory.warehouse_location "
                        + "(warehouse_id, location_code, location_name, status, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING warehouse_location_id",
                Long.class, warehouse_id, code, name, status, actor_user_id, actor_user_id);
    }

    public int update_location(long warehouse_location_id, String code, String name, String status,
                               long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.warehouse_location SET location_code = ?, location_name = ?, status = ?, "
                        + "updated_at = now(), updated_by_user_id = ? WHERE warehouse_location_id = ?",
                code, name, status, actor_user_id, warehouse_location_id);
    }

    public int deactivate_location(long warehouse_location_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.warehouse_location SET status = 'inactive', updated_at = now(), "
                        + "updated_by_user_id = ? WHERE warehouse_location_id = ?",
                actor_user_id, warehouse_location_id);
    }

    private inventory_unit_response map_unit(jpa_result_row row, int row_number) {
        return new inventory_unit_response(row.getLong("unit_of_measure_id"), row.getString("unit_code"),
                row.getString("unit_name"), row.getShort("decimal_places"), row.getString("status"));
    }

    private inventory_category_response map_category(jpa_result_row row, int row_number) {
        return new inventory_category_response(row.getLong("item_category_id"), row.getString("category_code"),
                row.getString("category_name"), row.getString("status"));
    }

    private inventory_supplier_response map_supplier(jpa_result_row row, int row_number) {
        return new inventory_supplier_response(row.getLong("supplier_id"), row.getString("supplier_code"),
                row.getString("supplier_name"), row.getString("phone_number"), row.getString("email"),
                row.getString("address"), row.getString("status"));
    }

    private inventory_warehouse_response map_warehouse(jpa_result_row row, int row_number) {
        return new inventory_warehouse_response(row.getLong("warehouse_id"), row.getString("warehouse_code"),
                row.getString("warehouse_name"), row.getString("address"), row.getString("status"));
    }

    private inventory_location_response map_location(jpa_result_row row, int row_number) {
        return new inventory_location_response(row.getLong("warehouse_location_id"),
                row.getLong("warehouse_id"), row.getString("location_code"),
                row.getString("location_name"), row.getString("status"));
    }
}
