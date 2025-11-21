package com.notarize.dto;

import lombok.Data;

@Data
public class NotarizationRequestDTO {
    // Add these getter methods
    private String fileContent;
    private String documentName;
    private String notaryAddress;

}