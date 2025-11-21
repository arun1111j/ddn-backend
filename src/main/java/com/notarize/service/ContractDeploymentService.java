package com.notarize.service;

import com.notarize.contracts.DocumentNotarization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;

@Slf4j
@Service
public class ContractDeploymentService {

    @Autowired
    private Web3j web3j;

    @Autowired
    private Credentials credentials;

    @Autowired
    private StaticGasProvider gasProvider;

    private DocumentNotarization contract;
    private String contractAddress;

    /**
     * Initialize contract - called by AutomatedContractService
     */
    public boolean initialize(String existingContractAddress, boolean shouldDeploy) {
        try {
            if (shouldDeploy || existingContractAddress == null || existingContractAddress.isEmpty()) {
                log.info("🚀 Deploying new smart contract...");
                return deployContract();
            } else {
                log.info("📋 Loading existing contract at: {}", existingContractAddress);
                return loadContract(existingContractAddress);
            }

        } catch (Exception e) {
            log.error("❌ Contract initialization failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Deploy new contract to the network
     */
    private boolean deployContract() throws Exception {
        log.info("⏳ Deploying DocumentNotarization contract...");

        // Check balance
        BigInteger balance = web3j.ethGetBalance(credentials.getAddress(),
                        org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                .send()
                .getBalance();

        log.info("💰 Account balance: {} ETH",
                new java.math.BigDecimal(balance).divide(
                        new java.math.BigDecimal("1000000000000000000")));

        if (balance.compareTo(BigInteger.ZERO) <= 0) {
            throw new RuntimeException("❌ Insufficient balance!");
        }

        log.info("📤 Sending deployment transaction...");

        contract = DocumentNotarization.deploy(
                web3j,
                credentials,
                gasProvider
        ).send();

        this.contractAddress = contract.getContractAddress();

        log.info("✅ Contract deployed successfully!");
        log.info("📋 Contract Address: {}", contractAddress);

        // Get transaction details
        java.util.Optional<TransactionReceipt> receiptOpt = contract.getTransactionReceipt();
        if (receiptOpt.isPresent()) {
            TransactionReceipt receipt = receiptOpt.get();
            log.info("🔗 Transaction Hash: {}", receipt.getTransactionHash());
            log.info("⛽ Gas Used: {}", receipt.getGasUsed());
            log.info("🔢 Block Number: {}", receipt.getBlockNumber());
        }

        // Save to file for future use
        saveContractAddress(contractAddress);

        log.warn("⚠️  IMPORTANT: Update application.properties:");
        log.warn("   web3j.contract-address={}", contractAddress);

        return testContract();
    }

    /**
     * Load existing contract
     */
    private boolean loadContract(String address) {
        try {
            log.info("📥 Loading contract from address: {}", address);

            contract = DocumentNotarization.load(
                    address,
                    web3j,
                    credentials,
                    gasProvider
            );
            this.contractAddress = address;

            log.info("✅ Contract loaded successfully");
            return testContract();

        } catch (Exception e) {
            log.error("❌ Failed to load contract: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Test contract functionality
     */
    private boolean testContract() {
        try {
            log.info("🧪 Testing contract connection...");

            BigInteger stakeAmount = contract.STAKE_AMOUNT().send();
            log.info("✅ Stake amount: {} wei ({} ETH)",
                    stakeAmount,
                    new java.math.BigDecimal(stakeAmount)
                            .divide(new java.math.BigDecimal("1000000000000000000")));

            BigInteger slashPercent = contract.SLASH_PERCENTAGE().send();
            log.info("✅ Slash percentage: {}%", slashPercent);

            log.info("🎉 Contract is ready for use!");
            return true;

        } catch (Exception e) {
            log.error("❌ Contract test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Save contract address to file for easy retrieval
     */
    private void saveContractAddress(String address) {
        try {
            java.nio.file.Path configPath = java.nio.file.Paths.get("deployed-contract.txt");
            String content = String.format(
                    "Contract Address: %s\n" +
                            "Deployed At: %s\n" +
                            "Network: Local Ganache\n",
                    address,
                    java.time.LocalDateTime.now()
            );
            java.nio.file.Files.writeString(configPath, content);
            log.info("💾 Contract info saved to deployed-contract.txt");
        } catch (Exception e) {
            log.warn("Could not save contract address: {}", e.getMessage());
        }
    }

    // ========== PUBLIC GETTERS ==========

    public DocumentNotarization getContract() {
        if (contract == null) {
            throw new IllegalStateException("Contract not initialized!");
        }
        return contract;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public boolean isContractReady() {
        if (contract == null) {
            return false;
        }

        try {
            return contract.isValid();
        } catch (Exception e) {
            log.error("Error checking contract validity: {}", e.getMessage());
            return false;
        }
    }

    public boolean isDeployed() {
        return contract != null;
    }
}