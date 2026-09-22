package com.carecode.domain.user.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프로필 수정은 번호를 비우면 '' 를 보낸다. 예전 패턴은 이것도, 하이픈 없는 번호도 400 으로 막아
 * 번호가 없는 사용자는 이름·주소만 고치려 해도 저장할 수 없었다.
 */
@DisplayName("프로필 수정 — 휴대폰 번호")
class UserUpdateRequestDtoTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource(value = {
            "010-1234-5678|010-1234-5678",
            "01012345678|010-1234-5678",
            "0111234567|011-123-4567",
            "' 010-1234-5678 '|010-1234-5678",
            "''|''"
    }, delimiter = '|')
    void acceptsAndNormalizes(String input, String expected) throws Exception {
        UserUpdateRequestDto dto = read(input);
        assertThat(dto.getPhoneNumber()).isEqualTo(expected);
        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"02-123-4567", "010-12-5678", "abc", "0101234567890"})
    void rejectsInvalid(String input) throws Exception {
        assertThat(validator.validate(read(input))).isNotEmpty();
    }

    private UserUpdateRequestDto read(String phone) throws Exception {
        return mapper.readValue("{\"name\":\"홍길동\",\"phoneNumber\":" + mapper.writeValueAsString(phone) + "}",
                UserUpdateRequestDto.class);
    }
}
