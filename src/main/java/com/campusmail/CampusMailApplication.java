package com.campusmail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CampusMailApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusMailApplication.class, args);
        System.out.println("项目启动成功！");
    }
}
