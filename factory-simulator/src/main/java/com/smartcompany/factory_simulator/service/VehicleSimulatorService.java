package com.smartcompany.factory_simulator.service;

import java.time.Instant;
import java.util.HashMap;
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
@ConditionalOnProperty(name = "simulator.vehicle.enabled", havingValue = "true", matchIfMissing = false)
public class VehicleSimulatorService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${simulator.vehicle.id:0}")
    private Long vehicleId;

    @Value("${simulator.vehicle.plate:disabled}")
    private String licensePlate;

    @Value("${simulator.vehicle.min.distance:0.0}")
    private double minDistance;

    @Value("${simulator.vehicle.max.distance:0.0}")
    private double maxDistance;

    @Value("${simulator.vehicle.test.mode:false}")
    private boolean testMode;

    // Pocetna lokacija (menja se simulacijom kretanja)
    @Value("${simulator.vehicle.start.lat:0}")
    private double latitude;

    @Value("${simulator.vehicle.start.lng:0}")
    private double longitude;

    private final Random random = new Random();

    @PostConstruct
    public void init() {
        System.out.println("[VehicleSimulator] Started vehicle '" + licensePlate + "' (id=" + vehicleId + ")");
        System.out.println("[VehicleSimulator] Test mode: " + testMode);
    }

    // ─── Heartbeat ────────────────────────────────────────────────────────────

    @Scheduled(fixedDelayString = "${simulator.vehicle.heartbeat.interval:30000}")
    public void sendHeartbeat() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("vehicleId", vehicleId);
        msg.put("timestamp", Instant.now().toString());

        rabbitTemplate.convertAndSend(
            SimulatorRabbitConfig.VEHICLE_EXCHANGE,
            "vehicle.heartbeat",
            msg
        );
        System.out.println("[VehicleSimulator] Heartbeat sent for vehicle " + vehicleId);
    }

    // ─── Lokacija/distanca (test mode: svaki minut) ──────────────────────────

    @Scheduled(fixedDelayString = "${simulator.vehicle.test.interval:60000}")
    public void sendLocationTestMode() {
        if (!testMode) return;
        sendLocation();
    }

    // ─── Lokacija/distanca (production mode: svakih 10 minuta) ──────────────

    @Scheduled(cron = "0 */10 * * * *")
    public void sendLocationScheduled() {
        if (testMode) return;
        sendLocation();
    }

    private void sendLocation() {
        // Simuliraj kretanje — mala promena koordinata
        latitude  += (random.nextDouble() - 0.5) * 0.01;
        longitude += (random.nextDouble() - 0.5) * 0.01;

        double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
        distance = Math.round(distance * 10.0) / 10.0;

        Map<String, Object> msg = new HashMap<>();
        msg.put("vehicleId", vehicleId);
        msg.put("latitude", Math.round(latitude * 1_000_000.0) / 1_000_000.0);
        msg.put("longitude", Math.round(longitude * 1_000_000.0) / 1_000_000.0);
        msg.put("distanceTraveled", distance);
        msg.put("timestamp", Instant.now().toString());

        rabbitTemplate.convertAndSend(
            SimulatorRabbitConfig.VEHICLE_EXCHANGE,
            "vehicle.location",
            msg
        );
        System.out.println("[VehicleSimulator] Location: lat=" + latitude + ", lng=" + longitude + ", distance=" + distance + "km");
    }
}