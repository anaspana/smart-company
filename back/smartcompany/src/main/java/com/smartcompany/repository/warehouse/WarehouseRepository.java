package com.smartcompany.repository.warehouse;

import com.smartcompany.model.warehouse.Warehouse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long>, JpaSpecificationExecutor<Warehouse>
{
        @Query("SELECT w FROM Warehouse w WHERE w.isOnline = true AND w.lastHeartbeat < :cutoff")
        List<Warehouse> findStaleOnlineWarehouses(@Param("cutoff") Instant cutoff);

        // Zaključava jedan magacin (SELECT ... FOR UPDATE) — koristi se u
        // handleHeartbeat() da spreči lost update sa checkHeartbeats().
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
        @Query("SELECT w FROM Warehouse w WHERE w.id = :id")
        Optional<Warehouse> findByIdForUpdate(@Param("id") Long id);

        // Ista logika kao findStaleOnlineWarehouses, ali zaključava svaki red
        // koji vraća — koristi se u checkHeartbeats() da spreči lost update
        // sa konkurentnim handleHeartbeat() pozivima za isti magacin.
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
        @Query("SELECT w FROM Warehouse w WHERE w.isOnline = true AND w.lastHeartbeat < :cutoff")
        List<Warehouse> findStaleOnlineWarehousesForUpdate(@Param("cutoff") Instant cutoff);
}