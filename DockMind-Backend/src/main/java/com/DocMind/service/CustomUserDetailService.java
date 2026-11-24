package com.DocMind.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.DocMind.entity.User;
import com.DocMind.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService{

	private final UserRepository userRepository;
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		Optional<User>user=userRepository.findByUsername(username);
		
		if(user.isEmpty()) {
			throw new UsernameNotFoundException("Invalid username or password");
		}
		
		
		return new CustomUserDetails(user.orElseThrow(()->new UsernameNotFoundException("Invalid username or password")));
	}

}
