package com.asc.studycompanion.common.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Map;

/**
 * Lightweight unauthenticated container healthcheck endpoint verifying JVM and PostgreSQL readiness.
 */
@RestController
@RequestMapping("/api/healthcheck")
@RequiredArgsConstructor
public class HealthcheckController {

    private final DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthcheck() {
        boolean dbUp = checkDatabase();
        HttpStatus status = dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(status).body(Map.of(
                "status", dbUp ? "UP" : "DOWN",
                "database", dbUp ? "UP" : "DOWN",
                "timestamp", Instant.now().toString()
        ));
    }

    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT 1");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next();
        } catch (Exception ex) {
            return false;
        }
    }
}
