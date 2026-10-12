package com.smartcompany.repository.company;

import com.smartcompany.model.company.Company;
import com.smartcompany.model.company.CompanyStatus;

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

public interface CompanyRepository extends JpaRepository<Company, Long>,
        JpaSpecificationExecutor<Company> {

    List<Company> findByOwnerUsernameAndStatus(String username, CompanyStatus status);

    // Zaključava zahtev za registraciju firme (SELECT ... FOR UPDATE) dok se
    // odluka ne upiše — sprečava da dva menadžera istovremeno obrade isti
    // PENDING zahtev.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT c FROM Company c WHERE c.id = :id")
    Optional<Company> findByIdForUpdate(@Param("id") Long id);
}