package com.smartcompany.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartcompany.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Account Activation", description = "Aktivacija naloga preko linka poslatog na email")
public class ActivationTokenController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Aktivacija naloga", description = "Javni endpoint")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nalog uspešno aktiviran"),
        @ApiResponse(responseCode = "400", description = "Token je nevalidan, istekao, ili je nalog već aktivan")
    })
    @SecurityRequirements
    @PostMapping("/activate/{username}")
    public ResponseEntity<String> activateAccount(@RequestParam String token, @PathVariable String username) {
        if (userService.validateActivationToken(username, token)) {
            userService.activateUser(username);
            return ResponseEntity.ok("Account activated successfully!");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired activation token.");
    }
}