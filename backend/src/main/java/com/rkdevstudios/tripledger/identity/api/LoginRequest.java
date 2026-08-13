package com.rkdevstudios.tripledger.identity.api;

public record LoginRequest(
        String email,
        String password
) {
}