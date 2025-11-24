package com.denden.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * DenDen System - Main Application
 * 
 * 會員認證系統主應用程式
 *  * 
 * @author Timmy
 * @version 1.0.0
 */
@SpringBootApplication
public class MemberAuthSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberAuthSystemApplication.class, args);
    }
}
