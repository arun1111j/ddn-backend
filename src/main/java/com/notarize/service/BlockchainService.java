package com.notarize.service;

import com.notarize.contracts.DocumentNotarization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.crypto.Credentials;
import org.web3j.tx.gas.StaticGasProvider;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
public class BlockchainService {

    @Autowired
    private Web3j web3j;

    @Autowired
    private Credentials credentials;

    @Autowired
    private StaticGasProvider gasProvider;

    // CHANGE: Use AutomatedContractService instead of ContractDeploymentService
    @Autowired
    private AutomatedContractService automatedContractService;

    private DocumentNotarization contract;

    @PostConstruct
    public void init() {
        try {
            log.info("🔧 Initializing BlockchainService with Infura...");

            // CHANGE: Wait for AutomatedContractService to be ready
            waitForContractService();

            // Get contract from AutomatedContractService
            if (!automatedContractService.isContractReady()) {
                log.warn("⚠️ Contract not ready, BlockchainService will start in limited mode");
                // Don't throw exception - allow service to start without contract
                return;
            }

            this.contract = automatedContractService.getContract();
            log.info("✅ BlockchainService initialized successfully");
            log.info("📋 Contract Address: {}", automatedContractService.getContractAddress());
            log.info("🎯 Active Service: {}", automatedContractService.getActiveService());

            // Test connection (but don't fail if it doesn't work)
            testContractConnection();

        } catch (Exception e) {
            log.error("❌ Failed to initialize BlockchainService: {}", e.getMessage());
            log.warn("⚠️ BlockchainService starting in limited mode without contract access");
            // Don't throw exception - allow service to start
        }
    }

    // ADD: Wait for contract service to be ready
    private void waitForContractService() throws InterruptedException {
        int maxWaitTime = 30000; // 30 seconds
        int waitInterval = 1000; // 1 second
        int totalWaited = 0;

        while (!automatedContractService.isContractReady() && totalWaited < maxWaitTime) {
            log.info("⏳ Waiting for contract service to be ready... ({}/{} ms)", totalWaited, maxWaitTime);
            Thread.sleep(waitInterval);
            totalWaited += waitInterval;
        }

        if (!automatedContractService.isContractReady()) {
            log.warn("⏰ Contract service not ready after {}ms, continuing without contract", maxWaitTime);
        }
    }

    // ========== DIAGNOSTIC METHOD ==========

    public void testContractConnection() {
        try {
            log.info("🧪 Testing contract connection...");

            BigInteger stakeAmount = getStakeAmount();
            log.info("✅ Stake Amount: {} WEI", stakeAmount);

            BigInteger slashPercentage = getSlashPercentage();
            log.info("✅ Slash Percentage: {}", slashPercentage);

            String contractAddress = getContractAddress();
            log.info("✅ Contract Address: {}", contractAddress);

            BigInteger blockNumber = getCurrentBlockNumber();
            log.info("✅ Current Block: {}", blockNumber);

            log.info("✅ All contract tests passed!");

        } catch (Exception e) {
            log.error("❌ Contract test failed: {}", e.getMessage());
            // Don't throw exception - just log the error
        }
    }
    public boolean isNotaryRegisteredOnBlockchain(String address) {
        try {
            validateContractState();
            DocumentNotarization.NotaryInfo notary = contract.getNotaryInfo(address).send();
            return notary != null && notary.getIsActive();
        } catch (Exception e) {
            log.debug("Notary check failed for {}: {}", address, e.getMessage());
            return false;
        }
    }

    // ========== Notary Management Methods ==========

    // ========== Notary Management Methods ==========

    public TransactionReceipt registerNotary(String notaryAddress, String name, BigInteger stakeAmount) throws Exception {
        log.info("📝 Registering notary on blockchain: {} | Address: {} | Stake: {} wei",
                name, notaryAddress, stakeAmount);
        validateContractState();

        try {
            // Debug: Check what we're sending
            log.info("🔍 Contract call details:");
            log.info("   - Name: {}", name);
            log.info("   - Stake Amount: {} wei", stakeAmount);
            log.info("   - Required Stake: {} wei", getStakeAmount());
            log.info("   - Sender Address: {}", getCurrentAccountAddress());

            // Check if stake amount matches exactly
            BigInteger requiredStake = getStakeAmount();
            if (!stakeAmount.equals(requiredStake)) {
                log.error("❌ Stake amount mismatch: got {}, expected {}", stakeAmount, requiredStake);
                throw new RuntimeException("Stake amount must be exactly " + requiredStake + " wei");
            }

            // Try the contract call
            log.info("🔄 Calling contract.registerAsNotary({}, {})", name, stakeAmount);
            TransactionReceipt receipt = contract.registerAsNotary(name, stakeAmount).send();

            log.info("✅ Notary registered. TX: {}", receipt.getTransactionHash());
            return receipt;

        } catch (Exception e) {
            log.error("❌ Contract call failed: {}", e.getMessage());
            log.error("📋 Error details:", e);
            throw e;
        }
    }

    public TransactionReceipt slashNotary(String notaryAddress, String ipfsCid) throws Exception {
        log.info("⚠️ Slashing notary: {} for IPFS CID: {}", notaryAddress, ipfsCid);
        validateContractState();
        validateIpfsCid(ipfsCid);

        TransactionReceipt receipt = contract.slashNotary(notaryAddress, ipfsCid).send();

        log.info("✅ Notary slashed. TX: {}", receipt.getTransactionHash());
        log.info("🔗 View on Etherscan: https://sepolia.etherscan.io/tx/{}", receipt.getTransactionHash());

        return receipt;
    }

    public TransactionReceipt withdrawStake() throws Exception {
        log.info("💰 Withdrawing stake for current notary");
        validateContractState();

        TransactionReceipt receipt = contract.withdrawStake().send();

        log.info("✅ Stake withdrawn. TX: {}", receipt.getTransactionHash());
        return receipt;
    }

    public boolean isConnected() {
        return isBlockchainConnected();
    }

    public boolean isNotaryRegistered(String address) {
        try {
            validateContractState();
            DocumentNotarization.NotaryInfo notary = getNotary(address);
            return notary != null && notary.getIsActive();
        } catch (Exception e) {
            log.debug("Notary check failed for {}: {}", address, e.getMessage());
            return false;
        }
    }

    // Fix the registerDocument method signature
    public String registerDocument(String ipfsCid, String documentName, String ownerAddress) {
        try {
            log.info("📄 Registering document: {} with IPFS CID: {}", documentName, ipfsCid);
            validateContractState();
            validateIpfsCid(ipfsCid);

            TransactionReceipt receipt = contract.registerDocument(ipfsCid, documentName).send();

            log.info("✅ Document registered. TX: {}", receipt.getTransactionHash());
            return receipt.getTransactionHash();

        } catch (Exception e) {
            log.error("❌ Document registration failed: {}", e.getMessage());
            throw new RuntimeException("Document registration failed: " + e.getMessage(), e);
        }
    }

    // ========== Document Management Methods ==========

    public TransactionReceipt registerDocument(String ipfsCid, String documentName) throws Exception {
        log.info("📄 Registering document: {} with IPFS CID: {}", documentName, ipfsCid);
        validateContractState();
        validateIpfsCid(ipfsCid);

        TransactionReceipt receipt = contract.registerDocument(ipfsCid, documentName).send();

        log.info("✅ Document registered. TX: {}", receipt.getTransactionHash());
        log.info("🔗 View on Etherscan: https://sepolia.etherscan.io/tx/{}", receipt.getTransactionHash());
        log.info("📌 IPFS CID: {}", ipfsCid);
        log.info("🔗 View on IPFS: https://gateway.pinata.cloud/ipfs/{}", ipfsCid);

        return receipt;
    }

    public TransactionReceipt notarizeDocument(String ipfsCid) throws Exception {
        log.info("✍️ Notarizing document with IPFS CID: {}", ipfsCid);
        validateContractState();
        validateIpfsCid(ipfsCid);

        TransactionReceipt receipt = contract.notarizeDocument(ipfsCid).send();

        log.info("✅ Document notarized. TX: {}", receipt.getTransactionHash());
        log.info("🔗 View on Etherscan: https://sepolia.etherscan.io/tx/{}", receipt.getTransactionHash());

        return receipt;
    }

    // ========== Document Query Methods ==========

    public DocumentNotarization.Document getDocument(String ipfsCid) throws Exception {
        log.debug("📖 Fetching document from blockchain: {}", ipfsCid);
        validateContractState();
        validateIpfsCid(ipfsCid);

        DocumentNotarization.Document document = contract.getDocument(ipfsCid).send();
        log.debug("✅ Document fetched successfully");

        return document;
    }

    public List<String> getDocumentNotaries(String ipfsCid) throws Exception {
        DocumentNotarization.Document document = getDocument(ipfsCid);
        List<String> notaries = document.getNotaries();
        log.debug("Found {} notaries for document", notaries.size());
        return notaries;
    }

    public boolean isDocumentNotarized(String ipfsCid) throws Exception {
        DocumentNotarization.Document document = getDocument(ipfsCid);
        boolean isNotarized = document.getIsNotarized();
        log.debug("Document notarization status: {}", isNotarized);
        return isNotarized;
    }

    public String getDocumentOwner(String ipfsCid) throws Exception {
        DocumentNotarization.Document document = getDocument(ipfsCid);
        return document.getOwner();
    }

    public BigInteger getDocumentTimestamp(String ipfsCid) throws Exception {
        DocumentNotarization.Document document = getDocument(ipfsCid);
        return document.getTimestamp();
    }

    public String getDocumentName(String ipfsCid) throws Exception {
        DocumentNotarization.Document document = getDocument(ipfsCid);
        return document.getDocumentName();
    }

    // ========== Notary Query Methods ==========

    public DocumentNotarization.NotaryInfo getNotary(String notaryAddress) throws Exception {
        log.debug("👤 Fetching notary details: {}", notaryAddress);
        validateContractState();

        DocumentNotarization.NotaryInfo notary = contract.getNotaryInfo(notaryAddress).send();
        log.debug("✅ Notary details fetched");

        return notary;
    }

    public boolean isNotaryActive(String notaryAddress) throws Exception {
        DocumentNotarization.NotaryInfo notary = getNotary(notaryAddress);
        return notary.getIsActive();
    }

    public BigInteger getNotaryStake(String notaryAddress) throws Exception {
        DocumentNotarization.NotaryInfo notary = getNotary(notaryAddress);
        return notary.getStakeAmount();
    }

    public BigInteger getNotarySuccessfulNotarizations(String notaryAddress) throws Exception {
        DocumentNotarization.NotaryInfo notary = getNotary(notaryAddress);
        return notary.getSuccessfulNotarizations();
    }

    public BigInteger getNotarySlashedCount(String notaryAddress) throws Exception {
        DocumentNotarization.NotaryInfo notary = getNotary(notaryAddress);
        return notary.getSlashedCount();
    }

    public String getNotaryName(String notaryAddress) throws Exception {
        DocumentNotarization.NotaryInfo notary = getNotary(notaryAddress);
        return notary.getName();
    }

    // ========== User Document Methods ==========

    public List<String> getUserDocuments(String userAddress) throws Exception {
        log.debug("📚 Fetching user documents for: {}", userAddress);
        validateContractState();

        List<String> userDocuments = contract.getUserDocuments(userAddress).send();
        log.debug("Found {} documents for user", userDocuments.size());

        return userDocuments;
    }

    public List<DocumentNotarization.Document> getUserDocumentsWithDetails(String userAddress) throws Exception {
        log.info("📚 Fetching detailed documents for user: {}", userAddress);
        List<String> ipfsCids = getUserDocuments(userAddress);
        List<DocumentNotarization.Document> documents = new java.util.ArrayList<>();

        for (String ipfsCid : ipfsCids) {
            try {
                DocumentNotarization.Document doc = getDocument(ipfsCid);
                documents.add(doc);
            } catch (Exception e) {
                log.warn("Failed to fetch details for document: {}", ipfsCid, e);
            }
        }

        log.info("✅ Retrieved {} document details", documents.size());
        return documents;
    }

    // ========== Contract Information Methods ==========

    public BigInteger getStakeAmount() throws Exception {
        validateContractState();
        return contract.STAKE_AMOUNT().send();
    }

    public BigInteger getSlashPercentage() throws Exception {
        validateContractState();
        return contract.SLASH_PERCENTAGE().send();
    }

    // CHANGE: Get contract address from AutomatedContractService
    public String getContractAddress() {
        return automatedContractService.getContractAddress();
    }

    // CHANGE: Check if contract is loaded via AutomatedContractService
    public boolean isContractLoaded() {
        return contract != null && automatedContractService.isContractReady();
    }

    // ========== Health Check Methods ==========

    public boolean isBlockchainConnected() {
        try {
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            return blockNumber != null;
        } catch (Exception e) {
            log.error("Blockchain connection check failed: {}", e.getMessage());
            return false;
        }
    }

    public BigInteger getCurrentBlockNumber() throws Exception {
        return web3j.ethBlockNumber().send().getBlockNumber();
    }

    public BigInteger getContractBalance() throws Exception {
        return web3j.ethGetBalance(
                getContractAddress(),
                org.web3j.protocol.core.DefaultBlockParameterName.LATEST
        ).send().getBalance();
    }

    public BigInteger getCurrentAccountBalance() throws Exception {
        return web3j.ethGetBalance(
                credentials.getAddress(),
                org.web3j.protocol.core.DefaultBlockParameterName.LATEST
        ).send().getBalance();
    }

    public String getCurrentAccountAddress() {
        return credentials.getAddress();
    }

    // ========== Validation Methods ==========

    public boolean isValidIpfsCid(String ipfsCid) {
        if (ipfsCid == null || ipfsCid.trim().isEmpty()) {
            return false;
        }
        return ipfsCid.startsWith("Qm") ||
                ipfsCid.startsWith("bafy") ||
                ipfsCid.startsWith("bafk");
    }

    public void validateIpfsCid(String ipfsCid) {
        if (!isValidIpfsCid(ipfsCid)) {
            throw new IllegalArgumentException("Invalid IPFS CID format: " + ipfsCid);
        }
    }

    // CHANGE: Validate contract state using AutomatedContractService
    private void validateContractState() {
        if (contract == null) {
            throw new IllegalStateException("Smart contract not initialized");
        }
        if (!automatedContractService.isContractReady()) {
            throw new IllegalStateException("Smart contract not ready");
        }
    }

    // ========== Document Existence Check ==========

    public boolean documentExists(String ipfsCid) {
        try {
            DocumentNotarization.Document doc = getDocument(ipfsCid);
            String owner = doc.getOwner();
            return owner != null && !owner.equals("0x0000000000000000000000000000000000000000");
        } catch (Exception e) {
            log.debug("Document does not exist: {}", ipfsCid);
            return false;
        }
    }

    // ========== Statistics ==========

    public int getUserDocumentCount(String userAddress) throws Exception {
        return getUserDocuments(userAddress).size();
    }

    public int getUserNotarizedDocumentCount(String userAddress) throws Exception {
        List<String> ipfsCids = getUserDocuments(userAddress);
        int count = 0;

        for (String ipfsCid : ipfsCids) {
            try {
                if (isDocumentNotarized(ipfsCid)) {
                    count++;
                }
            } catch (Exception e) {
                log.warn("Error checking notarization for: {}", ipfsCid);
            }
        }

        return count;
    }

    // ========== Transaction Helpers ==========

    public TransactionReceipt getTransactionReceipt(String txHash) throws Exception {
        org.web3j.protocol.core.methods.response.EthGetTransactionReceipt receipt =
                web3j.ethGetTransactionReceipt(txHash).send();

        return receipt.getTransactionReceipt()
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + txHash));
    }

    public boolean isTransactionSuccessful(String txHash) throws Exception {
        TransactionReceipt receipt = getTransactionReceipt(txHash);
        return receipt.isStatusOK();
    }

    // ========== Status Logging ==========

    public void logContractStatus() {
        try {
            log.info("=== Blockchain Service Status ===");
            log.info("✅ Connected: {}", isBlockchainConnected());
            log.info("✅ Contract Ready: {}", isContractLoaded());
            log.info("📋 Contract: {}", getContractAddress());
            log.info("👤 Account: {}", getCurrentAccountAddress());
            log.info("🔢 Block: {}", getCurrentBlockNumber());
            log.info("💰 Balance: {} wei", getCurrentAccountBalance());

            if (isContractLoaded()) {
                log.info("💎 Stake: {} wei", getStakeAmount());
                log.info("⚡ Slash: {}%", getSlashPercentage());
            }
        } catch (Exception e) {
            log.error("Error logging status: {}", e.getMessage());
        }
    }
}