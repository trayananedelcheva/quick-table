package com.quicktable.restaurantservice.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(name = "Client", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT token за потребител с роля CLIENT")
@SecurityScheme(name = "Restaurant Admin", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT token за потребител с роля RESTAURANT_ADMIN")
@SecurityScheme(name = "System Admin", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT token за потребител с роля SYSTEM_ADMIN")
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Restaurant Service API")
                        .description("API за управление на ресторанти и маси в Quick Table")
                        .version("1.0.0"));
    }
}
