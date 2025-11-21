package com.notarize.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents")
@Data
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_hash", unique = true, nullable = false)
    private String documentHash;

    @Column(name = "owner_address", nullable = false)
    private String ownerAddress;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "ipfs_hash")
    private String ipfsHash;

    private LocalDateTime timestamp;

    // ✅ MUST match repository query - use "notarized" (not "isNotarized")
    private boolean notarized = false;

    @ElementCollection
    @CollectionTable(name = "document_notaries", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "notary_address")
    private List<String> notaryAddresses = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_verified")
    private LocalDateTime lastVerified;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        timestamp = LocalDateTime.now();
        lastVerified = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastVerified = LocalDateTime.now();
    }
}