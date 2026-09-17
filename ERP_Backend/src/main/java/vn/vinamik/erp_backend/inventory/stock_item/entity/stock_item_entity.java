package vn.vinamik.erp_backend.inventory.stock_item.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity(name = "stock_item_entity")
@Table(schema = "inventory", name = "stock_item")
public class stock_item_entity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stock_item_id;

    @Column(name = "item_code", nullable = false, length = 60)
    private String item_code;

    @Column(name = "item_name", nullable = false, length = 180)
    private String item_name;

    @Column(name = "item_type", nullable = false, length = 24)
    private String item_type;

    @Column(name = "item_category_id")
    private Long item_category_id;

    @Column(name = "base_unit_of_measure_id", nullable = false)
    private Long base_unit_of_measure_id;

    @Column(name = "lot_controlled", nullable = false)
    private boolean lot_controlled;

    @Column(name = "minimum_stock_quantity", precision = 18, scale = 6)
    private BigDecimal minimum_stock_quantity;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant created_at;

    @Column(name = "created_by_user_id")
    private Long created_by_user_id;

    @Column(name = "updated_at", nullable = false)
    private Instant updated_at;

    @Column(name = "updated_by_user_id")
    private Long updated_by_user_id;

    protected stock_item_entity() {
    }

    public stock_item_entity(String item_code, String item_name, Long item_category_id,
                             Long base_unit_of_measure_id, boolean lot_controlled,
                             BigDecimal minimum_stock_quantity, String status, String description,
                             Long actor_user_id) {
        this(item_code, item_name, "raw_material", item_category_id, base_unit_of_measure_id,
                lot_controlled, minimum_stock_quantity, status, description, actor_user_id);
    }

    public stock_item_entity(String item_code, String item_name, String item_type, Long item_category_id,
                             Long base_unit_of_measure_id, boolean lot_controlled,
                             BigDecimal minimum_stock_quantity, String status, String description,
                             Long actor_user_id) {
        Instant now = Instant.now();
        this.item_code = item_code;
        this.item_name = item_name;
        this.item_type = item_type;
        this.item_category_id = item_category_id;
        this.base_unit_of_measure_id = base_unit_of_measure_id;
        this.lot_controlled = lot_controlled;
        this.minimum_stock_quantity = minimum_stock_quantity;
        this.status = status;
        this.description = description;
        this.created_at = now;
        this.created_by_user_id = actor_user_id;
        this.updated_at = now;
        this.updated_by_user_id = actor_user_id;
    }

    public void update_values(String item_code, String item_name, Long item_category_id,
                              Long base_unit_of_measure_id, boolean lot_controlled,
                              BigDecimal minimum_stock_quantity, String status, String description,
                              Long actor_user_id) {
        this.item_code = item_code;
        this.item_name = item_name;
        this.item_category_id = item_category_id;
        this.base_unit_of_measure_id = base_unit_of_measure_id;
        this.lot_controlled = lot_controlled;
        this.minimum_stock_quantity = minimum_stock_quantity;
        this.status = status;
        this.description = description;
        this.updated_at = Instant.now();
        this.updated_by_user_id = actor_user_id;
    }

    public void deactivate(Long actor_user_id) {
        this.status = "inactive";
        this.updated_at = Instant.now();
        this.updated_by_user_id = actor_user_id;
    }

    public Long stock_item_id() {
        return stock_item_id;
    }

    public String item_type() {
        return item_type;
    }
}



