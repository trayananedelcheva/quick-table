package com.quicktable.reservationservicev2.config;

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

@Configuration
public class SwaggerExamplesConfig {

    @Bean
    public OpenApiCustomizer schemaExamplesCustomizer() {
        return openApi -> {
            try (InputStream inputStream = getClass().getResourceAsStream("/swagger-examples.yaml")) {
                if (inputStream == null) return;
                Map<String, Object> config = new Yaml().load(inputStream);

                applySchemaExamples(openApi, config);
                applyParameterExamples(openApi, config);
            } catch (Exception e) {
                // swagger-examples.yaml е опционален — грешката не спира сървъра
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void applySchemaExamples(io.swagger.v3.oas.models.OpenAPI openApi, Map<String, Object> config) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) return;
        Map<String, Object> schemas = (Map<String, Object>) config.get("schemas");
        if (schemas == null) return;

        schemas.forEach((schemaName, example) -> {
            var schema = openApi.getComponents().getSchemas().get(schemaName);
            if (schema != null) schema.setExample(example);
        });
    }

    @SuppressWarnings("unchecked")
    private void applyParameterExamples(io.swagger.v3.oas.models.OpenAPI openApi, Map<String, Object> config) {
        if (openApi.getPaths() == null) return;
        Map<String, Object> paths = (Map<String, Object>) config.get("paths");
        if (paths == null) return;

        paths.forEach((pathKey, methodsObj) -> {
            PathItem pathItem = openApi.getPaths().get(pathKey);
            if (pathItem == null) return;

            Map<String, Map<String, Object>> methods = (Map<String, Map<String, Object>>) methodsObj;
            methods.forEach((httpMethod, paramExamples) -> {
                Operation operation = getOperation(pathItem, httpMethod);
                if (operation == null || operation.getParameters() == null) return;

                operation.getParameters().forEach(param -> {
                    Object example = paramExamples.get(param.getName());
                    if (example != null) param.setExample(example);
                });
            });
        });
    }

    private Operation getOperation(PathItem pathItem, String httpMethod) {
        return switch (httpMethod.toLowerCase()) {
            case "get" -> pathItem.getGet();
            case "post" -> pathItem.getPost();
            case "put" -> pathItem.getPut();
            case "delete" -> pathItem.getDelete();
            case "patch" -> pathItem.getPatch();
            default -> null;
        };
    }
}
