package com.DocMind.dto;


public record LoginResponse(
        String accessToken,
        UserDto user
) {
}