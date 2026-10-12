package com.smartcompany.repository.warehouse;

import com.smartcompany.model.warehouse.WarehouseItem;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WarehouseItemRepository extends JpaRepository<WarehouseItem, Long> {

    @Query("SELECT COALESCE(SUM(wi.quantity), 0) FROM WarehouseItem wi WHERE wi.product.id = :productId")
    Integer sumQuantityByProductId(@Param("productId") Long productId);

    // Zaključava sve redove ovog proizvoda (SELECT ... FOR UPDATE) dok se
    // transakcija ne završi. Poziva se iz OrderService.createOrder().
   @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT wi FROM WarehouseItem wi WHERE wi.product.id = :productId AND wi.quantity > 0 ORDER BY wi.id ASC")
    List<WarehouseItem> findByProductIdWithStock(@Param("productId") Long productId);

    Optional<WarehouseItem> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    boolean existsByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT wi FROM WarehouseItem wi WHERE wi.product.id = :productId AND wi.quantity >= :minQuantity ORDER BY wi.id ASC")
    List<WarehouseItem> findSingleWarehouseWithSufficientStock(@Param("productId") Long productId, @Param("minQuantity") Integer minQuantity);
}