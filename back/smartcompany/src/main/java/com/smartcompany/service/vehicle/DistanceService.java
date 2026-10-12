package com.smartcompany.service.vehicle;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.smartcompany.dto.vehicle.DistanceDataDto;
import com.smartcompany.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

@Service
public class DistanceService {

    @Autowired
    private InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    private static final DateTimeFormatter DAY_FMT   = DateTimeFormatter.ofPattern("MMM dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    public DistanceDataDto getDistanceData(
            Long vehicleId,
            String period, String customFrom, String customTo
    ) {
        Instant from;
        Instant to = Instant.now();
        String granularity;
        String windowPeriod;

        switch (period) {
            case "WEEK" -> {
                from = to.minus(Duration.ofDays(7));
                granularity = "day";
                windowPeriod = "1d";
            }
            case "MONTH" -> {
                from = to.minus(Duration.ofDays(30));
                granularity = "day";
                windowPeriod = "1d";
            }
            case "THREE_MONTHS" -> {
                from = to.minus(Duration.ofDays(90));
                granularity = "week";
                windowPeriod = "1w";
            }
            case "SIX_MONTHS" -> {
                from = to.minus(Duration.ofDays(180));
                granularity = "month";
                windowPeriod = "1mo";
            }
            case "YEAR" -> {
                from = to.minus(Duration.ofDays(365));
                granularity = "month";
                windowPeriod = "1mo";
            }
            case "CUSTOM" -> {
                if (customFrom == null || customTo == null)
                    throw new AppException("Custom period requires from and to dates", HttpStatus.BAD_REQUEST);
                from = LocalDate.parse(customFrom).atStartOfDay(ZoneOffset.UTC).toInstant();
                to   = LocalDate.parse(customTo).atTime(23, 59, 59).atOffset(ZoneOffset.UTC).toInstant();
                long days = Duration.between(from, to).toDays();
                if (days > 365)
                    throw new AppException("Custom period cannot exceed 365 days", HttpStatus.BAD_REQUEST);
                if (days <= 30) {
                    granularity = "day";
                    windowPeriod = "1d";
                } else if (days <= 90) {
                    granularity = "week";
                    windowPeriod = "1w";
                } else {
                    granularity = "month";
                    windowPeriod = "1mo";
                }
            }
            default -> throw new AppException("Invalid period: " + period, HttpStatus.BAD_REQUEST);
        }

        String flux = """
            from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r._measurement == "vehicle_distance")
              |> filter(fn: (r) => r.vehicle_id == "%d")
              |> filter(fn: (r) => r._field == "distance")
              |> aggregateWindow(every: %s, fn: sum, createEmpty: false)
              |> yield(name: "sum")
            """.formatted(bucket, from.toString(), to.toString(),
                         vehicleId, windowPeriod);

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        try {
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(flux);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Instant time = record.getTime();
                    Double value = (Double) record.getValue();
                    if (time != null && value != null) {
                        labels.add(formatLabel(time, granularity));
                        values.add(Math.round(value * 10.0) / 10.0);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[InfluxDB] Failed to query distance: " + e.getMessage());
            throw new AppException("Failed to fetch distance data", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new DistanceDataDto(granularity, labels, values);
    }

    private String formatLabel(Instant instant, String granularity) {
        LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
        return switch (granularity) {
            case "day"  -> date.format(DAY_FMT);
            case "week" -> "W" + date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                           + " " + date.format(DateTimeFormatter.ofPattern("MMM"));
            default     -> date.format(MONTH_FMT);
        };
    }
}