package vn.vinamik.erp_backend.quality_cost;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class quality_cost_repository {
    private final jpa_native_query_executor query_executor;

    public quality_cost_repository(jpa_native_query_executor query_executor) {
        this.query_executor = query_executor;
    }

    public quality_cost_page_response list(String resource, String search, String status, int page, int page_size) {
        String table = switch (resource) {
            case "inspections" -> "quality_cost.quality_inspection";
            case "nonconformances" -> "quality_cost.nonconformance";
            case "periods" -> "quality_cost.cost_period";
            case "calculations" -> "quality_cost.cost_calculation";
            case "price_proposals", "price_approvals" -> "quality_cost.price_proposal";
            case "rules" -> "quality_cost.cost_rule_version";
            default -> throw new IllegalArgumentException("Unknown quality/cost resource.");
        };
        String where = " WHERE (CAST(? AS text) IS NULL OR lower(CAST(row_data AS text)) LIKE '%' || lower(?) || '%')";
        // The table-specific query keeps the result projection small and index-friendly.
        String query;
        String count_query;
        List<Object> params = new ArrayList<>();
        List<Object> count_params = new ArrayList<>();
        if (resource.equals("inspections")) {
            query = "SELECT inspection_id AS id, inspection_code AS code, stock_item_id, lot_code, inspected_quantity, good_quantity, defective_quantity, status, inspected_on, notes FROM " + table + " WHERE (CAST(? AS text) IS NULL OR lower(inspection_code) LIKE '%' || lower(?) || '%' OR lower(COALESCE(lot_code, '')) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR status = ?) ORDER BY inspected_on DESC, inspection_id DESC LIMIT ? OFFSET ?";
            count_query = "SELECT count(*) FROM " + table + " WHERE (CAST(? AS text) IS NULL OR lower(inspection_code) LIKE '%' || lower(?) || '%' OR lower(COALESCE(lot_code, '')) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR status = ? )";
            params.addAll(java.util.Arrays.asList(search, search, search, status, status, page_size, page * page_size));
            count_params.addAll(java.util.Arrays.asList(search, search, search, status, status));
        } else if (resource.equals("nonconformances")) {
            query = "SELECT nonconformance_id AS id, nonconformance_code AS code, inspection_id, defect_code, quantity, disposition, status, resolved_on, notes FROM " + table + " WHERE (CAST(? AS text) IS NULL OR lower(nonconformance_code) LIKE '%' || lower(?) || '%' OR lower(defect_code) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR status = ?) ORDER BY created_at DESC, nonconformance_id DESC LIMIT ? OFFSET ?";
            count_query = "SELECT count(*) FROM " + table + " WHERE (CAST(? AS text) IS NULL OR lower(nonconformance_code) LIKE '%' || lower(?) || '%' OR lower(defect_code) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR status = ?)";
            params.addAll(java.util.Arrays.asList(search, search, search, status, status, page_size, page * page_size));
            count_params.addAll(java.util.Arrays.asList(search, search, search, status, status));
        } else if (resource.equals("periods")) {
            query = "SELECT period.cost_period_id AS id, period.period_code AS code, period.starts_on, period.ends_on, period.rule_version_id, rule.rule_code, period.status, period.notes FROM " + table + " period JOIN quality_cost.cost_rule_version rule ON rule.rule_version_id = period.rule_version_id WHERE (CAST(? AS text) IS NULL OR lower(period.period_code) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR period.status = ?) ORDER BY period.starts_on DESC, period.cost_period_id DESC LIMIT ? OFFSET ?";
            count_query = "SELECT count(*) FROM " + table + " period WHERE (CAST(? AS text) IS NULL OR lower(period.period_code) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR period.status = ?)";
            params.addAll(java.util.Arrays.asList(search, search, status, status, page_size, page * page_size));
            count_params.addAll(java.util.Arrays.asList(search, search, status, status));
        } else if (resource.equals("calculations")) {
            query = "SELECT calculation.cost_calculation_id AS id, calculation.cost_period_id, calculation.production_order_id, calculation.stock_item_id, calculation.stock_item_code, calculation.stock_item_name, calculation.good_quantity, calculation.material_cost, calculation.direct_labor_cost, calculation.overhead_cost, calculation.adjustment_amount, calculation.total_cost, calculation.unit_cost, calculation.status, calculation.calculated_at, calculation.notes FROM " + table + " calculation WHERE (CAST(? AS text) IS NULL OR lower(COALESCE(calculation.stock_item_code, '')) LIKE '%' || lower(?) || '%' OR lower(COALESCE(calculation.stock_item_name, '')) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR calculation.status = ?) ORDER BY calculation.calculated_at DESC, calculation.cost_calculation_id DESC LIMIT ? OFFSET ?";
            count_query = "SELECT count(*) FROM " + table + " calculation WHERE (CAST(? AS text) IS NULL OR lower(COALESCE(calculation.stock_item_code, '')) LIKE '%' || lower(?) || '%' OR lower(COALESCE(calculation.stock_item_name, '')) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR calculation.status = ?)";
            params.addAll(java.util.Arrays.asList(search, search, search, status, status, page_size, page * page_size));
            count_params.addAll(java.util.Arrays.asList(search, search, search, status, status));
        } else if (resource.equals("price_proposals") || resource.equals("price_approvals")) {
            query = "SELECT proposal.price_proposal_id AS id, proposal.proposal_code AS code, proposal.cost_period_id, proposal.stock_item_id, proposal.stock_item_code, proposal.stock_item_name, proposal.unit_cost, proposal.margin_percent, proposal.proposed_price, proposal.currency_code, proposal.status, proposal.effective_on, proposal.notes FROM " + table + " proposal WHERE (CAST(? AS text) IS NULL OR lower(proposal.proposal_code) LIKE '%' || lower(?) || '%' OR lower(COALESCE(proposal.stock_item_code, '')) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR proposal.status = ?) ORDER BY proposal.effective_on DESC, proposal.price_proposal_id DESC LIMIT ? OFFSET ?";
            count_query = "SELECT count(*) FROM " + table + " proposal WHERE (CAST(? AS text) IS NULL OR lower(proposal.proposal_code) LIKE '%' || lower(?) || '%' OR lower(COALESCE(proposal.stock_item_code, '')) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR proposal.status = ?)";
            params.addAll(java.util.Arrays.asList(search, search, search, status, status, page_size, page * page_size));
            count_params.addAll(java.util.Arrays.asList(search, search, search, status, status));
        } else {
            query = "SELECT rule_version_id AS id, rule_code AS code, material_valuation_method, labor_basis, overhead_basis, defective_policy, currency_code, rounding_scale, effective_from, effective_to, status, notes FROM " + table + " WHERE (CAST(? AS text) IS NULL OR lower(rule_code) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR status = ?) ORDER BY effective_from DESC, rule_version_id DESC LIMIT ? OFFSET ?";
            count_query = "SELECT count(*) FROM " + table + " WHERE (CAST(? AS text) IS NULL OR lower(rule_code) LIKE '%' || lower(?) || '%') AND (CAST(? AS text) IS NULL OR status = ?)";
            params.addAll(java.util.Arrays.asList(search, search, status, status, page_size, page * page_size));
            count_params.addAll(java.util.Arrays.asList(search, search, status, status));
        }
        long total = query_executor.queryForObject(count_query, Long.class, count_params.toArray());
        List<Map<String, Object>> items = query_executor.query(query, this::map_row, params.toArray());
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / page_size);
        return new quality_cost_page_response(items, page, page_size, total, total_pages);
    }

    public Map<String, Object> find(String resource, long id) {
        String sql = switch (resource) {
            case "inspections" -> "SELECT inspection_id AS id, inspection_code AS code, production_order_id, production_output_id, stock_item_id, lot_code, inspected_quantity, good_quantity, defective_quantity, status, inspected_on, notes FROM quality_cost.quality_inspection WHERE inspection_id = ?";
            case "nonconformances" -> "SELECT nonconformance_id AS id, nonconformance_code AS code, inspection_id, defect_code, quantity, disposition, status, resolved_on, notes FROM quality_cost.nonconformance WHERE nonconformance_id = ?";
            case "periods" -> "SELECT period.cost_period_id AS id, period.period_code AS code, period.starts_on, period.ends_on, period.rule_version_id, rule.rule_code, period.status, period.notes FROM quality_cost.cost_period period JOIN quality_cost.cost_rule_version rule ON rule.rule_version_id = period.rule_version_id WHERE period.cost_period_id = ?";
            case "calculations" -> "SELECT calculation.cost_calculation_id AS id, calculation.cost_period_id, calculation.production_order_id, calculation.stock_item_id, calculation.stock_item_code, calculation.stock_item_name, calculation.good_quantity, calculation.material_cost, calculation.direct_labor_cost, calculation.overhead_cost, calculation.adjustment_amount, calculation.total_cost, calculation.unit_cost, calculation.status, calculation.calculated_at, calculation.notes FROM quality_cost.cost_calculation calculation WHERE calculation.cost_calculation_id = ?";
            case "price_proposals", "price_approvals" -> "SELECT proposal.price_proposal_id AS id, proposal.proposal_code AS code, proposal.cost_period_id, proposal.stock_item_id, proposal.stock_item_code, proposal.stock_item_name, proposal.unit_cost, proposal.margin_percent, proposal.proposed_price, proposal.currency_code, proposal.status, proposal.effective_on, proposal.notes FROM quality_cost.price_proposal proposal WHERE proposal.price_proposal_id = ?";
            case "rules" -> "SELECT rule_version_id AS id, rule_code AS code, material_valuation_method, labor_basis, overhead_basis, defective_policy, currency_code, rounding_scale, effective_from, effective_to, status, notes FROM quality_cost.cost_rule_version WHERE rule_version_id = ?";
            default -> throw new IllegalArgumentException("Unknown quality/cost resource.");
        };
        Map<String, Object> result = query_executor.queryForObject(sql, this::map_row, id);
        if (result == null) throw new resource_not_found_exception("Quality/cost record");
        return result;
    }

    public long insert_rule(cost_rule_request request, long actor) {
        return required_id(query_executor.queryForObject("INSERT INTO quality_cost.cost_rule_version (rule_code, material_valuation_method, labor_basis, overhead_basis, defective_policy, currency_code, rounding_scale, effective_from, effective_to, notes, created_by_user_id, updated_by_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING rule_version_id", Long.class, request.rule_code().trim(), request.material_valuation_method().trim(), request.labor_basis().trim(), request.overhead_basis().trim(), request.defective_policy().trim(), normalize_currency(request.currency_code()), request.rounding_scale() == null ? 2 : request.rounding_scale(), request.effective_from(), request.effective_to(), normalize(request.notes()), actor, actor));
    }

    public int update_rule(long id, cost_rule_request request, long actor) {
        return query_executor.update("UPDATE quality_cost.cost_rule_version SET rule_code = ?, material_valuation_method = ?, labor_basis = ?, overhead_basis = ?, defective_policy = ?, currency_code = ?, rounding_scale = ?, effective_from = ?, effective_to = ?, notes = ?, updated_at = now(), updated_by_user_id = ? WHERE rule_version_id = ? AND status = 'draft'", request.rule_code().trim(), request.material_valuation_method().trim(), request.labor_basis().trim(), request.overhead_basis().trim(), request.defective_policy().trim(), normalize_currency(request.currency_code()), request.rounding_scale() == null ? 2 : request.rounding_scale(), request.effective_from(), request.effective_to(), normalize(request.notes()), actor, id);
    }

    public long insert_period(cost_period_request request, long actor) {
        return required_id(query_executor.queryForObject("INSERT INTO quality_cost.cost_period (period_code, starts_on, ends_on, rule_version_id, notes, created_by_user_id, updated_by_user_id) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING cost_period_id", Long.class, request.period_code().trim(), request.starts_on(), request.ends_on(), request.rule_version_id(), normalize(request.notes()), actor, actor));
    }

    public int update_period(long id, cost_period_request request, long actor) {
        return query_executor.update("UPDATE quality_cost.cost_period SET period_code = ?, starts_on = ?, ends_on = ?, rule_version_id = ?, notes = ?, updated_at = now(), updated_by_user_id = ? WHERE cost_period_id = ? AND status = 'draft'", request.period_code().trim(), request.starts_on(), request.ends_on(), request.rule_version_id(), normalize(request.notes()), actor, id);
    }

    public int change_period_status(long id, String status, long actor) {
        return query_executor.update("UPDATE quality_cost.cost_period SET status = ?, updated_at = now(), updated_by_user_id = ? WHERE cost_period_id = ? AND status <> 'locked'", status, actor, id);
    }

    public long insert_inspection(quality_inspection_request request, long actor) {
        return required_id(query_executor.queryForObject("INSERT INTO quality_cost.quality_inspection (inspection_code, production_order_id, production_output_id, stock_item_id, lot_code, inspected_quantity, good_quantity, defective_quantity, inspected_on, notes, created_by_user_id, updated_by_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING inspection_id", Long.class, request.inspection_code().trim(), request.production_order_id(), request.production_output_id(), request.stock_item_id(), normalize(request.lot_code()), request.inspected_quantity(), request.good_quantity(), request.defective_quantity(), request.inspected_on(), normalize(request.notes()), actor, actor));
    }

    public int update_inspection(long id, quality_inspection_request request, long actor) {
        return query_executor.update("UPDATE quality_cost.quality_inspection SET inspection_code = ?, production_order_id = ?, production_output_id = ?, stock_item_id = ?, lot_code = ?, inspected_quantity = ?, good_quantity = ?, defective_quantity = ?, inspected_on = ?, notes = ?, updated_at = now(), updated_by_user_id = ? WHERE inspection_id = ? AND status = 'draft'", request.inspection_code().trim(), request.production_order_id(), request.production_output_id(), request.stock_item_id(), normalize(request.lot_code()), request.inspected_quantity(), request.good_quantity(), request.defective_quantity(), request.inspected_on(), normalize(request.notes()), actor, id);
    }

    public int change_inspection_status(long id, String status, long actor) {
        return query_executor.update("UPDATE quality_cost.quality_inspection SET status = ?, updated_at = now(), updated_by_user_id = ? WHERE inspection_id = ? AND status IN ('draft', 'submitted', 'held')", status, actor, id);
    }

    public long insert_nonconformance(nonconformance_request request, long actor) {
        return required_id(query_executor.queryForObject("INSERT INTO quality_cost.nonconformance (nonconformance_code, inspection_id, defect_code, quantity, disposition, notes, created_by_user_id, updated_by_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING nonconformance_id", Long.class, request.nonconformance_code().trim(), request.inspection_id(), request.defect_code().trim(), request.quantity(), request.disposition().trim(), normalize(request.notes()), actor, actor));
    }

    public int update_nonconformance(long id, nonconformance_request request, long actor) {
        return query_executor.update("UPDATE quality_cost.nonconformance SET nonconformance_code = ?, inspection_id = ?, defect_code = ?, quantity = ?, disposition = ?, notes = ?, updated_at = now(), updated_by_user_id = ? WHERE nonconformance_id = ? AND status IN ('open', 'in_progress')", request.nonconformance_code().trim(), request.inspection_id(), request.defect_code().trim(), request.quantity(), request.disposition().trim(), normalize(request.notes()), actor, id);
    }

    public int change_nonconformance_status(long id, String status, Long actor, LocalDate resolved_on) {
        return query_executor.update("UPDATE quality_cost.nonconformance SET status = ?, resolved_on = ?, updated_at = now(), updated_by_user_id = ? WHERE nonconformance_id = ? AND status IN ('open', 'in_progress')", status, resolved_on, actor, id);
    }

    public long insert_calculation(cost_calculation_request request, BigDecimal total, BigDecimal unit_cost, long actor) {
        return required_id(query_executor.queryForObject("INSERT INTO quality_cost.cost_calculation (cost_period_id, production_order_id, stock_item_id, stock_item_code, stock_item_name, good_quantity, material_cost, direct_labor_cost, overhead_cost, adjustment_amount, total_cost, unit_cost, notes, created_by_user_id, updated_by_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING cost_calculation_id", Long.class, request.cost_period_id(), request.production_order_id(), request.stock_item_id(), normalize(request.stock_item_code()), normalize(request.stock_item_name()), request.good_quantity(), request.material_cost(), request.direct_labor_cost(), request.overhead_cost(), request.adjustment_amount() == null ? BigDecimal.ZERO : request.adjustment_amount(), total, unit_cost, normalize(request.notes()), actor, actor));
    }

    public int update_calculation(long id, cost_calculation_request request, BigDecimal total, BigDecimal unit_cost, long actor) {
        return query_executor.update("UPDATE quality_cost.cost_calculation SET cost_period_id = ?, production_order_id = ?, stock_item_id = ?, stock_item_code = ?, stock_item_name = ?, good_quantity = ?, material_cost = ?, direct_labor_cost = ?, overhead_cost = ?, adjustment_amount = ?, total_cost = ?, unit_cost = ?, notes = ?, calculated_at = now(), updated_at = now(), updated_by_user_id = ? WHERE cost_calculation_id = ? AND status = 'calculated'", request.cost_period_id(), request.production_order_id(), request.stock_item_id(), normalize(request.stock_item_code()), normalize(request.stock_item_name()), request.good_quantity(), request.material_cost(), request.direct_labor_cost(), request.overhead_cost(), request.adjustment_amount() == null ? BigDecimal.ZERO : request.adjustment_amount(), total, unit_cost, normalize(request.notes()), actor, id);
    }
    public int change_calculation_status(long id, String status, long actor) {
        return query_executor.update("UPDATE quality_cost.cost_calculation SET status = ?, updated_at = now(), updated_by_user_id = ? WHERE cost_calculation_id = ? AND status IN ('calculated', 'approved')", status, actor, id);
    }

    public long insert_price_proposal(price_proposal_request request, long actor) {
        return required_id(query_executor.queryForObject("INSERT INTO quality_cost.price_proposal (proposal_code, cost_period_id, stock_item_id, stock_item_code, stock_item_name, unit_cost, margin_percent, proposed_price, currency_code, effective_on, notes, created_by_user_id, updated_by_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING price_proposal_id", Long.class, request.proposal_code().trim(), request.cost_period_id(), request.stock_item_id(), normalize(request.stock_item_code()), normalize(request.stock_item_name()), request.unit_cost(), request.margin_percent(), request.proposed_price(), normalize_currency(request.currency_code()), request.effective_on(), normalize(request.notes()), actor, actor));
    }

    public int update_price_proposal(long id, price_proposal_request request, long actor) {
        return query_executor.update("UPDATE quality_cost.price_proposal SET proposal_code = ?, cost_period_id = ?, stock_item_id = ?, stock_item_code = ?, stock_item_name = ?, unit_cost = ?, margin_percent = ?, proposed_price = ?, currency_code = ?, effective_on = ?, notes = ?, updated_at = now(), updated_by_user_id = ? WHERE price_proposal_id = ? AND status IN ('draft', 'rejected')", request.proposal_code().trim(), request.cost_period_id(), request.stock_item_id(), normalize(request.stock_item_code()), normalize(request.stock_item_name()), request.unit_cost(), request.margin_percent(), request.proposed_price(), normalize_currency(request.currency_code()), request.effective_on(), normalize(request.notes()), actor, id);
    }

    public int change_price_status(long id, String status, long actor) {
        return query_executor.update("UPDATE quality_cost.price_proposal SET status = ?, approved_by_user_id = CASE WHEN ? IN ('approved', 'published') THEN ? ELSE approved_by_user_id END, approved_at = CASE WHEN ? IN ('approved', 'published') THEN now() ELSE approved_at END, updated_at = now(), updated_by_user_id = ? WHERE price_proposal_id = ? AND status IN ('draft', 'pending', 'rejected', 'approved')", status, status, actor, status, actor, id);
    }

    public int delete_draft(String resource, long id) {
        return switch (resource) {
            case "inspections" -> query_executor.update("DELETE FROM quality_cost.quality_inspection WHERE inspection_id = ? AND status = 'draft'", id);
            case "nonconformances" -> query_executor.update("DELETE FROM quality_cost.nonconformance WHERE nonconformance_id = ? AND status = 'open'", id);
            case "periods" -> query_executor.update("DELETE FROM quality_cost.cost_period WHERE cost_period_id = ? AND status = 'draft'", id);
            case "calculations" -> query_executor.update("DELETE FROM quality_cost.cost_calculation WHERE cost_calculation_id = ? AND status = 'calculated'", id);
            case "price_proposals" -> query_executor.update("DELETE FROM quality_cost.price_proposal WHERE price_proposal_id = ? AND status IN ('draft', 'rejected')", id);
            default -> 0;
        };
    }

    private Map<String, Object> map_row(jpa_result_row row, int row_number) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of("id", "code", "production_order_id", "production_output_id", "stock_item_id", "stock_item_code", "stock_item_name", "lot_code", "inspected_quantity", "good_quantity", "defective_quantity", "status", "inspected_on", "inspection_id", "defect_code", "quantity", "disposition", "resolved_on", "period_code", "starts_on", "ends_on", "rule_version_id", "rule_code", "material_valuation_method", "labor_basis", "overhead_basis", "defective_policy", "currency_code", "rounding_scale", "effective_from", "effective_to", "cost_period_id", "material_cost", "direct_labor_cost", "overhead_cost", "adjustment_amount", "total_cost", "unit_cost", "calculated_at", "margin_percent", "proposed_price", "effective_on", "notes")) {
            Object value = row.getObject(key);
            if (value != null) result.put(key, value);
        }
        return result;
    }

    private long required_id(Long id) {
        if (id == null) throw new IllegalStateException("Quality/cost identifier was not returned.");
        return id;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize_currency(String value) {
        String normalized = normalize(value);
        return normalized == null ? "VND" : normalized.toUpperCase();
    }
}
