package com.campusmail.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import com.campusmail.utils.SnowflakeIdGenerator;

@Configuration
@MapperScan("com.campusmail.mapper")
public class MyBatisConfig {

	@Bean
	public SnowflakeIdGenerator snowflakeIdGenerator() {
		// WorkerId 可从配置/环境变量获取，这里使用 1 作为默认
		return new SnowflakeIdGenerator(1);
	}
}
