package com.smartcompany.repository.vehicle;

import com.smartcompany.model.vehicle.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    List<VehicleModel> findByBrandId(Long brandId);
}