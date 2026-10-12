package com.smartcompany.dto.warehouse;

import java.time.Instant;

public record WarehouseHeartbeatMessage(
    Long warehouseId,
    Instant timestamp
) {}