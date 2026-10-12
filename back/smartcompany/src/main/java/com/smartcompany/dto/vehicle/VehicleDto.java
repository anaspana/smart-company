package com.smartcompany.dto.vehicle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Dostavno vozilo")
public record VehicleDto(
    @Schema(description = "ID vozila") Long id,
    @Schema(description = "Registarska oznaka", example = "NS-123-AB") String licensePlate,
    @Schema(description = "Ograničenje težine u kg") Double weightLimit,
    @Schema(description = "Marka") VehicleBrandDto brand,
    @Schema(description = "Model") VehicleModelDto model,
    @Schema(description = "Poslednja poznata geografska širina") Double latitude,
    @Schema(description = "Poslednja poznata geografska dužina") Double longitude,
    @JsonProperty("isOnline") @Schema(description = "Da li je simulator na mreži") boolean isOnline,
    @Schema(description = "Vreme poslednjeg heartbeat-a") String lastHeartbeat,
    @Schema(description = "S3 ključevi slika vozila") List<String> imageKeys
) {}