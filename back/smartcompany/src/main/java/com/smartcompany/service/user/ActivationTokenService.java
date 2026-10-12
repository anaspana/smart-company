package com.smartcompany.service.user;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class ActivationTokenService {

    public String generateActivationToken() {
        String token = UUID.randomUUID().toString();
        return token;
    }

}