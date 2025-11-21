package com.notarize.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class IpfsUtil {

    @Value("${pinata.api-key}")
    private String apiKey;

    @Value("${pinata.api-secret}")
    private String apiSecret;

    private static final String PINATA_API_URL = "https://api.pinata.cloud";
    private static final String PINATA_GATEWAY = "https://gateway.pinata.cloud";

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(PINATA_API_URL)
                .defaultHeader("pinata_api_key", apiKey)
                .defaultHeader("pinata_secret_api_key", apiSecret)
                .build();

        log.info("🔧 Initializing Pinata IPFS service...");
        testAuthentication();
    }

    /**
     * Test Pinata authentication
     */

    private void testAuthentication() {
        try {
            webClient.get()
                    .uri("/data/testAuthentication")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("✅ Pinata authentication successful");
        } catch (Exception e) {
            log.error("❌ Pinata authentication failed: {}", e.getMessage());
            throw new RuntimeException("Failed to authenticate with Pinata. Check your API keys!", e);
        }
    }

    /**
     * Upload base64 encoded content to IPFS via Pinata
     */
    public String uploadBase64ToIpfs(String base64Content) {
        try {
            log.info("📤 Uploading to IPFS via Pinata...");

            // Decode base64 to bytes
            byte[] fileBytes = Base64.getDecoder().decode(base64Content);

            log.debug("File size: {} bytes", fileBytes.length);

            // Build multipart form data
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", fileBytes)
                    .filename("document_" + System.currentTimeMillis())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM);

            // Upload to Pinata
            Map<String, Object> response = webClient.post()
                    .uri("/pinning/pinFileToIPFS")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("IpfsHash")) {
                String ipfsCid = (String) response.get("IpfsHash");
                log.info("✅ File uploaded to IPFS successfully!");
                log.info("📌 IPFS CID: {}", ipfsCid);
                log.info("🔗 Pinata Gateway: {}/ipfs/{}", PINATA_GATEWAY, ipfsCid);
                log.info("🔗 Public Gateway: https://ipfs.io/ipfs/{}", ipfsCid);

                return ipfsCid;
            }

            throw new RuntimeException("Failed to get IPFS hash from Pinata response");

        } catch (Exception e) {
            log.error("❌ Failed to upload to Pinata: {}", e.getMessage());
            throw new RuntimeException("IPFS upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Upload text content to IPFS
     */
    public String uploadTextToIpfs(String textContent) {
        String base64Content = Base64.getEncoder()
                .encodeToString(textContent.getBytes(StandardCharsets.UTF_8));
        return uploadBase64ToIpfs(base64Content);
    }

    /**
     * Download content from IPFS
     * Tries Pinata gateway first, then falls back to public gateways
     */
    public String downloadFromIpfs(String ipfsCid) {
        try {
            log.info("📥 Downloading from IPFS: {}", ipfsCid);

            // Try Pinata dedicated gateway first (fastest and most reliable)
            try {
                String url = PINATA_GATEWAY + "/ipfs/" + ipfsCid;
                log.debug("Trying Pinata gateway: {}", url);

                String content = WebClient.create(url)
                        .get()
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (content != null && !content.isEmpty()) {
                    log.info("✅ Downloaded from Pinata gateway ({} bytes)", content.length());
                    return content;
                }
            } catch (Exception e) {
                log.warn("Pinata gateway failed: {}", e.getMessage());
            }

            // Fallback to public IPFS gateway
            try {
                String url = "https://ipfs.io/ipfs/" + ipfsCid;
                log.debug("Trying public IPFS gateway: {}", url);

                String content = WebClient.create(url)
                        .get()
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (content != null && !content.isEmpty()) {
                    log.info("✅ Downloaded from public IPFS gateway ({} bytes)", content.length());
                    return content;
                }
            } catch (Exception e) {
                log.warn("Public IPFS gateway failed: {}", e.getMessage());
            }

            // Fallback to Cloudflare gateway
            try {
                String url = "https://cloudflare-ipfs.com/ipfs/" + ipfsCid;
                log.debug("Trying Cloudflare gateway: {}", url);

                String content = WebClient.create(url)
                        .get()
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (content != null && !content.isEmpty()) {
                    log.info("✅ Downloaded from Cloudflare gateway ({} bytes)", content.length());
                    return content;
                }
            } catch (Exception e) {
                log.warn("Cloudflare gateway failed: {}", e.getMessage());
            }

            throw new RuntimeException("All IPFS gateways failed to retrieve content");

        } catch (Exception e) {
            log.error("❌ Failed to download from IPFS: {}", e.getMessage());
            throw new RuntimeException("IPFS download failed: " + e.getMessage(), e);
        }
    }

    /**
     * Pin existing IPFS content by hash (if it's already on IPFS)
     */
    public void pinByHash(String ipfsCid) {
        try {
            log.info("📌 Pinning existing IPFS content: {}", ipfsCid);

            Map<String, String> pinData = Map.of(
                    "hashToPin", ipfsCid,
                    "pinataMetadata", "{\"name\":\"Notarized Document\"}"
            );

            webClient.post()
                    .uri("/pinning/pinByHash")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(pinData)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("✅ Content pinned successfully");

        } catch (Exception e) {
            log.error("❌ Failed to pin by hash: {}", e.getMessage());
            throw new RuntimeException("IPFS pinning failed", e);
        }
    }

    /**
     * Unpin content from Pinata (to manage storage)
     */
    public void unpinContent(String ipfsCid) {
        try {
            log.info("📍 Unpinning content: {}", ipfsCid);

            webClient.delete()
                    .uri("/pinning/unpin/" + ipfsCid)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("✅ Content unpinned successfully");

        } catch (Exception e) {
            log.warn("Failed to unpin content: {}", e.getMessage());
        }
    }

    /**
     * List all pinned files (useful for debugging)
     */
    public void listPinnedFiles() {
        try {
            log.info("📋 Fetching pinned files from Pinata...");

            Map<String, Object> response = webClient.get()
                    .uri("/data/pinList?status=pinned&pageLimit=10")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("rows")) {
                int count = ((java.util.List<?>) response.get("rows")).size();
                log.info("✅ Found {} pinned files", count);
                log.debug("Pinned files: {}", response);
            }
        } catch (Exception e) {
            log.error("Failed to list pinned files: {}", e.getMessage());
        }
    }

    /**
     * Get Pinata account usage info
     */
    public void getAccountInfo() {
        try {
            log.info("📊 Fetching Pinata account info...");

            Map<String, Object> response = webClient.get()
                    .uri("/data/userPinnedDataTotal")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                log.info("Account info: {}", response);
            }
        } catch (Exception e) {
            log.error("Failed to get account info: {}", e.getMessage());
        }
    }

    /**
     * Check if a file is pinned
     */
    public boolean isPinned(String ipfsCid) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/data/pinList?hashContains=" + ipfsCid)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("rows")) {
                java.util.List<?> rows = (java.util.List<?>) response.get("rows");
                return !rows.isEmpty();
            }

            return false;
        } catch (Exception e) {
            log.warn("Failed to check pin status for {}: {}", ipfsCid, e.getMessage());
            return false;
        }
    }

    /**
     * Get the Pinata gateway URL for a CID
     */
    public String getGatewayUrl(String ipfsCid) {
        return PINATA_GATEWAY + "/ipfs/" + ipfsCid;
    }

    /**
     * Get public IPFS URL for a CID
     */
    public String getPublicIpfsUrl(String ipfsCid) {
        return "https://ipfs.io/ipfs/" + ipfsCid;
    }
}