package com.smartcompany.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Zahtev za promenu lozinke prijavljenog korisnika")
public record ChangePasswordDto(
    @Schema(description = "Nova lozinka")
    @NotBlank(message = "Password must not be blank")
    @Size(min = 5, message = "Password must be at least 5 characters long") String newPassword
) {}