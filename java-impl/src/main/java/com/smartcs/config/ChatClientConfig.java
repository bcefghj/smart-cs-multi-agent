package com.smartcs.config;

import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient配置。
 * Spring Boot自动配置会根据application.yml创建ChatClient.Builder Bean。
 */
@Configuration
public class ChatClientConfig {
    // Spring AI auto-configuration handles ChatClient.Builder creation
}
