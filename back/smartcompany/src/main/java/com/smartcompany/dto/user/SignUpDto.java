package com.smartcompany.dto.user;

import com.smartcompany.model.user.UserType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Podaci za registraciju korisnika (kupac ili menadžer)")
public record SignUpDto(
    @Schema(description = "Korisničko ime", example = "andrija123")
    @NotBlank(message = "Username je obavezan") String username,

    @Schema(description = "Lozinka")
    @NotNull(message = "Lozinka je obavezna")
    @Size(min = 5, message = "Lozinka mora imati bar 5 karaktera") char[] password,

    @Schema(description = "Tip korisnika")
    @NotNull(message = "Tip korisnika je obavezan") UserType type,

    @Schema(description = "Da li je nalog aktivan (postavlja server)") boolean isActive,
    @Schema(description = "Da li korisnik mora da promeni lozinku") boolean mustChangePassword,

    @Schema(description = "Email adresa", example = "korisnik@example.com")
    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email nije validan") String email,

    @Schema(description = "S3 ključ profilne slike") String image,
    @Schema(description = "Token za aktivaciju (postavlja server)") String activationToken
) {}