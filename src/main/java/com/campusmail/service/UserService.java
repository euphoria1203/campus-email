package com.campusmail.service;

import com.campusmail.dto.UserLoginDTO;
import com.campusmail.dto.UserRegisterDTO;
import com.campusmail.entity.User;

import java.util.Map;

public interface UserService {
    User register(UserRegisterDTO request);

    Map<String, Object> login(UserLoginDTO request);
}
