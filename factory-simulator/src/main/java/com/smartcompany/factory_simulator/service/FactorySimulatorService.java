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
@ConditionalOnProperty(name = "simulator.factory.enabled", havingValue = "true", matchIfMissing = false)
public class FactorySimulatorService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${simulator.factory.id:0}")
    private Long factoryId;

    @Value("${simulator.factory.name:disabled}")
    private String factoryName;

    // Format: "productId:minQty:maxQty,productId:minQty:maxQty"
    @Value("${simulator.products:1:0:0}")
    private String productsConfig;

    @Value("${simulator.production.test.mode:false}")
    private boolean testMode;

    private final Random random = new Random();
    // Lista proizvoda: [productId, minQty, maxQty]
    private List<long[]> products = new ArrayList<>();

    @PostConstruct
    public void init() {
        // Parsiranje konfiguracije proizvoda
        for (String entry : productsConfig.split(",")) {
            String[] parts = entry.trim().split(":");
            if (parts.length == 3) {
                products.add(new long[]{
                        Long.parseLong(parts[0]),
                        Long.parseLong(parts[1]),
                        Long.parseLong(parts[2])
                });
            }
        }
        System.out.println("[Simulator] Started factory '" + factoryName + "' (id=" + factoryId + ")");
        System.out.println("[Simulator] Products: " + products.size());
        System.out.println("[Simulator] Test mode: " + testMode);
    }

    // ─── Heartbeat ────────────────────────────────────────────────────────────

    @Scheduled(fixedDelayString = "${simulator.heartbeat.interval:30000}")
    public void sendHeartbeat() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("factoryId", factoryId);
        msg.put("timestamp", Instant.now().toString());

        rabbitTemplate.convertAndSend(
                SimulatorRabbitConfig.FACTORY_EXCHANGE,
                "factory.heartbeat",
                msg
        );
        System.out.println("[Simulator] Heartbeat sent to " + "factory " + factoryId + " at: " + Instant.now());
    }

    // Production report (test mode: svaki minut)

    @Scheduled(fixedDelayString = "${simulator.production.test.interval:60000}")
    public void sendProductionReportTestMode() {
        if (!testMode) return;
        sendProductionReport();
    }

    // Production report (production mode: 2x dnevno podne i ponoć)

    @Scheduled(cron = "0 0 0,12 * * *")
    public void sendProductionReportScheduled() {
        if (testMode) return;
        sendProductionReport();
    }

    private void sendProductionReport() {
        for (long[] product : products) {
            long productId = product[0];
            long minQty    = product[1];
            long maxQty    = product[2];
            int quantity   = (int) (minQty + (long) (random.nextDouble() * (maxQty - minQty)));

            Map<String, Object> msg = new HashMap<>();
            msg.put("factoryId", factoryId);
            msg.put("productId", productId);
            msg.put("quantity", quantity);
            msg.put("timestamp", Instant.now().toString());

            rabbitTemplate.convertAndSend(
                    SimulatorRabbitConfig.FACTORY_EXCHANGE,
                    "factory.production",
                    msg
            );
            System.out.println("[Simulator] Production report: product=" + productId + ", qty=" + quantity);
        }
    }
}
