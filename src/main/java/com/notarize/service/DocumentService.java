package com.notarize.service;

import com.notarize.dto.DocumentDTO;
import com.notarize.dto.NotarizationRequestDTO;
import com.notarize.model.Document;
import com.notarize.repository.DocumentRepository;
import com.notarize.service.BlockchainService;
import com.notarize.util.HashUtil;
import com.notarize.util.IpfsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private IpfsUtil ipfsUtil;

    @Autowired
    private HashUtil hashUtil;

    @Autowired
    private NotaryService notaryService;

    // ========== DOCUMENT REGISTRATION METHODS ==========

    /**
     * Register document with blockchain (requires notary)
     */
    public DocumentDTO registerDocument(NotarizationRequestDTO request) throws Exception {
        // Calculate document hash for database reference
        String documentHash = hashUtil.calculateSHA256Hash(request.getFileContent());

        // Check if document already exists
        if (documentRepository.existsByDocumentHash(documentHash)) {
            throw new RuntimeException("Document already registered");
        }

        // Upload to IPFS - returns IPFS CID directly
        String ipfsCid = ipfsUtil.uploadBase64ToIpfs(request.getFileContent());

        // Validate IPFS CID
        blockchainService.validateIpfsCid(ipfsCid);

        // Register on blockchain using IPFS CID
        TransactionReceipt receipt = blockchainService.registerDocument(
                ipfsCid,
                request.getDocumentName()
        );

        log.info("Document registered on blockchain. Transaction hash: {}", receipt.getTransactionHash());

        // Save to database
        Document document = new Document();
        document.setDocumentHash(documentHash);
        document.setOwnerAddress(request.getNotaryAddress());
        document.setDocumentName(request.getDocumentName());
        document.setIpfsHash(ipfsCid);
        document.setTimestamp(LocalDateTime.now());
        document.setNotarized(false);
        document.setNotaryAddresses(new ArrayList<>());
        document.setCreatedAt(LocalDateTime.now());

        Document savedDocument = documentRepository.save(document);

        return convertToDTO(savedDocument);
    }

    /**
     * Register document without blockchain (for testing)
     */
    public DocumentDTO registerDocumentSimple(String documentName, String fileContent, String ownerAddress) {
        try {
            log.info("Registering document without blockchain: {}", documentName);

            // Calculate document hash for database reference
            String documentHash = hashUtil.calculateSHA256Hash(fileContent);

            // Check if document already exists
            if (documentRepository.existsByDocumentHash(documentHash)) {
                throw new RuntimeException("Document already registered");
            }

            // Upload to IPFS as plain text
            String ipfsCid;
            try {
                ipfsCid = ipfsUtil.uploadTextToIpfs(fileContent);
                log.info("✅ IPFS upload successful: {}", ipfsCid);
            } catch (Exception e) {
                log.warn("IPFS upload failed, using fallback: {}", e.getMessage());
                ipfsCid = "ipfs_failed_" + System.currentTimeMillis();
            }

            // Save to database only (no blockchain)
            Document document = new Document();
            document.setDocumentHash(documentHash);
            document.setOwnerAddress(ownerAddress);
            document.setDocumentName(documentName);
            document.setIpfsHash(ipfsCid);
            document.setTimestamp(LocalDateTime.now());
            document.setNotarized(false);
            document.setNotaryAddresses(new ArrayList<>());
            document.setCreatedAt(LocalDateTime.now());

            Document savedDocument = documentRepository.save(document);
            log.info("✅ Document saved to database with hash: {}", documentHash);

            return convertToDTO(savedDocument);

        } catch (Exception e) {
            log.error("Simple document registration failed: {}", e.getMessage());
            throw new RuntimeException("Failed to register document: " + e.getMessage());
        }
    }

    /**
     * Register document with blockchain tolerance (best for production)
     */
    public DocumentDTO registerDocumentTolerant(NotarizationRequestDTO request) {
        try {
            log.info("Registering document (tolerant mode): {}", request.getDocumentName());

            // Calculate document hash for database reference
            String documentHash = hashUtil.calculateSHA256Hash(request.getFileContent());

            // Check if document already exists
            if (documentRepository.existsByDocumentHash(documentHash)) {
                throw new RuntimeException("Document already registered");
            }

            // Upload to IPFS
            String ipfsCid = ipfsUtil.uploadBase64ToIpfs(request.getFileContent());
            log.info("✅ IPFS upload successful: {}", ipfsCid);

            // Validate IPFS CID
            blockchainService.validateIpfsCid(ipfsCid);

            // Try blockchain registration (but don't fail if it doesn't work)
            boolean blockchainSuccess = false;
            String transactionHash = null;

            try {
                TransactionReceipt receipt = blockchainService.registerDocument(ipfsCid, request.getDocumentName());
                blockchainSuccess = true;
                transactionHash = receipt.getTransactionHash();
                log.info("✅ Document registered on blockchain. Transaction hash: {}", transactionHash);
            } catch (Exception e) {
                log.warn("⚠️ Blockchain registration failed, but document saved locally: {}", e.getMessage());
                // Continue without blockchain
            }

            // Save to database
            Document document = new Document();
            document.setDocumentHash(documentHash);
            document.setOwnerAddress(request.getNotaryAddress());
            document.setDocumentName(request.getDocumentName());
            document.setIpfsHash(ipfsCid);
            document.setTimestamp(LocalDateTime.now());
            document.setNotarized(blockchainSuccess); // Only mark as notarized if blockchain succeeded
            document.setNotaryAddresses(new ArrayList<>());
            document.setCreatedAt(LocalDateTime.now());

            Document savedDocument = documentRepository.save(document);
            log.info("✅ Document saved to database with hash: {}", documentHash);

            return convertToDTO(savedDocument);

        } catch (Exception e) {
            log.error("Document registration failed: {}", e.getMessage());
            throw new RuntimeException("Failed to register document: " + e.getMessage());
        }
    }

    // ========== NOTARIZATION METHODS ==========

    /**
     * Notarize document with file content validation
     */
    public DocumentDTO notarizeDocument(String documentHash, String notaryAddress, String fileContent) throws Exception {
        // Validate file content matches the hash
        String calculatedHash = hashUtil.calculateSHA256Hash(fileContent);
        if (!documentHash.equals(calculatedHash)) {
            throw new RuntimeException("Document hash does not match provided file content");
        }

        // Check if notary is active
        if (!notaryService.isNotaryActive(notaryAddress)) {
            throw new RuntimeException("Notary is not active or not found");
        }

        // Proceed with notarization
        return notarizeDocument(documentHash, notaryAddress);
    }

    /**
     * Notarize document without file content validation
     */
    public DocumentDTO notarizeDocument(String documentHash, String notaryAddress) throws Exception {
        Document document = documentRepository.findByDocumentHash(documentHash)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Check if notary is active
        if (!notaryService.isNotaryActive(notaryAddress)) {
            throw new RuntimeException("Notary is not active or not found");
        }

        // Get IPFS CID from document
        String ipfsCid = document.getIpfsHash();
        if (ipfsCid == null || ipfsCid.isEmpty()) {
            throw new RuntimeException("Document has no IPFS CID");
        }

        // Notarize on blockchain using IPFS CID
        TransactionReceipt receipt = blockchainService.notarizeDocument(ipfsCid);

        document.setNotarized(true);
        if (!document.getNotaryAddresses().contains(notaryAddress)) {
            document.getNotaryAddresses().add(notaryAddress);
        }

        Document updatedDocument = documentRepository.save(document);

        // Update notary statistics
        notaryService.incrementSuccessfulNotarizations(notaryAddress);

        log.info("Document notarized. Transaction hash: {}", receipt.getTransactionHash());

        return convertToDTO(updatedDocument);
    }

    // ========== DOCUMENT RETRIEVAL METHODS ==========

    public DocumentDTO getDocument(String documentHash) throws Exception {
        Document document = documentRepository.findByDocumentHash(documentHash)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Get IPFS CID
        String ipfsCid = document.getIpfsHash();
        if (ipfsCid == null || ipfsCid.isEmpty()) {
            throw new RuntimeException("Document has no IPFS CID");
        }

        // Verify with blockchain using IPFS CID
        boolean isNotarizedOnChain = blockchainService.isDocumentNotarized(ipfsCid);

        DocumentDTO dto = convertToDTO(document);
        dto.setNotarized(isNotarizedOnChain);

        return dto;
    }

    /**
     * Get document by IPFS CID
     */
    public DocumentDTO getDocumentByIpfsCid(String ipfsCid) throws Exception {
        Document document = documentRepository.findByIpfsHash(ipfsCid)
                .orElseThrow(() -> new RuntimeException("Document not found with IPFS CID: " + ipfsCid));

        // Verify with blockchain using IPFS CID
        boolean isNotarizedOnChain = blockchainService.isDocumentNotarized(ipfsCid);

        DocumentDTO dto = convertToDTO(document);
        dto.setNotarized(isNotarizedOnChain);

        log.info("Retrieved document by IPFS CID: {}", ipfsCid);
        return dto;
    }

    public List<DocumentDTO> getUserDocuments(String userAddress) {
        return documentRepository.findByOwnerAddress(userAddress)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<DocumentDTO> getAllDocuments() {
        return documentRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<DocumentDTO> getNotarizedDocuments() {
        return documentRepository.findByNotarized(true)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<DocumentDTO> getDocumentsByNotary(String notaryAddress) {
        return documentRepository.findByNotaryAddressesContaining(notaryAddress)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ========== VERIFICATION METHODS ==========

    public boolean verifyDocumentIntegrity(String documentHash, String fileContent) throws Exception {
        String calculatedHash = hashUtil.calculateSHA256Hash(fileContent);
        return documentHash.equals(calculatedHash);
    }

    /**
     * Verify document with IPFS download
     */
    public boolean verifyDocumentWithIpfs(String documentHash) throws Exception {
        try {
            Document document = documentRepository.findByDocumentHash(documentHash)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            String ipfsCid = document.getIpfsHash();
            if (ipfsCid == null || ipfsCid.isEmpty()) {
                throw new RuntimeException("No IPFS CID found for document");
            }

            // Download content from IPFS
            String downloadedContent = ipfsUtil.downloadFromIpfs(ipfsCid);

            // Calculate hash of downloaded content
            String calculatedHash = hashUtil.calculateSHA256Hash(downloadedContent);

            // Verify hash matches
            boolean hashMatches = documentHash.equals(calculatedHash);

            // Also verify with blockchain using IPFS CID
            boolean isNotarizedOnChain = blockchainService.isDocumentNotarized(ipfsCid);

            log.info("IPFS verification for document {}: Hash matches = {}, Notarized on chain = {}",
                    documentHash, hashMatches, isNotarizedOnChain);

            return hashMatches && isNotarizedOnChain;

        } catch (Exception e) {
            log.error("Error verifying document with IPFS: {}", e.getMessage());
            throw new RuntimeException("Failed to verify document with IPFS: " + e.getMessage());
        }
    }

    /**
     * Manual verification with enhanced checks
     */
    public boolean manuallyVerifyDocument(String documentHash) throws Exception {
        try {
            Document document = documentRepository.findByDocumentHash(documentHash)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            String ipfsCid = document.getIpfsHash();
            if (ipfsCid == null || ipfsCid.isEmpty()) {
                throw new RuntimeException("No IPFS CID found for document");
            }

            // Check blockchain notarization status using IPFS CID
            boolean isNotarizedOnChain = blockchainService.isDocumentNotarized(ipfsCid);

            // Check IPFS integrity
            boolean ipfsIntegrity = true;
            try {
                ipfsIntegrity = verifyDocumentWithIpfs(documentHash);
            } catch (Exception e) {
                log.warn("IPFS verification failed for document {}: {}", documentHash, e.getMessage());
                ipfsIntegrity = false;
            }

            // Update document verification timestamp
            document.setLastVerified(LocalDateTime.now());
            documentRepository.save(document);

            boolean isVerified = isNotarizedOnChain && ipfsIntegrity;
            log.info("Manual verification for document {}: Notarized on chain = {}, IPFS integrity = {}, Overall = {}",
                    documentHash, isNotarizedOnChain, ipfsIntegrity, isVerified);

            return isVerified;

        } catch (Exception e) {
            log.error("Error in manual document verification: {}", e.getMessage());
            throw new RuntimeException("Manual verification failed: " + e.getMessage());
        }
    }

    // ========== UTILITY METHODS ==========

    public String downloadFromIpfs(String documentHash) {
        try {
            log.info("Downloading document from IPFS: {}", documentHash);

            // Get document from database
            Document document = documentRepository.findByDocumentHash(documentHash)
                    .orElseThrow(() -> new RuntimeException("Document not found in database"));

            String ipfsCid = document.getIpfsHash();
            if (ipfsCid == null || ipfsCid.isEmpty()) {
                throw new RuntimeException("Document has no IPFS CID");
            }

            log.info("Downloading content from IPFS with CID: {}", ipfsCid);

            // Use the IpfsUtil to download the content
            String fileContent = ipfsUtil.downloadFromIpfs(ipfsCid);

            if (fileContent == null || fileContent.isEmpty()) {
                throw new RuntimeException("Failed to download content from IPFS or content is empty");
            }

            log.info("Successfully downloaded content from IPFS, CID: {}", ipfsCid);
            return fileContent;

        } catch (Exception e) {
            log.error("Error downloading from IPFS for document {}: {}", documentHash, e.getMessage());
            throw new RuntimeException("Failed to download document from IPFS: " + e.getMessage());
        }
    }

    public boolean documentExists(String documentHash) {
        return documentRepository.existsByDocumentHash(documentHash);
    }

    public void deleteDocument(String documentHash) throws Exception {
        Document document = documentRepository.findByDocumentHash(documentHash)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        documentRepository.delete(document);
        log.info("Document deleted: {}", documentHash);
    }

    public DocumentDTO updateDocumentMetadata(String documentHash, String documentName) throws Exception {
        Document document = documentRepository.findByDocumentHash(documentHash)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.setDocumentName(documentName);
        Document updatedDocument = documentRepository.save(document);

        log.info("Document metadata updated: {}", documentHash);
        return convertToDTO(updatedDocument);
    }

    /**
     * Get documents needing verification (older than 24 hours)
     */
    public List<DocumentDTO> getDocumentsNeedingVerification() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        return documentRepository.findByLastVerifiedBeforeOrLastVerifiedIsNull(twentyFourHoursAgo)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ========== BATCH OPERATIONS ==========

    /**
     * Verification result for batch operations
     */
    public List<VerificationResult> verifyMultipleDocuments(List<String> documentHashes) {
        return documentHashes.stream()
                .map(hash -> {
                    VerificationResult result = new VerificationResult();
                    result.setDocumentHash(hash);
                    try {
                        boolean verified = manuallyVerifyDocument(hash);
                        result.setVerified(verified);
                        result.setMessage("Verification successful");
                    } catch (Exception e) {
                        result.setVerified(false);
                        result.setMessage("Verification failed: " + e.getMessage());
                    }
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get verification status for single document
     */
    public VerificationStatus getDocumentVerificationStatus(String documentHash) throws Exception {
        Document document = documentRepository.findByDocumentHash(documentHash)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        VerificationStatus status = new VerificationStatus();
        status.setDocumentHash(documentHash);
        status.setDocumentName(document.getDocumentName());
        status.setNotarized(document.isNotarized());
        status.setLastVerified(document.getLastVerified());
        status.setNotaryCount(document.getNotaryAddresses().size());

        // Check blockchain status using IPFS CID
        String ipfsCid = document.getIpfsHash();
        if (ipfsCid != null && !ipfsCid.isEmpty()) {
            status.setNotarizedOnChain(blockchainService.isDocumentNotarized(ipfsCid));

            // Check IPFS availability
            try {
                String content = ipfsUtil.downloadFromIpfs(ipfsCid);
                status.setIpfsAvailable(content != null && !content.isEmpty());
            } catch (Exception e) {
                status.setIpfsAvailable(false);
            }
        } else {
            status.setNotarizedOnChain(false);
            status.setIpfsAvailable(false);
        }

        return status;
    }

    // ========== HELPER METHODS ==========

    private DocumentDTO convertToDTO(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setDocumentHash(document.getDocumentHash());
        dto.setOwnerAddress(document.getOwnerAddress());
        dto.setDocumentName(document.getDocumentName());
        dto.setIpfsHash(document.getIpfsHash());
        dto.setTimestamp(document.getTimestamp());
        dto.setNotarized(document.isNotarized());
        dto.setNotaryAddresses(document.getNotaryAddresses());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setLastVerified(document.getLastVerified());
        return dto;
    }
    /**
     * Get document without blockchain verification (for download, etc.)
     */
    public DocumentDTO getDocumentSimple(String documentHash) {
        try {
            Document document = documentRepository.findByDocumentHash(documentHash)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            // Return DTO without blockchain verification
            return convertToDTO(document);

        } catch (Exception e) {
            log.error("Error getting document: {}", e.getMessage());
            throw new RuntimeException("Failed to get document: " + e.getMessage());
        }
    }

    // ========== INNER CLASSES ==========

    public static class VerificationResult {
        private String documentHash;
        private boolean verified;
        private String message;

        public String getDocumentHash() { return documentHash; }
        public void setDocumentHash(String documentHash) { this.documentHash = documentHash; }
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class VerificationStatus {
        private String documentHash;
        private String documentName;
        private boolean notarized;
        private boolean notarizedOnChain;
        private boolean ipfsAvailable;
        private LocalDateTime lastVerified;
        private int notaryCount;

        public String getDocumentHash() { return documentHash; }
        public void setDocumentHash(String documentHash) { this.documentHash = documentHash; }
        public String getDocumentName() { return documentName; }
        public void setDocumentName(String documentName) { this.documentName = documentName; }
        public boolean isNotarized() { return notarized; }
        public void setNotarized(boolean notarized) { this.notarized = notarized; }
        public boolean isNotarizedOnChain() { return notarizedOnChain; }
        public void setNotarizedOnChain(boolean notarizedOnChain) { this.notarizedOnChain = notarizedOnChain; }
        public boolean isIpfsAvailable() { return ipfsAvailable; }
        public void setIpfsAvailable(boolean ipfsAvailable) { this.ipfsAvailable = ipfsAvailable; }
        public LocalDateTime getLastVerified() { return lastVerified; }
        public void setLastVerified(LocalDateTime lastVerified) { this.lastVerified = lastVerified; }
        public int getNotaryCount() { return notaryCount; }
        public void setNotaryCount(int notaryCount) { this.notaryCount = notaryCount; }
    }
}