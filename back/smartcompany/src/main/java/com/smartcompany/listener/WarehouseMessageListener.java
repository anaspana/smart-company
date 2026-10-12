package com.smartcompany.listener;

import com.smartcompany.config.RabbitMQConfig;
import com.smartcompany.dto.warehouse.TemperatureMessage;
import com.smartcompany.dto.warehouse.WarehouseHeartbeatMessage;
import com.smartcompany.service.warehouse.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarehouseMessageListener {

    private final WarehouseService warehouseService;

    @RabbitListener(queues = RabbitMQConfig.WAREHOUSE_HEARTBEAT_QUEUE)
    public void handleHeartbeat(WarehouseHeartbeatMessage message) {
        warehouseService.handleHeartbeat(message.warehouseId());
    }

    @RabbitListener(queues = RabbitMQConfig.WAREHOUSE_TEMPERATURE_QUEUE)
    public void handleTemperature(TemperatureMessage message) {
        warehouseService.handleTemperature(message);
    }
}