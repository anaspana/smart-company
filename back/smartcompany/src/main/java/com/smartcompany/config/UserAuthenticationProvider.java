package com.smartcompany.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.smartcompany.dto.user.SignUpDto;
import com.smartcompany.dto.user.UserDto;
import com.smartcompany.model.user.UserType;
import com.smartcompany.service.user.UserService;

import jakarta.annotation.PostConstruct;

@Component
public class UserAuthenticationProvider {

    @Value("${security.jwt.token.secret-key:secret-key}")
    private String secretKey;

    @Autowired
    private UserService userService;

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());

        List<UserDto> currentUserList = userService.getAll();
        if (currentUserList.isEmpty()) {
            String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz";
            StringBuilder salt = new StringBuilder();
            Random rnd = new Random();
            while (salt.length() < 20) {
                int index = (int) (rnd.nextFloat() * SALTCHARS.length());
                salt.append(SALTCHARS.charAt(index));
            }
            String saltStr = salt.toString();

            SignUpDto signUpDto = new SignUpDto(
                    "admin", saltStr.toCharArray(), UserType.SuperAdmin,
                    true, true,
                    "admin@gmail.com", "profile.jpg", null);
            userService.registerSuperAdmin(signUpDto);

            try {
                File file = new File("superadmin.txt");
                file.createNewFile();
                FileWriter myWriter = new FileWriter(file);
                myWriter.write(saltStr);
                myWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String createToken(UserDto user) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + 3600000);

        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create()
                .withSubject(user.getUsername())
                .withIssuedAt(now)
                .withExpiresAt(validity)
                .withClaim("email", user.getEmail())
                .withClaim("type", user.getType().toString())
                .withClaim("isActive", String.valueOf(user.isActive()))
                .withClaim("mustChangePassword", String.valueOf(user.isMustChangePassword()))
                .withClaim("image", String.valueOf(user.getImage()))
                .withClaim("activationToken", String.valueOf(user.getActivationToken()))
                .sign(algorithm);
    }

    public Authentication validateToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT decoded = verifier.verify(token);

        UserDto user = new UserDto(
                decoded.getSubject(), null,
                UserType.valueOf(decoded.getClaim("type").asString()),
                Boolean.parseBoolean(decoded.getClaim("isActive").asString()),
                Boolean.parseBoolean(decoded.getClaim("mustChangePassword").asString()),
                decoded.getClaim("email").asString(),
                decoded.getClaim("image").asString(),
                decoded.getClaim("activationToken").asString()
        );

        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    public Authentication validateTokenStrongly(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT decoded = verifier.verify(token);
        UserDto user = userService.findByUsername(decoded.getSubject());
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}