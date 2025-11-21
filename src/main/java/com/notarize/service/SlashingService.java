package com.notarize.service;

import com.notarize.dto.DocumentDTO;
import com.notarize.model.Notary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SlashingService {

    @Autowired
    private NotaryService notaryService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private BlockchainService blockchainService;

    private Map<String, LocalDateTime> notarizationAttempts = new HashMap<>();
    private Map<String, Integer> notaryErrorCounts = new HashMap<>();

    // Detect double notarization attempts
    public void detectDoubleNotarization(String documentHash, String notaryAddress) {
        String key = documentHash + "-" + notaryAddress;
        if (notarizationAttempts.containsKey(key)) {
            // Notary trying to notarize same document twice
            log.warn("Double notarization attempt detected for notary: {}", notaryAddress);
            slashNotary(notaryAddress, documentHash, "DOUBLE_NOTARIZATION");
        }
        notarizationAttempts.put(key, LocalDateTime.now());

        // Clean old entries (older than 24 hours)
        cleanOldAttempts();
    }

    // Verify document integrity and slash if compromised
    public void verifyDocumentAndSlash(String documentHash) {
        try {
            DocumentDTO document = documentService.getDocument(documentHash);
            String downloadedContent = documentService.downloadFromIpfs(document.getIpfsHash());
            boolean isIntegrityValid = documentService.verifyDocumentIntegrity(documentHash, downloadedContent);

            if (!isIntegrityValid) {
                log.error("Document integrity compromised: {}", documentHash);
                for (String notaryAddress : document.getNotaryAddresses()) {
                    slashNotary(notaryAddress, documentHash, "INTEGRITY_VIOLATION");
                }
            }
        } catch (Exception e) {
            log.error("Error verifying document integrity for {}: {}", documentHash, e.getMessage());
            // Optionally, you might want to handle this differently
            // For example, increment error count for associated notaries
        }
    }

    // Monitor notary performance
    public void monitorNotaryPerformance(String notaryAddress) {
        try {
            Notary notary = notaryService.getNotary(notaryAddress);
            double errorRate = calculateErrorRate(notaryAddress);

            if (errorRate > 0.1) { // 10% error rate threshold
                log.warn("Notary {} has high error rate: {}", notaryAddress, errorRate);
                // Could trigger temporary suspension or increased monitoring
            }
        } catch (Exception e) {
            log.error("Error monitoring notary performance: {}", e.getMessage());
        }
    }

    // Main slashing method
    public void slashNotary(String notaryAddress, String documentHash, String reason) {
        try {
            log.info("Slashing notary {} for reason: {}", notaryAddress, reason);

            // Record the slashing event
            incrementErrorCount(notaryAddress);

            // Execute slash on blockchain
            notaryService.slashNotary(notaryAddress, documentHash);

            // Log the slashing event for audit
            logSlashingEvent(notaryAddress, documentHash, reason);

        } catch (Exception e) {
            log.error("Error slashing notary {}: {}", notaryAddress, e.getMessage());
        }
    }

    // Scheduled tasks for continuous monitoring
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void scheduledIntegrityCheck() {
        log.info("Running scheduled document integrity checks");
        // Check a sample of documents periodically
        // This would be more sophisticated in production
    }

    @Scheduled(fixedRate = 600000) // 10 minutes
    public void scheduledNotaryMonitoring() {
        log.info("Running scheduled notary performance monitoring");
        // Monitor all active notaries
    }

    private double calculateErrorRate(String notaryAddress) {
        int errorCount = notaryErrorCounts.getOrDefault(notaryAddress, 0);
        // This would need total notarization count from blockchain
        return errorCount / 100.0; // Simplified calculation
    }

    private void incrementErrorCount(String notaryAddress) {
        notaryErrorCounts.put(notaryAddress,
                notaryErrorCounts.getOrDefault(notaryAddress, 0) + 1);
    }

    private void cleanOldAttempts() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        notarizationAttempts.entrySet().removeIf(entry ->
                entry.getValue().isBefore(twentyFourHoursAgo));
    }

    private void logSlashingEvent(String notaryAddress, String documentHash, String reason) {
        // This would save to a dedicated slashing events table
        log.info("SLASHING_EVENT - Notary: {}, Document: {}, Reason: {}",
                notaryAddress, documentHash, reason);
    }
}