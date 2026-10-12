package com.smartcompany.service.factory;

import com.smartcompany.dto.factory.ProductionDataDto;
import com.smartcompany.exception.AppException;
import com.smartcompany.repository.factory.ProductionRecordRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductionService {

    @Autowired
    private ProductionRecordRepository productionRecordRepository;

    private static final DateTimeFormatter DAY_FMT   = DateTimeFormatter.ofPattern("MMM dd");
    private static final DateTimeFormatter WEEK_FMT  = DateTimeFormatter.ofPattern("'W'w MMM");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    /**
     * Vraća agregiranu količinu po periodu za grafikon.
     * Granularnost:
     *   WEEK (7d)              → po danu
     *   MONTH (30d)            → po danu
     *   THREE_MONTHS (90d)     → po nedelji
     *   SIX_MONTHS (180d)      → po mesecu
     *   YEAR (365d)            → po mesecu
     *   CUSTOM                 → ≤30d → dan, ≤90d → nedelja, ostalo → mesec
     */
    public ProductionDataDto getProductionData(
            Long factoryId, Long productId,
            String period, String customFrom, String customTo
    ) {
        Instant from;
        Instant to = Instant.now();
        String granularity;

        switch (period) {
            case "WEEK" -> {
                from = to.minus(Duration.ofDays(7));
                granularity = "day";
            }
            case "MONTH" -> {
                from = to.minus(Duration.ofDays(30));
                granularity = "day";
            }
            case "THREE_MONTHS" -> {
                from = to.minus(Duration.ofDays(90));
                granularity = "week";
            }
            case "SIX_MONTHS" -> {
                from = to.minus(Duration.ofDays(180));
                granularity = "month";
            }
            case "YEAR" -> {
                from = to.minus(Duration.ofDays(365));
                granularity = "month";
            }
            case "CUSTOM" -> {
                if (customFrom == null || customTo == null)
                    throw new AppException("Custom period requires from and to dates", HttpStatus.BAD_REQUEST);
                from = LocalDate.parse(customFrom).atStartOfDay(ZoneOffset.UTC).toInstant();
                to   = LocalDate.parse(customTo).atTime(23, 59, 59).atOffset(ZoneOffset.UTC).toInstant();
                long days = Duration.between(from, to).toDays();
                if (days > 365)
                    throw new AppException("Custom period cannot exceed 365 days", HttpStatus.BAD_REQUEST);
                if (days <= 30)       granularity = "day";
                else if (days <= 90)  granularity = "week";
                else                  granularity = "month";
            }
            default -> throw new AppException("Invalid period: " + period, HttpStatus.BAD_REQUEST);
        }

        List<Object[]> rows = switch (granularity) {
            case "day"   -> productionRecordRepository.sumByDay(factoryId, productId, from, to);
            case "week"  -> productionRecordRepository.sumByWeek(factoryId, productId, from, to);
            default      -> productionRecordRepository.sumByMonth(factoryId, productId, from, to);
        };

        List<String> labels = new ArrayList<>();
        List<Long>   values = new ArrayList<>();

        for (Object[] row : rows) {
            // row[0] = date/timestamp, row[1] = SUM
            String label = formatLabel(row[0], granularity);
            labels.add(label);
            values.add(((Number) row[1]).longValue());
        }

        return new ProductionDataDto(granularity, labels, values);
    }

    private String formatLabel(Object dateObj, String granularity) {
        // PostgreSQL vraća java.sql.Date ili Timestamp
        LocalDate date;
        if (dateObj instanceof java.sql.Date d) {
            date = d.toLocalDate();
        } else if (dateObj instanceof java.sql.Timestamp ts) {
            date = ts.toLocalDateTime().toLocalDate();
        } else {
            date = LocalDate.parse(dateObj.toString().substring(0, 10));
        }

        return switch (granularity) {
            case "day"   -> date.format(DAY_FMT);
            case "week"  -> "W" + date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                             + " " + date.format(DateTimeFormatter.ofPattern("MMM"));
            default      -> date.format(MONTH_FMT);
        };
    }
}