package com.smartcompany.dto.factory;

import java.time.Instant;

public record ProductionMessage(
        Long factoryId,
        Long productId,
        Integer quantity,
        Instant timestamp
) {}