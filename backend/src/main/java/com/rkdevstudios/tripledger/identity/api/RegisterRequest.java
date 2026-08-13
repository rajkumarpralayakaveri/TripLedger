package com.rkdevstudios.tripledger.identity.api;

public record RegisterRequest(
    String name,
    String email,
    String password,
    String avatarUrl
) {}
