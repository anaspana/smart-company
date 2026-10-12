package com.smartcompany.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Agregirani podaci pređene distance za grafik (5.12)")
public record DistanceDataDto(
    @Schema(description = "Granularnost", allowableValues = {"day", "week", "month"}) String granularity,
    @Schema(description = "Labele za x-osu") List<String> labels,
    @Schema(description = "Pređena distanca u km po periodu") List<Double> values
) {}