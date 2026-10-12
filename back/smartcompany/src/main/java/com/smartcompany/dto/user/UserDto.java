package com.smartcompany.dto.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smartcompany.model.user.UserType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Podaci o prijavljenom korisniku")
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties({"authorities", "enabled", "accountNonLocked", "credentialsNonExpired", "accountNonExpired", "password"})
public class UserDto implements UserDetails {

    @Schema(description = "Korisničko ime") private String username;
    @Schema(description = "JWT token") private String token;
    @Schema(description = "Tip korisnika") private UserType type;
    @JsonProperty("isActive") @Schema(description = "Da li je nalog aktivan") private boolean isActive;
    @JsonProperty("mustChangePassword") @Schema(description = "Da li mora da promeni lozinku") private boolean mustChangePassword;
    @Schema(description = "Email adresa") private String email;
    @Schema(description = "S3 ključ profilne slike") private String image;
    @Schema(hidden = true) private String activationToken;

    @Override
    @JsonDeserialize(using = CustomAuthorityDeserializer.class)
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new SimpleGrantedAuthority("ROLE_" + this.getType()));
        return list;
    }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return username; }
}