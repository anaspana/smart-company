package com.smartcompany.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Podaci za prijavu")
public record CredentialsDto(
    @Schema(description = "Korisničko ime", example = "andrija123") @NotBlank String username,
    @Schema(description = "Lozinka") @NotNull char[] password
) {}