package com.smartcompany.dto.factory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartcompany.dto.location.CityDto;
import com.smartcompany.dto.location.CountryDto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Fabrika")
public record FactoryDto(
    @Schema(description = "ID fabrike") Long id,
    @Schema(description = "Naziv fabrike") String name,
    @Schema(description = "Adresa fabrike") String address,
    @Schema(description = "Država") CountryDto country,
    @Schema(description = "Grad") CityDto city,
    @Schema(description = "Geografska širina") Double latitude,
    @Schema(description = "Geografska dužina") Double longitude,
    @JsonProperty("isOnline") @Schema(description = "Da li je simulator trenutno na mreži") boolean isOnline,
    @Schema(description = "Vreme poslednjeg heartbeat-a (ISO-8601)") String lastHeartbeat,
    @Schema(description = "S3 ključevi slika fabrike") List<String> imageKeys,
    @Schema(description = "ID-jevi proizvoda koje fabrika proizvodi") List<Long> productIds
) {}