package com.carecode.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * XML 응답을 {@link JsonNode} 로 변환한다.
 *
 * <p>공공데이터포털 데이터셋 중 일부(예: 심평원 병원정보서비스)는 XML 만 반환한다.
 * {@code jackson-dataformat-xml} 을 클래스패스에 넣으면 Spring MVC 가
 * XML 메시지 컨버터를 자동 등록해 애플리케이션 응답 협상 동작까지 바뀌므로,
 * 의존성을 늘리지 않고 JDK 내장 DOM 파서로 변환한다.
 */
@Component
public class XmlResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * XML 문자열을 JsonNode 트리로 변환한다.
     *
     * @return 파싱할 수 없으면 null
     */
    public JsonNode parse(String xml) {
        if (xml == null || xml.isBlank()) {
            return null;
        }
        try {
            Document document = newSecureBuilder().parse(new InputSource(new StringReader(xml)));
            document.getDocumentElement().normalize();
            return toNode(document.getDocumentElement());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 외부 엔티티 참조(XXE)를 차단한 DocumentBuilder.
     * 외부에서 받은 XML 을 파싱하므로 기본 설정을 그대로 쓰면 안 된다.
     */
    private DocumentBuilder newSecureBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }

    private JsonNode toNode(Element element) {
        NodeList children = element.getChildNodes();

        boolean hasElementChild = false;
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                hasElementChild = true;
                break;
            }
        }

        // 자식 엘리먼트가 없으면 텍스트 값 노드
        if (!hasElementChild) {
            String text = element.getTextContent();
            return objectMapper.getNodeFactory().textNode(text != null ? text.trim() : "");
        }

        ObjectNode node = objectMapper.createObjectNode();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element childElement = (Element) child;
            String name = childElement.getNodeName();
            JsonNode childNode = toNode(childElement);

            JsonNode existing = node.get(name);
            if (existing == null) {
                node.set(name, childNode);
            } else if (existing.isArray()) {
                // 같은 이름이 반복되면 배열로 모은다 (<item>...</item><item>...</item>)
                ((ArrayNode) existing).add(childNode);
            } else {
                ArrayNode array = objectMapper.createArrayNode();
                array.add(existing);
                array.add(childNode);
                node.set(name, array);
            }
        }
        return node;
    }
}
