package vn.vinamik.erp_backend.quality_cost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class quality_cost_service {
    private static final Logger logger = LoggerFactory.getLogger(quality_cost_service.class);
    private final quality_cost_repository repository;
    private final audit_event_writer audit_writer;

    public quality_cost_service(quality_cost_repository repository, audit_event_writer audit_writer) {
        this.repository = repository;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public quality_cost_page_response search(String resource, String search, String status, int page, int page_size) {
        if (page < 0) page = 0;
        if (page_size < 1) page_size = 50;
        if (page_size > 200) page_size = 200;
        return repository.list(resource, normalize(search), normalize(status), page, page_size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> find(String resource, long id) {
        return repository.find(resource, id);
    }

    @Transactional
    public Map<String, Object> create_rule(cost_rule_request request, authenticated_user actor, String correlation_id) {
        validate_rule(request);
        long id = repository.insert_rule(request, actor.user_id());
        audit(actor, "cost_rule_create", "cost_rule_version", id, correlation_id, Map.of("rule_code", request.rule_code()));
        return repository.find("rules", id);
    }

    @Transactional
    public Map<String, Object> update_rule(long id, cost_rule_request request, authenticated_user actor, String correlation_id) {
        validate_rule(request);
        if (repository.update_rule(id, request, actor.user_id()) == 0) throw new resource_not_found_exception("Draft cost rule");
        audit(actor, "cost_rule_update", "cost_rule_version", id, correlation_id, Map.of("rule_code", request.rule_code()));
        return repository.find("rules", id);
    }

    @Transactional
    public Map<String, Object> create_period(cost_period_request request, authenticated_user actor, String correlation_id) {
        validate_period(request);
        long id = repository.insert_period(request, actor.user_id());
        audit(actor, "cost_period_create", "cost_period", id, correlation_id, Map.of("period_code", request.period_code()));
        return repository.find("periods", id);
    }

    @Transactional
    public Map<String, Object> update_period(long id, cost_period_request request, authenticated_user actor, String correlation_id) {
        validate_period(request);
        if (repository.update_period(id, request, actor.user_id()) == 0) throw new resource_not_found_exception("Draft cost period");
        audit(actor, "cost_period_update", "cost_period", id, correlation_id, Map.of("period_code", request.period_code()));
        return repository.find("periods", id);
    }

    @Transactional
    public Map<String, Object> create_inspection(quality_inspection_request request, authenticated_user actor, String correlation_id) {
        validate_inspection(request);
        long id = repository.insert_inspection(request, actor.user_id());
        audit(actor, "quality_inspection_create", "quality_inspection", id, correlation_id, Map.of("inspection_code", request.inspection_code()));
        return repository.find("inspections", id);
    }

    @Transactional
    public Map<String, Object> update_inspection(long id, quality_inspection_request request, authenticated_user actor, String correlation_id) {
        validate_inspection(request);
        if (repository.update_inspection(id, request, actor.user_id()) == 0) throw new resource_not_found_exception("Draft quality inspection");
        audit(actor, "quality_inspection_update", "quality_inspection", id, correlation_id, Map.of("inspection_code", request.inspection_code()));
        return repository.find("inspections", id);
    }

    @Transactional
    public Map<String, Object> create_nonconformance(nonconformance_request request, authenticated_user actor, String correlation_id) {
        validate_nonconformance(request);
        long id = repository.insert_nonconformance(request, actor.user_id());
        audit(actor, "quality_nonconformance_create", "nonconformance", id, correlation_id, Map.of("nonconformance_code", request.nonconformance_code()));
        return repository.find("nonconformances", id);
    }

    @Transactional
    public Map<String, Object> update_nonconformance(long id, nonconformance_request request, authenticated_user actor, String correlation_id) {
        validate_nonconformance(request);
        if (repository.update_nonconformance(id, request, actor.user_id()) == 0) throw new resource_not_found_exception("Open nonconformance");
        audit(actor, "quality_nonconformance_update", "nonconformance", id, correlation_id, Map.of("nonconformance_code", request.nonconformance_code()));
        return repository.find("nonconformances", id);
    }

    @Transactional
    public Map<String, Object> create_calculation(cost_calculation_request request, authenticated_user actor, String correlation_id) {
        validate_calculation(request);
        BigDecimal adjustment = request.adjustment_amount() == null ? BigDecimal.ZERO : request.adjustment_amount();
        BigDecimal total = request.material_cost().add(request.direct_labor_cost()).add(request.overhead_cost()).add(adjustment).setScale(2, RoundingMode.HALF_UP);
        BigDecimal unit = total.divide(request.good_quantity(), 6, RoundingMode.HALF_UP);
        long id = repository.insert_calculation(request, total, unit, actor.user_id());
        audit(actor, "cost_calculation_create", "cost_calculation", id, correlation_id, Map.of("cost_period_id", request.cost_period_id(), "total_cost", total));
        return repository.find("calculations", id);
    }

    @Transactional
    public Map<String, Object> update_calculation(long id, cost_calculation_request request, authenticated_user actor, String correlation_id) {
        validate_calculation(request);
        BigDecimal adjustment = request.adjustment_amount() == null ? BigDecimal.ZERO : request.adjustment_amount();
        BigDecimal total = request.material_cost().add(request.direct_labor_cost()).add(request.overhead_cost()).add(adjustment).setScale(2, RoundingMode.HALF_UP);
        BigDecimal unit = total.divide(request.good_quantity(), 6, RoundingMode.HALF_UP);
        if (repository.update_calculation(id, request, total, unit, actor.user_id()) == 0) throw new resource_not_found_exception("Calculated cost result");
        audit(actor, "cost_calculation_update", "cost_calculation", id, correlation_id, Map.of("cost_period_id", request.cost_period_id(), "total_cost", total));
        return repository.find("calculations", id);
    }
    @Transactional
    public Map<String, Object> create_price_proposal(price_proposal_request request, authenticated_user actor, String correlation_id) {
        validate_price(request);
        long id = repository.insert_price_proposal(request, actor.user_id());
        audit(actor, "price_proposal_create", "price_proposal", id, correlation_id, Map.of("proposal_code", request.proposal_code()));
        return repository.find("price_proposals", id);
    }

    @Transactional
    public Map<String, Object> update_price_proposal(long id, price_proposal_request request, authenticated_user actor, String correlation_id) {
        validate_price(request);
        if (repository.update_price_proposal(id, request, actor.user_id()) == 0) throw new resource_not_found_exception("Draft price proposal");
        audit(actor, "price_proposal_update", "price_proposal", id, correlation_id, Map.of("proposal_code", request.proposal_code()));
        return repository.find("price_proposals", id);
    }

    @Transactional
    public Map<String, Object> change_status(String resource, long id, String next_status, authenticated_user actor, String correlation_id) {
        String status = normalize(next_status);
        if (status == null) throw new IllegalArgumentException("Status is required.");
        Map<String, Object> current = repository.find(resource, id);
        String current_status = String.valueOf(current.getOrDefault("status", ""));
        if (!allowed(resource, current_status, status)) throw new IllegalArgumentException("The requested status transition is not allowed.");
        int changed;
        if (resource.equals("inspections")) changed = repository.change_inspection_status(id, status, actor.user_id());
        else if (resource.equals("nonconformances")) changed = repository.change_nonconformance_status(id, status, actor.user_id(), status.equals("resolved") ? LocalDate.now() : null);
        else if (resource.equals("periods")) changed = repository.change_period_status(id, status, actor.user_id());
        else if (resource.equals("calculations")) changed = repository.change_calculation_status(id, status, actor.user_id());
        else if (resource.equals("price_proposals") || resource.equals("price_approvals")) changed = repository.change_price_status(id, status, actor.user_id());
        else changed = 0;
        if (changed == 0) throw new field_conflict_exception("status", "The record could not be transitioned from its current state.");
        audit(actor, resource + "_status_change", resource, id, correlation_id, Map.of("from", current_status, "to", status));
        logger.info("Đã chuyển trạng thái quality/cost; resource={}, id={}, from={}, to={}, actor_user_id={}, correlation_id={}", resource, id, current_status, status, actor.user_id(), correlation_id);
        return repository.find(resource, id);
    }

    @Transactional
    public void delete(String resource, long id, authenticated_user actor, String correlation_id) {
        repository.find(resource, id);
        if (repository.delete_draft(resource, id) == 0) throw new IllegalArgumentException("Only draft records can be deleted.");
        audit(actor, resource + "_delete", resource, id, correlation_id, Map.of());
    }

    private boolean allowed(String resource, String current, String next) {
        return switch (resource) {
            case "inspections" -> (current.equals("draft") && next.equals("submitted")) || (current.equals("submitted") && List.of("passed", "failed", "held").contains(next)) || (current.equals("held") && next.equals("released")) || (List.of("draft", "submitted", "held").contains(current) && next.equals("cancelled"));
            case "nonconformances" -> (current.equals("open") && next.equals("in_progress")) || (List.of("open", "in_progress").contains(current) && List.of("resolved", "cancelled").contains(next));
            case "periods" -> (current.equals("draft") && next.equals("open")) || (current.equals("open") && next.equals("calculating")) || (current.equals("calculating") && next.equals("calculated")) || (current.equals("calculated") && next.equals("approved")) || (current.equals("approved") && next.equals("locked")) || (!current.equals("locked") && next.equals("cancelled"));
            case "calculations" -> (current.equals("calculated") && next.equals("approved")) || (current.equals("approved") && next.equals("locked")) || (current.equals("calculated") && next.equals("cancelled"));
            case "price_proposals", "price_approvals" -> (current.equals("draft") && next.equals("pending")) || (current.equals("pending") && next.equals("approved")) || (current.equals("approved") && next.equals("published")) || (current.equals("rejected") && next.equals("pending")) || (!current.equals("published") && next.equals("cancelled"));
            default -> false;
        };
    }

    private void validate_rule(cost_rule_request request) {
        if (request.effective_to() != null && request.effective_to().isBefore(request.effective_from())) throw new IllegalArgumentException("Rule effective end cannot be before its start.");
        if (request.rounding_scale() != null && (request.rounding_scale() < 0 || request.rounding_scale() > 6)) throw new IllegalArgumentException("Rounding scale must be between 0 and 6.");
    }

    private void validate_period(cost_period_request request) {
        if (request.ends_on().isBefore(request.starts_on())) throw new IllegalArgumentException("Cost period end cannot be before its start.");
    }

    private void validate_inspection(quality_inspection_request request) {
        BigDecimal total = request.good_quantity().add(request.defective_quantity());
        if (total.compareTo(request.inspected_quantity()) > 0) throw new IllegalArgumentException("Good and defective quantities cannot exceed inspected quantity.");
    }

    private void validate_nonconformance(nonconformance_request request) {
        if (!List.of("hold", "rework", "scrap", "return", "release").contains(request.disposition())) throw new IllegalArgumentException("Disposition is invalid.");
    }

    private void validate_calculation(cost_calculation_request request) {
        if (request.material_cost().signum() < 0 || request.direct_labor_cost().signum() < 0 || request.overhead_cost().signum() < 0) throw new IllegalArgumentException("Cost components cannot be negative.");
    }

    private void validate_price(price_proposal_request request) {
        if (request.margin_percent().signum() < 0 || request.unit_cost().signum() < 0 || request.proposed_price().signum() < 0) throw new IllegalArgumentException("Price values cannot be negative.");
    }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private void audit(authenticated_user actor, String action, String entity, long id, String correlation_id, Map<String, Object> metadata) {
        audit_writer.write(actor.user_id(), "quality_cost", action, entity, String.valueOf(id), correlation_id, metadata);
    }
}
