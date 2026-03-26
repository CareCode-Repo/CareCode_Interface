package com.carecode.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * API 문서 생성기
 * 
 * 기능:
 * 1. Swagger JSON에서 상세한 API 문서 생성
 * 2. 파라미터, 요청/응답 예제, 데이터 모델 정보 포함
 * 3. 태그별 그룹화 및 구조화된 문서 생성
 */
@Component
public class ApiDocumentationGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();


    // 메인 메서드 - 독립 실행용

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("사용법: java ApiDocumentationGenerator <swagger-url> <output-path>");
            System.exit(1);
        }
        
        String swaggerUrl = args[0];
        String outputPath = args[1];
        
        try {
            System.out.println("🚀 API 문서 생성을 시작합니다...");
            System.out.println("📥 Swagger JSON 다운로드: " + swaggerUrl);
            
            ApiDocumentationGenerator generator = new ApiDocumentationGenerator();
            generator.generateDetailedDocumentation(swaggerUrl, outputPath);
            
            System.out.println("✅ API 문서 생성 완료: " + outputPath);
        } catch (Exception e) {
            System.err.println("❌ API 문서 생성 실패: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }


    // 상세한 API 문서 생성

    public void generateDetailedDocumentation(String swaggerUrl, String outputPath) throws IOException {
        // Swagger JSON 다운로드
        JsonNode swaggerJson = objectMapper.readTree(new URL(swaggerUrl));
        
        // 상세 AsciiDoc 생성
        String asciiDoc = generateDetailedAsciiDoc(swaggerJson);
        
        // 파일 저장
        Files.write(Paths.get(outputPath), asciiDoc.getBytes());
    }


    // Swagger JSON을 파싱하여 상세한 AsciiDoc 생성

    private String generateDetailedAsciiDoc(JsonNode swaggerJson) {
        StringBuilder asciiDoc = new StringBuilder();
        
        // 헤더
        asciiDoc.append("= CareCode API 문서\n");
        asciiDoc.append(":toc: left\n");
        asciiDoc.append(":toclevels: 3\n");
        asciiDoc.append(":sectnums:\n");
        asciiDoc.append(":source-highlighter: highlightjs\n");
        asciiDoc.append(":icons: font\n\n");
        
        // 기본 정보
        asciiDoc.append("== 개요\n\n");
        asciiDoc.append("이 문서는 실제 Swagger JSON에서 동적으로 생성되었습니다.\n");
        asciiDoc.append("모든 API 엔드포인트, 파라미터, 요청/응답 예제가 포함되어 있습니다.\n\n");
        
        // API 정보
        generateApiInfo(asciiDoc, swaggerJson);
        
        // 인증 정보
        generateAuthenticationInfo(asciiDoc);
        
        // API 엔드포인트
        generateApiEndpoints(asciiDoc, swaggerJson);
        
        // 데이터 모델
        generateDataModels(asciiDoc, swaggerJson);
        
        // 응답 코드
        generateResponseCodes(asciiDoc);
        
        return asciiDoc.toString();
    }


    // API 정보 생성

    private void generateApiInfo(StringBuilder asciiDoc, JsonNode swaggerJson) {
        asciiDoc.append("=== API 정보\n\n");
        
        JsonNode info = swaggerJson.get("info");
        if (info != null) {
            if (info.has("title")) {
                asciiDoc.append("* **제목**: ").append(info.get("title").asText()).append("\n");
            }
            if (info.has("version")) {
                asciiDoc.append("* **버전**: ").append(info.get("version").asText()).append("\n");
            }
            if (info.has("description")) {
                asciiDoc.append("* **설명**: ").append(info.get("description").asText()).append("\n");
            }
        }
        asciiDoc.append("* **Base URL**: `http://13.209.36.209:8081`\n");
        asciiDoc.append("* **인증**: JWT Bearer Token\n\n");
    }


    // 인증 정보 생성

    private void generateAuthenticationInfo(StringBuilder asciiDoc) {
        asciiDoc.append("== 인증\n\n");
        asciiDoc.append("=== JWT 토큰\n\n");
        asciiDoc.append("API 요청 시 Authorization 헤더에 JWT 토큰을 포함해야 합니다:\n\n");
        asciiDoc.append("[source,http]\n");
        asciiDoc.append("----\n");
        asciiDoc.append("Authorization: Bearer <your-jwt-token>\n");
        asciiDoc.append("----\n\n");
    }


    // API 엔드포인트 생성

    private void generateApiEndpoints(StringBuilder asciiDoc, JsonNode swaggerJson) {
        asciiDoc.append("== API 엔드포인트\n\n");
        
        JsonNode paths = swaggerJson.get("paths");
        if (paths != null) {
            // 태그별로 그룹화
            Map<String, List<Map.Entry<String, JsonNode>>> groupedEndpoints = groupEndpointsByTag(paths);
            
            for (Map.Entry<String, List<Map.Entry<String, JsonNode>>> group : groupedEndpoints.entrySet()) {
                String tag = group.getKey();
                List<Map.Entry<String, JsonNode>> endpoints = group.getValue();
                
                asciiDoc.append("=== ").append(tag).append("\n\n");
                
                for (Map.Entry<String, JsonNode> endpoint : endpoints) {
                    String path = endpoint.getKey();
                    JsonNode pathNode = endpoint.getValue();
                    
                    // HTTP 메서드별로 처리
                    for (String method : Arrays.asList("get", "post", "put", "delete", "patch")) {
                        if (pathNode.has(method)) {
                            JsonNode methodNode = pathNode.get(method);
                            generateDetailedEndpointDocumentation(asciiDoc, path, method.toUpperCase(), methodNode);
                        }
                    }
                }
                asciiDoc.append("\n");
            }
        }
    }


    // 데이터 모델 생성

    private void generateDataModels(StringBuilder asciiDoc, JsonNode swaggerJson) {
        asciiDoc.append("== 데이터 모델\n\n");
        
        JsonNode components = swaggerJson.get("components");
        if (components != null && components.has("schemas")) {
            JsonNode schemas = components.get("schemas");
            Iterator<Map.Entry<String, JsonNode>> schemaIterator = schemas.fields();
            
            while (schemaIterator.hasNext()) {
                Map.Entry<String, JsonNode> schema = schemaIterator.next();
                String schemaName = schema.getKey();
                JsonNode schemaNode = schema.getValue();
                
                generateDetailedSchemaDocumentation(asciiDoc, schemaName, schemaNode);
            }
        }
    }


    // 응답 코드 생성

    private void generateResponseCodes(StringBuilder asciiDoc) {
        asciiDoc.append("== 응답 코드\n\n");
        asciiDoc.append("|코드|설명|\n");
        asciiDoc.append("|---|---|\n");
        asciiDoc.append("|200|성공|\n");
        asciiDoc.append("|201|생성됨|\n");
        asciiDoc.append("|400|잘못된 요청|\n");
        asciiDoc.append("|401|인증 실패|\n");
        asciiDoc.append("|403|권한 없음|\n");
        asciiDoc.append("|404|찾을 수 없음|\n");
        asciiDoc.append("|500|서버 오류|\n\n");
    }


    // 엔드포인트를 태그별로 그룹화

    private Map<String, List<Map.Entry<String, JsonNode>>> groupEndpointsByTag(JsonNode paths) {
        Map<String, List<Map.Entry<String, JsonNode>>> grouped = new LinkedHashMap<>();
        
        Iterator<Map.Entry<String, JsonNode>> pathIterator = paths.fields();
        while (pathIterator.hasNext()) {
            Map.Entry<String, JsonNode> path = pathIterator.next();
            String pathKey = path.getKey();
            JsonNode pathNode = path.getValue();
            
            // 각 HTTP 메서드에서 태그 추출
            for (String method : Arrays.asList("get", "post", "put", "delete", "patch")) {
                if (pathNode.has(method)) {
                    JsonNode methodNode = pathNode.get(method);
                    if (methodNode.has("tags") && methodNode.get("tags").isArray()) {
                        JsonNode tags = methodNode.get("tags");
                        for (JsonNode tag : tags) {
                            String tagName = tag.asText();
                            grouped.computeIfAbsent(tagName, k -> new ArrayList<>())
                                    .add(new AbstractMap.SimpleEntry<>(pathKey, pathNode));
                        }
                    }
                }
            }
        }
        
        return grouped;
    }


    // 상세한 엔드포인트 문서 생성

    private void generateDetailedEndpointDocumentation(StringBuilder asciiDoc, String path, String method, JsonNode methodNode) {
        // 요약
        if (methodNode.has("summary")) {
            asciiDoc.append("==== ").append(methodNode.get("summary").asText()).append("\n");
        }
        
        // 설명
        if (methodNode.has("description")) {
            asciiDoc.append(methodNode.get("description").asText()).append("\n\n");
        }
        
        // HTTP 요청 예제
        generateHttpRequestExample(asciiDoc, path, method, methodNode);
        
        // 파라미터 정보
        generateParameterInfo(asciiDoc, methodNode);
        
        // 응답 정보
        generateResponseInfo(asciiDoc, methodNode);
    }


    // HTTP 요청 예제 생성

    private void generateHttpRequestExample(StringBuilder asciiDoc, String path, String method, JsonNode methodNode) {
        asciiDoc.append("[source,http]\n");
        asciiDoc.append("----\n");
        asciiDoc.append(method).append(" ").append(path).append("\n");
        
        // 헤더 추가
        if (methodNode.has("security") && methodNode.get("security").isArray()) {
            asciiDoc.append("Authorization: Bearer <token>\n");
        }
        
        // Content-Type 추가
        if (methodNode.has("requestBody")) {
            asciiDoc.append("Content-Type: application/json\n\n");
            generateRequestBodyExample(asciiDoc, methodNode.get("requestBody"));
        }
        
        asciiDoc.append("----\n\n");
    }


    // 파라미터 정보 생성

    private void generateParameterInfo(StringBuilder asciiDoc, JsonNode methodNode) {
        if (methodNode.has("parameters") && methodNode.get("parameters").isArray()) {
            asciiDoc.append("**파라미터:**\n\n");
            JsonNode parameters = methodNode.get("parameters");
            for (JsonNode param : parameters) {
                String name = param.get("name").asText();
                String in = param.get("in").asText();
                String description = param.has("description") ? param.get("description").asText() : "";
                boolean required = param.has("required") ? param.get("required").asBoolean() : false;
                
                asciiDoc.append("* `").append(name).append("` (").append(in).append(")");
                if (required) {
                    asciiDoc.append(" **필수**");
                }
                asciiDoc.append(" - ").append(description).append("\n");
                
                // 파라미터 스키마 정보
                if (param.has("schema")) {
                    JsonNode schema = param.get("schema");
                    if (schema.has("type")) {
                        asciiDoc.append("  * 타입: `").append(schema.get("type").asText()).append("`\n");
                    }
                    if (schema.has("format")) {
                        asciiDoc.append("  * 형식: `").append(schema.get("format").asText()).append("`\n");
                    }
                    if (schema.has("example")) {
                        asciiDoc.append("  * 예제: `").append(schema.get("example").asText()).append("`\n");
                    }
                }
            }
            asciiDoc.append("\n");
        }
    }


    // 응답 정보 생성

    private void generateResponseInfo(StringBuilder asciiDoc, JsonNode methodNode) {
        if (methodNode.has("responses")) {
            asciiDoc.append("**응답:**\n\n");
            JsonNode responses = methodNode.get("responses");
            Iterator<Map.Entry<String, JsonNode>> responseIterator = responses.fields();
            
            while (responseIterator.hasNext()) {
                Map.Entry<String, JsonNode> response = responseIterator.next();
                String statusCode = response.getKey();
                JsonNode responseNode = response.getValue();
                
                asciiDoc.append("* `").append(statusCode).append("`");
                if (responseNode.has("description")) {
                    asciiDoc.append(" - ").append(responseNode.get("description").asText());
                }
                asciiDoc.append("\n");
                
                // 응답 스키마 정보
                if (responseNode.has("content") && responseNode.get("content").has("application/json")) {
                    JsonNode content = responseNode.get("content").get("application/json");
                    if (content.has("schema")) {
                        JsonNode schema = content.get("schema");
                        if (schema.has("$ref")) {
                            String ref = schema.get("$ref").asText();
                            String schemaName = ref.substring(ref.lastIndexOf("/") + 1);
                            asciiDoc.append("  * 응답 타입: `").append(schemaName).append("`\n");
                        }
                    }
                }
            }
            asciiDoc.append("\n");
        }
    }


    // 요청 본문 예제 생성

    private void generateRequestBodyExample(StringBuilder asciiDoc, JsonNode requestBody) {
        if (requestBody.has("content") && requestBody.get("content").has("application/json")) {
            JsonNode schema = requestBody.get("content").get("application/json").get("schema");
            if (schema != null) {
                asciiDoc.append("{\n");
                if (schema.has("properties")) {
                    JsonNode properties = schema.get("properties");
                    Iterator<Map.Entry<String, JsonNode>> propIterator = properties.fields();
                    while (propIterator.hasNext()) {
                        Map.Entry<String, JsonNode> prop = propIterator.next();
                        String propName = prop.getKey();
                        JsonNode propNode = prop.getValue();
                        
                        asciiDoc.append("  \"").append(propName).append("\": ");
                        
                        if (propNode.has("type")) {
                            String type = propNode.get("type").asText();
                            switch (type) {
                                case "string":
                                    if (propNode.has("example")) {
                                        asciiDoc.append("\"").append(propNode.get("example").asText()).append("\"");
                                    } else {
                                        asciiDoc.append("\"example\"");
                                    }
                                    break;
                                case "integer":
                                    if (propNode.has("example")) {
                                        asciiDoc.append(propNode.get("example").asText());
                                    } else {
                                        asciiDoc.append("0");
                                    }
                                    break;
                                case "boolean":
                                    asciiDoc.append("true");
                                    break;
                                case "array":
                                    asciiDoc.append("[]");
                                    break;
                                default:
                                    asciiDoc.append("\"example\"");
                            }
                        } else {
                            asciiDoc.append("\"example\"");
                        }
                        
                        if (propIterator.hasNext()) {
                            asciiDoc.append(",");
                        }
                        asciiDoc.append("\n");
                    }
                }
                asciiDoc.append("}\n");
            }
        }
    }


    // 상세한 스키마 문서 생성

    private void generateDetailedSchemaDocumentation(StringBuilder asciiDoc, String schemaName, JsonNode schemaNode) {
        asciiDoc.append("=== ").append(schemaName).append("\n\n");
        
        if (schemaNode.has("description")) {
            asciiDoc.append(schemaNode.get("description").asText()).append("\n\n");
        }
        
        if (schemaNode.has("properties")) {
            asciiDoc.append("**속성:**\n\n");
            JsonNode properties = schemaNode.get("properties");
            Iterator<Map.Entry<String, JsonNode>> propIterator = properties.fields();
            
            while (propIterator.hasNext()) {
                Map.Entry<String, JsonNode> prop = propIterator.next();
                String propName = prop.getKey();
                JsonNode propNode = prop.getValue();
                
                asciiDoc.append("* `").append(propName).append("`");
                
                if (propNode.has("type")) {
                    asciiDoc.append(" (").append(propNode.get("type").asText()).append(")");
                }
                
                if (propNode.has("description")) {
                    asciiDoc.append(" - ").append(propNode.get("description").asText());
                }
                
                if (propNode.has("required") && propNode.get("required").asBoolean()) {
                    asciiDoc.append(" **필수**");
                }
                
                asciiDoc.append("\n");
                
                // 추가 속성 정보
                if (propNode.has("format")) {
                    asciiDoc.append("  * 형식: `").append(propNode.get("format").asText()).append("`\n");
                }
                if (propNode.has("example")) {
                    asciiDoc.append("  * 예제: `").append(propNode.get("example").asText()).append("`\n");
                }
                if (propNode.has("enum")) {
                    asciiDoc.append("  * 가능한 값: ");
                    JsonNode enumValues = propNode.get("enum");
                    for (int i = 0; i < enumValues.size(); i++) {
                        if (i > 0) asciiDoc.append(", ");
                        asciiDoc.append("`").append(enumValues.get(i).asText()).append("`");
                    }
                    asciiDoc.append("\n");
                }
            }
            
            asciiDoc.append("\n");
            
            // JSON 예제
            generateSchemaJsonExample(asciiDoc, properties);
        }
    }


    // 스키마 JSON 예제 생성

    private void generateSchemaJsonExample(StringBuilder asciiDoc, JsonNode properties) {
        asciiDoc.append("[source,json]\n");
        asciiDoc.append("----\n");
        asciiDoc.append("{\n");
        
        Iterator<Map.Entry<String, JsonNode>> propIterator = properties.fields();
        while (propIterator.hasNext()) {
            Map.Entry<String, JsonNode> prop = propIterator.next();
            String propName = prop.getKey();
            JsonNode propNode = prop.getValue();
            
            asciiDoc.append("  \"").append(propName).append("\": ");
            
            if (propNode.has("type")) {
                String type = propNode.get("type").asText();
                switch (type) {
                    case "string":
                        if (propNode.has("example")) {
                            asciiDoc.append("\"").append(propNode.get("example").asText()).append("\"");
                        } else {
                            asciiDoc.append("\"string\"");
                        }
                        break;
                    case "integer":
                        if (propNode.has("example")) {
                            asciiDoc.append(propNode.get("example").asText());
                        } else {
                            asciiDoc.append("0");
                        }
                        break;
                    case "boolean":
                        asciiDoc.append("true");
                        break;
                    case "array":
                        asciiDoc.append("[]");
                        break;
                    default:
                        asciiDoc.append("\"value\"");
                }
            } else {
                asciiDoc.append("\"value\"");
            }
            
            if (propIterator.hasNext()) {
                asciiDoc.append(",");
            }
            asciiDoc.append("\n");
        }
        
        asciiDoc.append("}\n");
        asciiDoc.append("----\n\n");
    }
} 