package com.DocMind.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.DocMind.repository.UserRepository;
import com.DocMind.service.CustomUserDetailService;
import com.DocMind.service.CustomUserDetails;
import com.DocMind.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter{

	private final JwtService jwtService;
	private final CustomUserDetailService customUserDetailsService;
	private final UserRepository userRepo;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		//Logic to implement validations
		
		String authHeader=request.getHeader("Authorization");
		if(authHeader==null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		   //setting the context
		String token=authHeader.substring(7);
		String userName;
		try {
			userName=jwtService.extractUsername(token);
			if(userName==null || !jwtService.isTokenValid(token, customUserDetailsService.loadUserByUsername(userName))) {
				filterChain.doFilter(request, response);
				return;
			}
			var user=userRepo.findByUsername(userName).orElseThrow(()->new RuntimeException("User not found"));
			var authentication=new UsernamePasswordAuthenticationToken(user,null,new CustomUserDetails(user).getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			
			SecurityContextHolder.getContext().setAuthentication(authentication);
			
		}catch(Exception e) {
			filterChain.doFilter(request, response);
			e.printStackTrace();
			return;
		}
		filterChain.doFilter(request, response);
	}

}
