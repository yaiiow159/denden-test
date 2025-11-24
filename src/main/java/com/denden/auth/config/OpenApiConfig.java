package com.denden.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 配置類別
 * 提供 API 文件的基本資訊、安全配置和伺服器設定
 *
 * @author Timmy
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 配置 OpenAPI 文件
     *
     * @return OpenAPI 配置物件
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url(baseUrl)
                                .description("API Server")
                ))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                createBearerSecurityScheme()));
    }

    /**
     * 建立 API 基本資訊
     *
     * @return API 資訊物件
     */
    private Info apiInfo() {
        return new Info()
                .title("DenDen two factories login System API")
                .version("1.0.0")
                .description("""
                        # DenDen API 文件
                        """)
                .contact(new Contact()
                        .name("Timmy")
                        .email("examyou076@gmail.com")
                        .url("https://github.com/denden/member-auth-system"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * 建立 JWT Bearer 認證 Security Scheme
     *
     * @return SecurityScheme 物件
     */
    private SecurityScheme createBearerSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("請輸入從登入 API 取得的 JWT Token");
    }
}
