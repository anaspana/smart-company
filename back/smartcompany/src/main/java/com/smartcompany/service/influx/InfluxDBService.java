package com.smartcompany.service.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class InfluxDBService {

    @Autowired
    private InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.org}")
    private String org;

    /**
     * Beleži heartbeat/status događaj fabrike.
     * measurement: factory_status
     * tags: factory_id
     * fields: online (1 = online, 0 = offline)
     */
    public void writeHeartbeat(Long factoryId, boolean online, Instant timestamp) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point
                    .measurement("factory_status")
                    .addTag("factory_id", String.valueOf(factoryId))
                    .addField("online", online ? 1 : 0)
                    .time(timestamp, WritePrecision.MS);
            writeApi.writePoint(bucket, org, point);
        } catch (Exception e) {
            // Log, ne pucaj celu aplikaciju zbog InfluxDB greške
            System.err.println("[InfluxDB] Failed to write heartbeat for factory " + factoryId + ": " + e.getMessage());
        }
    }

    /**
     * Beleži podatke o proizvodnji.
     * measurement: factory_production
     * tags: factory_id, product_id
     * fields: quantity
     */
    public void writeProduction(Long factoryId, Long productId, int quantity, Instant timestamp) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point
                    .measurement("factory_production")
                    .addTag("factory_id", String.valueOf(factoryId))
                    .addTag("product_id", String.valueOf(productId))
                    .addField("quantity", quantity)
                    .time(timestamp, WritePrecision.MS);
            writeApi.writePoint(bucket, org, point);
        } catch (Exception e) {
            System.err.println("[InfluxDB] Failed to write production for factory " + factoryId + ": " + e.getMessage());
        }
    }

    /**
     * Beleži kada fabrika prelazi u offline stanje (heartbeat checker).
     */
    public void writeOfflineEvent(Long factoryId) {
        writeHeartbeat(factoryId, false, Instant.now());
    }

    public void writeWarehouseHeartbeat(Long warehouseId) {
        writeWarehouseStatus(warehouseId, true, Instant.now());
    }

    public void writeWarehouseOnlineEvent(Long warehouseId) {
        writeWarehouseStatus(warehouseId, true, Instant.now());
    }

    public void writeWarehouseOfflineEvent(Long warehouseId) {
        writeWarehouseStatus(warehouseId, false, Instant.now());
    }

    private void writeWarehouseStatus(Long warehouseId, boolean online, Instant timestamp) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point
                .measurement("warehouse_status")
                .addTag("warehouse_id", String.valueOf(warehouseId))
                .addField("online", online ? 1 : 0)
                .time(timestamp, WritePrecision.MS);
            writeApi.writePoint(bucket, org, point);
        } catch (Exception e) {
            System.err.println("[InfluxDB] Failed to write warehouse status for " + warehouseId + ": " + e.getMessage());
        }
    }

    public void writeTemperature(Long warehouseId, Long sectorId, Double temperature, Instant timestamp) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point
                .measurement("warehouse_temperature")
                .addTag("warehouse_id", String.valueOf(warehouseId))
                .addTag("sector_id", String.valueOf(sectorId))
                .addField("temperature", temperature)
                .time(timestamp, WritePrecision.MS);
            writeApi.writePoint(bucket, org, point);
        } catch (Exception e) {
            System.err.println("[InfluxDB] Failed to write temperature for warehouse " + warehouseId + ": " + e.getMessage());
        }
    }

    public void writeVehicleHeartbeat(Long vehicleId) {
        writeVehicleStatus(vehicleId, true, Instant.now());
    }

    public void writeVehicleOnlineEvent(Long vehicleId) {
        writeVehicleStatus(vehicleId, true, Instant.now());
    }

    public void writeVehicleOfflineEvent(Long vehicleId) {
        writeVehicleStatus(vehicleId, false, Instant.now());
    }

    private void writeVehicleStatus(Long vehicleId, boolean online, Instant timestamp) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point
                .measurement("vehicle_status")
                .addTag("vehicle_id", String.valueOf(vehicleId))
                .addField("online", online ? 1 : 0)
                .time(timestamp, WritePrecision.MS);
            writeApi.writePoint(bucket, org, point);
        } catch (Exception e) {
            System.err.println("[InfluxDB] Failed to write vehicle status for " + vehicleId + ": " + e.getMessage());
        }
    }

    public void writeVehicleDistance(Long vehicleId, Double distance, Instant timestamp) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point
                .measurement("vehicle_distance")
                .addTag("vehicle_id", String.valueOf(vehicleId))
                .addField("distance", distance)
                .time(timestamp, WritePrecision.MS);
            writeApi.writePoint(bucket, org, point);
        } catch (Exception e) {
            System.err.println("[InfluxDB] Failed to write vehicle distance for " + vehicleId + ": " + e.getMessage());
        }
    }
}