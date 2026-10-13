package tech.vaibhavlachhwani.magnus.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String username;
    private String[] roles;
}
