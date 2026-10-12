package com.smartcompany.listener;

import com.smartcompany.config.RabbitMQConfig;
import com.smartcompany.dto.vehicle.VehicleHeartbeatMessage;
import com.smartcompany.dto.vehicle.VehicleLocationMessage;
import com.smartcompany.service.vehicle.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleMessageListener {

    private final VehicleService vehicleService;

    @RabbitListener(queues = RabbitMQConfig.VEHICLE_HEARTBEAT_QUEUE)
    public void handleHeartbeat(VehicleHeartbeatMessage message) {
        vehicleService.handleHeartbeat(message.vehicleId());
    }

    @RabbitListener(queues = RabbitMQConfig.VEHICLE_LOCATION_QUEUE)
    public void handleLocation(VehicleLocationMessage message) {
        vehicleService.handleLocation(message);
    }
}