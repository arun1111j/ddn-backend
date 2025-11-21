package com.notarize.util;

import com.notarize.contracts.DocumentNotarization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert;

import java.math.BigInteger;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ContractDeployerUtil {

    private final Web3j web3j;
    private final Credentials credentials;

    @Value("${web3j.contract-config-file:contract-config.properties}")
    private String contractConfigFile;

    @Value("${web3j.gas-price:20000000000}")
    private BigInteger gasPrice;

    @Value("${web3j.gas-limit:6721975}")
    private BigInteger gasLimit;

    // Local development settings
    private static final BigInteger LOCAL_GAS_PRICE = BigInteger.valueOf(20_000_000_000L);
    private static final BigInteger LOCAL_GAS_LIMIT = BigInteger.valueOf(6_721_975L);
    private static final BigInteger MIN_BALANCE_LOCAL = Convert.toWei("0.01", Convert.Unit.ETHER).toBigInteger();

    private DocumentNotarization contract;
    private ContractGasProvider gasProvider;
    private boolean isLocalNetwork = false;
    private String contractAddress;

    public ContractDeployerUtil(Web3j web3j, Credentials credentials) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.gasProvider = new StaticGasProvider(
                gasPrice != null ? gasPrice : LOCAL_GAS_PRICE,
                gasLimit != null ? gasLimit : LOCAL_GAS_LIMIT
        );
    }

    /**
     * Initialize contract - called by AutomatedContractService
     */
    public boolean initializeContract(String configuredContractAddress, boolean autoDeploy) {
        try {
            log.info("🚀 Initializing smart contract for local development...");

            // Detect if we're using local network
            detectNetworkType();

            // Try to load existing contract first
            if (!autoDeploy && configuredContractAddress != null && !configuredContractAddress.isEmpty()) {
                if (isValidContractAddress(configuredContractAddress)) {
                    loadExistingContract(configuredContractAddress);
                    return true;
                } else {
                    log.warn("Configured contract address is invalid: {}", configuredContractAddress);
                }
            }

            // Try to load from config file
            String savedAddress = loadContractAddressFromConfig();
            if (savedAddress != null && isValidContractAddress(savedAddress)) {
                this.contractAddress = savedAddress;
                loadExistingContract(savedAddress);
                return true;
            }

            // Deploy new contract if auto-deploy enabled
            if (autoDeploy) {
                log.info("No valid contract address found, deploying new contract...");
                deployNewContractMethod();
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("❌ Failed to initialize smart contract", e);
            log.warn("⚠️ Contract initialization failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Detect if we're using local network
     */
    private void detectNetworkType() throws Exception {
        try {
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            isLocalNetwork = clientVersion.toLowerCase().contains("ganache") ||
                    clientVersion.toLowerCase().contains("testrpc") ||
                    clientVersion.contains("EthereumJS TestRPC");

            log.info("🌐 Network detected: {}", isLocalNetwork ? "Local Ganache" : "Remote Network");
            log.info("🔧 Client: {}", clientVersion);

        } catch (Exception e) {
            log.warn("Could not detect network type, assuming local: {}", e.getMessage());
            isLocalNetwork = true;
        }
    }

    /**
     * Deploy a new instance of the DocumentNotarization contract
     */
    public String deployNewContractMethod() throws Exception {
        log.info("🔄 Starting new contract deployment on {}...",
                isLocalNetwork ? "Local Ganache" : "Remote Network");

        // Check if account has sufficient balance
        BigInteger balance = getAccountBalance();
        BigInteger minBalance = isLocalNetwork ? MIN_BALANCE_LOCAL :
                Convert.toWei("0.1", Convert.Unit.ETHER).toBigInteger();

        if (balance.compareTo(minBalance) < 0) {
            String errorMsg = String.format(
                    "❌ Insufficient balance for deployment. Current: %s ETH, Required: > %s ETH",
                    Convert.fromWei(balance.toString(), Convert.Unit.ETHER),
                    Convert.fromWei(minBalance.toString(), Convert.Unit.ETHER)
            );

            if (isLocalNetwork) {
                log.warn("{} - But continuing anyway for local development", errorMsg);
            } else {
                throw new RuntimeException(errorMsg);
            }
        }

        log.info("👤 Deployer address: {}", credentials.getAddress());
        log.info("💰 Account balance: {} ETH", Convert.fromWei(balance.toString(), Convert.Unit.ETHER));
        log.info("📦 Gas Limit: {}", gasProvider.getGasLimit());

        // Use optimized gas for local development
        ContractGasProvider deploymentGasProvider = isLocalNetwork ?
                new StaticGasProvider(LOCAL_GAS_PRICE, LOCAL_GAS_LIMIT) : gasProvider;

        log.info("📄 Deploying DocumentNotarization contract...");

        // Deploy contract
        org.web3j.protocol.core.RemoteCall<DocumentNotarization> deployCall = DocumentNotarization.deploy(
                web3j,
                credentials,
                deploymentGasProvider
        );

        DocumentNotarization newContract = deployCall.send();

        this.contract = newContract;
        this.contractAddress = newContract.getContractAddress();

        log.info("✅ Contract deployed successfully!");
        log.info("📝 Contract Address: {}", contractAddress);

        // Get transaction receipt safely
        try {
            java.util.Optional<TransactionReceipt> receiptOptional = newContract.getTransactionReceipt();
            if (receiptOptional.isPresent()) {
                TransactionReceipt receipt = receiptOptional.get();
                log.info("🔗 Transaction Hash: {}", receipt.getTransactionHash());
                log.info("⛽ Gas Used: {}", receipt.getGasUsed());
                log.info("📦 Block Number: {}", receipt.getBlockNumber());

                if (receipt.getGasUsed() != null) {
                    BigInteger cost = receipt.getGasUsed().multiply(deploymentGasProvider.getGasPrice());
                    log.info("💵 Deployment Cost: {} ETH",
                            Convert.fromWei(cost.toString(), Convert.Unit.ETHER));
                }
            } else {
                log.warn("Transaction receipt not available yet");
            }
        } catch (Exception e) {
            log.warn("Could not get transaction receipt: {}", e.getMessage());
        }

        // Save contract address to config file
        saveContractAddressToConfig(contractAddress);

        // Verify contract deployment
        verifyContractDeployment();

        return contractAddress;
    }

    /**
     * Load existing contract from address
     */
    public void loadExistingContract(String address) throws Exception {
        log.info("📂 Loading existing contract at address: {}", address);

        this.contract = DocumentNotarization.load(
                address,
                web3j,
                credentials,
                gasProvider
        );
        this.contractAddress = address;

        // Verify the contract is valid
        verifyContractDeployment();

        log.info("✅ Contract loaded successfully from address: {}", address);
    }

    /**
     * Verify that the contract is properly deployed and accessible
     */
    private void verifyContractDeployment() throws Exception {
        if (contract == null) {
            throw new IllegalStateException("Contract is not initialized");
        }

        try {
            // For local development, use shorter timeout
            if (isLocalNetwork) {
                contract.STAKE_AMOUNT().send();
                log.info("✅ Local contract verification successful");
            } else {
                BigInteger stakeAmount = contract.STAKE_AMOUNT().send();
                BigInteger slashPercentage = contract.SLASH_PERCENTAGE().send();

                log.info("✅ Contract verification successful");
                log.info("   - Stake amount: {} WEI ({} ETH)", stakeAmount,
                        Convert.fromWei(stakeAmount.toString(), Convert.Unit.ETHER));
                log.info("   - Slash percentage: {}%", slashPercentage);
            }

        } catch (Exception e) {
            if (isLocalNetwork) {
                log.warn("⚠️ Contract verification had issues but continuing for local development: {}",
                        e.getMessage());
            } else {
                log.error("❌ Contract verification failed", e);
                throw new RuntimeException("Contract verification failed - address may be invalid", e);
            }
        }
    }

    /**
     * Check if a contract address is valid and contains code
     */
    public boolean isValidContractAddress(String address) {
        if (address == null || address.length() != 42 || !address.startsWith("0x")) {
            return false;
        }

        try {
            String code = web3j.ethGetCode(address, org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                    .send().getCode();

            boolean hasCode = code != null && !code.equals("0x") && !code.equals("0x0");

            if (isLocalNetwork && !hasCode) {
                log.warn("⚠️ Contract address {} has no code on local network", address);
            }

            return hasCode;

        } catch (Exception e) {
            if (isLocalNetwork) {
                log.warn("⚠️ Assuming contract is valid for local development: {}", e.getMessage());
                return true;
            }
            log.warn("Error checking contract address validity: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the current account balance
     */
    public BigInteger getAccountBalance() throws Exception {
        return web3j.ethGetBalance(credentials.getAddress(), org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                .send().getBalance();
    }

    /**
     * Save contract address to configuration file
     */
    private void saveContractAddressToConfig(String address) {
        try {
            File configFile = new File(contractConfigFile);
            File parentDir = configFile.getParentFile();

            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("# Smart Contract Configuration - Auto-generated\n");
                writer.write("# Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
                writer.write("# Network: " + (isLocalNetwork ? "local-ganache" : "sepolia-testnet") + "\n");
                writer.write("contract.address=" + address + "\n");
                writer.write("contract.deployer=" + credentials.getAddress() + "\n");
                writer.write("contract.network=" + (isLocalNetwork ? "local-ganache" : "sepolia-testnet") + "\n");
            }

            log.info("💾 Contract address saved to: {}", configFile.getAbsolutePath());

        } catch (IOException e) {
            log.warn("Failed to save contract address to config file: {}", e.getMessage());
        }
    }

    /**
     * Load contract address from configuration file
     */
    public String loadContractAddressFromConfig() {
        try {
            File configFile = new File(contractConfigFile);
            if (!configFile.exists()) {
                return null;
            }

            String content = new String(Files.readAllBytes(Paths.get(contractConfigFile)));
            for (String line : content.split("\n")) {
                if (line.startsWith("contract.address=") && !line.startsWith("#")) {
                    String address = line.substring("contract.address=".length()).trim();
                    log.info("📂 Loaded contract address from config file: {}", address);
                    return address;
                }
            }

        } catch (IOException e) {
            log.warn("Failed to load contract address from config file: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Get contract instance
     */
    public DocumentNotarization getContract() {
        if (contract == null) {
            log.warn("⚠️ Contract not initialized - returning null");
            return null;
        }
        return contract;
    }

    /**
     * Get current contract address
     */
    public String getContractAddress() {
        return contractAddress;
    }

    /**
     * Check if contract is properly initialized
     */
    public boolean isContractInitialized() {
        return contract != null;
    }

    /**
     * Check if we're using local network
     */
    public boolean isLocalNetwork() {
        return isLocalNetwork;
    }

    /**
     * Check blockchain connection
     */
    public boolean isBlockchainConnected() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber() != null;
        } catch (Exception e) {
            log.error("Blockchain connection failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if contract is valid and accessible
     */
    public boolean isContractValid() {
        try {
            if (contract == null) return false;

            if (isLocalNetwork) {
                return contract.isValid();
            } else {
                contract.STAKE_AMOUNT().send();
                return true;
            }
        } catch (Exception e) {
            log.warn("Contract validation failed: {}", e.getMessage());
            return false;
        }
    }
}