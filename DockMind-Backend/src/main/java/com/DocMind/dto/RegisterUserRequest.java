package com.DocMind.dto;

public record RegisterUserRequest(
        String username,
        String email,
        String password
) {
}