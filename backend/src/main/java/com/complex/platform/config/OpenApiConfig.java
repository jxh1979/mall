package com.complex.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI complexOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("综合体管理系统 API")
                        .version("0.1.0")
                        .description("Java 21 + Spring Boot 4.x 正式后端 API 文档"))
                .addServersItem(new Server()
                        .url("/")
                        .description("current deployment"));
    }
}
