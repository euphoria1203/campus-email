package com.campusmail.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.campusmail.mapper")
public class MyBatisConfig {
}
