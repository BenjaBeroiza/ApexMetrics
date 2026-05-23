package com.apexmetrics.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private long expiresIn;
    private String username;
    private String role;
}
