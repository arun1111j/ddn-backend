package com.notarize.controller;

import com.notarize.service.AutomatedContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final AutomatedContractService contractService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        // Basic application health
        health.put("status", "UP");
        health.put("service", "document-notarization");

        // Blockchain health
        Map<String, Object> blockchain = new HashMap<>();
        blockchain.put("contractReady", contractService.isContractReady());
        blockchain.put("contractStatus", contractService.getContractStatus());
        blockchain.put("contractAddress", contractService.getContractAddress());

        health.put("blockchain", blockchain);
        health.put("timestamp", java.time.Instant.now().toString());

        return ResponseEntity.ok(health);
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> readiness = new HashMap<>();

        boolean contractReady = contractService.isContractReady();
        readiness.put("status", contractReady ? "READY" : "NOT_READY");
        readiness.put("contractAvailable", contractReady);

        if (contractReady) {
            return ResponseEntity.ok(readiness);
        } else {
            return ResponseEntity.status(503).body(readiness);
        }
    }
}