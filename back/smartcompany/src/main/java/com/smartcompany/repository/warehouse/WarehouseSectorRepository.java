package com.smartcompany.repository.warehouse;

import com.smartcompany.model.warehouse.WarehouseSector;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface WarehouseSectorRepository extends JpaRepository<WarehouseSector, Long> {

    List<WarehouseSector> findByWarehouseId(Long warehouseId);

    // Zaključava jedan sektor (SELECT ... FOR UPDATE) — koristi se u
    // handleTemperature() da spreči lost update ako dve poruke za isti
    // sektor stignu skoro istovremeno (npr. requeue nakon lock timeout-a).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT s FROM WarehouseSector s WHERE s.id = :id")
    Optional<WarehouseSector> findByIdForUpdate(@Param("id") Long id);
}