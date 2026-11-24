package com.DocMind.dto;

public record LoginRequest(
        String username,
        String password
) {
}