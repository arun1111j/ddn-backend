package com.notarize.controller;

import com.notarize.repository.NotaryRepository;
import com.notarize.service.BlockchainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private BlockchainService blockchainService;
    @Autowired
    private NotaryRepository notaryRepository;

    @GetMapping("/blockchain")
    public ResponseEntity<Map<String, Object>> getBlockchainInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            // Basic connection info
            info.put("blockchainConnected", blockchainService.isBlockchainConnected());
            info.put("contractLoaded", blockchainService.isContractLoaded());
            info.put("contractAddress", blockchainService.getContractAddress());
            info.put("currentAccount", blockchainService.getCurrentAccountAddress());

            // Balance information
            BigInteger balance = blockchainService.getCurrentAccountBalance();
            info.put("currentAccountBalance", balance.toString());
            info.put("currentAccountBalanceETH",
                    new BigDecimal(balance).divide(new BigDecimal("1000000000000000000")));

            // Contract information
            if (blockchainService.isContractLoaded()) {
                try {
                    BigInteger stakeAmount = blockchainService.getStakeAmount();
                    info.put("stakeAmount", stakeAmount.toString());
                    info.put("stakeAmountETH",
                            new BigDecimal(stakeAmount).divide(new BigDecimal("1000000000000000000")));

                    BigInteger slashPercentage = blockchainService.getSlashPercentage();
                    info.put("slashPercentage", slashPercentage.toString() + "%");

                } catch (Exception e) {
                    info.put("contractInfoError", e.getMessage());
                }
            }

            // Block information
            try {
                BigInteger blockNumber = blockchainService.getCurrentBlockNumber();
                info.put("currentBlock", blockNumber.toString());
            } catch (Exception e) {
                info.put("blockInfoError", e.getMessage());
            }

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            log.error("Error getting blockchain info", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get blockchain info: " + e.getMessage()));
        }
    }

    @GetMapping("/contract")
    public ResponseEntity<Map<String, Object>> getContractInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            if (!blockchainService.isContractLoaded()) {
                return ResponseEntity.ok(Map.of("error", "Contract not loaded"));
            }

            // Get detailed contract information
            BigInteger stakeAmount = blockchainService.getStakeAmount();
            BigInteger slashPercentage = blockchainService.getSlashPercentage();

            info.put("stakeAmountWEI", stakeAmount.toString());
            info.put("stakeAmountETH",
                    new BigDecimal(stakeAmount).divide(new BigDecimal("1000000000000000000")));
            info.put("slashPercentage", slashPercentage.toString() + "%");
            info.put("contractAddress", blockchainService.getContractAddress());

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            log.error("Error getting contract info", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get contract info: " + e.getMessage()));
        }
    }

    @GetMapping("/notary-status")
    public ResponseEntity<?> checkNotaryStatus(@RequestParam String address) {
        try {
            Map<String, Object> status = new HashMap<>();

            // Check if registered on blockchain
            boolean onBlockchain = blockchainService.isNotaryRegisteredOnBlockchain(address);
            status.put("registeredOnBlockchain", onBlockchain);

            // Check if registered in database
            boolean inDatabase = notaryRepository.existsByNotaryAddress(address);
            status.put("registeredInDatabase", inDatabase);

            // Get current account
            status.put("currentSender", blockchainService.getCurrentAccountAddress());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalanceInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            BigInteger balance = blockchainService.getCurrentAccountBalance();
            BigInteger balanceETH = balance.divide(new BigInteger("1000000000000000000"));

            info.put("balanceWEI", balance.toString());
            info.put("balanceETH", balanceETH.toString());
            info.put("account", blockchainService.getCurrentAccountAddress());

            // Check if we have enough for 1 ETH stake + gas
            BigInteger oneETH = new BigInteger("1000000000000000000");
            BigInteger estimatedGas = new BigInteger("20000000000000000"); // 0.02 ETH
            BigInteger totalNeeded = oneETH.add(estimatedGas);

            info.put("hasEnoughFor1ETHStake", balance.compareTo(totalNeeded) > 0);
            info.put("requiredFor1ETHStake", totalNeeded.toString());

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            log.error("Error getting balance info", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get balance info: " + e.getMessage()));
        }
    }

    @GetMapping("/contract-status")
    public ResponseEntity<?> getContractStatus(@RequestParam String address) {
        try {
            log.info("Checking contract status for address: {}", address);

            Map<String, Object> status = new HashMap<>();

            // Check contract basics
            status.put("contractAddress", blockchainService.getContractAddress());

            // Check stake amount
            try {
                BigInteger stakeAmount = blockchainService.getStakeAmount();
                status.put("stakeAmount", stakeAmount.toString());
                status.put("stakeAmountETH", convertWeiToEth(stakeAmount));
            } catch (Exception e) {
                status.put("stakeAmountError", e.getMessage());
            }

            // Check if address is registered as notary
            try {
                boolean isNotary = blockchainService.isNotaryRegistered(address);
                status.put("isNotaryRegistered", isNotary);
            } catch (Exception e) {
                status.put("notaryCheckError", e.getMessage());
            }

            // Check current block
            try {
                BigInteger blockNumber = blockchainService.getCurrentBlockNumber();
                status.put("currentBlock", blockNumber.toString());
            } catch (Exception e) {
                status.put("blockNumberError", e.getMessage());
            }

            // Test document existence with random hash
            try {
                String testHash = "test_document_hash_" + System.currentTimeMillis();
                boolean docExists = blockchainService.documentExists(testHash);
                status.put("testDocumentExists", docExists);
            } catch (Exception e) {
                status.put("documentCheckError", e.getMessage());
            }

            log.info("Contract status check completed");
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Error checking contract status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse("Debug failed: " + e.getMessage()));
        }
    }

    @GetMapping("/test-connection")
    public ResponseEntity<?> testConnection() {
        try {
            Map<String, Object> result = new HashMap<>();

            // Test basic connection
            result.put("connected", blockchainService.isConnected());
            result.put("contractAddress", blockchainService.getContractAddress());

            // Test contract call
            try {
                BigInteger stake = blockchainService.getStakeAmount();
                result.put("stakeAmount", stake.toString());
                result.put("contractWorking", true);
            } catch (Exception e) {
                result.put("contractWorking", false);
                result.put("contractError", e.getMessage());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Connection test failed: " + e.getMessage()));
        }
    }

    @PostMapping("/test-registration")
    public ResponseEntity<?> testDocumentRegistration(@RequestParam String ipfsCid) {
        try {
            log.info("Testing document registration with IPFS CID: {}", ipfsCid);

            Map<String, Object> result = new HashMap<>();

            // Try to register a test document using correct 2-parameter method
            TransactionReceipt receipt = blockchainService.registerDocument(ipfsCid, "Test Document");

            result.put("success", true);
            result.put("transactionHash", receipt.getTransactionHash());
            result.put("message", "Document registered successfully");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Test registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse("Test registration failed: " + e.getMessage()));
        }
    }

    private String convertWeiToEth(BigInteger wei) {
        try {
            // Simple conversion: 1 ETH = 10^18 WEI
            BigDecimal weiDecimal = new BigDecimal(wei);
            BigDecimal ethDecimal = weiDecimal.divide(new BigDecimal("1000000000000000000"));
            return ethDecimal.toString() + " ETH";
        } catch (Exception e) {
            return "Conversion failed";
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return error;
    }
}