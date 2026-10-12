package com.smartcompany.repository.location;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcompany.model.location.Country;

public interface CountryRepository extends JpaRepository<Country, Long> {}