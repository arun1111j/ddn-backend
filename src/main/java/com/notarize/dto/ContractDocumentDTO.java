package com.notarize.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractDocumentDTO {
    private String ipfsCid;
    private String owner;
    private BigInteger timestamp;
    private String documentName;
    private Boolean isNotarized;
    private List<String> notaries;
}
