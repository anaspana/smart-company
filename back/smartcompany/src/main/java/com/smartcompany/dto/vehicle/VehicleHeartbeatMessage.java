package com.smartcompany.dto.vehicle;

import java.time.Instant;

public record VehicleHeartbeatMessage(
    Long vehicleId,
    Instant timestamp
) {}