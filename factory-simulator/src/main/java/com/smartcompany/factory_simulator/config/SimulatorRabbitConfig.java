package com.smartcompany.factory_simulator.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimulatorRabbitConfig {

    // Factory
    public static final String FACTORY_EXCHANGE         = "factory.exchange";
    public static final String FACTORY_HEARTBEAT_QUEUE  = "factory.heartbeat";
    public static final String FACTORY_PRODUCTION_QUEUE = "factory.production";

    // Warehouse
    public static final String WAREHOUSE_EXCHANGE          = "warehouse.exchange";
    public static final String WAREHOUSE_HEARTBEAT_QUEUE   = "warehouse.heartbeat";
    public static final String WAREHOUSE_TEMPERATURE_QUEUE = "warehouse.temperature";

    // Vehicle
    public static final String VEHICLE_EXCHANGE        = "vehicle.exchange";
    public static final String VEHICLE_HEARTBEAT_QUEUE = "vehicle.heartbeat";
    public static final String VEHICLE_LOCATION_QUEUE  = "vehicle.location";

    @Bean
    public TopicExchange factoryExchange() {
        return new TopicExchange(FACTORY_EXCHANGE);
    }

    @Bean
    public TopicExchange warehouseExchange() {
        return new TopicExchange(WAREHOUSE_EXCHANGE);
    }

    @Bean
    public TopicExchange vehicleExchange() {
        return new TopicExchange(VEHICLE_EXCHANGE);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}