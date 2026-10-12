package com.smartcompany.dto.vehicle;

import java.time.Instant;

public record VehicleLocationMessage(
    Long vehicleId,
    Double latitude,
    Double longitude,
    Double distanceTraveled, // km od prethodnog javljanja
    Instant timestamp
) {}