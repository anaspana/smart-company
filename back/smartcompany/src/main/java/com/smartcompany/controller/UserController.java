package com.smartcompany.controller;

import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.smartcompany.config.UserAuthenticationProvider;
import com.smartcompany.dto.user.ChangePasswordDto;
import com.smartcompany.dto.user.CredentialsDto;
import com.smartcompany.dto.user.SignUpDto;
import com.smartcompany.dto.user.UserDto;
import com.smartcompany.exception.AppException;
import com.smartcompany.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Tag(name = "Users", description = "Upravljanje korisnicima, prijava i registracija")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserAuthenticationProvider userAuthenticationProvider;

    @Operation(summary = "Dohvata korisnika po korisničkom imenu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Korisnik pronađen"),
        @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen")
    })
    @GetMapping(value = "/users/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDto> getUser(@PathVariable("username") String username) {
        UserDto user = userService.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        user.setToken("");
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @Operation(summary = "Dohvata listu menadžera sa pretragom i paginacijom", description = "Zahteva SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stranica menadžera vraćena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup"),
        @ApiResponse(responseCode = "403", description = "Nedovoljna prava pristupa")
    })
    @GetMapping(value = "/users/managers", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SuperAdmin')")
    public ResponseEntity<Page<UserDto>> getManagers(
            @Parameter(description = "Pretraga po korisničkom imenu") @RequestParam(defaultValue = "") String search,
            @Parameter(description = "Broj stranice") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Veličina stranice") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        Page<UserDto> managers = userService.getManagers(search, pageable);
        return ResponseEntity.ok(managers);
    }

    @Operation(summary = "Blokira menadžera", description = "Zahteva SuperAdmin rolu. SuperAdmin se ne može blokirati.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menadžer blokiran"),
        @ApiResponse(responseCode = "403", description = "Pokušaj blokiranja SuperAdmin naloga"),
        @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PutMapping(value = "/users/{username}/block")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ResponseEntity<UserDto> blockManager(@PathVariable String username) {
        UserDto updated = userService.blockManager(username);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Odblokirava menadžera", description = "Zahteva SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menadžer odblokiran"),
        @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PutMapping(value = "/users/{username}/unblock")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ResponseEntity<UserDto> unblockManager(@PathVariable String username) {
        UserDto updated = userService.unblockManager(username);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Prijava korisnika", description = "Javni endpoint")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prijava uspešna, vraća JWT token"),
        @ApiResponse(responseCode = "400", description = "Pogrešna lozinka"),
        @ApiResponse(responseCode = "403", description = "Nalog je blokiran ili nije aktiviran"),
        @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen")
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody @Valid CredentialsDto credentialsDto) {
        UserDto userDto = userService.login(credentialsDto);
        userDto.setToken(userAuthenticationProvider.createToken(userDto));
        return ResponseEntity.ok(userDto);
    }

    @Operation(summary = "Registracija kupca", description = "Javni endpoint. Šalje aktivacioni email.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Kupac registrovan, nalog neaktivan do aktivacije"),
        @ApiResponse(responseCode = "400", description = "Korisničko ime već zauzeto ili nevalidan zahtev")
    })
    @SecurityRequirements
    @PostMapping("/register/customer")
    public ResponseEntity<UserDto> register(@RequestBody @Valid SignUpDto user) {
        UserDto createdUser = userService.registerCustomer(user);
        createdUser.setToken(userAuthenticationProvider.createToken(createdUser));
        return ResponseEntity.created(URI.create("/api/users/" + createdUser.getUsername())).body(createdUser);
    }

    @Operation(summary = "Provera da li je korisničko ime zauzeto", description = "Javni endpoint, koristi se u formi registracije")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Korisničko ime je slobodno"),
        @ApiResponse(responseCode = "409", description = "Korisničko ime je zauzeto")
    })
    @SecurityRequirements
    @GetMapping("/users/check-username")
    public ResponseEntity<Void> checkUsername(@Parameter(description = "Korisničko ime za proveru") @RequestParam String username) {
        if (userService.isUsernameTaken(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Registracija menadžera", description = "Zahteva SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Menadžer registrovan"),
        @ApiResponse(responseCode = "400", description = "Korisničko ime već zauzeto ili nevalidan zahtev"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PostMapping("/register/manager")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ResponseEntity<UserDto> registerManager(@RequestBody @Valid SignUpDto user) {
        UserDto createdUser;
        try {
            createdUser = userService.registerManager(user);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.created(URI.create("/api/users/" + createdUser.getUsername())).body(createdUser);
    }

    @Operation(summary = "Ažuriranje korisnika")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Korisnik ažuriran"),
        @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PutMapping(value = "/account/{username}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDto> update(@RequestBody @Valid SignUpDto signUpDto, @PathVariable String username)
            throws Exception {
        UserDto updatedUser = userService.update(signUpDto, username);
        updatedUser.setToken(userAuthenticationProvider.createToken(updatedUser));
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Promena lozinke prijavljenog korisnika")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lozinka promenjena, vraća novi token"),
        @ApiResponse(responseCode = "400", description = "Nevalidna nova lozinka"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PutMapping("/account/change-password")
    public ResponseEntity<UserDto> changePassword(@Valid @RequestBody ChangePasswordDto dto) {
        UserDto updatedUser = userService.changePasswordAndReturn(dto);
        String newToken = userAuthenticationProvider.createToken(updatedUser);
        updatedUser.setToken(newToken);
        return ResponseEntity.ok(updatedUser);
    }
}