package com.example.springboot.controller.dto;
import lombok.Data;
@Data
public class PasswordChangeDTO {
    private Integer uid;
    @lombok.ToString.Exclude private String oldPassword;
    @lombok.ToString.Exclude private String newPassword;
}
