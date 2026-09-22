package com.carecode.core.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("파일 시그니처")
class FileSignatureValidatorTest {

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void recognizesAllowedFormats() {
        assertThat(FileSignatureValidator.matches("jpg", bytes(0xFF, 0xD8, 0xFF, 0xE0))).isTrue();
        assertThat(FileSignatureValidator.matches("JPEG", bytes(0xFF, 0xD8, 0xFF, 0xE1))).isTrue();
        assertThat(FileSignatureValidator.matches("png", bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A))).isTrue();
        assertThat(FileSignatureValidator.matches("gif", ascii("GIF89a...."))).isTrue();
        assertThat(FileSignatureValidator.matches("webp", ascii("RIFF\0\0\0\0WEBPVP8 "))).isTrue();
        assertThat(FileSignatureValidator.matches("heic", ascii("\0\0\0\u0018ftypheic\0\0\0\0"))).isTrue();
        assertThat(FileSignatureValidator.matches("pdf", ascii("%PDF-1.7\n"))).isTrue();
    }

    @Test
    @DisplayName("다른 형식이거나, 짧거나, 모르는 확장자면 거절한다")
    void rejectsMismatches() {
        assertThat(FileSignatureValidator.matches("png", ascii("<svg xmlns="))).isFalse();
        assertThat(FileSignatureValidator.matches("pdf", bytes(0xFF, 0xD8, 0xFF))).isFalse();
        assertThat(FileSignatureValidator.matches("webp", ascii("RIFF\0\0\0\0WAVE"))).isFalse();
        assertThat(FileSignatureValidator.matches("heic", ascii("\0\0\0\u0018ftypisom\0\0\0\0"))).isFalse();
        assertThat(FileSignatureValidator.matches("png", bytes(0x89, 'P'))).isFalse();
        assertThat(FileSignatureValidator.matches("exe", ascii("MZ\u0090\0"))).isFalse();
        assertThat(FileSignatureValidator.matches(null, bytes(1))).isFalse();
    }
}
