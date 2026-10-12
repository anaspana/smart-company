package com.smartcompany.repository.vehicle;

import com.smartcompany.model.vehicle.Vehicle;

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

public interface VehicleRepository extends JpaRepository<Vehicle, Long>,
        JpaSpecificationExecutor<Vehicle> {

    @Query("SELECT v FROM Vehicle v WHERE v.isOnline = true AND v.lastHeartbeat < :cutoff")
    List<Vehicle> findStaleOnlineVehicles(@Param("cutoff") Instant cutoff);

    // Zaključava jedno vozilo — koristi se u handleHeartbeat() i
    // handleLocation() da spreči lost update sa checkHeartbeats() ili
    // sa istovremenim heartbeat/location porukama za isto vozilo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") Long id);

    // Zaključana varijanta findStaleOnlineVehicles — koristi se u checkHeartbeats().
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT v FROM Vehicle v WHERE v.isOnline = true AND v.lastHeartbeat < :cutoff")
    List<Vehicle> findStaleOnlineVehiclesForUpdate(@Param("cutoff") Instant cutoff);
}