package com.rkdevstudios.tripledger.identity.api;

import com.rkdevstudios.tripledger.common.ApiResponse;
import com.rkdevstudios.tripledger.common.ApiRoutes;
import com.rkdevstudios.tripledger.identity.application.UserService;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.identity.security.JwtService;
import com.rkdevstudios.tripledger.identity.security.RefreshToken;
import com.rkdevstudios.tripledger.identity.security.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.API_V1 + "/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthController(
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(
            @RequestBody RegisterRequest request
    ) {

        try {

            User user = userService.registerUser(
                    request.name(),
                    request.email(),
                    request.password(),
                    request.avatarUrl()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(Map.of(
                            "id", user.getId(),
                            "name", user.getName(),
                            "email", user.getEmail()
                    )));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "REGISTRATION_FAILED",
                            e.getMessage()
                    ));
        }
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @RequestBody LoginRequest request
    ) {

        Optional<User> userOpt = userService.getUserByEmail(request.email());

        if (userOpt.isEmpty() ||
                !passwordEncoder.matches(
                        request.password(),
                        userOpt.get().getPasswordHash())) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(
                            "UNAUTHORIZED",
                            "Invalid email or password"
                    ));
        }

        User user = userOpt.get();

        String accessToken = jwtService.generateToken(user.getEmail());

        String refreshTokenString = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenString,
                user,
                Instant.now().plusSeconds(604800)
        );

        refreshTokenRepository.save(refreshToken);

        return ResponseEntity.ok(
                ApiResponse.success(Map.of(
                        "accessToken", accessToken,
                        "refreshToken", refreshTokenString,
                        "user", Map.of(
                                "id", user.getId(),
                                "name", user.getName(),
                                "email", user.getEmail(),
                                "avatarUrl",
                                user.getAvatarUrl() == null ? "" : user.getAvatarUrl()
                        )
                ))
        );
    }

    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(
            @RequestBody RefreshTokenRequest request
    ) {

        Optional<RefreshToken> tokenOpt =
                refreshTokenRepository.findByToken(request.refreshToken());

        if (tokenOpt.isEmpty() ||
                tokenOpt.get().getExpiryDate().isBefore(Instant.now())) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(
                            "UNAUTHORIZED",
                            "Invalid or expired refresh token"
                    ));
        }

        RefreshToken refreshToken = tokenOpt.get();
        User user = refreshToken.getUser();

        String accessToken = jwtService.generateToken(user.getEmail());

        refreshTokenRepository.delete(refreshToken);

        String newRefreshToken = UUID.randomUUID().toString();

        refreshTokenRepository.save(
                new RefreshToken(
                        newRefreshToken,
                        user,
                        Instant.now().plusSeconds(604800)
                )
        );

        return ResponseEntity.ok(
                ApiResponse.success(Map.of(
                        "accessToken", accessToken,
                        "refreshToken", newRefreshToken
                ))
        );
    }

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody LogoutRequest request
    ) {

        if (request.refreshToken() != null &&
                !request.refreshToken().isBlank()) {

            refreshTokenRepository.deleteById(request.refreshToken());
        }

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}