package com.carecode.core.config;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger/OpenAPI 3 설정
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${springdoc.swagger-ui.server-url:}")
    private String customServerUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CareCode API")
                        .description("육아 지원 플랫폼 맘편한의 REST API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CareCode Team")
                                .email("dhxogns920@naver.com")
                                .url("https://carecode.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(createServerList())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요. 예: Bearer {token}")
                                .name("Authorization")
                                .in(SecurityScheme.In.HEADER))
                        .addSecuritySchemes("API Key", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API 키를 입력하세요"))
                        .addSecuritySchemes("Basic Auth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("기본 인증 정보를 입력하세요")));
    }
    
    /**
     * 환경별 서버 목록 생성
     */
    private List<Server> createServerList() {
        List<Server> servers = new ArrayList<>();
        
        // 커스텀 서버 URL이 설정된 경우 (환경변수로 주입)
        if (customServerUrl != null && !customServerUrl.isEmpty()) {
            servers.add(new Server()
                    .url(customServerUrl)
                    .description("현재 환경 서버"));
        } else {
            servers.add(new Server()
                    .url("http://localhost:" + serverPort)
                    .description("로컬 개발 서버"));
        }
        return servers;
    }
} 