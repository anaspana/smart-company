package com.smartcompany.dto.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Agregirani podaci temperature za grafik (5.11)")
public record TemperatureDataDto(
    @Schema(description = "Granularnost", allowableValues = {"day", "week", "month"}) String granularity,
    @Schema(description = "Labele za x-osu") List<String> labels,
    @Schema(description = "Prosečna temperatura po periodu (°C)") List<Double> values
) {}