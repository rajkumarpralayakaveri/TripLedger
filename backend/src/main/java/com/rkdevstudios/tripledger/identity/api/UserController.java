package com.rkdevstudios.tripledger.identity.api;

import com.rkdevstudios.tripledger.common.ApiRoutes;
import com.rkdevstudios.tripledger.common.ApiResponse;
import com.rkdevstudios.tripledger.identity.application.UserService;
import com.rkdevstudios.tripledger.identity.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiRoutes.API_V1 + "/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> getMe() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "Not authenticated"));
        }
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
        )));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUser(@PathVariable("id") String id) {
        return userService.getUserById(id)
            .map(user -> ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
            ))))
            .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("USER_NOT_FOUND", "User not found")));
    }
}
