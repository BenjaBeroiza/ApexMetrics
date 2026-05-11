package com.apexmetrics.auth.service;

import com.apexmetrics.auth.dto.AuthResponseDTO;
import com.apexmetrics.auth.dto.LoginRequestDTO;
import com.apexmetrics.auth.dto.RegisterRequestDTO;

public interface IAuthService {
    AuthResponseDTO register(RegisterRequestDTO dto);
    AuthResponseDTO authenticate(LoginRequestDTO dto);
}
