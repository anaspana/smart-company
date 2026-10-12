package com.smartcompany.dto.company;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Odluka menadžera o zahtevu za registraciju firme")
public record CompanyDecisionDto(
    @Schema(description = "true = odobri, false = odbij", example = "true") boolean approve,
    @Schema(description = "Razlog odbijanja — obavezan ako je approve=false", example = "Dokumenti nisu čitljivi") String rejectionReason
) {}