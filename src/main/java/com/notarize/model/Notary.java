package com.notarize.model;

import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "notaries")
public class Notary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String notaryAddress;

    @Column(nullable = false)
    private String name;

    // CHANGED: Renamed from isActive to active
    private boolean active;

    private BigDecimal stakeAmount;

    private int successfulNotarizations;

    private int slashedCount;

    @Column(nullable = false)
    private java.time.LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = java.time.LocalDateTime.now();
    }
}