package com.smartcompany.dto.factory;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Agregirani podaci proizvodnje za grafik (5.10)")
public record ProductionDataDto(
    @Schema(description = "Granularnost", allowableValues = {"day", "week", "month"}) String granularity,
    @Schema(description = "Labele za x-osu") List<String> labels,
    @Schema(description = "Vrednosti za y-osu (proizvedene jedinice)") List<Long> values
) {}