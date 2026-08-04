package com.carecode.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XML 응답 파서 검증.
 *
 * <p>심평원 병원정보서비스처럼 XML 만 반환하는 데이터셋을 처리하기 위한 것으로,
 * 외부에서 받은 XML 을 다루므로 XXE 차단이 특히 중요하다.
 */
@DisplayName("XmlResponseParser")
class XmlResponseParserTest {

    private final XmlResponseParser parser = new XmlResponseParser();

    @Test
    @DisplayName("반복되는 엘리먼트를 배열로 모은다")
    void collectsRepeatedElementsAsArray() {
        String xml = """
                <response><body><items>
                  <item><ykiho>A1</ykiho><yadmNm>행복소아과</yadmNm></item>
                  <item><ykiho>A2</ykiho><yadmNm>사랑소아과</yadmNm></item>
                </items></body></response>
                """;

        JsonNode root = parser.parse(xml);
        JsonNode items = root.path("body").path("items").path("item");

        assertThat(items.isArray()).isTrue();
        assertThat(items).hasSize(2);
        assertThat(items.get(0).path("yadmNm").asText()).isEqualTo("행복소아과");
        assertThat(items.get(1).path("ykiho").asText()).isEqualTo("A2");
    }

    @Test
    @DisplayName("항목이 하나면 객체로 반환된다")
    void singleItemBecomesObject() {
        String xml = """
                <response><body><items>
                  <item><ykiho>A1</ykiho><yadmNm>행복소아과</yadmNm></item>
                </items></body></response>
                """;

        JsonNode item = parser.parse(xml).path("body").path("items").path("item");

        assertThat(item.isObject()).isTrue();
        assertThat(item.path("yadmNm").asText()).isEqualTo("행복소아과");
    }

    @Test
    @DisplayName("좌표와 전화번호 등 값 노드를 문자열로 읽는다")
    void readsLeafValues() {
        String xml = "<item><XPos>127.05</XPos><YPos>37.51</YPos><telno>02-123-4567</telno></item>";

        JsonNode node = parser.parse(xml);

        assertThat(node.path("XPos").asText()).isEqualTo("127.05");
        assertThat(node.path("YPos").asText()).isEqualTo("37.51");
        assertThat(node.path("telno").asText()).isEqualTo("02-123-4567");
    }

    @Test
    @DisplayName("XXE 공격 페이로드는 파싱하지 않는다")
    void rejectsXxePayload() {
        String malicious = """
                <?xml version="1.0"?>
                <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
                <item><name>&xxe;</name></item>
                """;

        // DOCTYPE 선언 자체를 막으므로 파싱이 실패하고 null 이 된다.
        assertThat(parser.parse(malicious)).isNull();
    }

    @Test
    @DisplayName("빈 입력과 잘못된 XML은 null을 반환한다")
    void returnsNullForInvalidInput() {
        assertThat(parser.parse(null)).isNull();
        assertThat(parser.parse("")).isNull();
        assertThat(parser.parse("<broken>")).isNull();
    }
}
