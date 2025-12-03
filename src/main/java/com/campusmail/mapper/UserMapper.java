package com.campusmail.mapper;

import com.campusmail.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {

    void insert(User user);

    Optional<User> findByUsername(@Param("username") String username);

    Optional<User> findById(@Param("id") Long id);
}
