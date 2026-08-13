package com.rkdevstudios.tripledger.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping(ApiRoutes.API_V1 + "/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, String>> getHealth() {
        return ApiResponse.success(Map.of(
            "status", "UP",
            "application", "TripLedger Backend",
            "version", "v1"
        ));
    }
}
