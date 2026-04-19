package com.example.demo.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight endpoint used by the frontend keep-alive ping.
 * Prevents Render free tier from spinning down the service.
 * Must be permitted in SecurityConfig (no auth required).
 */
@RestController
@RequestMapping("/api/auth")
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}