package vn.vinamik.erp_backend.human_resources.contract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Creates a small set of contract lifecycle examples for local/test environments.
 *
 * The switch is disabled by default. Every write goes through the contract
 * service; the only fixture-only update is extending an existing active
 * contract's end date so the 15-day warning can be tested after a database
 * already contains the generated fixture.
 */
@Component
public class human_resources_contract_fixture_bootstrapper implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(human_resources_contract_fixture_bootstrapper.class);

    private final human_resources_contract_repository contract_repository;
    private final human_resources_contract_service contract_service;
    private final audit_event_writer audit_writer;
    private final boolean enabled;

    public human_resources_contract_fixture_bootstrapper(
            human_resources_contract_repository contract_repository,
            human_resources_contract_service contract_service,
            audit_event_writer audit_writer,
            @Value("${erp.fixture.contract.enabled:false}") boolean enabled) {
        this.contract_repository = contract_repository;
        this.contract_service = contract_service;
        this.audit_writer = audit_writer;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (!enabled) {
            return;
        }
        var actor = contract_repository.active_super_admin_for_fixture();
        if (actor.isEmpty()) {
            logger.warn("Không có super admin đang hoạt động; chưa thể seed dữ liệu hợp đồng local.");
            return;
        }
        var fixture_actor = new authenticated_user(
                actor.get().user_id(), actor.get().username(), List.of(), true);
        int created = 0;
        int synchronized_expiry = 0;
        for (fixture_contract_spec spec : fixture_contract_specs()) {
            var employee = contract_repository.employee_by_code_for_fixture(spec.employee_code());
            if (employee.isEmpty()) {
                logger.warn("Không tìm thấy nhân viên {} khi seed hợp đồng local.", spec.employee_code());
                continue;
            }
            try {
                var existing = contract_repository.contract_by_code_for_fixture(spec.contract_code());
                if (existing.isPresent()) {
                    if ("active".equals(spec.status())
                            && (!spec.effective_to().equals(existing.get().effective_to())
                            || existing.get().effective_to() == null)) {
                        int updated = contract_repository.update_fixture_effective_to(
                                spec.contract_code(), spec.effective_to(), fixture_actor.user_id());
                        if (updated > 0) {
                            audit_writer.write(fixture_actor.user_id(), "hr", "contract_fixture_sync",
                                    "employment_contract",
                                    String.valueOf(existing.get().employment_contract_id()),
                                    "fixture-contract-bootstrap",
                                    Map.of("contract_code", spec.contract_code(),
                                            "effective_to", spec.effective_to().toString()));
                            synchronized_expiry += updated;
                        }
                    }
                    continue;
                }

                employment_contract_response created_contract = contract_service.create(
                        new employment_contract_request(
                                spec.contract_code(),
                                employee.get().employee_id(),
                                spec.contract_type(),
                                spec.effective_from(),
                                spec.effective_to(),
                                spec.base_salary(),
                                "VND",
                                "draft",
                                spec.notes()),
                        fixture_actor,
                        "fixture-contract-bootstrap");
                if ("active".equals(spec.status())) {
                    contract_service.change_status(
                            created_contract.employment_contract_id(),
                            new employment_contract_status_request(
                                    "active", "Kích hoạt dữ liệu cảnh báo sắp hết hạn trong môi trường local."),
                            fixture_actor,
                            "fixture-contract-bootstrap");
                }
                created++;
            } catch (RuntimeException exception) {
                logger.warn("Không thể seed hợp đồng {} qua service; lý do={}",
                        spec.contract_code(), exception.getMessage());
            }
        }
        logger.info("Đã seed {} hợp đồng local và đồng bộ {} ngày hết hạn để kiểm thử cảnh báo 15 ngày.",
                created, synchronized_expiry);
    }

    private List<fixture_contract_spec> fixture_contract_specs() {
        return List.of(
                new fixture_contract_spec("contract_2024_0005", "vmk0005", "fixed_term_36_months",
                        LocalDate.of(2024, 1, 1), LocalDate.of(2026, 9, 30),
                        new BigDecimal("45000000"), "Hợp đồng cố định sắp đến hạn cần theo dõi gia hạn.", "active"),
                new fixture_contract_spec("contract_2024_0009", "vmk0009", "fixed_term_36_months",
                        LocalDate.of(2024, 1, 1), LocalDate.of(2026, 10, 1),
                        new BigDecimal("42000000"), "Hợp đồng cố định còn 8 ngày hiệu lực.", "active"),
                new fixture_contract_spec("contract_2024_0013", "vmk0013", "fixed_term_36_months",
                        LocalDate.of(2024, 1, 8), LocalDate.of(2026, 10, 3),
                        new BigDecimal("48750000"), "Hợp đồng cố định còn 10 ngày hiệu lực.", "active"),
                new fixture_contract_spec("contract_2024_0017", "vmk0017", "fixed_term_36_months",
                        LocalDate.of(2024, 1, 1), LocalDate.of(2026, 10, 5),
                        new BigDecimal("50250000"), "Hợp đồng cố định cần chuẩn bị phụ lục gia hạn.", "active"),
                new fixture_contract_spec("contract_2024_0021", "vmk0021", "fixed_term_36_months",
                        LocalDate.of(2024, 1, 1), LocalDate.of(2026, 10, 8),
                        new BigDecimal("48000000"), "Hợp đồng cố định còn 15 ngày hiệu lực.", "active"),
                new fixture_contract_spec("contract_2026_state_draft", "vmk0591", "fixed_term_12_months",
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                        new BigDecimal("18500000"), "Hợp đồng chờ hoàn thiện hồ sơ và phê duyệt.", "draft"),
                new fixture_contract_spec("contract_2026_state_draft_0002", "vmk0592", "fixed_term_12_months",
                        LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30),
                        new BigDecimal("18500000"), "Hồ sơ tái ký đang chờ bổ sung giấy tờ và phê duyệt.", "draft"),
                new fixture_contract_spec("contract_2026_state_draft_0003", "vmk0593", "fixed_term_12_months",
                        LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30),
                        new BigDecimal("19250000"), "Bản nháp hợp đồng chờ đối chiếu thông tin nhân viên.", "draft"),
                new fixture_contract_spec("contract_2026_state_draft_0004", "vmk0594", "fixed_term_12_months",
                        LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30),
                        new BigDecimal("20000000"), "Bản nháp hợp đồng chờ người lao động xác nhận.", "draft")
        );
    }

    private record fixture_contract_spec(
            String contract_code,
            String employee_code,
            String contract_type,
            LocalDate effective_from,
            LocalDate effective_to,
            BigDecimal base_salary,
            String notes,
            String status) {
    }
}
