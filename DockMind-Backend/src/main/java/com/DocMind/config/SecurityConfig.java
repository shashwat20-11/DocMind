//package com.DocMind.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//import com.DocMind.entity.Role;
//
//import lombok.RequiredArgsConstructor;
//
//@Configuration
//@RequiredArgsConstructor
//@EnableMethodSecurity(prePostEnabled=true)
//public class SecurityConfig {
//
//	private final JwtAuthenticationFilter jwtAuthFilter;
//	
//	@Bean
//	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
//		httpSecurity.csrf(AbstractHttpConfigurer::disable)
//        			.cors(Customizer.withDefaults())
//        			.sessionManagement(session ->
//        								session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//        			.authorizeHttpRequests(
//        						auth ->
//        								auth
//        								.requestMatchers(
//        								"/api/auth/**",
//                                        "/v3/api-docs/**",
//                                        "/swagger-ui/**",
//                                        "/swagger-ui.html").permitAll()
//        								.requestMatchers("/api/admin/**").hasRole("ADMIN")
//        							    .requestMatchers("/api/**").hasRole("USER")
//                                .anyRequest().authenticated()
//        )
//        .addFilterAfter(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
//        .exceptionHandling(exception ->
//                exception.authenticationEntryPoint((request, response, authException) ->
//                {
//                    System.out.println("Unauthorized");
//                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
//                }))
//;
//
//			return httpSecurity.build();
//	}
//	
//	@Bean
//	public PasswordEncoder passwordEncoder() {
//		return new BCryptPasswordEncoder();
//	}
//	
//	@Bean
//    public AuthenticationManager authenticationManager(
//            AuthenticationConfiguration configuration
//    ) throws Exception {
//        return configuration.getAuthenticationManager();
//    }
//}




package com.DocMind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @PostConstruct
    public void enableSecurityContextPropagation() {
        // Propagate SecurityContext to async/worker threads (SSE / Reactive streams)
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Allow ASYNC, FORWARD, and ERROR dispatches without re-authorization checks
                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

                // Public endpoints
                .requestMatchers(
                    "/api/auth/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // Role-restricted endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")

                .anyRequest().authenticated()
            )
            // Execute JWT filter BEFORE UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception ->
                exception.authenticationEntryPoint((request, response, authException) -> {
                    // Prevent writing to response if stream has already committed
                    if (!response.isCommitted()) {
                        response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
                    }
                })
            );

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
