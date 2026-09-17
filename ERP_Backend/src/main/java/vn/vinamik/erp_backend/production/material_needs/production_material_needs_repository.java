package vn.vinamik.erp_backend.production.material_needs;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class production_material_needs_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public production_material_needs_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public order_header find_order(long production_order_id) {
        List<order_header> headers = jpa_query_executor.query(
                "SELECT production_order_id, order_code, target_quantity "
                        + "FROM production.production_order WHERE production_order_id = ?",
                (result_set, row_number) -> new order_header(
                        result_set.getLong("production_order_id"),
                        result_set.getString("order_code"),
                        result_set.getBigDecimal("target_quantity")),
                production_order_id);
        if (headers.isEmpty()) {
            throw new resource_not_found_exception("Production order");
        }
        return headers.getFirst();
    }

    public List<requirement> find_requirements(long production_order_id) {
        return jpa_query_executor.query(
                "SELECT requirement.material_stock_item_id, bom_line.material_item_code_snapshot, "
                        + "bom_line.material_item_name_snapshot, requirement.unit_code_snapshot, requirement.required_quantity "
                        + "FROM production.production_order_material_requirement AS requirement "
                        + "JOIN production.bom_line AS bom_line ON bom_line.bom_line_id = requirement.bom_line_id "
                        + "WHERE requirement.production_order_id = ? ORDER BY bom_line.line_number, requirement.material_stock_item_id",
                this::map_requirement, production_order_id);
    }

    private requirement map_requirement(jpa_result_row result_set, int row_number) {
        return new requirement(
                result_set.getLong("material_stock_item_id"),
                result_set.getString("material_item_code_snapshot"),
                result_set.getString("material_item_name_snapshot"),
                result_set.getString("unit_code_snapshot"),
                result_set.getBigDecimal("required_quantity"));
    }

    record order_header(long production_order_id, String order_code, BigDecimal target_quantity) {
    }

    record requirement(long material_stock_item_id, String material_item_code,
                       String material_item_name, String unit_code, BigDecimal required_quantity) {
    }
}
