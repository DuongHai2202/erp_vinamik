package vn.vinamik.erp_backend.production.material_needs;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.inventory.api.inventory_stock_balance_contract;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class production_material_needs_service {
    private static final int quantity_scale = 6;
    private final production_material_needs_repository material_needs_repository;
    private final inventory_stock_balance_contract stock_balance_contract;

    public production_material_needs_service(
            production_material_needs_repository material_needs_repository,
            inventory_stock_balance_contract stock_balance_contract) {
        this.material_needs_repository = material_needs_repository;
        this.stock_balance_contract = stock_balance_contract;
    }

    @Transactional(readOnly = true)
    public material_needs_response find_by_order_id(long production_order_id) {
        production_material_needs_repository.order_header order =
                material_needs_repository.find_order(production_order_id);
        List<production_material_needs_repository.requirement> requirements =
                material_needs_repository.find_requirements(production_order_id);
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("The production order has no material requirements.");
        }
        Map<Long, BigDecimal> available_quantities = stock_balance_contract.find_available_quantities(
                requirements.stream().map(requirement -> requirement.material_stock_item_id()).toList());
        List<material_need_response> items = requirements.stream().map(requirement -> {
            BigDecimal available = normalize(available_quantities.getOrDefault(
                    requirement.material_stock_item_id(), BigDecimal.ZERO));
            BigDecimal required = normalize(requirement.required_quantity());
            BigDecimal shortage = normalize(required.subtract(available).max(BigDecimal.ZERO));
            return new material_need_response(
                    requirement.material_stock_item_id(), requirement.material_item_code(),
                    requirement.material_item_name(), requirement.unit_code(), required, available, shortage);
        }).toList();
        return new material_needs_response(order.production_order_id(), order.order_code(),
                order.target_quantity(), items);
    }

    private BigDecimal normalize(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(quantity_scale, RoundingMode.HALF_UP);
    }
}
