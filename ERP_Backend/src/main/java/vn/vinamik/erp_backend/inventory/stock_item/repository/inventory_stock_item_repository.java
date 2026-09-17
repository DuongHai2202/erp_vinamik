package vn.vinamik.erp_backend.inventory.stock_item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vinamik.erp_backend.inventory.stock_item.entity.stock_item_entity;

public interface inventory_stock_item_repository extends JpaRepository<stock_item_entity, Long> {
    @Query("select count(i) > 0 from stock_item_entity i where i.item_code = :item_code")
    boolean exists_by_item_code(@Param("item_code") String item_code);

    @Query("select count(i) > 0 from stock_item_entity i where i.item_code = :item_code and i.stock_item_id <> :stock_item_id")
    boolean exists_by_item_code_excluding_id(@Param("item_code") String item_code,
                                             @Param("stock_item_id") Long stock_item_id);
}

