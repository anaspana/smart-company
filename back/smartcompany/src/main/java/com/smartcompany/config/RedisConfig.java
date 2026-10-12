package com.smartcompany.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.smartcompany.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.math.")
                .allowIfSubType("org.springframework.data.domain.")
                .build();

        // PROPERTY stil — @class polje na svakom objektu. VAŽNO: ovo radi
        // ispravno samo za NE-final konkretne klase (npr. ArrayList). Nikad
        // ne keširati rezultat .toList() (vraća final ImmutableCollections
        // klasu) — koristiti .collect(Collectors.toList()) umesto toga.
        mapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);

        return mapper;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("categories", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("products", defaultConfig.entryTtl(Duration.ofSeconds(60)));
        cacheConfigs.put("product", defaultConfig.entryTtl(Duration.ofSeconds(60)));
        cacheConfigs.put("factories", defaultConfig.entryTtl(Duration.ofSeconds(60)));
        cacheConfigs.put("warehouses", defaultConfig.entryTtl(Duration.ofSeconds(60)));
        cacheConfigs.put("vehicles", defaultConfig.entryTtl(Duration.ofSeconds(60)));
        cacheConfigs.put("managers", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("users", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("countries", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("cities", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("vehicle-brands", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("vehicle-models", defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}