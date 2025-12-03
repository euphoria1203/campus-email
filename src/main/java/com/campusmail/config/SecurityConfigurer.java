package com.campusmail.config;

import com.campusmail.service.JwtService;
import com.campusmail.utils.JwtAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class SecurityConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    private final JwtService jwtService;

    public SecurityConfigurer(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void configure(HttpSecurity http) {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    }
}
