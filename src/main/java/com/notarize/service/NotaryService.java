package com.notarize.service;

import com.notarize.dto.NotaryRegistrationDTO;
import com.notarize.model.Notary;
import com.notarize.model.Document;
import com.notarize.repository.NotaryRepository;
import com.notarize.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
public class NotaryService {

    @Autowired
    private NotaryRepository notaryRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private BlockchainService blockchainService;

    @Value("${notary.stake-amount:1.0}")
    private BigDecimal stakeAmount;

    public Notary registerNotary(NotaryRegistrationDTO registrationDTO) throws Exception {
        log.info("🎯 STARTING NOTARY REGISTRATION");
        log.info("📍 Notary: {} | Address: {}",
                registrationDTO.getName(), registrationDTO.getNotaryAddress());

        // Check database
        if (notaryRepository.existsByNotaryAddress(registrationDTO.getNotaryAddress())) {
            throw new RuntimeException("Notary already registered in database");
        }

        // Check blockchain
        if (blockchainService.isNotaryRegisteredOnBlockchain(registrationDTO.getNotaryAddress())) {
            throw new RuntimeException("Notary already registered on blockchain");
        }

        // Use exact stake amount required by contract
        BigInteger stakeInWei = new BigInteger("1000000000000000000"); // 1 ETH in wei
        log.info("💰 Using fixed stake: {} wei (1 ETH)", stakeInWei);

        try {
            // Register on blockchain
            TransactionReceipt receipt = blockchainService.registerNotary(
                    registrationDTO.getNotaryAddress(),
                    registrationDTO.getName(),
                    stakeInWei
            );

            log.info("✅ Blockchain registration successful!");

            // Save to database
            Notary notary = new Notary();
            notary.setNotaryAddress(registrationDTO.getNotaryAddress());
            notary.setName(registrationDTO.getName());
            notary.setActive(true);
            notary.setStakeAmount(new BigDecimal("1.0"));
            notary.setSuccessfulNotarizations(0);
            notary.setSlashedCount(0);

            Notary savedNotary = notaryRepository.save(notary);
            log.info("🎉 Notary registered successfully. ID: {}", savedNotary.getId());

            return savedNotary;

        } catch (Exception e) {
            log.error("❌ Registration failed: {}", e.getMessage());
            throw new RuntimeException("Failed to register notary: " + e.getMessage(), e);
        }
    }

    /**
     * Alternative method: Slash notary directly using IPFS CID
     */
    public void slashNotaryByIpfsCid(String notaryAddress, String ipfsCid) throws Exception {
        Notary notary = notaryRepository.findByNotaryAddress(notaryAddress)
                .orElseThrow(() -> new RuntimeException("Notary not found"));

        // Validate IPFS CID format
        blockchainService.validateIpfsCid(ipfsCid);

        // Slash on blockchain using IPFS CID directly
        TransactionReceipt receipt = blockchainService.slashNotary(notaryAddress, ipfsCid);

        // Update local record
        notary.setSlashedCount(notary.getSlashedCount() + 1);
        notary.setStakeAmount(notary.getStakeAmount().multiply(BigDecimal.valueOf(0.9))); // 10% slash

        if (notary.getStakeAmount().compareTo(stakeAmount.divide(BigDecimal.valueOf(2))) < 0) {
            notary.setActive(false);
            log.info("Notary {} deactivated due to low stake", notaryAddress);
        }

        notaryRepository.save(notary);
        log.info("Notary {} slashed for IPFS CID {}. Blockchain TX: {}",
                notaryAddress, ipfsCid, receipt.getTransactionHash());
    }

    public Notary getNotary(String notaryAddress) {
        return notaryRepository.findByNotaryAddress(notaryAddress)
                .orElseThrow(() -> new RuntimeException("Notary not found"));
    }

    public List<Notary> getActiveNotaries() {
        return notaryRepository.findByActiveTrue();
    }

    public boolean isNotaryActive(String notaryAddress) {
        try {
            Notary notary = getNotary(notaryAddress);
            return notary.isActive();
        } catch (Exception e) {
            log.warn("Notary not found or inactive: {}", notaryAddress);
            return false;
        }
    }

    public double calculateNotaryReputation(String notaryAddress) {
        try {
            Notary notary = getNotary(notaryAddress);

            int totalOperations = notary.getSuccessfulNotarizations() + notary.getSlashedCount();
            if (totalOperations == 0) {
                return 100.0; // Default reputation for new notaries
            }

            double reputation = (double) notary.getSuccessfulNotarizations() / totalOperations * 100;
            return Math.max(0.0, Math.min(100.0, reputation)); // Clamp between 0-100
        } catch (Exception e) {
            log.error("Error calculating reputation for notary {}: {}", notaryAddress, e.getMessage());
            return 0.0;
        }
    }

    public void withdrawStake(String notaryAddress) throws Exception {
        Notary notary = notaryRepository.findByNotaryAddress(notaryAddress)
                .orElseThrow(() -> new RuntimeException("Notary not found"));

        if (notary.isActive()) {
            throw new RuntimeException("Active notaries cannot withdraw stake. Deactivate first.");
        }

        if (notary.getStakeAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("No stake available to withdraw");
        }

        log.info("Initiating stake withdrawal for notary: {}", notaryAddress);

        // Call blockchain withdraw method
        TransactionReceipt receipt = blockchainService.withdrawStake();

        BigDecimal withdrawnAmount = notary.getStakeAmount();
        notary.setStakeAmount(BigDecimal.ZERO);
        notaryRepository.save(notary);

        log.info("Stake withdrawal completed for notary: {}. Amount: {}. TX: {}",
                notaryAddress, withdrawnAmount, receipt.getTransactionHash());
    }

    public void incrementSuccessfulNotarizations(String notaryAddress) {
        try {
            Notary notary = getNotary(notaryAddress);
            notary.setSuccessfulNotarizations(notary.getSuccessfulNotarizations() + 1);
            notaryRepository.save(notary);
            log.debug("Incremented successful notarizations for notary: {}", notaryAddress);
        } catch (Exception e) {
            log.error("Error incrementing notarizations for notary {}: {}", notaryAddress, e.getMessage());
        }
    }

    public NotaryStatistics getNotaryStatistics(String notaryAddress) {
        try {
            Notary notary = getNotary(notaryAddress);
            NotaryStatistics stats = new NotaryStatistics();
            stats.setNotaryAddress(notaryAddress);
            stats.setName(notary.getName());
            stats.setActive(notary.isActive());
            stats.setStakeAmount(notary.getStakeAmount());
            stats.setSuccessfulNotarizations(notary.getSuccessfulNotarizations());
            stats.setSlashedCount(notary.getSlashedCount());
            stats.setReputationScore(calculateNotaryReputation(notaryAddress));
            return stats;
        } catch (Exception e) {
            log.error("Error getting statistics for notary {}: {}", notaryAddress, e.getMessage());
            return null;
        }
    }
    /**
     * Slash notary using document hash (looks up IPFS CID from database)
     */
    public void slashNotary(String notaryAddress, String documentHash) throws Exception {
        Notary notary = notaryRepository.findByNotaryAddress(notaryAddress)
                .orElseThrow(() -> new RuntimeException("Notary not found"));

        // Get document to retrieve IPFS CID
        Document document = documentRepository.findByDocumentHash(documentHash)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        String ipfsCid = document.getIpfsHash();
        if (ipfsCid == null || ipfsCid.isEmpty()) {
            throw new RuntimeException("Document has no IPFS CID");
        }

        // Validate IPFS CID format
        blockchainService.validateIpfsCid(ipfsCid);

        // Slash on blockchain using IPFS CID directly
        TransactionReceipt receipt = blockchainService.slashNotary(notaryAddress, ipfsCid);

        // Update local record
        notary.setSlashedCount(notary.getSlashedCount() + 1);
        notary.setStakeAmount(notary.getStakeAmount().multiply(BigDecimal.valueOf(0.9))); // 10% slash

        if (notary.getStakeAmount().compareTo(stakeAmount.divide(BigDecimal.valueOf(2))) < 0) {
            notary.setActive(false);
            log.info("Notary {} deactivated due to low stake", notaryAddress);
        }

        notaryRepository.save(notary);
        log.info("Notary {} slashed for document {} (IPFS CID: {}). Blockchain TX: {}",
                notaryAddress, documentHash, ipfsCid, receipt.getTransactionHash());
    }

    public void deactivateNotary(String notaryAddress) {
        try {
            Notary notary = getNotary(notaryAddress);
            notary.setActive(false);
            notaryRepository.save(notary);
            log.info("Notary deactivated: {}", notaryAddress);
        } catch (Exception e) {
            log.error("Error deactivating notary {}: {}", notaryAddress, e.getMessage());
            throw new RuntimeException("Failed to deactivate notary");
        }
    }

    public void addStake(String notaryAddress, BigDecimal additionalStake) throws Exception {
        Notary notary = getNotary(notaryAddress);

        // Convert additional stake to Wei
        BigInteger additionalStakeWei = Convert.toWei(additionalStake, Convert.Unit.ETHER).toBigInteger();

        // Note: You would need to add an addStake method to your smart contract and BlockchainService
        // For now, just update local database
        BigDecimal newStakeAmount = notary.getStakeAmount().add(additionalStake);
        notary.setStakeAmount(newStakeAmount);

        // Reactivate if stake meets minimum requirement
        if (!notary.isActive() && newStakeAmount.compareTo(stakeAmount) >= 0) {
            notary.setActive(true);
            log.info("Notary {} reactivated with additional stake", notaryAddress);
        }

        notaryRepository.save(notary);
        log.info("Additional stake added for notary {}. New stake: {}", notaryAddress, newStakeAmount);
    }

    // Inner class for notary statistics
    public static class NotaryStatistics {
        private String notaryAddress;
        private String name;
        private boolean active;
        private BigDecimal stakeAmount;
        private int successfulNotarizations;
        private int slashedCount;
        private double reputationScore;

        public String getNotaryAddress() { return notaryAddress; }
        public void setNotaryAddress(String notaryAddress) { this.notaryAddress = notaryAddress; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public BigDecimal getStakeAmount() { return stakeAmount; }
        public void setStakeAmount(BigDecimal stakeAmount) { this.stakeAmount = stakeAmount; }

        public int getSuccessfulNotarizations() { return successfulNotarizations; }
        public void setSuccessfulNotarizations(int successfulNotarizations) { this.successfulNotarizations = successfulNotarizations; }

        public int getSlashedCount() { return slashedCount; }
        public void setSlashedCount(int slashedCount) { this.slashedCount = slashedCount; }

        public double getReputationScore() { return reputationScore; }
        public void setReputationScore(double reputationScore) { this.reputationScore = reputationScore; }
    }
}