package com.smartcompany.repository.factory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartcompany.model.factory.ProductionRecord;

import java.time.Instant;
import java.util.List;

public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, Long> {

    // Ukupna kolicina po danu za grafike
    @Query(value = """
        SELECT DATE_TRUNC('day', r.timestamp) as day, SUM(r.quantity) as total
        FROM production_record r
        WHERE r.factory_id = :factoryId
          AND r.product_id = :productId
          AND r.timestamp BETWEEN :from AND :to
        GROUP BY DATE_TRUNC('day', r.timestamp)
        ORDER BY DATE_TRUNC('day', r.timestamp)
    """, nativeQuery = true)
    List<Object[]> sumByDay(@Param("factoryId") Long factoryId,
                            @Param("productId") Long productId,
                            @Param("from") Instant from,
                            @Param("to") Instant to);

    // Ukupna količina po nedelji
    @Query(value = """
        SELECT DATE_TRUNC('week', r.timestamp) as week, SUM(r.quantity) as total
        FROM production_record r
        WHERE r.factory_id = :factoryId
          AND r.product_id = :productId
          AND r.timestamp BETWEEN :from AND :to
        GROUP BY DATE_TRUNC('week', r.timestamp)
        ORDER BY DATE_TRUNC('week', r.timestamp)
    """, nativeQuery = true)
    List<Object[]> sumByWeek(@Param("factoryId") Long factoryId,
                             @Param("productId") Long productId,
                             @Param("from") Instant from,
                             @Param("to") Instant to);

    // Ukupna količina po mesecu
    @Query(value = """
        SELECT DATE_TRUNC('month', r.timestamp) as month, SUM(r.quantity) as total
        FROM production_record r
        WHERE r.factory_id = :factoryId
          AND r.product_id = :productId
          AND r.timestamp BETWEEN :from AND :to
        GROUP BY DATE_TRUNC('month', r.timestamp)
        ORDER BY DATE_TRUNC('month', r.timestamp)
    """, nativeQuery = true)
    List<Object[]> sumByMonth(@Param("factoryId") Long factoryId,
                              @Param("productId") Long productId,
                              @Param("from") Instant from,
                              @Param("to") Instant to);

    boolean existsByFactoryId(Long factoryId);

    boolean existsByProductId(Long productId);
}