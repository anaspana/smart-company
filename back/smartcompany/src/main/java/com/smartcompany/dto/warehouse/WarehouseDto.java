package com.smartcompany.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartcompany.dto.location.CityDto;
import com.smartcompany.dto.location.CountryDto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Magacin")
public record WarehouseDto(
    @Schema(description = "ID magacina") Long id,
    @Schema(description = "Naziv magacina") String name,
    @Schema(description = "Adresa magacina") String address,
    @Schema(description = "Država") CountryDto country,
    @Schema(description = "Grad") CityDto city,
    @Schema(description = "Geografska širina") Double latitude,
    @Schema(description = "Geografska dužina") Double longitude,
    @JsonProperty("isOnline") @Schema(description = "Da li je simulator na mreži") boolean isOnline,
    @Schema(description = "Vreme poslednjeg heartbeat-a") String lastHeartbeat,
    @Schema(description = "S3 ključevi slika magacina") List<String> imageKeys,
    @Schema(description = "Sektori magacina") List<SectorDto> sectors
) {
    @Schema(description = "Sektor magacina sa poslednjim očitavanjem temperature")
    public record SectorDto(
        @Schema(description = "ID sektora") Long id,
        @Schema(description = "Naziv sektora", example = "Hladna komora A") String name,
        @Schema(description = "Poslednja očitana temperatura (°C)") Double lastTemperature,
        @Schema(description = "Vreme poslednjeg očitavanja") String lastMeasuredAt
    ) {}
}