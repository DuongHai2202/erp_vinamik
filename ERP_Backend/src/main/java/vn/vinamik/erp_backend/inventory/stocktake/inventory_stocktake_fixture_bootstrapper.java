package vn.vinamik.erp_backend.inventory.stocktake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.util.List;

/**
 * Creates local-only stocktake sessions when explicitly enabled for a test
 * environment. Production keeps this switch disabled by default.
 */
@Component
public class inventory_stocktake_fixture_bootstrapper implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(inventory_stocktake_fixture_bootstrapper.class);

    private final inventory_stocktake_repository stocktake_repository;
    private final inventory_stocktake_service stocktake_service;
    private final boolean enabled;

    public inventory_stocktake_fixture_bootstrapper(
            inventory_stocktake_repository stocktake_repository,
            inventory_stocktake_service stocktake_service,
            @Value("${erp.fixture.stocktake.enabled:false}") boolean enabled) {
        this.stocktake_repository = stocktake_repository;
        this.stocktake_service = stocktake_service;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!enabled) {
            return;
        }
        if (stocktake_repository.count_all_stocktakes() > 0) {
            logger.info("Đã có phiên kiểm kê trong database; bỏ qua seed local.");
            return;
        }
        var actor = stocktake_repository.active_super_admin_for_fixture();
        if (actor.isEmpty()) {
            logger.warn("Không có super admin đang hoạt động; chưa thể seed phiên kiểm kê local.");
            return;
        }
        List<inventory_stocktake_repository.fixture_warehouse> warehouses =
                stocktake_repository.active_warehouses_for_fixture();
        authenticated_user fixture_actor = new authenticated_user(
                actor.get().user_id(), actor.get().username(), List.of(), true);
        int created = 0;
        for (inventory_stocktake_repository.fixture_warehouse warehouse : warehouses) {
            String code = "stocktake_2026_seed_" + warehouse.warehouse_code();
            try {
                stocktake_service.create(
                        new stocktake_request(code, warehouse.warehouse_id(),
                                "Phiên kiểm kê local để kiểm tra luồng đối chiếu số tồn."),
                        fixture_actor, "fixture-stocktake-bootstrap");
                created++;
            } catch (RuntimeException exception) {
                logger.warn("Không thể seed phiên kiểm kê local cho kho {}; lý do={}",
                        warehouse.warehouse_code(), exception.getMessage());
            }
        }
        logger.info("Đã seed {} phiên kiểm kê local qua service nghiệp vụ.", created);
    }
}
