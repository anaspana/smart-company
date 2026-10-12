package com.smartcompany.repository.location;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcompany.model.location.City;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByCountryId(Long countryId);
}