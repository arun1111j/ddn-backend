package com.notarize.service;

import com.notarize.contracts.DocumentNotarization;
import com.notarize.util.ContractDeployerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;

import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class AutomatedContractService {

    private final Web3j web3j;
    private final ContractDeployerUtil contractDeployerUtil;
    private final ContractDeploymentService contractDeploymentService;

    @Value("${web3j.contract-address:}")
    private String configuredContractAddress;

    @Value("${web3j.auto-deploy:true}")
    private boolean autoDeploy;

    @Value("${web3j.max-retries:3}")
    private int maxRetries;

    @Value("${web3j.deploy-new-contract:false}")
    private boolean shouldDeployNewContract;

    private DocumentNotarization contract;
    private String currentContractAddress;
    private boolean contractReady = false;

    public AutomatedContractService(Web3j web3j,
                                    ContractDeployerUtil contractDeployerUtil,
                                    ContractDeploymentService contractDeploymentService) {
        this.web3j = web3j;
        this.contractDeployerUtil = contractDeployerUtil;
        this.contractDeploymentService = contractDeploymentService;
    }

    @PostConstruct
    public void initialize() {
        log.info("🤖 Starting automated contract deployment...");

        try {
            if (!contractDeployerUtil.isBlockchainConnected()) {
                log.error("❌ Blockchain not connected. Application will start without contract.");
                return;
            }

            // Try to initialize contract with retry logic
            initializeContractWithRetry();

        } catch (Exception e) {
            log.error("❌ Automated contract initialization failed: {}", e.getMessage());
            log.warn("⚠️ Application starting without blockchain features");
        }
    }

    private void initializeContractWithRetry() {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("🔄 Contract initialization attempt {}/{}", attempt, maxRetries);

                if (tryLoadExistingContract() || tryDeployNewContract()) {
                    contractReady = true;
                    log.info("✅ Contract automation completed successfully");
                    printContractInfo();
                    return;
                }

                if (attempt < maxRetries) {
                    log.warn("⚠️ Attempt {} failed, retrying in 5 seconds...", attempt);
                    Thread.sleep(5000);
                }

            } catch (Exception e) {
                log.error("❌ Attempt {} failed: {}", attempt, e.getMessage());
            }
        }

        log.error("❌ All contract initialization attempts failed");
    }

    private boolean tryLoadExistingContract() {
        try {
            // Try ContractDeploymentService first
            if (contractDeploymentService.initialize(configuredContractAddress, false)) {
                this.contract = contractDeploymentService.getContract();
                this.currentContractAddress = contractDeploymentService.getContractAddress();
                log.info("✅ Successfully loaded contract via ContractDeploymentService");
                return true;
            }

            // Fallback to ContractDeployerUtil - FIXED METHOD CALLS
            if (tryLoadWithContractDeployerUtil()) {
                this.contract = contractDeployerUtil.getContract();
                this.currentContractAddress = contractDeployerUtil.getContractAddress();
                log.info("✅ Successfully loaded contract via ContractDeployerUtil");
                return true;
            }

        } catch (Exception e) {
            log.warn("❌ Failed to load existing contract: {}", e.getMessage());
        }
        return false;
    }

    private boolean tryLoadWithContractDeployerUtil() {
        try {
            // First try the configured address
            if (configuredContractAddress != null && !configuredContractAddress.trim().isEmpty()) {
                if (contractDeployerUtil.isValidContractAddress(configuredContractAddress)) {
                    contractDeployerUtil.loadExistingContract(configuredContractAddress);
                    return true;
                } else {
                    log.warn("⚠️ Configured contract address is invalid: {}", configuredContractAddress);
                }
            }

            // Then try address from config file
            String savedAddress = contractDeployerUtil.loadContractAddressFromConfig();
            if (savedAddress != null && contractDeployerUtil.isValidContractAddress(savedAddress)) {
                contractDeployerUtil.loadExistingContract(savedAddress);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.warn("Failed to load contract with ContractDeployerUtil: {}", e.getMessage());
            return false;
        }
    }

    private boolean tryDeployNewContract() {
        if (!autoDeploy && !shouldDeployNewContract) {
            log.info("⏸️ Auto-deployment disabled, skipping new deployment");
            return false;
        }

        try {
            log.info("🚀 Starting new contract deployment...");

            // Try ContractDeploymentService first
            if (contractDeploymentService.initialize(null, true)) {
                this.contract = contractDeploymentService.getContract();
                this.currentContractAddress = contractDeploymentService.getContractAddress();
                log.info("✅ Successfully deployed new contract via ContractDeploymentService: {}", currentContractAddress);
                return true;
            }

            // Fallback to ContractDeployerUtil
            String newAddress = contractDeployerUtil.deployNewContractMethod();
            this.contract = contractDeployerUtil.getContract();
            this.currentContractAddress = newAddress;
            log.info("✅ Successfully deployed new contract via ContractDeployerUtil: {}", newAddress);
            return true;

        } catch (Exception e) {
            log.error("❌ New contract deployment failed: {}", e.getMessage());
            return false;
        }
    }

    private void printContractInfo() {
        log.info("=== CONTRACT DEPLOYMENT SUMMARY ===");
        log.info("📍 Contract Address: {}", currentContractAddress);
        log.info("✅ Status: {}", contractReady ? "READY" : "NOT READY");
        log.info("🔗 Network: {}", contractDeployerUtil.isLocalNetwork() ? "Local Ganache" : "Remote Network");
        log.info("🎯 Primary Service: {}", getPrimaryService());

        // Additional debug info
        log.info("🔧 ContractDeployerUtil Initialized: {}", contractDeployerUtil.isContractInitialized());
        log.info("🔧 ContractDeploymentService Deployed: {}", contractDeploymentService.isDeployed());
    }

    private String getPrimaryService() {
        if (contractDeploymentService.isDeployed() && contractDeploymentService.getContractAddress() != null) {
            return "ContractDeploymentService";
        } else if (contractDeployerUtil.isContractInitialized()) {
            return "ContractDeployerUtil";
        }
        return "None";
    }

    // Getters and status methods
    public DocumentNotarization getContract() {
        if (!contractReady) {
            throw new IllegalStateException("Contract not ready");
        }

        // Return contract from the service that actually has it
        if (contractDeploymentService.isDeployed()) {
            return contractDeploymentService.getContract();
        } else if (contractDeployerUtil.isContractInitialized()) {
            return contractDeployerUtil.getContract();
        }

        throw new IllegalStateException("No contract available");
    }

    public String getContractAddress() {
        if (contractDeploymentService.isDeployed() && contractDeploymentService.getContractAddress() != null) {
            return contractDeploymentService.getContractAddress();
        } else if (contractDeployerUtil.isContractInitialized()) {
            return contractDeployerUtil.getContractAddress();
        }
        return currentContractAddress;
    }

    public boolean isContractReady() {
        return contractReady;
    }

    public String getContractStatus() {
        if (!contractReady) return "NOT_READY";

        String address = getContractAddress();
        return "READY - " + (address != null ? address.substring(0, 10) + "..." : "UNKNOWN");
    }

    public String getActiveService() {
        return getPrimaryService();
    }

    /**
     * Get detailed service status for debugging
     */
    public String getDetailedStatus() {
        return String.format(
                "AutomatedContractService Status: %s%n" +
                        "ContractDeploymentService: %s (%s)%n" +
                        "ContractDeployerUtil: %s (%s)%n" +
                        "Contract Address: %s",
                contractReady ? "READY" : "NOT_READY",
                contractDeploymentService.isDeployed() ? "ACTIVE" : "INACTIVE",
                contractDeploymentService.getContractAddress(),
                contractDeployerUtil.isContractInitialized() ? "ACTIVE" : "INACTIVE",
                contractDeployerUtil.getContractAddress(),
                getContractAddress()
        );
    }
}