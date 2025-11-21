package com.notarize.controller;

import com.notarize.dto.NotaryRegistrationDTO;
import com.notarize.model.Notary;
import com.notarize.service.NotaryService;
import com.notarize.service.SlashingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notaries")
public class NotaryController {

    @Autowired
    private NotaryService notaryService;

    @Autowired
    private SlashingService slashingService;

    @PostMapping("/register")
    public ResponseEntity<Notary> registerNotary(@RequestBody NotaryRegistrationDTO registrationDTO) {
        try {
            Notary notary = notaryService.registerNotary(registrationDTO);
            return ResponseEntity.ok(notary);
        } catch (Exception e) {
            log.error("Error registering notary", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Enhanced slash endpoint with reason
    @PostMapping("/slash")
    public ResponseEntity<Void> slashNotary(
            @RequestParam String notaryAddress,
            @RequestParam String documentHash,
            @RequestParam(required = false) String reason) {
        try {
            String slashReason = (reason != null) ? reason : "MANUAL_SLASH";
            slashingService.slashNotary(notaryAddress, documentHash, slashReason);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error slashing notary", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Quick slash without reason (backward compatibility)
    @PostMapping("/slash/quick")
    public ResponseEntity<Void> slashNotaryQuick(
            @RequestParam String notaryAddress,
            @RequestParam String documentHash) {
        try {
            notaryService.slashNotary(notaryAddress, documentHash);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error slashing notary", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{notaryAddress}")
    public ResponseEntity<Notary> getNotary(@PathVariable String notaryAddress) {
        try {
            Notary notary = notaryService.getNotary(notaryAddress);
            return ResponseEntity.ok(notary);
        } catch (Exception e) {
            log.error("Error getting notary", e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<Notary>> getActiveNotaries() {
        try {
            List<Notary> notaries = notaryService.getActiveNotaries();
            return ResponseEntity.ok(notaries);
        } catch (Exception e) {
            log.error("Error getting active notaries", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Check if notary is active
    @GetMapping("/{notaryAddress}/active")
    public ResponseEntity<Boolean> isNotaryActive(@PathVariable String notaryAddress) {
        try {
            boolean isActive = notaryService.isNotaryActive(notaryAddress);
            return ResponseEntity.ok(isActive);
        } catch (Exception e) {
            log.error("Error checking notary status", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Get notary reputation score
    @GetMapping("/{notaryAddress}/reputation")
    public ResponseEntity<Double> getNotaryReputation(@PathVariable String notaryAddress) {
        try {
            double reputation = notaryService.calculateNotaryReputation(notaryAddress);
            return ResponseEntity.ok(reputation);
        } catch (Exception e) {
            log.error("Error getting notary reputation", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Monitor notary performance
    @PostMapping("/{notaryAddress}/monitor")
    public ResponseEntity<Void> monitorNotary(@PathVariable String notaryAddress) {
        try {
            slashingService.monitorNotaryPerformance(notaryAddress);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error monitoring notary", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Withdraw stake (for notaries)
    @PostMapping("/{notaryAddress}/withdraw-stake")
    public ResponseEntity<Void> withdrawStake(@PathVariable String notaryAddress) {
        try {
            notaryService.withdrawStake(notaryAddress);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error withdrawing stake", e);
            return ResponseEntity.badRequest().build();
        }
    }
}