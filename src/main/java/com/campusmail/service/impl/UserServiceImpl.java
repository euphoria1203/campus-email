package com.campusmail.service.impl;

import com.campusmail.dto.UserLoginDTO;
import com.campusmail.dto.UserRegisterDTO;
import com.campusmail.entity.MailAccount;
import com.campusmail.entity.User;
import com.campusmail.mapper.MailAccountMapper;
import com.campusmail.mapper.UserMapper;
import com.campusmail.service.JwtService;
import com.campusmail.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final MailAccountMapper mailAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public UserServiceImpl(UserMapper userMapper,
                           MailAccountMapper mailAccountMapper,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           UserDetailsService userDetailsService) {
        this.userMapper = userMapper;
        this.mailAccountMapper = mailAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Override
    @Transactional
    public User register(UserRegisterDTO request) {
        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getUsername());  // 默认昵称为用户名
        user.setStatus(1);  // 正常状态
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        // 创建默认邮件账户
        MailAccount mailAccount = new MailAccount();
        mailAccount.setUserId(user.getId());
        mailAccount.setEmailAddress(request.getEmail());
        mailAccount.setDisplayName(request.getUsername());
        mailAccount.setSmtpHost("localhost");
        mailAccount.setSmtpPort(25);
        mailAccount.setIsDefault(true);
        mailAccountMapper.insert(mailAccount);

        return user;
    }

    @Override
    public Map<String, Object> login(UserLoginDTO request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(authentication.getName());
        String token = jwtService.generateToken(userDetails.getUsername(), Map.of("roles", userDetails.getAuthorities()));
        
        // 获取用户信息
        User user = userMapper.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("nickname", user.getNickname());
        
        return result;
    }
}
