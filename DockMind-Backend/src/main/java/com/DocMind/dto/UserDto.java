package com.DocMind.dto;

import com.DocMind.entity.Role;

public record UserDto(
        Long id,
        String username,
        String email,
        Role role
) {
}