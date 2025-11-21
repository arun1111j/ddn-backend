package com.notarize.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentDTO {
    private String documentHash;
    private String ownerAddress;
    private String documentName;
    private String ipfsHash;
    private LocalDateTime timestamp;
    private boolean notarized;
    private List<String> notaryAddresses;
    private LocalDateTime createdAt;
    private LocalDateTime lastVerified;


}