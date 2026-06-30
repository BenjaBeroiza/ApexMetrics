package com.apexmetrics.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileDTO {

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 100, message = "El país no puede superar 100 caracteres")
    private String country;
}
