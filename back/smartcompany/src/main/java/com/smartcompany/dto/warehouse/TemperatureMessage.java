package com.smartcompany.dto.warehouse;

import java.time.Instant;

public record TemperatureMessage(
    Long warehouseId,
    Long sectorId,
    Double temperature,
    Instant timestamp
) {}