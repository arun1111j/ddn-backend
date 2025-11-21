package com.notarize.controller;

import com.notarize.dto.DocumentDTO;
import com.notarize.dto.NotarizationRequestDTO;
import com.notarize.model.Document;
import com.notarize.repository.DocumentRepository;
import com.notarize.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController

@RequestMapping("/api/documents")
@CrossOrigin(origins = "*") // Adjust for production
public class DocumentController {

    @Autowired
    private DocumentService documentService;
    @GetMapping("/simple/{documentHash}")
    public ResponseEntity<?> getDocumentSimple(@PathVariable String documentHash) {
        try {
            log.debug("Fetching document (simple mode): {}", documentHash);
            DocumentDTO document = documentService.getDocumentSimple(documentHash);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            log.error("Document not found: {}", documentHash);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Document not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve document: " + e.getMessage()));
        }
    }
    @PostMapping("/register-simple")
    public ResponseEntity<?> registerDocumentSimple(
            @RequestParam String documentName,
            @RequestParam String fileContent,
            @RequestParam String ownerAddress) {
        try {
            log.info("Registering document (simple mode): {}", documentName);
            DocumentDTO document = documentService.registerDocumentSimple(documentName, fileContent, ownerAddress);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            log.error("Error registering document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error registering document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to register document: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerDocument(@RequestBody NotarizationRequestDTO request) {
        try {
            log.info("Registering document: {}", request.getDocumentName());
            DocumentDTO document = documentService.registerDocument(request);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            log.error("Error registering document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error registering document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to register document: " + e.getMessage()));
        }
    }

    @PostMapping("/notarize")
    public ResponseEntity<?> notarizeDocument(
            @RequestParam String documentHash,
            @RequestParam String notaryAddress,
            @RequestParam String fileContent) {
        try {
            log.info("Notarizing document {} by notary {}", documentHash, notaryAddress);
            DocumentDTO document = documentService.notarizeDocument(documentHash, notaryAddress, fileContent);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            log.error("Error notarizing document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error notarizing document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to notarize document: " + e.getMessage()));
        }
    }

    @PostMapping("/notarize/unverified")
    public ResponseEntity<?> notarizeDocumentWithoutValidation(
            @RequestParam String documentHash,
            @RequestParam String notaryAddress) {
        try {
            log.info("Notarizing document {} without validation by notary {}", documentHash, notaryAddress);
            DocumentDTO document = documentService.notarizeDocument(documentHash, notaryAddress);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            log.error("Error notarizing document without validation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error notarizing document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to notarize document: " + e.getMessage()));
        }
    }

    @GetMapping("/{documentHash}")
    public ResponseEntity<?> getDocument(@PathVariable String documentHash) {
        try {
            log.debug("Fetching document: {}", documentHash);
            DocumentDTO document = documentService.getDocument(documentHash);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            log.error("Document not found: {}", documentHash);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Document not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve document: " + e.getMessage()));
        }
    }

    // NEW: Get document by IPFS CID
    @GetMapping("/ipfs/{ipfsCid}")
    public ResponseEntity<?> getDocumentByIpfsCid(@PathVariable String ipfsCid) {
        try {
            log.debug("Fetching document by IPFS CID: {}", ipfsCid);
            DocumentDTO document = documentService.getDocumentByIpfsCid(ipfsCid);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            log.error("Document not found for IPFS CID: {}", ipfsCid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Document not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting document by IPFS CID", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve document: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userAddress}")
    public ResponseEntity<?> getUserDocuments(@PathVariable String userAddress) {
        try {
            log.debug("Fetching documents for user: {}", userAddress);
            List<DocumentDTO> documents = documentService.getUserDocuments(userAddress);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error getting user documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve user documents: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllDocuments() {
        try {
            log.debug("Fetching all documents");
            List<DocumentDTO> documents = documentService.getAllDocuments();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error getting all documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve documents: " + e.getMessage()));
        }
    }

    @GetMapping("/notarized")
    public ResponseEntity<?> getNotarizedDocuments() {
        try {
            log.debug("Fetching all notarized documents");
            List<DocumentDTO> documents = documentService.getNotarizedDocuments();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error getting notarized documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve notarized documents: " + e.getMessage()));
        }
    }

    @GetMapping("/notary/{notaryAddress}")
    public ResponseEntity<?> getDocumentsByNotary(@PathVariable String notaryAddress) {
        try {
            log.debug("Fetching documents notarized by: {}", notaryAddress);
            List<DocumentDTO> documents = documentService.getDocumentsByNotary(notaryAddress);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error getting documents by notary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve documents: " + e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyDocument(
            @RequestParam String documentHash,
            @RequestParam String fileContent) {
        try {
            log.info("Verifying document integrity: {}", documentHash);
            boolean isValid = documentService.verifyDocumentIntegrity(documentHash, fileContent);
            Map<String, Object> response = new HashMap<>();
            response.put("documentHash", documentHash);
            response.put("isValid", isValid);
            response.put("message", isValid ? "Document integrity verified" : "Document integrity check failed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error verifying document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to verify document: " + e.getMessage()));
        }
    }

    @PostMapping("/verify/{documentHash}")
    public ResponseEntity<?> verifyDocumentWithIpfs(@PathVariable String documentHash) {
        try {
            log.info("Verifying document with IPFS: {}", documentHash);
            boolean isValid = documentService.verifyDocumentWithIpfs(documentHash);
            Map<String, Object> response = new HashMap<>();
            response.put("documentHash", documentHash);
            response.put("isValid", isValid);
            response.put("message", isValid ? "Document verified with IPFS and blockchain" : "Document verification failed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error verifying document with IPFS", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to verify document: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-manual/{documentHash}")
    public ResponseEntity<?> manuallyVerifyDocument(@PathVariable String documentHash) {
        try {
            log.info("Manually verifying document: {}", documentHash);
            boolean isValid = documentService.manuallyVerifyDocument(documentHash);
            Map<String, Object> response = new HashMap<>();
            response.put("documentHash", documentHash);
            response.put("isValid", isValid);
            response.put("message", isValid ? "Document verification successful" : "Document verification failed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in manual document verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to verify document: " + e.getMessage()));
        }
    }

    @GetMapping("/needing-verification")
    public ResponseEntity<?> getDocumentsNeedingVerification() {
        try {
            log.debug("Fetching documents needing verification");
            List<DocumentDTO> documents = documentService.getDocumentsNeedingVerification();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error getting documents needing verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve documents: " + e.getMessage()));
        }
    }

    // NEW: Get document verification status
    @GetMapping("/{documentHash}/status")
    public ResponseEntity<?> getDocumentVerificationStatus(@PathVariable String documentHash) {
        try {
            log.debug("Fetching verification status for document: {}", documentHash);
            DocumentService.VerificationStatus status = documentService.getDocumentVerificationStatus(documentHash);
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            log.error("Document not found: {}", documentHash);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Document not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting document verification status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve status: " + e.getMessage()));
        }
    }

    // NEW: Batch verification
    @PostMapping("/verify-batch")
    public ResponseEntity<?> verifyMultipleDocuments(@RequestBody List<String> documentHashes) {
        try {
            log.info("Batch verifying {} documents", documentHashes.size());
            List<DocumentService.VerificationResult> results = documentService.verifyMultipleDocuments(documentHashes);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error in batch verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to verify documents: " + e.getMessage()));
        }
    }

    // NEW: Download document from IPFS
    @GetMapping("/{documentHash}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable String documentHash) {
        try {
            log.info("Downloading document from IPFS: {}", documentHash);
            DocumentDTO document = documentService.getDocument(documentHash);
            String content = documentService.downloadFromIpfs(document.getIpfsHash());

            Map<String, Object> response = new HashMap<>();
            response.put("documentHash", documentHash);
            response.put("ipfsCid", document.getIpfsHash());
            response.put("documentName", document.getDocumentName());
            response.put("content", content);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Document not found: {}", documentHash);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Document not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error downloading document from IPFS", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to download document: " + e.getMessage()));
        }
    }

    // NEW: Check if document exists
    @GetMapping("/{documentHash}/exists")
    public ResponseEntity<?> documentExists(@PathVariable String documentHash) {
        try {
            boolean exists = documentService.documentExists(documentHash);
            Map<String, Object> response = new HashMap<>();
            response.put("documentHash", documentHash);
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking document existence", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to check document: " + e.getMessage()));
        }
    }

    // NEW: Update document metadata
    @PutMapping("/{documentHash}")
    public ResponseEntity<?> updateDocumentMetadata(
            @PathVariable String documentHash,
            @RequestParam String documentName) {
        try {
            log.info("Updating metadata for document: {}", documentHash);
            DocumentDTO document = documentService.updateDocumentMetadata(documentHash, documentName);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            log.error("Document not found: {}", documentHash);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Document not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating document metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update document: " + e.getMessage()));
        }
    }

    // NEW: Delete document (optional - use with caution)
    @DeleteMapping("/{documentHash}")
    public ResponseEntity<?> deleteDocument(@PathVariable String documentHash) {
        try {
            log.warn("Deleting document: {}", documentHash);
            documentService.deleteDocument(documentHash);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document deleted successfully");
            response.put("documentHash", documentHash);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Document not found: {}", documentHash);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Document not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete document: " + e.getMessage()));
        }
    }

    // Helper method to create consistent error responses
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return error;
    }
}