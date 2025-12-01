package com.notarize.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractNotaryInfoDTO {
    private String notaryAddress;
    private String name;
    private Boolean isActive;
    private BigInteger stakeAmount;
    private BigInteger successfulNotarizations;
    private BigInteger slashedCount;
}
