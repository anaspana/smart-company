package com.smartcompany.dto.factory;

import java.time.Instant;

public record HeartbeatMessage(
        Long factoryId,
        Instant timestamp
) {}