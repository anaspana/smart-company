package com.smartcompany.listener;

import com.smartcompany.config.RabbitMQConfig;
import com.smartcompany.dto.factory.HeartbeatMessage;
import com.smartcompany.dto.factory.ProductionMessage;
import com.smartcompany.exception.AppException;
import com.smartcompany.model.factory.Factory;
import com.smartcompany.model.factory.ProductionRecord;
import com.smartcompany.model.product.Product;
import com.smartcompany.repository.factory.FactoryRepository;
import com.smartcompany.repository.factory.ProductionRecordRepository;
import com.smartcompany.repository.product.ProductRepository;
import com.smartcompany.service.influx.InfluxDBService;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class FactoryMessageListener {

    @Autowired
    private FactoryRepository factoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductionRecordRepository productionRecordRepository;

    @Autowired
    private InfluxDBService influxDBService;

    /**
     * Prima heartbeat poruku od simulatora.
     * 1. Ažurira isOnline=true i lastHeartbeat u PostgreSQL
     * 2. Beleži online event u InfluxDB
     */
    @RabbitListener(queues = RabbitMQConfig.FACTORY_HEARTBEAT_QUEUE)
    @Transactional
    public void handleHeartbeat(HeartbeatMessage msg) {
        try {
            // PESIMISTIČKO ZAKLJUČAVANJE: findByIdForUpdate radi SELECT ... FOR
            // UPDATE — sprečava da se ovaj heartbeat i checkHeartbeats() sudare
            // nad isOnline/lastHeartbeat iste fabrike.
            Factory factory = factoryRepository.findByIdForUpdate(msg.factoryId())
                    .orElseThrow(() -> new AppException("Factory not found: " + msg.factoryId(), HttpStatus.NOT_FOUND));

            factory.setOnline(true);
            factory.setLastHeartbeat(Instant.now());
            factoryRepository.save(factory);

            influxDBService.writeHeartbeat(msg.factoryId(), true, msg.timestamp());

        } catch (AppException e) {
            System.err.println("[Heartbeat] " + e.getMessage());
        } catch (Exception e) {
            // Ovo hvata i lock timeout (CannotAcquireLockException /
            // PessimisticLockingFailureException) — poruka će se requeue-ovati
            // od strane Spring AMQP-a i pokušati ponovo, što je ok jer je
            // situacija tranzijentna.
            System.err.println("[Heartbeat] Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Prima production report od simulatora.
     * 1. Čuva ProductionRecord u PostgreSQL
     * 2. Beleži u InfluxDB
     */
    @RabbitListener(queues = RabbitMQConfig.FACTORY_PRODUCTION_QUEUE)
    @Transactional
    public void handleProduction(ProductionMessage msg) {
        try {
            Factory factory = factoryRepository.findById(msg.factoryId())
                    .orElseThrow(() -> new AppException("Factory not found: " + msg.factoryId(), HttpStatus.NOT_FOUND));

            Product product = productRepository.findById(msg.productId())
                    .orElseThrow(() -> new AppException("Product not found: " + msg.productId(), HttpStatus.NOT_FOUND));

            // PostgreSQLproduction record
            ProductionRecord record = new ProductionRecord();
            record.setFactory(factory);
            record.setProduct(product);
            record.setQuantity(msg.quantity());
            record.setTimestamp(msg.timestamp() != null ? msg.timestamp() : Instant.now());
            productionRecordRepository.save(record);

            // InfluxDB
            influxDBService.writeProduction(
                    msg.factoryId(),
                    msg.productId(),
                    msg.quantity(),
                    record.getTimestamp()
            );

        } catch (AppException e) {
            System.err.println("[Production] " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Production] Unexpected error: " + e.getMessage());
        }
    }
}