package com.DocMind.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.DocMind.dto.RegisterUserRequest;
import com.DocMind.dto.UserDto;
import com.DocMind.entity.Role;
import com.DocMind.entity.User;
import com.DocMind.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	public UserDto registerUser(@Valid RegisterUserRequest registerUserRequest) {
		
		 User user = new User();
	        user.setUsername(registerUserRequest.username());
	        user.setEmail(registerUserRequest.email());
	        user.setPassword(passwordEncoder.encode(registerUserRequest.password()));
	        user.setRole(Role.USER);
	        User savedUser = userRepository.save(user);
	        return new UserDto(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole());
	}

}
