package com.rkdevstudios.tripledger.identity.api;

public record LogoutRequest(
        String refreshToken
) {
}