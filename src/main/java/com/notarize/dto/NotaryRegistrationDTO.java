package com.notarize.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class NotaryRegistrationDTO {
    private String notaryAddress;
    private String name;
    private BigDecimal stakeAmount;
}