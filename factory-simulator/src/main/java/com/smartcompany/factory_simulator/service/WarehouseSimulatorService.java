package com.smartcompany.factory_simulator.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.smartcompany.factory_simulator.config.SimulatorRabbitConfig;

import jakarta.annotation.PostConstruct;

@Service
@ConditionalOnProperty(name = "simulator.warehouse.enabled", havingValue = "true", matchIfMissing = false)
public class WarehouseSimulatorService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${simulator.warehouse.id:0}")
    private Long warehouseId;

    @Value("${simulator.warehouse.name:disabled}")
    private String warehouseName;

    // Format: "sectorId:minTemp:maxTemp,sectorId:minTemp:maxTemp"
    @Value("${simulator.warehouse.sectors:1:0.0:0.0}")
    private String sectorsConfig;

    @Value("${simulator.warehouse.test.mode:false}")
    private boolean testMode;

    private final Random random = new Random();
    // [sectorId, minTemp*10, maxTemp*10] — množimo sa 10 da radimo sa intovima
    private List<long[]> sectors = new ArrayList<>();

    @PostConstruct
    public void init() {
        for (String entry : sectorsConfig.split(",")) {
            String[] parts = entry.trim().split(":");
            if (parts.length == 3) {
                sectors.add(new long[]{
                    Long.parseLong(parts[0]),
                    (long)(Double.parseDouble(parts[1]) * 10),
                    (long)(Double.parseDouble(parts[2]) * 10)
                });
            }
        }
        System.out.println("[WarehouseSimulator] Started warehouse '" + warehouseName + "' (id=" + warehouseId + ")");
        System.out.println("[WarehouseSimulator] Sectors: " + sectors.size());
        System.out.println("[WarehouseSimulator] Test mode: " + testMode);
    }

    // ─── Heartbeat ────────────────────────────────────────────────────────────

    @Scheduled(fixedDelayString = "${simulator.warehouse.heartbeat.interval:30000}")
    public void sendHeartbeat() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("warehouseId", warehouseId);
        msg.put("timestamp", Instant.now().toString());

        rabbitTemplate.convertAndSend(
            SimulatorRabbitConfig.WAREHOUSE_EXCHANGE,
            "warehouse.heartbeat",
            msg
        );
        System.out.println("[WarehouseSimulator] Heartbeat sent for warehouse " + warehouseId);
    }

    // ─── Temperatura (test mode: svaki minut) ────────────────────────────────

    @Scheduled(fixedDelayString = "${simulator.warehouse.test.interval:60000}")
    public void sendTemperatureTestMode() {
        if (!testMode) return;
        sendTemperature();
    }

    // ─── Temperatura (production mode: svakih 10 minuta) ────────────────────

    @Scheduled(cron = "0 */10 * * * *")
    public void sendTemperatureScheduled() {
        if (testMode) return;
        sendTemperature();
    }

    private void sendTemperature() {
        for (long[] sector : sectors) {
            long sectorId = sector[0];
            double minTemp = sector[1] / 10.0;
            double maxTemp = sector[2] / 10.0;
            double temperature = minTemp + random.nextDouble() * (maxTemp - minTemp);
            temperature = Math.round(temperature * 10.0) / 10.0;

            Map<String, Object> msg = new HashMap<>();
            msg.put("warehouseId", warehouseId);
            msg.put("sectorId", sectorId);
            msg.put("temperature", temperature);
            msg.put("timestamp", Instant.now().toString());

            rabbitTemplate.convertAndSend(
                SimulatorRabbitConfig.WAREHOUSE_EXCHANGE,
                "warehouse.temperature",
                msg
            );
            System.out.println("[WarehouseSimulator] Temperature: sector=" + sectorId + ", temp=" + temperature + "°C");
        }
    }
}